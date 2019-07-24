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

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

class LocusActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        private const val REQUEST_CODE_LOCATION_SETTINGS = 123
        private const val COARSE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_COARSE_LOCATION
        private const val FINE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION
        private const val PERMISSION_REQUEST_CODE = 777
        private const val SETTINGS_ACTIVITY_REQUEST_CODE = 659
        private const val PREF_NAME = "locus_pref"
    }

    private val localBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }
    private var configuration: Configuration = Configuration()
    private var isResolutionEnabled: Boolean = false
    private val pref: SharedPreferences by lazy {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        isResolutionEnabled =
            intent?.getParcelableExtra<Configuration>(Constants.INTENT_EXTRA_CONFIGURATION)?.let {
                configuration = it
                configuration.shouldResolveRequest
            } ?: false

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
        if (!hasPermission()) {
            //doesn't have permission, checking if user has been asked for permission earlier
            if (shouldShowRationale()) {
                // should show rationale
                logDebug("should display rationale")
                showPermissionRationale()
            } else {
                if (isPermissionAskedFirstTime()) {
                    logDebug("permission asked first time")
                    // request permission
                    setPermissionAsked()
                    requestPermission()
                } else {
                    // permanently denied
                    showPermanentlyDeniedDialog()
                }
            }
        } else {
            // permission is already granted
            onPermissionGranted()
        }
    }

    /**
     * Determines whether the rationale needs to be shown or not
     * @return Boolean true if needs to be shown, false otherwise
     */
    private fun shouldShowRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            COARSE_LOCATION_PERMISSION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_LOCATION_PERMISSION)
    }

    /**
     * Checks whether the app has location permission or not
     * @return true is the app has location permission, false otherwise.
     * */
    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, COARSE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED

    /**
     * Displays a permission rationale dialog
     */
    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle(configuration.rationaleTitle)
            .setMessage(configuration.rationaleText)
            .setPositiveButton(R.string.grant) { dialog, _ ->
                requestPermission()
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
     * Checks whether the requested permission is asked for the first time or not.
     *
     * The value is stored in the shared preferences.
     * @receiver Fragment is used to get context.
     * @return Boolean true if the permission is asked for the first time, false otherwise.
     */
    private fun isPermissionAskedFirstTime(): Boolean = pref.getBoolean(FINE_LOCATION_PERMISSION, true)

    /**
     * Writes the false value into shared preferences which indicates that the location permission has been requested previously.
     * @receiver Fragment is used to get context.
     */
    private fun setPermissionAsked() = pref.edit().putBoolean(FINE_LOCATION_PERMISSION, false).commit()

    /**
     * Actual request for the permission
     * */
    private fun requestPermission() =
        ActivityCompat.requestPermissions(
            this,
            arrayOf(FINE_LOCATION_PERMISSION, COARSE_LOCATION_PERMISSION),
            PERMISSION_REQUEST_CODE
        )

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    /**
     * handles the flow when the permission is granted successfully. It either sends success broadcast if the permission is granted and location setting resolution is disabled or proceeds for checking location settings
     */
    private fun onPermissionGranted() {
        if (isResolutionEnabled) {
            checkIfLocationSettingsAreEnabled()
        } else {
            shouldProceedForLocation()
        }
    }

    /**
     * Sends denied broadcast when user denies to grant location permission
     */
    private fun onPermissionDenied() {
        logDebug("Sending permission denied")
        sendResultBroadcast(Intent(packageName).putExtra(Constants.INTENT_EXTRA_PERMISSION_RESULT, Constants.DENIED))
    }

    /**
     * Shows permission permanently blocked dialog
     */
    private fun showPermanentlyDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(configuration.blockedTitle)
            .setMessage(configuration.blockedText)
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
        sendResultBroadcast(
            Intent(packageName).putExtra(
                Constants.INTENT_EXTRA_PERMISSION_RESULT,
                Constants.PERMANENTLY_DENIED
            )
        )
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
        if (checkIfRequiredLocationSettingsAreEnabled()) {
            shouldProceedForLocation()
        } else {
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(configuration.locationRequest)
            builder.setAlwaysShow(true)

            val client = LocationServices.getSettingsClient(this)
            val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
            locationSettingsResponseTask.addOnSuccessListener {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                shouldProceedForLocation()
            }
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    onResolutionNeeded(exception)
                } else {
                    // resolution failed somehow
                    onResolutionDenied()
                }
            }
        }
    }

    /**
     * Sends success broadcast so that location retrieval process can be initiated
     */
    private fun shouldProceedForLocation() {
        sendResultBroadcast(Intent(packageName).putExtra(Constants.INTENT_EXTRA_PERMISSION_RESULT, Constants.GRANTED))
    }

    /**
     * Checks whether the device location settings match with what the user requested
     * @return true is the current location settings satisfies the requirement, false otherwise.
     * */
    private fun checkIfRequiredLocationSettingsAreEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * This function is called when resolution of location settings is needed.
     * Shows a dialog that location resolution is needed.
     * @param exception is an instance of ResolvableApiException which determines whether the resolution
     * is possible or not
     * */
    private fun onResolutionNeeded(exception: ResolvableApiException) {
        exception.printStackTrace()
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (!isFinishing) {
            AlertDialog.Builder(this)
                .setTitle(configuration.resolutionTitle)
                .setMessage(configuration.resolutionText)
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
                .takeIf { !isFinishing }?.show()
        }
    }

    /**
     * Sends broadcast indicating the denial of user for resolving location settings
     */
    private fun onResolutionDenied() {
        sendResultBroadcast(
            Intent(packageName).putExtra(
                Constants.INTENT_EXTRA_PERMISSION_RESULT,
                Constants.RESOLUTION_FAILED
            )
        )
    }

    /**
     * Initiates location settings resolution process.
     * @param exception is used to resolve location settings
     * */
    private fun resolveLocationSettings(exception: Exception) {
        val resolvable = exception as ResolvableApiException
        try {
            startIntentSenderForResult(
                resolvable.resolution.intentSender,
                REQUEST_CODE_LOCATION_SETTINGS,
                null,
                0,
                0,
                0,
                null
            )
        } catch (e1: IntentSender.SendIntentException) {
            e1.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            if (resultCode == RESULT_OK) {
                shouldProceedForLocation()
            } else {
                sendResultBroadcast(
                    Intent(packageName).putExtra(
                        Constants.INTENT_EXTRA_PERMISSION_RESULT,
                        Constants.LOCATION_SETTINGS_DENIED
                    )
                )
            }
        } else if (requestCode == SETTINGS_ACTIVITY_REQUEST_CODE) {
            if (hasPermission()) {
                onPermissionGranted()
            } else {
                onPermissionPermanentlyDenied()
            }
        }
    }

    /**
     * Sends local broadcast with provided [intent]
     * @param intent Intent contains data that needs to be sent into the broadcast
     */
    private fun sendResultBroadcast(intent: Intent) {
        intent.action = packageName
        logDebug("Sending permission broadcast: $intent")
        localBroadcastManager.sendBroadcast(intent)
        isRequestingPermission.set(false)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRequestingPermission.set(false)
    }
}
