/*
 * Copyright © 2019 Birju Vachhani (https://github.com/BirjuVachhani)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.birjuvachhani.locus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes


/*
 * Created by Birju Vachhani on 17 July 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Activity that handles permission model as well as location settings resolution process
 * @property config Current configuration to be used for the library
 * @property pref SharedPreferences instance to managed permission model
 * @property permissions Permissions that needs to be requested based on the [config]
 */
class LocusActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_LOCATION_SETTINGS = 545
        private const val SETTINGS_ACTIVITY_REQUEST_CODE = 659
        private const val PREF_NAME = "locus_pref"
    }

    private var config: Configuration = Configuration()
    private val pref: SharedPreferences by lazy {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private var permissions: Array<String> = arrayOf()

    private lateinit var locationPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var allPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        logDebug("Has config: ${intent?.hasExtra(Constants.INTENT_EXTRA_CONFIGURATION)}")
        intent?.getParcelableExtra<Configuration>(Constants.INTENT_EXTRA_CONFIGURATION)?.let {
            logDebug("Received location config.")
            config = it
        } ?: logError("No config is sent to the permission activity")
        val isSingleUpdate =
            intent?.getBooleanExtra(Constants.INTENT_EXTRA_IS_SINGLE_UPDATE, false) ?: false
        permissions =
            if (config.enableBackgroundUpdates && !isSingleUpdate) locationPermissions + backgroundPermission else locationPermissions
        initializePermissionLaunchers()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            initSplitPermissionModel()
            initPermissionModel()
        } else {
            initPermissionModel()
        }
    }

    private fun initializePermissionLaunchers() {
        allPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result: Map<String, Boolean> ->
            when {
                result.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    logDebug("User interaction was cancelled.")
                }
                result.values.all { it } -> {
                    // All Permissions are granted.
                    logDebug("All permissions are granted")
                    onPermissionGranted()
                }
                else -> {
                    logDebug("Some permissions are denied.")
                    onPermissionDenied()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            locationPermissionsLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result: Map<String, Boolean> ->
                when {
                    result.isEmpty() -> {
                        // If user interaction was interrupted, the permission request is cancelled and you
                        // receive empty arrays.
                        logDebug("Android R+: user interaction was cancelled.")
                    }
                    result.values.all { it } -> {
                        // All Permissions are granted.
                        logDebug("Android R+: location permissions are granted")
                        if (config.enableBackgroundUpdates) {
                            logDebug("Android R+: requesting background location permission")
                            backgroundLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            onPermissionGranted()
                        }
                    }
                    else -> {
                        if (locationPermissions.any(::shouldShowRationale)) {
                            logDebug("Android R+: location permissions are denied.")
                            onPermissionDenied()
                            resetLocationPermissionBlocked()
                        } else {
                            if (isLocationPermissionBlockedFirstTime()) {
                                logDebug("Android R+: location permissions are denied second time.")
                                setLocationPermissionBlocked()
                                onPermissionDenied()
                            } else {
                                logDebug("Android R+: location permissions are blocked.")
                                showPermanentlyDeniedDialog()
                            }
                        }
                    }
                }
            }

            backgroundLocationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                when {
                    isGranted -> {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                        logDebug("Android R+: background location permission is granted.")
                        onPermissionGranted()
                    }
                    config.forceBackgroundUpdates -> {
                        logDebug("Android R+: background location permission is denied. and it was forced.")
                        showPermanentlyDeniedDialog()
                    }
                    else -> {
                        if (backgroundPermission.any(::shouldShowRationale)) {
                            logDebug("Android R+: background location permission is denied.")
                            onPermissionDenied()
                            resetBackgroundLocationPermissionBlocked()
                        } else {
                            if (isBackgroundLocationPermissionBlockedFirstTime()) {
                                logDebug("Android R+: background location permissions is denied second time.")
                                setBackgroundLocationPermissionBlocked()
                                onPermissionDenied()
                            } else {
                                logDebug("Android R+: background location permissions is blocked.")
                                showPermanentlyDeniedDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Initiates permission model to request location permission.
     *
     * It follows google's recommendations for permission model.
     *
     * 1. Check whether the permission is already granted or not.
     * 2. If not, then check if rationale should be displayed or not.
     * 3. If not, check if the permission is requested for the first time or not.
     * 4. If yes, save that in preferences and request permission.
     * 5. If not, then the permission is permanently denied.
     */
    private fun initPermissionModel() {
        logDebug("==============================================")
        logDebug("Initializing permission model")
        logDebug("==============================================")
        if (hasAllPermissions()) {
            // permission is already granted
            onPermissionGranted()
        } else {
            //doesn't have all the permission, checking if user has been asked for permission earlier
            if (needToShowRationale()) {
                // User has been asked for the permission
                logDebug("should display rationale for location permission")
                showPermissionRationale()
            } else {
                requestForPermissions()
//                if (isAnyPermissionAskedFirstTime()) {
//                    logDebug("permission asked first time")
//                    // request permission
//                    setPermissionAsked()
//                    requestForPermissions()
//                } else {
//                    // permanently denied
//                    showPermanentlyDeniedDialog()
//                }
            }
        }
    }

    /**
     * Initiates permission model to request location permission.
     *
     * It follows google's recommendations for permission model.
     *
     * 1. Check whether the permission is already granted or not.
     * 2. If not, then check if rationale should be displayed or not.
     * 3. If not, check if the permission is requested for the first time or not.
     * 4. If yes, save that in preferences and request permission.
     * 5. If not, then the permission is permanently denied.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initSplitPermissionModel() {
        val needBackgroundLocation = config.enableBackgroundUpdates || config.forceBackgroundUpdates
        val hasBackgroundLocationPermission = backgroundPermission.all(::hasPermission)
        val hasLocationPermission = locationPermissions.all(::hasPermission)

        val shouldShowRationaleForBackgroundLocationPermission =
            backgroundPermission.any(::shouldShowRationale)
        val shouldShowRationaleForLocationPermissions =
            locationPermissions.any(::shouldShowRationale)

        logDebug("==============================================")
        logDebug("Android R+: Initializing permission model")
        logDebug("==============================================")

        if (hasLocationPermission) {
            logDebug("Android R+: has location permission")
            // Location permission is already granted
            if (needBackgroundLocation) {
                logDebug("Android R+: need background location permission")
                if (hasBackgroundLocationPermission) {
                    logDebug("Android R+: has background location permission")
                    onPermissionGranted()
                } else {
                    if (shouldShowRationaleForBackgroundLocationPermission) {
                        logDebug("Android R+: show rationale for background location permission")
                        showPermissionRationale()
                    } else {
                        requestForPermissions()
                    }
                }
            } else {
                logDebug("Android R+: background location permission is not needed")
                onPermissionGranted()
            }
        } else {
            logDebug("Android R+: don't have location permission")
            //doesn't have all the permission, checking if user has been asked for permission earlier
            if (shouldShowRationaleForLocationPermissions) {
                // User has been asked for the permission
                logDebug("Android R+: show rationale for location permission")
                showPermissionRationale()
            } else {
//                if (isLocationPermissionAskedInPast()) {
//                    logDebug("Android R+: location permissions are blocked")
//                    // permanently denied
//                    showPermanentlyDeniedDialog()
//                } else {
//                    // request permission
//                    requestForPermissions()
//                }
//                if(!isLocationPermissionBlockedFirstTime()) {
//                    resetLocationPermissionBlocked()
//                }
                requestForPermissions()
            }
        }
    }

    private fun isLocationPermissionBlockedFirstTime(): Boolean {
        return !pref.getBoolean("LOCATION_PERMISSION", false)
    }

    private fun isBackgroundLocationPermissionBlockedFirstTime(): Boolean {
        return !pref.getBoolean("BACKGROUND_LOCATION_PERMISSION", false)
    }

    private fun setLocationPermissionBlocked() {
        pref.edit { putBoolean("LOCATION_PERMISSION", true) }
    }
    private fun setBackgroundLocationPermissionBlocked() {
        pref.edit { putBoolean("BACKGROUND_LOCATION_PERMISSION", true) }
    }

    private fun resetLocationPermissionBlocked() =
        pref.edit { remove("LOCATION_PERMISSION") }

    private fun resetBackgroundLocationPermissionBlocked() =
        pref.edit { remove("BACKGROUND_LOCATION_PERMISSION") }

    private fun requestForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            logDebug("Android R+: requesting location permissions")
//            requestForPermissionsSeparately()
            allPermissionsLauncher.launch(permissions)
        } else {
            logDebug("Requesting all permissions.")
            allPermissionsLauncher.launch(permissions)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestForPermissionsSeparately() {
        // Android 11: Request background location separately

        // If your app targets Android 11 or higher, the system enforces this best practice.
        // If you request a foreground location permission and the background location
        // permission at the same time, the system ignores the request and doesn't grant
        // your app either permission.

        // Because of this reason, it will only request normal location permission first and
        // then it asks for background location permission.
        locationPermissionsLauncher.launch(locationPermissions)
    }

    private fun hasAllPermissions(): Boolean = permissions.all(::hasPermission)

    private fun needToShowRationale(): Boolean = permissions.any(::shouldShowRationale)

    /**
     * Displays a permission rationale dialog
     */
    private fun showPermissionRationale() {
        val title = getString(R.string.locus_rationale_title)
        val message = getString(R.string.locus_rationale_message)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.grant) { dialog, _ ->
                requestForPermissions()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.deny) { dialog, _ ->
                dialog.dismiss()
                onPermissionDenied()
            }
            .setCancelable(false)
            .create()
            .takeIf { !isFinishing }?.show()
    }

    /**
     * Displays a permission rationale dialog
     */
    private fun showBackgroundLocationPermissionDialog() {
        val title = getString(R.string.locus_background_location_title)
        val message = getString(R.string.locus_background_location_message)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.grant) { dialog, _ ->
                requestForPermissions()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.deny) { dialog, _ ->
                dialog.dismiss()
                onPermissionDenied()
            }
            .setCancelable(false)
            .create()
            .takeIf { !isFinishing }?.show()
    }

    /**
     * handles the flow when the permission is granted successfully. It either sends success broadcast if the permission is granted and location setting resolution is disabled or proceeds for checking location settings
     */
    private fun onPermissionGranted() {
        if (config.shouldResolveRequest) {
            checkIfLocationSettingsAreEnabled()
        } else {
            shouldProceedForLocation()
        }
    }

    /**
     * Sends denied broadcast when user denies to grant location permission
     */
    private fun onPermissionDenied() {
//        logDebug("onPermissionDenied")
//        if (!needToShowRationale()) {
//            logDebug("showPermanentlyDeniedDialog")
//            showPermanentlyDeniedDialog()
//        } else {
//            logDebug("Sending permission denied")
//            postResult(Constants.DENIED)
//        }
        logDebug("Sending permission denied")
        postResult(Constants.DENIED)
    }

    /**
     * Shows permission permanently blocked dialog
     */
    private fun showPermanentlyDeniedDialog() {
        val title = getString(R.string.locus_permission_blocked_title)
        val message = getString(R.string.locus_permission_blocked_message)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.open_settings) { dialog, _ ->
                openSettings()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                onPermissionPermanentlyDenied()
            }
            .setCancelable(false)
            .create()
            .takeIf { !isFinishing }?.show()
    }

    /**
     * Sends broadcast indicating permanent denial of location permission
     */
    private fun onPermissionPermanentlyDenied() {
        postResult(Constants.PERMANENTLY_DENIED)
    }

    /**
     * Opens app settings screen
     */
    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, SETTINGS_ACTIVITY_REQUEST_CODE)
    }

    /**
     * Checks whether the current location settings allows retrieval of location or not.
     * If settings are isLoggingEnabled then retrieves the location, otherwise initiate the process of settings resolution
     * */
    private fun checkIfLocationSettingsAreEnabled() {
        checkSettings(success = { shouldProceedForLocation() }) { exception ->
            if (exception is ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        logDebug("Location settings resolution is required")
                        onResolutionNeeded(exception)
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        logDebug("cannot change settings, continue with current settings")
                        shouldProceedForLocation()
                    }
                    else -> logDebug("something went wrong while processing location settings resolution request: $exception")
                }
            } else {
                logDebug("Location settings resolution denied")
                // resolution failed somehow
                onResolutionDenied()
            }
        }
    }

    private fun checkSettings(
        success: (LocationSettingsResponse) -> Unit,
        failure: (Exception) -> Unit
    ) {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(config.locationRequest)
        builder.setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnSuccessListener {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            success(it)
        }.addOnFailureListener { exception ->
            failure(exception)
        }
    }

    /**
     * Sends success broadcast so that location retrieval process can be initiated
     */
    private fun shouldProceedForLocation() {
        clearPermissionNotificationIfAny()
        postResult(Constants.GRANTED)
    }

    private fun clearPermissionNotificationIfAny() {
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.cancel(Constants.PERMISSION_NOTIFICATION_ID)
    }

    /**
     * This function is called when resolution of location settings is needed.
     * Shows a dialog that location resolution is needed.
     * @param exception is an instance of ResolvableApiException which determines whether the resolution
     * is possible or not
     * */
    private fun onResolutionNeeded(exception: Exception) {
        exception.printStackTrace()
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val title = getString(R.string.locus_location_resolution_title)
        val message = getString(R.string.locus_location_resolution_message)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.enable) { dialog, _ ->
                resolveLocationSettings(exception)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                onResolutionDenied()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    /**
     * Sends broadcast indicating the denial of user for resolving location settings
     */
    private fun onResolutionDenied() {
        postResult(Constants.RESOLUTION_FAILED)
    }

    /**
     * Initiates location settings resolution process.
     * @param exception is used to resolve location settings
     * */
    private fun resolveLocationSettings(exception: Exception) {
        val resolvable = exception as? ResolvableApiException ?: return
        try {
            resolvable.startResolutionForResult(this, REQUEST_CODE_LOCATION_SETTINGS)
        } catch (e1: IntentSender.SendIntentException) {
            e1.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            if (resultCode == RESULT_OK) {
                shouldProceedForLocation()
            } else {
                checkSettings(success = { shouldProceedForLocation() }) {
                    postResult(Constants.LOCATION_SETTINGS_DENIED)
                }
            }
        } else if (requestCode == SETTINGS_ACTIVITY_REQUEST_CODE) {
            if (hasAllPermissions()) {
                onPermissionGranted()
            } else {
                onPermissionPermanentlyDenied()
            }
        }
    }

    /**
     * Posts results on [permissionLiveData]
     * @param status Status of the permission model and location resolution process
     */
    private fun postResult(status: String) {
        logDebug("Posting permission result: $intent")
        permissionLiveData.postValue(status)
        isRequestingPermission.set(false)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRequestingPermission.set(false)
    }
}
