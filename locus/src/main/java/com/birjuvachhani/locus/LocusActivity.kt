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
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

class LocusActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        private const val REQUEST_CODE_LOCATION_SETTINGS = 123
        private const val LOCATION_PERMISSION = android.Manifest.permission.ACCESS_COARSE_LOCATION
        private const val PERMISSION_REQUEST_CODE = 777
    }

    private val localBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }
    private lateinit var locationRequest: LocationRequest
    private var isResolutionEnabled: Boolean = false
    private val pref: SharedPreferences by lazy {
        getSharedPreferences("permissions_pref", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        isResolutionEnabled =
            intent?.getParcelableExtra<LocationRequest>(Constants.INTENT_EXTRA_LOCATION_REQUEST)?.let {
                locationRequest = it
                true
            } ?: false

        processIntent(intent)
        initPermissionModel()
    }

    private fun processIntent(intent: Intent?) {
        intent ?: return
        // TODO: get data from intent like dialog texts and titles
    }

    /**
     * Initiates permission model to request [permission].
     *
     * It follows google's recommendations for permission model.
     *
     * 1. Check whether the permission is already granted or not.
     * 2. If not, then check if rationale should be displayed or not.
     * 3. If not, check if the permission is requested for the first time or not.
     * 4. If yes, save that in preferences and request permission.
     * 5. If not, then the permission is permanently denied.
     * @param permission String is the permission that needs to be requested.
     */
    private fun initPermissionModel() {
        Log.e(this::class.java.simpleName, "Initializing permission model")
        if (!hasPermission()) {
            //doesn't have permission, checking if user has been asked for permission earlier
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, LOCATION_PERMISSION)) {
                // should show rationale
                showPermissionRationale()
            } else {
                if (isPermissionAskedFirstTime(LOCATION_PERMISSION)) {
                    // request permission
                    setPermissionAsked(LOCATION_PERMISSION)
                    requestPermission(LOCATION_PERMISSION)
                } else {
                    // permanently denied
                    onPermissionPermanentlyDenied()
                }
            }
        } else {
            // permission is already granted
            onPermissionGranted()
        }
    }

    /**
     * Checks whether the app has location permission or not
     * @return true is the app has location permission, false otherwise.
     * */
    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This feature requires location permission to function. Please grant location permission.")
            .setPositiveButton("Grant") { dialog, _ ->
                initPermissionModel()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
                onPermissionDenied()
            }.takeIf { !isFinishing }?.show()
    }

    /**
     * Checks whether the requested permission is asked for the first time or not.
     *
     * The value is stored in the shared preferences.
     * @receiver Fragment is used to get context.
     * @param permission String is the permission that needs to be checked.
     * @return Boolean true if the permission is asked for the first time, false otherwise.
     */
    private fun isPermissionAskedFirstTime(permission: String): Boolean = pref.getBoolean(permission, true)

    /**
     * Writes the false value into shared preferences which indicates that the [permission] has been requested previously.
     * @receiver Fragment is used to get context.
     * @param permission String is the permission that needs to be checked.
     */
    private fun setPermissionAsked(permission: String) = pref.edit().putBoolean(permission, false).apply()

    /**
     * Actual request for the permission
     * */
    private fun requestPermission(permission: String) =
        ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun onPermissionGranted() {
        if (isResolutionEnabled) {
            checkIfLocationSettingsAreEnabled()
        } else {
            shouldProceedForLocation()
        }
    }

    private fun onPermissionDenied() {
        // TODO: send broadcast
    }

    private fun onPermissionPermanentlyDenied() {
        AlertDialog.Builder(this)
            .setTitle("Permission Blocked")
            .setMessage("This feature requires location permission to function. Please grant location permission for settings.")
            .setPositiveButton("OPEN SETTINGS") { dialog, _ ->
                openSettings()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
                onPermissionDenied()
            }.takeIf { !isFinishing }?.show()
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
        // TODO: how to get result from this activity
    }

    /**
     * Checks whether the current location settings allows retrieval of location or not.
     * If settings are enabled then retrieves the location, otherwise initiate the process of settings resolution
     * */
    private fun checkIfLocationSettingsAreEnabled() {
        if (checkIfRequiredLocationSettingsAreEnabled()) {
            shouldProceedForLocation()
        } else {
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(locationRequest)
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
                    // TODO: resolution failed
                }
            }
        }
    }

    private fun shouldProceedForLocation() {
        // TODO: send success broadcast
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
            android.app.AlertDialog.Builder(this)
                .setTitle("Location is currently disabled")
                .setMessage("Please enable access to Location from Settings.")
                .setPositiveButton(R.string.btn_settings) { dialog, _ ->
                    resolveLocationSettings(exception)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                    dialog.dismiss()
                    onResolutionDenied()
                }.create().takeIf { !isFinishing }?.show()
        }
    }

    private fun onResolutionDenied() {
        // TODO: send broadcast of resolution denied
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

    fun sendResultBroadcast(intent: Intent) {
        intent.action = packageName
        localBroadcastManager.sendBroadcast(intent)
        // TODO: determine if needed to be finished
//        finish()
    }
}
