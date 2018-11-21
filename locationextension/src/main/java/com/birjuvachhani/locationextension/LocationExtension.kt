/*
 * Copyright 2018 BirjuVachhani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.birjuvachhani.locationextension

import android.location.Location
import android.support.v4.app.FragmentActivity
import com.google.android.gms.location.LocationRequest

/**
 * Created by Birju Vachhani on 09-11-2018.
 */


/**
 * A helper class for location extension which provides dsl extensions for getting location
 *
 * @param func provides a function block to configure dialogs and LocationRequest object
 * @param activity is used to display dialog and to initiate the helper class for location
 *
 * */
class GeoLocation(private val activity: FragmentActivity, func: LocationOptions.() -> Unit = {}) {

    private var options = LocationOptions()

    /**
     * This class is used to create a default LocationRequest object if not provided externally
     * */
    private object Defaults {
        internal const val INTERVAL_IN_MS = 1000L
        internal const val FASTEST_INTERVAL_IN_MS = 1000L
        internal const val MAX_WAIT_TIME_IN_MS = 1000L
        internal const val NUMBER_OF_UPDATES = 1
    }

    /**
     * Initializes GeoLocation class with default LocationOptions or user specific if provided externally
     * */
    init {
        options.locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        options.locationRequest.interval = Defaults.INTERVAL_IN_MS
        options.locationRequest.fastestInterval = Defaults.FASTEST_INTERVAL_IN_MS
        options.locationRequest.numUpdates = Defaults.NUMBER_OF_UPDATES
        options.locationRequest.maxWaitTime = Defaults.MAX_WAIT_TIME_IN_MS
        options(func)
    }

    /**
     * creates LocationOptions object from user configuration
     *
     * @param func is a lambda receiver for LocationOptions which is used to build LocationOptions object
     *
     * */
    private fun options(func: LocationOptions.() -> Unit) {
        val locationOptions = LocationOptions()
        func.invoke(locationOptions)
        locationOptions.request.invoke(locationOptions.locationRequest)
        this.options = locationOptions
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     *
     * @param success is a function which will be called when location is retrieved successfully.
     *
     * */
    fun getCurrentLocation(success: (Location) -> Unit) {
        getLocationHelper(activity).apply {
            startLocationProcess(options, success, {}, true)
        }
    }

    /**
     * This function is used to get location for one time only.
     * It also provides mechanism to handle errors externally.
     *
     * @param success is a function which will be called when location is retrieved successfully.
     *
     * @param failure is a function which will be called when user denies location permission
     * or there's any error while retrieving location.
     *
     * */
    fun getCurrentLocation(
        success: (Location) -> Unit,
        failure: (LocationError) -> Unit
    ) {
        getLocationHelper(activity).apply {
            startLocationProcess(options, success, failure, true)
        }
    }

    /**
     * This function is used to get location updates continuously. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     *
     * @param success is a function which will be called everytime when there's new update for location
     *
     * */
    fun listenForLocation(success: (Location) -> Unit) {
        getLocationHelper(activity).apply {
            startLocationProcess(options, success, {}, false)
        }
    }

    /**
     * This function is used to get location updates continuously. It handles most of the errors internally and also
     * provides mechanism to handle errors externally.
     *
     * @param success is a function which will be called every time when there's new update for location
     *
     * @param failure is a function which will be called when user denies location permission
     * or there's any error while retrieving location.
     *
     * */
    fun listenForLocation(
        success: (Location) -> Unit,
        failure: (error: LocationError) -> Unit
    ) {
        getLocationHelper(activity).apply {
            startLocationProcess(options, success, failure, false)
        }
    }

    /**
     * Getter method to get an Instance of LocationHelper. It always return an existing instance if there's any,
     * otherwise creates a new instance.
     *
     * @param activity is used to initiate LocationHelper instance.
     *
     * @return Instance of LocationHelper class which can be used to initiate Location Retrieval process.
     * */
    private fun getLocationHelper(activity: FragmentActivity): LocationHelper {
        val frag = activity.supportFragmentManager.findFragmentByTag(LocationHelper.TAG)
        return if (frag == null) {
            val mLocationHelper = LocationHelper.newInstance()
            activity.supportFragmentManager.beginTransaction()
                .add(mLocationHelper, LocationHelper.TAG)
                .commitNow()
            mLocationHelper
        } else {
            frag as LocationHelper
        }
    }

    /**
     * This function is used to stop receiving location updates.
     *
     * */
    fun stopTrackingLocation() {
        getLocationHelper(activity).stopContinuousLocation()
    }
}

/**
 * Data class to store location related configurations which includes dialog messages and instance of LocationRequest
 * class.
 *
 * */
data class LocationOptions(
    var rationaleText: String = "Location permission is required in order to use this feature properly.Please grant the permission.",
    var blockedText: String = "Location permission is blocked. Please allow permission from settings screen to use this feature",
    var request: LocationRequest.() -> Unit = {},
    internal var locationRequest: LocationRequest = LocationRequest()
)