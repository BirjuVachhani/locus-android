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

package com.birjuvachhani.locationextension

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.Fragment
import com.bext.alertDialog
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

/*
 * Created by Birju Vachhani on 09 November 2018
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Helper headless fragment to handle permission model and location retrieval process.
 * */
internal class LocationHelper : Fragment() {
    private var isOneTime = false
    private var isRationaleDisplayed = false
    private var isJustBlocked = true
    private var options: LocationOptions = LocationOptions()
    internal val locationLiveData = MutableLiveData<LocusResult>()
    private var isDisposed = false

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            locationResult?.let { result ->
                if (result.locations.isNotEmpty()) {
                    sendResult(LocusResult.Success(result.locations.first()))
                    if (isOneTime) {
                        isDisposed = true
                        stopContinuousLocation()
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "LocationHelper"
        private const val REQUEST_CODE_LOCATION_SETTINGS = 123
        private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val REQUEST_LOCATION_CODE = 7

        /**
         * Creates a new instance o this class and returns it.
         * */
        fun newInstance(options: LocationOptions): LocationHelper {
            return LocationHelper().apply { this.options = options }
        }
    }

    /**
     * retainInstance if set to true, makes sure that this fragment instance persist though configuration changes.
     * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        arguments?.let { args ->
            if (args.containsKey(Constants.IS_ONE_TIME_BUNDLE_KEY)) {
                isOneTime = args.getBoolean(Constants.IS_ONE_TIME_BUNDLE_KEY)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isDisposed) return
        if (hasPermissionsNotDefinedInManifest()) {
            locationLiveData.value =
                LocusResult.Failure(
                    Throwable(
                        """No location permission is defined in manifest.
                            Please make sure that location permission is added to the manifest"""
                    )
                )
            return
        }
        initPermissionModel()
    }

    private fun hasPermissionsNotDefinedInManifest(): Boolean =
        requireContext().packageManager
            .getPackageInfo(
                requireContext().packageName,
                PackageManager.GET_PERMISSIONS
            )?.requestedPermissions?.run {
            !contains(Manifest.permission.ACCESS_FINE_LOCATION)
                    && !contains(Manifest.permission.ACCESS_COARSE_LOCATION)
        } ?: true

    /**
     * Initiates permission model to request location permission in order to retrieve location successfully.=
     * */
    fun initPermissionModel() {
        when (hasLocationPermission()) {
            //has permission to access location
            true -> initLocationTracking()
            false -> {
                //doesn't have permission, checking if user has been asked for permission earlier
                when (isFirstRequest()) {
                    //permission is requested first time, directly prompt user for permission
                    true -> requestLocationPermission()
                    false -> {
                        //permission is not asked for first time, display rationaleText and then prompt user for permission
                        displayRationale()
                        isRationaleDisplayed = true
                    }
                }
            }
        }
    }

    /**
     * Displays a rational dialog to provide more information about the necessity of location permission
     * */
    private fun displayRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.permission_required_title))
            .setMessage(options.rationaleText)
            .setPositiveButton(getString(R.string.grant)) { dialog, _ ->
                requestLocationPermission()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                sendResult(LocusResult.Failure(Throwable(Constants.DENIED)))
            }.create().takeIf { !requireActivity().isFinishing }?.show()
    }

    /**
     * Requests user for location permission
     * */
    private fun requestLocationPermission() = requestPermissions(arrayOf(LOCATION_PERMISSION), REQUEST_LOCATION_CODE)

    /**
     * Checks whether the location permission is requested for the first time or not.
     * @return true if the location permission is requested for the first time, false otherwise.
     * */
    private fun isFirstRequest(): Boolean = if (isApiLevelAbove23())
        !requireActivity().shouldShowRequestPermissionRationale(LOCATION_PERMISSION)
    else
        false

    /**
     * Checks whether the app has location permission or not
     * @return true is the app has location permission, false otherwise.
     * */
    private fun hasLocationPermission(): Boolean = if (isApiLevelAbove23())
        requireActivity().checkSelfPermission(LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED
    else
        true

    /**
     * Checks whether the android os version is 23+ or not.
     * @return true is the android version is 23 or above, false otherwise.
     * */
    private fun isApiLevelAbove23(): Boolean = Build.VERSION.SDK_INT >= 23

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocationTracking()
                } else {
                    if (!shouldShowRequestPermissionRationale(LOCATION_PERMISSION)) {
                        //means permission is permanently blocked by user
                        if (!isJustBlocked) {
                            sendResult(LocusResult.Failure(Throwable(Constants.PERMANENTLY_DENIED)))
                            showPermissionBlockedDialog()
                        } else
                            isJustBlocked = false
                    }
                    sendResult(LocusResult.Failure(Throwable(Constants.DENIED)))
                }
            }
        }
    }

    /**
     * This function is used to show a 'Permission Blocked' dialog when the permission is permanently denied.
     * */
    private fun showPermissionBlockedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.permission_blocked_title))
            .setMessage(options.blockedText)
            .setPositiveButton(getString(R.string.enable)) { dialog, _ ->
                dialog.dismiss()
                openSettings()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }.create()
            .takeIf { !requireActivity().isFinishing }?.show()
    }

    /**
     * Opens app settings screen
     * */
    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            data?.let {
                val locationSettingsStates = LocationSettingsStates.fromIntent(it)
                when (resultCode) {
                    Activity.RESULT_OK -> initPermissionModel()
                    Activity.RESULT_CANCELED ->
                        // The user was asked to change settings, but chose not to
                        onResolveLocationSettingCancelled(locationSettingsStates)
                }
            }
        }
    }

    /**
     * Called when resolution of location setting is cancelled
     * @param locationSettingsStates a settings state instance that determines if the current location settings are
     * usable or not.
     * */
    private fun onResolveLocationSettingCancelled(locationSettingsStates: LocationSettingsStates) {
        if (locationSettingsStates.isLocationPresent && locationSettingsStates.isLocationUsable) {
            initPermissionModel()
        }
    }

    /**
     * This function initiates location tracking if the permission model succeeds.
     * */
    private fun initLocationTracking() {
        //init location here
        initializeFusedLocationProviderClient()
        checkIfLocationSettingsAreEnabled()
    }

    /**
     * Initiates FusedLocationProviderClient instance
     * */
    private fun initializeFusedLocationProviderClient() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    /**
     * Checks whether the current location settings allows retrieval of location or not.
     * If settings are enabled then retrieves the location, otherwise initiate the process of settings resolution
     * */
    private fun checkIfLocationSettingsAreEnabled() {
        if (checkIfRequiredLocationSettingsAreEnabled()) {
            getLastKnownLocation()
        } else {
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(options.locationRequest)
            builder.setAlwaysShow(true)

            val client = LocationServices.getSettingsClient(requireContext())
            val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
            locationSettingsResponseTask.addOnSuccessListener {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                getLastKnownLocation()
            }
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    onResolutionNeeded(exception)
                } else {
                    sendResult(LocusResult.Failure(exception))
                }
            }
        }
    }

    /**
     * This function is called when resolution of location settings is needed.
     * Shows a dialog that location resolution is needed.
     * @param exception is an instance of ResolvableApiException which determines whether the resolution
     * is possible or not
     * */
    private fun onResolutionNeeded(exception: ResolvableApiException) {
        exception.printStackTrace()
        if (!shouldBeAllowedToProceed()) return
        if (!requireActivity().isFinishing) {
            requireContext().alertDialog {
                title = getString(R.string.location_is_currently_disabled)
                message = getString(R.string.please_enable_access_to_location)
                positiveButtonText = getString(R.string.btn_settings)
                positiveButtonClick = {
                    resolveLocationSettings(exception)
                }
                negativeButtonText = getString(R.string.btn_cancel)
                negativeButtonClick = {}
            }
        }
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

    /**
     * Checks whether to continue the process or not. Makes sure the fragment is in foreground.
     * */
    private fun shouldBeAllowedToProceed(): Boolean = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    /**
     * Checks whether the device location settings match with what the user requested
     * @return true is the current location settings satisfies the requirement, false otherwise.
     * */
    private fun checkIfRequiredLocationSettingsAreEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * Retrieves the last known location using FusedLocationProviderClient.
     * In case of no last known location, initiates continues location to get a result.
     * */
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        if (!isOneTime) {
            startContinuousLocation()
            return
        }
        mFusedLocationProviderClient?.lastLocation?.addOnSuccessListener { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                sendResult(LocusResult.Success(location))
                isDisposed = true
            } else {
                startContinuousLocation()
            }
        }?.addOnFailureListener { exception ->
            sendResult(LocusResult.Failure(exception))
        }
    }

    /**
     * Starts continuous location tracking using FusedLocationProviderClient
     * */
    @SuppressLint("MissingPermission")
    private fun startContinuousLocation() {
        mFusedLocationProviderClient?.requestLocationUpdates(
            options.locationRequest,
            mLocationCallback,
            Looper.getMainLooper()
        )?.addOnFailureListener { exception ->
            sendResult(LocusResult.Failure(exception))
        }
    }

    /**
     * Stops location tracking by removing location callback from FusedLocationProviderClient
     * */
    internal fun stopContinuousLocation() {
        mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
    }

    /**
     * Sets result into live data synchronously
     * */
    private fun sendResult(result: LocusResult) {
        locationLiveData.value = result
    }

    /**
     * Sets result into live data asynchronously
     * */
    private fun sendResultAsync(result: LocusResult) = locationLiveData.postValue(result)

    fun reset() {
        isDisposed = false
    }
}