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
    private var options = Configuration()
    private lateinit var locationprovider: LocationProvider
    private val permissionBroadcastReceiver = PermissionBroadcastReceiver()
    private var runnable: Runnable? = null

    companion object {

        /**
         * Enables logging for this library
         * @param shouldLog Boolean true to enable, false otherwise
         */
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
     * Initiates location retrieval process to receive only one location result.
     *
     * This method internally handles runtime location permission which is needed for Android M and above. It also handles rationale dialogs and permission blocked dialogs also. These dialogs can be configured using [configure] method. It also handles location resolution process for requested location settings. It shows setting resolution dialog if needed and ask for user's permission to change location settings. Please note that these success and failure callbacks are lifecycle aware so no updates will be dispatched if the Activity is not in right state.
     *
     * @param activity FragmentActivity is the Activity instance from where the location request is triggered
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides a success block which will grant access to retrieved location
     * @return BlockExecution that can be used to handle exceptions and failures during location retrieval process
     */
    fun <T> getCurrentLocation(
        activity: FragmentActivity,
        func: Location.() -> Unit
    ): BlockExecution where T : Context, T : LifecycleOwner {
        initLocationProvider(activity)
        val blockExecution = observeOneTimeUpdates(activity, func)

        checkAndStartUpdates(activity)
        return blockExecution
    }

    /**
     * Initiates location retrieval process to receive only one location result.
     *
     * This method internally handles runtime location permission which is needed for Android M and above. It also handles rationale dialogs and permission blocked dialogs also. These dialogs can be configured using [configure] method. It also handles location resolution process for requested location settings. It shows setting resolution dialog if needed and ask for user's permission to change location settings. Please note that these success and failure callbacks are lifecycle aware so no updates will be dispatched if the Fragment is not in right state.
     *
     * @param fragment Fragment is the Fragment from which the location request is initiated
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides a success block which will grant access to retrieved location
     * @return BlockExecution that can be used to handle exceptions and failures during location retrieval process
     */
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
     * Initiates location retrieval process to receive continuous location updates.
     *
     * This method internally handles runtime location permission which is needed for Android M and above. It also handles rationale dialogs and permission blocked dialogs also. These dialogs can be configured using [configure] method. It also handles location resolution process for requested location settings. It shows setting resolution dialog if needed and ask for user's permission to change location settings. Please note that these success and failure callbacks are lifecycle aware so no updates will be dispatched if the Activity is not in right state.
     *
     * @param activity FragmentActivity is the Activity instance from where the location request is triggered
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides a success block which will grant access to retrieved location
     * @return BlockExecution that can be used to handle exceptions and failures during location retrieval process
     */
    fun listenForLocation(activity: FragmentActivity, func: Location.() -> Unit): BlockExecution {
        initLocationProvider(activity)
        val blockExecution = observeForContinuesUpdates(activity, func)

        checkAndStartUpdates(activity)
        return blockExecution
    }

    /**
     * Initiates location retrieval process to receive continuous location updates.
     *
     * This method internally handles runtime location permission which is needed for Android M and above. It also handles rationale dialogs and permission blocked dialogs also. These dialogs can be configured using [configure] method. It also handles location resolution process for requested location settings. It shows setting resolution dialog if needed and ask for user's permission to change location settings. Please note that these success and failure callbacks are lifecycle aware so no updates will be dispatched if the Fragment is not in right state.
     *
     * @param fragment Fragment is the Fragment from which the location request is initiated
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides a success block which will grant access to retrieved location
     * @return BlockExecution that can be used to handle exceptions and failures during location retrieval process
     */
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

    /**
     * Registers an observer on location results that observes continuously. This observation is done in lifecycle aware way. That means that no updates will be dispatched if if it is not the right lifecycle state.
     * @param owner LifecycleOwner is the owner of the lifecycle that will be used to observe on location results
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> will called when a new result is available
     * @return BlockExecution provides a way to handle errors and exceptions occurred during this process
     */
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

    /**
     * Checks for location permission and then initiates permission model if the location permission is not granted already.
     * @param context Context is the Android Context
     */
    private fun checkAndStartUpdates(context: Context) {
        if (hasLocationPermission(context) && isSettingsEnabled(context)) {
            locationprovider.startContinuousLocation(options.locationRequest)
            runnable?.run()
            runnable = null
        } else {
            LocalBroadcastManager
                .getInstance(context)
                .registerReceiver(permissionBroadcastReceiver, IntentFilter(context.packageName))
            initPermissionRequest(context)
        }
    }

    /**
     * Registers an observer on location results that observes only for one time. This observation is done in lifecycle aware way. That means that no updates will be dispatched if if it is not the right lifecycle state.
     * @param owner LifecycleOwner is the owner of the lifecycle that will be used to observe on location results
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> will called when a new result is available
     * @return BlockExecution provides a way to handle errors and exceptions occurred during this process
     */
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

    /**
     * Initiates permission request by starting [LocusActivity] which is responsible to handle permission model and location settings resolution
     * @param context Context is the Android Context used to start the [LocusActivity]
     */
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

    /**
     * Checks whether the app has location permission or not
     * @param context Context is the Android Context used to request a check for location permission
     * @return Boolean true if the location permission is already granted, false otherwise
     */
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

    /**
     * Initializes [LocationProvider] which is responsible to start and stop location updates
     * @param context Context is the Android Context used to initialize [LocationProvider]
     */
    private fun initLocationProvider(context: Context) {
        if (!::locationprovider.isInitialized) {
            locationprovider = LocationProvider(context)
        }
    }

    /**
     * Stops the ongoing location retrieval process.
     *
     * Note that this only stops location updates, it cannot stop ongoing permission request.
     *
     * @param owner LifecycleOwner is the same  Lifecycle owner that is used to start these location updates
     */
    fun stopTrackingLocation(owner: LifecycleOwner) {
        locationprovider.locationLiveData.removeObservers(owner)
        locationprovider.stopContinuousLocation()
    }

    /**
     * Resets location configs to default
     */
    fun setDefaultConfig() {
        options = Configuration()
    }

    /**
     * Receives local broadcasts related to permission model
     */
    inner class PermissionBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            logDebug("Received Permission broadcast")
            val status = intent?.getStringExtra(Constants.INTENT_EXTRA_PERMISSION_RESULT) ?: return
            isRequestingPermission.set(false)
            when (status) {
                "granted" -> {
                    initLocationProvider(context)
                    locationprovider.startContinuousLocation(options.locationRequest)
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

