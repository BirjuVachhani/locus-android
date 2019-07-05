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

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

/*
 * Created by Birju Vachhani on 10 April 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

class LocationProvider(context: Context, val options: LocationOptions, val isOneTime: Boolean) {

    private var mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    internal val locationLiveData = MutableLiveData<LocusResult>()

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            locationResult?.let { result ->
                if (result.locations.isNotEmpty()) {
                    sendResult(LocusResult.Success(result.locations.first()))
                    if (isOneTime) {
                        stopContinuousLocation()
                        locationLiveData.value = null
                    }
                }
            }
        }
    }

    init {
        checkIfLocationSettingsAreEnabled(context, options)
    }

    /**
     * Checks whether the current location settings allows retrieval of location or not.
     * If settings are enabled then retrieves the location, otherwise initiate the process of settings resolution
     * */
    private fun checkIfLocationSettingsAreEnabled(context: Context, options: LocationOptions) {
        if (checkIfRequiredLocationSettingsAreEnabled(context)) {
            getLastKnownLocation()
        } else {
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(options.locationRequest)
            builder.setAlwaysShow(true)

            val client = LocationServices.getSettingsClient(context)
            val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
            locationSettingsResponseTask.addOnSuccessListener {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                getLastKnownLocation()
            }
            locationSettingsResponseTask.addOnFailureListener { exception ->
                sendResult(LocusResult.Failure(exception))
            }
        }
    }

    /**
     * Checks whether the device location settings match with what the user requested
     * @return true is the current location settings satisfies the requirement, false otherwise.
     * */
    private fun checkIfRequiredLocationSettingsAreEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
                stopContinuousLocation()
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
     * Sets result into live data synchronously
     * */
    private fun sendResult(result: LocusResult) {
        locationLiveData.value = result
    }

    /**
     * Stops location tracking by removing location callback from FusedLocationProviderClient
     * */
    internal fun stopContinuousLocation() {
        mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
    }
}