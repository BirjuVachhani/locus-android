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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Created by Birju Vachhani on 10 April 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

internal class LocationProvider(context: Context) {

    private val options: Configuration = Configuration()
    private val isRequestOngoing = AtomicBoolean().apply { set(false) }

    private var mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    internal val locationLiveData = MutableLiveData<LocusResult>()

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            locationResult?.let { result ->
                if (result.locations.isNotEmpty()) {
                    logDebug("Received location ${result.locations.first()}")
                    sendResult(LocusResult.Success(result.locations.first()))
                }
            }
        }
    }

    /**
     * Starts continuous location tracking using FusedLocationProviderClient
     * */
    @SuppressLint("MissingPermission")
    internal fun startContinuousLocation() {
        if (isRequestOngoing.getAndSet(true)) return
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
        locationLiveData.postValue(result)
    }

    /**
     * Stops location tracking by removing location callback from FusedLocationProviderClient
     * */
    internal fun stopContinuousLocation() {
        logDebug("Stopping location updates")
        isRequestOngoing.set(false)
        locationLiveData.postValue(null)
        mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
    }
}