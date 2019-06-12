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

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.util.Log
import com.google.android.gms.location.LocationRequest


/*
 * Created by Birju Vachhani on 09 November 2018
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Marker class for Locus Extensions
 * */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY)
internal annotation class LocusMarker

/**
 * A helper class for location extension which provides dsl extensions for getting location
 * */
@LocusMarker
class Locus(func: LocationOptions.() -> Unit = {}) {

    private var options = LocationOptions()

    init {
        configure(func)
    }

    /**
     * This class is used to create a default LocationRequest object if not provided externally
     * */
    private object Defaults {
        internal const val INTERVAL_IN_MS = 1000L
        internal const val FASTEST_INTERVAL_IN_MS = 1000L
        internal const val MAX_WAIT_TIME_IN_MS = 1000L
    }

    /**
     * Initializes Locus class with default LocationOptions or user specific if provided externally
     * */
    init {
        options.locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        options.locationRequest.interval = Defaults.INTERVAL_IN_MS
        options.locationRequest.fastestInterval = Defaults.FASTEST_INTERVAL_IN_MS
        options.locationRequest.maxWaitTime = Defaults.MAX_WAIT_TIME_IN_MS
    }

    /**
     * creates LocationOptions object from user configuration
     * @param func is a lambda receiver for LocationOptions which is used to build LocationOptions object
     * */
    fun configure(func: LocationOptions.() -> Unit) {
        func(options)
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun getCurrentLocation(activity: FragmentActivity, func: Location.() -> Unit): BlockExecution {
        val helper = getOrInitLocationHelper(activity.supportFragmentManager, true)
        val blockExecution = BlockExecution()
        helper.reset()
        helper.locationLiveData.watch(activity) { locus ->
            when (locus) {
                is LocusResult.Success -> {
                    func(locus.location)
                }
                is LocusResult.Failure -> {
                    blockExecution(locus.error)
                }
            }
        }
        Handler().post { helper.initPermissionModel() }
        return blockExecution
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun getCurrentLocation(fragment: Fragment, func: Location.() -> Unit): BlockExecution {
        val helper = getOrInitLocationHelper(fragment.childFragmentManager, true)
        val blockExecution = BlockExecution()
        helper.reset()
        helper.locationLiveData.watch(fragment) { locus ->
            when (locus) {
                is LocusResult.Success -> {
                    func(locus.location)
                }
                is LocusResult.Failure -> {
                    blockExecution(locus.error)
                }
            }
        }
        Handler().post { helper.initPermissionModel() }
        return blockExecution
    }

    /**
     * This function is used to get location updates continuously. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun listenForLocation(activity: FragmentActivity, func: Location.() -> Unit): BlockExecution {
        val helper = getOrInitLocationHelper(activity.supportFragmentManager)
        val blockExecution = BlockExecution()
        helper.reset()
        helper.locationLiveData.watch(activity) { locus ->
            when (locus) {
                is LocusResult.Success -> {
                    func(locus.location)
                }
                is LocusResult.Failure -> {
                    blockExecution(locus.error)
                }
            }
        }
        Handler().post { helper.initPermissionModel() }
        return blockExecution
    }

    /**
     * This function is used to get location updates continuously. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun listenForLocation(fragment: Fragment, func: Location.() -> Unit): BlockExecution {
        val helper = getOrInitLocationHelper(fragment.childFragmentManager)
        val blockExecution = BlockExecution()
        helper.reset()
        helper.locationLiveData.watch(fragment) { locus ->
            when (locus) {
                is LocusResult.Success -> {
                    func(locus.location)
                }
                is LocusResult.Failure -> {
                    blockExecution(locus.error)
                }
            }
        }
        Handler().post { helper.initPermissionModel() }
        return blockExecution
    }

    /**
     * Getter method to get an Instance of LocationHelper. It always return an existing instance if there's any,
     * otherwise creates a new instance.
     * @return Instance of LocationHelper class which can be used to initiate Location Retrieval process.
     * */
    private fun getOrInitLocationHelper(manager: FragmentManager, isOneTime: Boolean = false): LocationHelper {
        var helper = manager.findFragmentByTag(LocationHelper.TAG) as? LocationHelper
        if (helper == null) {
            Log.e("LocationHelper", "No instance found so creating new")
            helper = LocationHelper.newInstance(options)
            helper.arguments = Bundle().apply {
                putBoolean(Constants.IS_ONE_TIME_BUNDLE_KEY, isOneTime)
            }
            manager.beginTransaction().add(helper, LocationHelper.TAG).commitNow()
        }
        return helper
    }

    /**
     * This function is used to stop receiving location updates.
     * */
    fun stopTrackingLocation(fragment: Fragment) {
        getOrInitLocationHelper(fragment.childFragmentManager).stopContinuousLocation()
    }

    /**
     * This function is used to stop receiving location updates.
     * */
    fun stopTrackingLocation(activity: FragmentActivity) {
        getOrInitLocationHelper(activity.supportFragmentManager).stopContinuousLocation()
    }
}

