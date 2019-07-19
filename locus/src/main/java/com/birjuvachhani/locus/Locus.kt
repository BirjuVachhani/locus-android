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
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Created by Birju Vachhani on 09 November 2018
 * Copyright © 2019 locus-android. All rights reserved.
 */

internal val isRequestingPermission = AtomicBoolean().apply {
    set(false)
}

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
    private val permissionBroadcastReceiver = PermissionBroadcastReceiver()
    private var runnable: Runnable? = null

    companion object {
        fun setLogging(shouldLog: Boolean) {
            isLoggingEnabled = shouldLog
        }
    }

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
    fun <T> getCurrentLocation(
        contextAndLifecycleOwner: T,
        func: Location.() -> Unit
    ): BlockExecution where T : Context, T : LifecycleOwner {
        initLocationProvider(contextAndLifecycleOwner)
        val blockExecution = observeOneTimeUpdates(contextAndLifecycleOwner, func)

        checkAndStartUpdates(contextAndLifecycleOwner)
        return blockExecution
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun getCurrentLocation(fragment: Fragment, func: Location.() -> Unit): BlockExecution {
        if (!fragment.isAdded || fragment.activity == null) {
            Log.e("Locus", "Cannot start location updates, Fragment is not attached yet.")
            return BlockExecution()
        }
        initLocationProvider(fragment.requireContext())
        val blockExecution = observeOneTimeUpdates(fragment, func)

        checkAndStartUpdates(fragment.requireContext())
        return blockExecution
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun listenForLocation(activity: FragmentActivity, func: Location.() -> Unit): BlockExecution {
        initLocationProvider(activity)
        val blockExecution = observeForContinuesUpdates(activity, func)

        checkAndStartUpdates(activity)
        return blockExecution
    }

    /**
     * This function is used to get location for one time only. It handles most of the errors internally though
     * it doesn't any mechanism to handle errors externally.
     * */
    fun listenForLocation(fragment: Fragment, func: Location.() -> Unit): BlockExecution {
        if (!fragment.isAdded || fragment.activity == null) {
            Log.e("Locus", "Cannot start location updates, Fragment is not attached yet.")
            return BlockExecution()
        }
        initLocationProvider(fragment.requireContext())
        val blockExecution = observeForContinuesUpdates(fragment, func)

        checkAndStartUpdates(fragment.requireContext())
        return blockExecution
    }

    private fun observeForContinuesUpdates(owner: LifecycleOwner, func: Location.() -> Unit): BlockExecution {
        val blockExecution = BlockExecution()
        runnable = Runnable {
            locationprovider.locationLiveData.removeObservers(owner)
            locationprovider.locationLiveData.watch(owner) { result ->
                when (result) {
                    is LocusResult.Success -> {
                        func(result.location)
                    }
                    is LocusResult.Failure -> {
                        blockExecution(result.error)
                        logError(result.error)
                    }
                }
            }
        }
        return blockExecution
    }


    private fun checkAndStartUpdates(context: Context) {
        if (hasLocationPermission(context) && isSettingsEnabled(context)) {
            locationprovider.startContinuousLocation()
            runnable?.run()
            runnable = null
        } else {
            LocalBroadcastManager
                .getInstance(context)
                .registerReceiver(permissionBroadcastReceiver, IntentFilter(context.packageName))
            initPermissionRequest(context)
        }
    }

    private fun observeOneTimeUpdates(owner: LifecycleOwner, func: Location.() -> Unit): BlockExecution {
        val blockExecution = BlockExecution()

        runnable = Runnable {
            locationprovider.locationLiveData.removeObservers(owner)
            locationprovider.locationLiveData.watch(owner) { result ->
                when (result) {
                    is LocusResult.Success -> {
                        func(result.location)
                    }
                    is LocusResult.Failure -> {
                        blockExecution(result.error)
                        logError(result.error)
                    }
                }
                locationprovider.stopContinuousLocation()
                locationprovider.locationLiveData.removeObservers(owner)
            }
        }
        return blockExecution
    }

    private fun initPermissionRequest(context: Context) {
        if (!isRequestingPermission.getAndSet(true)) {
            context.startActivity(Intent(context, LocusActivity::class.java).apply {
                if (options.shouldResolveRequest) {
                    putExtra(Constants.INTENT_EXTRA_CONFIGURATION, options)
                }
            })
        } else {
            logDebug("A request is already ongoing")
        }
    }

    private fun hasLocationPermission(context: Context) = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Checks whether the device location settings match with what the user requested
     * @return true is the current location settings satisfies the requirement, false otherwise.
     * */
    private fun isSettingsEnabled(context: Context): Boolean {
        if (!options.shouldResolveRequest) return true
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun initLocationProvider(context: Context) {
        if (!::locationprovider.isInitialized) {
            locationprovider = LocationProvider(context)
        }
    }

    /**
     * This function is used to stop receiving location updates.
     * */
    fun stopTrackingLocation(fragment: Fragment) {
        locationprovider.locationLiveData.removeObservers(fragment)
        locationprovider.stopContinuousLocation()
    }

    /**
     * This function is used to stop receiving location updates.
     * */
    fun stopTrackingLocation(activity: FragmentActivity) {
        if (::locationprovider.isInitialized) {
            locationprovider.locationLiveData.removeObservers(activity)
            locationprovider.stopContinuousLocation()
        }
    }

    inner class PermissionBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            logDebug("Received Permission broadcast")
            val status = intent?.getStringExtra(Constants.INTENT_EXTRA_PERMISSION_RESULT) ?: return
            isRequestingPermission.set(false)
            when (status) {
                "granted" -> {
                    initLocationProvider(context)
                    locationprovider.startContinuousLocation()
                    runnable?.run()
                    runnable = null
                    logDebug("Permission granted")
                }
                "denied" -> {
                    // TODO: permission denied, let the user know
                    logDebug("Permission denied")
                }
                "permanently_denied" -> {
                    // TODO: permission denied, let the user know
                    logDebug("Permission permanently denied")
                }
                "resolution_failed" -> {
                    // TODO: resolution failed. Do something!
                    logDebug("Location settings resolution failed")
                }
                "location_settings_denied" -> {
                    // TODO: user denied turn on location settings!
                    logDebug("User denied turn on location settings")
                }
            }
            LocalBroadcastManager.getInstance(context).unregisterReceiver(permissionBroadcastReceiver)
        }
    }
}

