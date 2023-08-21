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
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import java.util.concurrent.atomic.AtomicBoolean
import com.google.android.gms.location.FusedLocationProviderClient as GMSFusedLocationProviderClient
import com.google.android.gms.location.LocationCallback as GMSLocationCallback
import com.google.android.gms.location.LocationRequest as GMSLocationRequest
import com.google.android.gms.location.LocationResult as GMSLocationResult
import com.google.android.gms.location.LocationServices as GMSLocationServices
import com.huawei.hms.location.FusedLocationProviderClient as HMSFusedLocationProviderClient
import com.huawei.hms.location.LocationCallback as HMSLocationCallback
import com.huawei.hms.location.LocationRequest as HMSLocationRequest
import com.huawei.hms.location.LocationResult as HMSLocationResult
import com.huawei.hms.location.LocationServices as HMSLocationServices

/*
 * Created by Birju Vachhani on 10 April 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Responsible for starting and stopping location updates=
 * @property isRequestOngoing AtomicBoolean which indicates that whether a location request is ongoing or not
 * @property gmsFusedLocationProviderClient (com.google.android.gms.location.FusedLocationProviderClient..com.google.android.gms.location.FusedLocationProviderClient?) used to request location
 * @property hmsFusedLocationProviderClient (com.huawei.hms.location.FusedLocationProviderClient..com.huawei.hms.location.FusedLocationProviderClient?) used to request location
 * @constructor
 */
internal class LocationProvider(context: Context) {

    private val pendingIntent: PendingIntent by lazy {
        LocationBroadcastReceiver.getPendingIntent(context)
    }
    private val isRequestOngoing = AtomicBoolean().apply { set(false) }

    private val gmsFusedLocationProviderClient: GMSFusedLocationProviderClient by lazy {
        GMSLocationServices.getFusedLocationProviderClient(context)
    }

    private val hmsFusedLocationProviderClient: HMSFusedLocationProviderClient by lazy {
        HMSLocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Starts continuous location tracking using FusedLocationProviderClient which is provided by
     * Google mobile service.
     *
     * If somehow continuous location retrieval fails then it tries to retrieve last known location.
     *
     * @param request is an instance of [GMSLocationRequest]
     */
    @SuppressLint("MissingPermission")
    internal fun startUpdates(request: GMSLocationRequest) {
        if (isRequestOngoing.getAndSet(true)) return
        logDebug("Starting location updates using Google mobile service")
        gmsFusedLocationProviderClient.requestLocationUpdates(request, pendingIntent)
            .addOnFailureListener { e ->
                logError(e)
                logDebug("Continuous location updates failed, retrieving last known location from Google mobile service")
                gmsFusedLocationProviderClient.lastLocation.addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener
                    it.result?.let { location ->
                        locationLiveData.postValue(LocusResult.success(location))
                    }
                }.addOnFailureListener {
                    locationLiveData.postValue(LocusResult.error(error = it))
                }
            }
    }

    /**
     * Starts continuous location tracking using FusedLocationProviderClient which is provided by
     * Huawei Mobile service.
     *
     * If somehow continuous location retrieval fails then it tries to retrieve last known location.
     *
     * @param request is an instance of [HMSLocationRequest]
     */
    @SuppressLint("MissingPermission")
    internal fun startUpdates(request: HMSLocationRequest) {
        if (isRequestOngoing.getAndSet(true)) return
        logDebug("Starting location updates using using Huawei mobile service")
        hmsFusedLocationProviderClient.requestLocationUpdates(request, pendingIntent)
            .addOnFailureListener { e ->
                logError(e)
                logDebug("Continuous location updates failed, retrieving last known location from Huawei mobile service")
                hmsFusedLocationProviderClient.lastLocation.addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener
                    it.result?.let { location ->
                        locationLiveData.postValue(LocusResult.success(location))
                    }
                }.addOnFailureListener {
                    locationLiveData.postValue(LocusResult.error(error = it))
                }
            }
    }

    /**
     * Initiates process to retrieve single location update
     * @param request LocationRequest instance that will be used to get location from Google mobile
     * Service LocationRequest.
     *
     * @param onUpdate Called on success/failure result of the single update retrieval process
     */
    @SuppressLint("MissingPermission")
    internal fun getSingleUpdate(
        request: GMSLocationRequest, onUpdate: (LocusResult) -> Unit,
    ) {
        fun startUpdates() {
            val callback = object : GMSLocationCallback() {
                override fun onLocationResult(result: GMSLocationResult) {
                    result.lastLocation?.let { onUpdate(LocusResult.success(it)) }
                    gmsFusedLocationProviderClient.removeLocationUpdates(this)
                }
            }
            gmsFusedLocationProviderClient.requestLocationUpdates(
                GMSLocationRequest.Builder(request).setMaxUpdates(1).build(),
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { error ->
                logError(error)
                onUpdate(LocusResult.error(error = error))
            }
        }

        gmsFusedLocationProviderClient.getCurrentLocation(
            request.priority, EmptyCancellationToken()
        ).addOnSuccessListener { location ->
            if (location != null) onUpdate(LocusResult.success(location)) else startUpdates()
        }.addOnFailureListener {
            logError(it)
            logDebug("Looks like last known location is not available in Google mobile service, requesting a new location update")
            startUpdates()
        }
    }

    /**
     * Initiates process to retrieve single location update
     *
     * @param request LocationRequest instance that will be used to get location form Huawei Mobile
     * Service LocationRequest.
     *
     * @param onUpdate Called on success/failure result of the single update retrieval process
     */
    @SuppressLint("MissingPermission")
    internal fun getSingleUpdate(
        request: HMSLocationRequest, onUpdate: (LocusResult) -> Unit,
    ) {
        fun startUpdates() {
            val callback = object : HMSLocationCallback() {
                override fun onLocationResult(result: HMSLocationResult) {
                    result.lastLocation?.let { onUpdate(LocusResult.success(it)) }
                    hmsFusedLocationProviderClient.removeLocationUpdates(this)
                }
            }
            hmsFusedLocationProviderClient.requestLocationUpdates(
                request.setNumUpdates(1), callback, Looper.getMainLooper()
            ).addOnFailureListener { error ->
                logError(error)
                onUpdate(LocusResult.error(error = error))
            }
        }

        hmsFusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) onUpdate(LocusResult.success(location)) else startUpdates()
        }.addOnFailureListener {
            logError(it)
            logDebug("Looks like last known location is not available in Huawei Mobile Service, requesting a new location update")
            startUpdates()
        }
    }

    /**
     * Stops location tracking by removing location callback from FusedLocationProviderClient
     * */
    internal fun stopUpdates() {
        logDebug("Stopping background location updates")
        isRequestOngoing.set(false)
        locationLiveData = MutableLiveData()
        gmsFusedLocationProviderClient.removeLocationUpdates(pendingIntent)
        hmsFusedLocationProviderClient.removeLocationUpdates(pendingIntent)
    }
}

class EmptyCancellationToken : CancellationToken() {
    override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken = this

    override fun isCancellationRequested(): Boolean = false
}
