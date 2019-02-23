/*
 * Copyright 2019 Birju Vachhani (https://github.com/BirjuVachhani)
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

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.location.Location
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import com.google.android.gms.location.LocationRequest


/**
 * Created by Birju Vachhani on 09-11-2018.
 */

/**
 * Marker class for Locus Extensions
 * */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY)
internal annotation class LocusMarker

/**
 * A helper class for location extension which provides dsl extensions for getting location
 * @param func provides a function block to configure dialogs and LocationRequest object
 * */
@LocusMarker
class Locus(func: LocationOptions.() -> Unit = {}) {

    private var options = LocationOptions()

    private var fragmentManager: FragmentManager? = null

    private var locationHelper: LocationHelper? = null

    constructor(activity: FragmentActivity, func: LocationOptions.() -> Unit = {}) : this(func) {
        fragmentManager = activity.supportFragmentManager
    }

    constructor(fragment: Fragment, func: LocationOptions.() -> Unit = {}) : this(func) {
        fragment.lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onFragmentManagerAvailable() {
                fragmentManager = fragment.fragmentManager
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onFragmentManagerDestroyed() {
                fragmentManager = null
            }
        })
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
        configure(func)
    }

    /**
     * creates LocationOptions object from user configuration
     * @param func is a lambda receiver for LocationOptions which is used to build LocationOptions object
     * */
    private fun configure(func: LocationOptions.() -> Unit) {
        func(options)
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun getCurrentLocation(owner: LifecycleOwner, func: Location.() -> Unit): BlockExecution {
        initLocationHelper(true)
        val blockExecution = BlockExecution()
        locationHelper?.locationLiveData?.watch(owner) { locus ->
            when (locus) {
                is LocusResult.Success -> {
                    func(locus.location)
                }
                is LocusResult.Failure -> {
                    blockExecution(locus.error)
                }
            }
        }
        return blockExecution
    }

    /**
     * This function is used to get location updates continuously. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun listenForLocation(owner: LifecycleOwner, func: Location.() -> Unit): BlockExecution {
        initLocationHelper()
        val blockExecution = BlockExecution()
        locationHelper?.locationLiveData?.watch(owner) { locus ->
            when (locus) {
                is LocusResult.Success -> {
                    func(locus.location)
                }
                is LocusResult.Failure -> {
                    blockExecution(locus.error)
                }
            }
        }
        return blockExecution
    }

    /**
     * Getter method to get an Instance of LocationHelper. It always return an existing instance if there's any,
     * otherwise creates a new instance.
     * @return Instance of LocationHelper class which can be used to initiate Location Retrieval process.
     * */
    private fun initLocationHelper(isOneTime: Boolean = false) {
        locationHelper = fragmentManager?.findFragmentByTag(LocationHelper.TAG) as? LocationHelper
        if (locationHelper == null) {
            locationHelper = LocationHelper.newInstance(options)
            locationHelper?.let { helper ->
                helper.arguments = Bundle().apply {
                    putBoolean(Constants.IS_ONE_TIME_BUNDLE_KEY, isOneTime)
                }
                fragmentManager?.beginTransaction()
                    ?.add(helper, LocationHelper.TAG)
                    ?.commit()
            }
        } else {
            locationHelper?.initPermissionModel()
        }
    }

    /**
     * This function is used to stop receiving location updates.
     * */
    fun stopTrackingLocation() {
        locationHelper?.stopContinuousLocation()
    }
}

