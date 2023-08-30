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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import com.birjuvachhani.locus.LocusLocationRequest.Companion.checkAvailableService
import com.google.android.gms.common.api.ApiException as GMSApiException
import com.google.android.gms.common.api.ResolvableApiException as GMSResolvableApiException
import com.google.android.gms.location.LocationServices as GMSLocationServices
import com.google.android.gms.location.LocationSettingsRequest as GMSLocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse as GMSLocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes as GMSLocationSettingsStatusCodes
import com.huawei.hms.common.ApiException as HMSApiException
import com.huawei.hms.common.ResolvableApiException as HMSResolvableApiException
import com.huawei.hms.location.LocationServices as HMSLocationServices
import com.huawei.hms.location.LocationSettingsRequest as HMSLocationSettingsRequest
import com.huawei.hms.location.LocationSettingsResponse as HMSLocationSettingsResponse
import com.huawei.hms.location.LocationSettingsStatusCodes as HMSLocationSettingsStatusCodes

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
class LocusActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        private const val REQUEST_CODE_LOCATION_SETTINGS = 545
        private const val PERMISSION_REQUEST_CODE = 777
        private const val SETTINGS_ACTIVITY_REQUEST_CODE = 659
        private const val PREF_NAME = "locus_pref"
    }

    private lateinit var config: Configuration

    private val pref: SharedPreferences by lazy {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private var permissions: Array<String> = arrayOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        intent?.getParcelableExtra<Configuration>(Constants.INTENT_EXTRA_CONFIGURATION)?.let {
            config = it
        } ?: logError("No config is sent to the permission activity")
        val isSingleUpdate =
            intent?.getBooleanExtra(Constants.INTENT_EXTRA_IS_SINGLE_UPDATE, false) ?: false
        permissions =
            if (config.enableBackgroundUpdates && !isSingleUpdate) locationPermissions + backgroundPermission else locationPermissions
        initPermissionModel()
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

        logDebug("Initializing permission model")
        if (!hasAllPermissions()) {
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
        } else {
            // permission is already granted
            onPermissionGranted()
        }
    }

    private fun requestForPermissions() =
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val perms = if (config.enableBackgroundUpdates && config.forceBackgroundUpdates) {
            locationPermissions + backgroundPermission
        } else {
            locationPermissions
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request is cancelled, and you
                    // receive empty arrays.
                    logDebug("User interaction was cancelled.")
                grantResults.all { it == PackageManager.PERMISSION_GRANTED } -> onPermissionGranted()
                perms.all { grantResults[permissions.indexOf(it)] == PackageManager.PERMISSION_GRANTED } -> onPermissionGranted()
                else -> onPermissionDenied()
            }
        }
    }

    /**
     * handles the flow when the permission is granted successfully. It either sends success broadcast if the
     * permission is granted and location setting resolution is disabled or proceeds for checking location settings.
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
        if (!needToShowRationale()) {
            showPermanentlyDeniedDialog()
        } else {
            logDebug("Sending permission denied")
            postResult(Constants.DENIED)
        }
    }

    /**
     * Shows permission permanently blocked dialog
     */
    private fun showPermanentlyDeniedDialog() {
        /// TODO: Remove usage of deprecated properties after a month
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
     * If settings are isLoggingEnabled then retrieves the location, otherwise initiate the process
     * of settings resolution.
     * */
    private fun checkIfLocationSettingsAreEnabled() {
        fun resolveException() {
            logDebug("Location settings resolution denied")
            // resolution failed somehow
            onResolutionDenied()
        }

        config.locationRequest.checkAvailableService(
            onGMSAvailable = {
                checkSettingsWithGMS(success = { shouldProceedForLocation() }) { exception ->
                    if (exception is GMSApiException) {
                        handelGMSApiException(exception)
                    } else {
                        resolveException()
                    }
                }
            },
            onHMSAvailable = {
                checkSettingsWithHMS(success = { shouldProceedForLocation() }) { exception ->
                    if (exception is HMSApiException) {
                        handelHMSApiException(exception)
                    } else {
                        resolveException()
                    }
                }
            },
        )
    }

    private fun handelHMSApiException(exception: HMSApiException) {
        when (exception.statusCode) {
            HMSLocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                logDebug("Location settings resolution is required")
                onResolutionNeeded(exception)
            }

            HMSLocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                logDebug("cannot change settings, continue with current settings")
                shouldProceedForLocation()
            }

            else -> logDebug("something went wrong while processing location settings resolution request: $exception")
        }
    }

    private fun handelGMSApiException(exception: GMSApiException) {
        when (exception.statusCode) {
            GMSLocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                logDebug("Location settings resolution is required")
                onResolutionNeeded(exception)
            }

            GMSLocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                logDebug("cannot change settings, continue with current settings")
                shouldProceedForLocation()
            }

            else -> logDebug("something went wrong while processing location settings resolution request: $exception")
        }
    }

    private fun checkSettingsWithGMS(
        success: (GMSLocationSettingsResponse) -> Unit,
        failure: (Exception) -> Unit,
    ) {
        val builder = GMSLocationSettingsRequest.Builder()
        builder.addLocationRequest((config.locationRequest as LocusLocationRequest.LocusGMSLocationRequest).locationRequest)
        builder.setAlwaysShow(true)

        val client = GMSLocationServices.getSettingsClient(this)
        val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnSuccessListener {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            success(it)
        }.addOnFailureListener { exception ->
            failure(exception)
        }
    }

    private fun checkSettingsWithHMS(
        success: (HMSLocationSettingsResponse) -> Unit,
        failure: (Exception) -> Unit,
    ) {
        val builder = HMSLocationSettingsRequest.Builder()
        builder.addLocationRequest((config.locationRequest as LocusLocationRequest.LocusHMSLocationRequest).locationRequest)
        builder.setAlwaysShow(true)

        val client = HMSLocationServices.getSettingsClient(this)
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
        config.locationRequest.checkAvailableService(
            onGMSAvailable = {
                (exception as? GMSResolvableApiException)?.let {
                    try {
                        it.startResolutionForResult(this, REQUEST_CODE_LOCATION_SETTINGS)
                    } catch (e1: IntentSender.SendIntentException) {
                        e1.printStackTrace()
                    }
                }
            },
            onHMSAvailable = {
                (exception as? HMSResolvableApiException)?.let {
                    try {
                        it.startResolutionForResult(this, REQUEST_CODE_LOCATION_SETTINGS)
                    } catch (e1: IntentSender.SendIntentException) {
                        e1.printStackTrace()
                    }
                }
            },
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            if (resultCode == RESULT_OK) {
                shouldProceedForLocation()
            } else {
                config.locationRequest.checkAvailableService(
                    onGMSAvailable = {
                        checkSettingsWithGMS(success = { shouldProceedForLocation() }) {
                            postResult(Constants.LOCATION_SETTINGS_DENIED)
                        }
                    },
                    onHMSAvailable = {
                        checkSettingsWithHMS(success = { shouldProceedForLocation() }) {
                            postResult(Constants.LOCATION_SETTINGS_DENIED)
                        }
                    }
                )
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
