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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

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
class Locus(func: Configuration.() -> Unit = {}) {

    private val options = Configuration()
    private lateinit var locationprovider: LocationProvider
    private var isOneTime: Boolean = false
    private val permissionBroadcastReceiver = PermissionBroadcastReceiver()

    init {
        configure(func)
    }

    /**
     * creates Configuration object from user configuration
     * @param func is a lambda receiver for Configuration which is used to build Configuration object
     * */
    fun configure(func: Configuration.() -> Unit) {
        func(options)
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun getCurrentLocation(activity: FragmentActivity, func: Location.() -> Unit): BlockExecution {
        initLocationProvider(activity)
        isOneTime = true
        val blockExecution = BlockExecution()

        /*val helper = getOrInitLocationHelper(activity.supportFragmentManager, true)
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
        Handler().post { helper.initPermissionModel() }*/
        return blockExecution
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    /*fun getCurrentLocation(fragment: Fragment, func: Location.() -> Unit): BlockExecution {
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
    }*/

    /**
     * This function is used to get location updates continuously. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    /*fun listenForLocation(activity: FragmentActivity, func: Location.() -> Unit): BlockExecution {
        initLocationProvider(activity)
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
    }*/

    private fun initLocationProvider(context: Context) {
        if (!::locationprovider.isInitialized) {
            locationprovider = LocationProvider(context)
        }
    }

    /**
     * This function is used to get location updates continuously. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    /*fun listenForLocation(fragment: Fragment, func: Location.() -> Unit): BlockExecution {
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
    }*/


    /**
     * This function is used to stop receiving location updates.
     * */
    fun stopTrackingLocation(fragment: Fragment) {
        // TODO
    }

    /**
     * This function is used to stop receiving location updates.
     * */
    fun stopTrackingLocation(activity: FragmentActivity) {
        // TODO
    }

    inner class PermissionBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            val status = intent?.getStringExtra(Constants.INTENT_EXTRA_PERMISSION_RESULT) ?: return
            when (status) {
                "granted" -> {
                    // TODO: start location updates
                }
                "denied" -> {
                    // TODO: permission denied, let the user know
                }
                "resolution_failed" -> {
                    // TODO: resolution failed. Do something!
                }
            }
            // TODO: process received state of permission request
            // TODO: determine if needs to be unregistered
            LocalBroadcastManager.getInstance(context).unregisterReceiver(permissionBroadcastReceiver)
        }
    }
}

