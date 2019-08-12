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
import android.app.PendingIntent
import android.content.Context
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Created by Birju Vachhani on 10 April 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Responsible for starting and stopping location updates=
 * @property isRequestOngoing AtomicBoolean which indicates that whether a location request is ongoing or not
 * @property mFusedLocationProviderClient (com.google.android.gms.location.FusedLocationProviderClient..com.google.android.gms.location.FusedLocationProviderClient?) used to request location
 * @property locationLiveData MutableLiveData<LocusResult> contains location results
 * @property mLocationCallback <no name provided>
 * @constructor
 */
internal class LocationProvider(context: Context) {

    private val pendingIntent: PendingIntent by lazy {
        LocationBroadcastReceiver.getPendingIntent(context)
    }
    private val isRequestOngoing = AtomicBoolean().apply { set(false) }

    private var mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    internal var locationLiveData = MutableLiveData<LocusResult>()

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
     *
     * If somehow continuous location retrieval fails then it tries to retrieve last known location.
     * */
    @SuppressLint("MissingPermission")
    internal fun startContinuousLocation(request: LocationRequest) {
        if (isRequestOngoing.getAndSet(true)) return
        mFusedLocationProviderClient?.requestLocationUpdates(
            request,
            mLocationCallback,
            Looper.getMainLooper()
        )?.addOnFailureListener {
            mFusedLocationProviderClient?.lastLocation?.addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                it.result?.let { location ->
                    sendResult(LocusResult.Success(location))
                }
            }?.addOnFailureListener {
                sendResult(LocusResult.Failure(it))
            }
        }
    }

    /**
     * Starts continuous location tracking using FusedLocationProviderClient
     *
     * If somehow continuous location retrieval fails then it tries to retrieve last known location.
     * */
    @SuppressLint("MissingPermission")
    internal fun startBackgroundLocationUpdates(context: Context, request: LocationRequest) {
        if (isRequestOngoing.getAndSet(true)) return
        logDebug("Starting location updates")
        mFusedLocationProviderClient?.requestLocationUpdates(
            request,
            pendingIntent
        )?.addOnFailureListener {
            mFusedLocationProviderClient?.lastLocation?.addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                it.result?.let { location ->
                    backgroundLocationLiveData.postValue(LocusResult.Success(location))
                }
            }?.addOnFailureListener {
                backgroundLocationLiveData.postValue(LocusResult.Failure(it))
            }
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
        locationLiveData = MutableLiveData()
        mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
    }

    /**
     * Stops location tracking by removing location callback from FusedLocationProviderClient
     * */
    internal fun stopBackgroundLocation() {
        logDebug("Stopping background location updates")
        isRequestOngoing.set(false)
        backgroundLocationLiveData = MutableLiveData()
        mFusedLocationProviderClient?.removeLocationUpdates(pendingIntent)
    }
}