package com.birjuvachhani.locationextension

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
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


/**
 * Created by Birju Vachhani on 09-11-2018.
 */
class LocationHelper : Fragment() {
    var isOneTime = false
    private var isRationaleDisplayed = false
    private var isJustBlocked = true
    private var options: LocationOptions = LocationOptions()
    private val REQUEST_CODE_LOCATION_SETTINGS = 123
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            locationResult?.let {
                if (it.locations.isNotEmpty()) {
                    success(it.locations.first())

                    if (isOneTime) {
                        stopContinuousLocation()
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "LocationHelper"
        fun newInstance(): LocationHelper {
            return LocationHelper()
        }
    }

    var success: (Location) -> Unit = {}
    var failure: (LocationError) -> Unit = {}

    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private val REQUEST_LOCATION_CODE = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    fun startLocationProcess(
        options: LocationOptions = LocationOptions(),
        success: (Location) -> Unit,
        failure: (LocationError) -> Unit,
        isOneTime: Boolean
    ) {
        this.options = options
        this.success = success
        this.failure = failure
        this.isOneTime = isOneTime
        initPermissionModel()
    }

    private fun initPermissionModel() {
        when (hasLocationPermission()) {
            //has permission to access location
            true -> initLocationTracking()
            false -> {
                //doesn't have permission, checking if user has been asked for permission earlier
                when (isFirstRequest()) {
                    //permission is requested first time, directly prompt user for permission
                    true -> requestLocationPermission()
                    false -> {
                        //permission is not asked for first time, display rationale and then prompt user for permission
                        displayRationale()
                        isRationaleDisplayed = true
                    }
                }
            }
        }
    }

    private fun displayRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Location Permission Required!")
            .setMessage(options.rationale)
            .setPositiveButton("GRANT") { dialog, _ ->
                requestLocationPermission()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
                failure(LocationError(true))
            }.create().show()
    }

    private fun requestLocationPermission() {
        requestPermissions(arrayOf(LOCATION_PERMISSION), REQUEST_LOCATION_CODE)
    }

    private fun isFirstRequest(): Boolean {
        return if (isApiLevelAbove23())
            !requireActivity().shouldShowRequestPermissionRationale(LOCATION_PERMISSION)
        else
            false
    }

    private fun hasLocationPermission(): Boolean {
        return if (isApiLevelAbove23())
            requireActivity().checkSelfPermission(LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED
        else
            true
    }

    private fun isApiLevelAbove23(): Boolean {
        return Build.VERSION.SDK_INT >= 23
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocationTracking()
                } else {
                    if (!shouldShowRequestPermissionRationale(LOCATION_PERMISSION)) {
                        //means permission is permanently blocked by user
                        if (!isJustBlocked) {
                            showPermissionBlockedDialog()
                        } else
                            isJustBlocked = false
                    }
                    failure(LocationError(true))
                }
            }
        }
    }

    private fun showPermissionBlockedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Location Permission Blocked")
            .setMessage(options.blocked)
            .setPositiveButton("ENABLE") { dialog, _ ->
                dialog.dismiss()
                openSettings()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.create().show()
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    /*
    *
    * Location Settings and Location Request Handling
    *
    */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            val locationSettingsStates = LocationSettingsStates.fromIntent(data!!)
            when (resultCode) {
                Activity.RESULT_OK -> initPermissionModel()
                Activity.RESULT_CANCELED ->
                    // The user was asked to change settings, but chose not to
                    onResolveLocationSettingCancelled(locationSettingsStates)
                else -> {
                }
            }
        }
    }

    private fun onResolveLocationSettingCancelled(locationSettingsStates: LocationSettingsStates) {
        if (locationSettingsStates.isLocationPresent && locationSettingsStates.isLocationUsable) {
            initPermissionModel()
        }
    }

    private fun initLocationTracking() {
        //init location here

        initializeFusedLocationProviderClient()
        checkIfLocationSettingsAreEnabled()
    }

    // Initializes FusedLocationProviderClient
    private fun initializeFusedLocationProviderClient() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

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
                // ...
                getLastKnownLocation()
            }
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    onResolutionNeeded(exception)
                } else {
                    failure(LocationError(false, exception))
                }
            }
        }
    }

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

    private fun resolveLocationSettings(exception: Exception) {
        val resolvable = exception as ResolvableApiException
        try {
//            resolvable.startResolutionForResult(requireActivity(), REQUEST_CODE_LOCATION_SETTINGS)
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

    private fun shouldBeAllowedToProceed(): Boolean {
        return lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }

    // Checks if the device location settings match with what user requested
    private fun checkIfRequiredLocationSettingsAreEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        if (!isOneTime) {
            startContinuousLocation()
            return
        }
        mFusedLocationProviderClient?.lastLocation?.addOnSuccessListener { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                success(location)
            } else {
                startContinuousLocation()
            }
        }?.addOnFailureListener { exception ->
            failure(LocationError(false, exception))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startContinuousLocation() {
        mFusedLocationProviderClient?.requestLocationUpdates(
            options.locationRequest,
            mLocationCallback,
            Looper.getMainLooper()
        )?.addOnFailureListener { exception ->
            failure(LocationError(false, exception))
        }
    }

    // Stops location updates
    internal fun stopContinuousLocation() {
        mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
    }
}

class LocationError(val isPermissionDenied: Boolean, val throwable: Throwable? = null)