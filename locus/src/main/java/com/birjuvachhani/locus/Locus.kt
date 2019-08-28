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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
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
 * A helper class for location extension which provides
 * dsl extensions for getting location
 *
 * @property options Configuration holds the configuration of the library
 *
 * @property locationProvider LocationProvider is used to retrieve location
 *
 * @property permissionBroadcastReceiver PermissionBroadcastReceiver
 * receives all the permission broadcast from the [LocusActivity]
 *
 * @property runnable Runnable? holds references to the LiveData observers
 * that needs to be executed after starting the location updates in order
 * to not miss the location updates.
 */
@LocusMarker
class Locus(func: Configuration.() -> Unit = {}) {
    private var options = Configuration()
    private lateinit var locationProvider: LocationProvider
    private val permissionBroadcastReceiver = PermissionBroadcastReceiver()
    private var runnable: Runnable? = null

    companion object {

        /**
         * Enables logging for this library
         *
         * Most of the events are logged on debug level and errors are logged on error level.
         *
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
     *
     * @param func is a lambda receiver for Configuration which is used
     * to build Configuration object
     * */
    fun configure(func: Configuration.() -> Unit) {
        func(options)
    }

    /**
     * Starts location updates that be used while the app is in background.
     *
     * The problem with the FusedLocationProviderClient is that when we provide
     * a LocationCallback instance to receive location updates, it stops receiving
     * updates after some time when app goes in background. This use case prevents
     * from getting location updates in services and while the app is in background.
     *
     * The preferable workaround is to use PendingIntent instead of using LocationCallback
     * to receive location updates. This way works perfectly even when the app is in background.
     * Therefore, this method is expected to be used in services and in cases where background
     * location updates are needed.
     *
     * @param context Context is the Android context
     *
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> lambda block
     * will be executed on location process result
     *
     * @return BlockExecution which can be used to register failure callback
     */
    fun startBackgroundLocationUpdates(
        context: Context,
        func: Location.() -> Unit
    ): BlockExecution {
        initLocationProvider(context)
        val blockExecution = BlockExecution()
        runnable = Runnable {
            backgroundLocationLiveData.observeForever { result ->
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
        checkAndStartBackgroundUpdates(context)
        return blockExecution
    }

    /**
     * Initiates location retrieval process to receive only one location result.
     *
     * This method internally handles runtime location permission which is needed
     * for Android M and above. It also handles rationale dialogs and permission
     * blocked dialogs also. These dialogs can be configured using [configure] method.
     * It also handles location resolution process for requested location settings.
     * It shows setting resolution dialog if needed and ask for user's permission to
     * change location settings. Please note that these success and failure callbacks
     * are lifecycle aware so no updates will be dispatched if the Activity
     * is not in right state.
     *
     * @param activity FragmentActivity is the Activity instance from where
     * the location request is triggered
     *
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides
     * a success block which will grant access to retrieved location
     *
     * @return BlockExecution that can be used to handle exceptions and failures
     * during location retrieval process
     */
    fun getCurrentLocation(activity: FragmentActivity, func: Location.() -> Unit): BlockExecution {
        initLocationProvider(activity)
        val blockExecution = observeOneTimeUpdates(activity, func)

        checkAndStartUpdates(activity)
        return blockExecution
    }

    /**
     * Initiates location retrieval process to receive only one location result.
     *
     * This method internally handles runtime location permission which is needed
     * for Android M and above. It also handles rationale dialogs and permission
     * blocked dialogs also. These dialogs can be configured using [configure] method.
     * It also handles location resolution process for requested location settings.
     * It shows setting resolution dialog if needed and ask for user's permission to
     * change location settings. Please note that these success and failure callbacks
     * are lifecycle aware so no updates will be dispatched if the Fragment is not
     * in right state.
     *
     * @param fragment Fragment is the Fragment from which the location request is initiated
     *
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides
     * a success block which will grant access to retrieved location
     *
     * @return BlockExecution that can be used to handle exceptions and
     * failures during location retrieval process
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
     * This method internally handles runtime location permission which is needed
     * for Android M and above. It also handles rationale dialogs and permission blocked
     * dialogs also. These dialogs can be configured using [configure] method.
     * It also handles location resolution process for requested location settings.
     * It shows setting resolution dialog if needed and ask for user's permission to
     * change location settings. Please note that these success and failure callbacks
     * are lifecycle aware so no updates will be dispatched if the Activity
     * is not in right state.
     *
     * @param activity FragmentActivity is the Activity instance from where
     * the location request is triggered
     *
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides
     * a success block which will grant access to retrieved location
     *
     * @return BlockExecution that can be used to handle exceptions and failures
     * during location retrieval process
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
     * This method internally handles runtime location permission which is needed
     * for Android M and above. It also handles rationale dialogs and permission
     * blocked dialogs also. These dialogs can be configured using [configure] method.
     * It also handles location resolution process for requested location settings.
     * It shows setting resolution dialog if needed and ask for user's permission to
     * change location settings. Please note that these success and failure callbacks
     * are lifecycle aware so no updates will be dispatched if the Fragment
     * is not in right state.
     *
     * @param fragment Fragment is the Fragment from which the
     * location request is initiated
     *
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides
     * a success block which will grant access to retrieved location
     *
     * @return BlockExecution that can be used to handle exceptions and failures
     * during location retrieval process
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
     * Initiates location retrieval process to receive one time location
     * update without executing permission model.
     *
     * This method can be used to receive one time location update from services
     * or from background. It doesn't handle location permission model so before
     * calling this method, ensure that the location permission is granted.
     *
     * @param context context used to initialize FusedLocationProviderClient
     *
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides
     * a success block which will grant access to retrieved location
     *
     * @return BlockExecution that can be used to handle exceptions and failures
     * during location retrieval process
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    fun fetchCurrentLocation(context: Context, func: Location.() -> Unit): BlockExecution {
        initLocationProvider(context)
        val blockExecution = BlockExecution()
        runnable = Runnable {
            locationProvider.locationLiveData.observeOnce { result ->
                when (result) {
                    is LocusResult.Success -> {
                        func(result.location)
                        locationProvider.stopContinuousLocation()
                    }
                    is LocusResult.Failure -> {
                        blockExecution(result.error)
                        logError(result.error)
                    }
                }
                locationProvider.stopContinuousLocation()
            }
        }
        checkAndStartUpdatesWithoutPermissionRequest(context)
        return blockExecution
    }

    /**
     * Initiates location retrieval process to receive continuous location
     * updates without executing permission model.
     *
     * This method can be used to receive location updates from services or
     * from background. It doesn't handle location permission model so before
     * calling this method, ensure that the location permission is granted.
     * Also, it doesn't observe location updates in lifecycle aware way.
     * So don't forget to stop updates manually when needed to be stopped.
     *
     * @param context context is the Fragment from which the
     * location request is initiated
     *
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides
     * a success block which will grant access to retrieved location
     *
     * @return BlockExecution that can be used to handle exceptions and failures
     * during location retrieval process
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    fun observeLocationUpdates(context: Context, func: Location.() -> Unit): BlockExecution {
        initLocationProvider(context)
        val blockExecution = BlockExecution()
        runnable = Runnable {
            locationProvider.locationLiveData.observeForever { result ->
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
        checkAndStartUpdatesWithoutPermissionRequest(context)
        return blockExecution
    }

    /**
     * Registers an observer on location results that observes continuously.
     * This observation is done in lifecycle aware way. That means that no updates
     * will be dispatched if if it is not the right lifecycle state.
     *
     * @param owner LifecycleOwner is the owner of the lifecycle that will be
     * used to observe on location results
     *
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> will
     * called when a new result is available
     *
     * @return BlockExecution provides a way to handle errors and exceptions
     * occurred during this process
     */
    private fun observeForContinuesUpdates(owner: LifecycleOwner, func: Location.() -> Unit): BlockExecution {
        val blockExecution = BlockExecution()
        runnable = Runnable {
            locationProvider.locationLiveData.removeObservers(owner)
            locationProvider.locationLiveData.watch(owner) { result ->
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
     * Checks for location permission and then initiates permission model
     * if the location permission is not granted already.
     * @param context Context is the Android Context
     */
    private fun checkAndStartUpdatesWithoutPermissionRequest(context: Context) {
        if (hasLocationPermission(context) && isSettingsEnabled(context)) {
            locationProvider.startContinuousLocation(options.locationRequest)
            runnable?.run()
            runnable = null
        } else {
            locationProvider.locationLiveData.postValue(LocusResult.Failure(Throwable("Cannot continue without location permission")))
        }
    }

    /**
     * Checks for location permission and then initiates permission model
     * if the location permission is not granted already.
     * @param context Context is the Android Context
     */
    private fun checkAndStartUpdates(context: Context) {
        if (hasLocationPermission(context) && isSettingsEnabled(context)) {
            locationProvider.startContinuousLocation(options.locationRequest)
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
     * Checks for location permission and then initiates permission model
     * if the location permission is not granted already.
     * @param context Context is the Android Context
     */
    private fun checkAndStartBackgroundUpdates(context: Context) {
        if (hasLocationPermission(context) && isSettingsEnabled(context)) {
            locationProvider.startBackgroundLocationUpdates(context, options.locationRequest)
            runnable?.run()
            runnable = null
        } else {
            LocalBroadcastManager
                .getInstance(context)
                .registerReceiver(permissionBroadcastReceiver, IntentFilter(context.packageName))
            initPermissionRequestForBackgroundUpdates(context)
        }
    }

    /**
     * Registers an observer on location results that observes only for one time.
     * This observation is done in lifecycle aware way. That means that no updates
     * will be dispatched if if it is not the right lifecycle state.
     * @param owner LifecycleOwner is the owner of the lifecycle that will be
     * used to observe on location results
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> will
     * called when a new result is available
     * @return BlockExecution provides a way to handle errors and exceptions
     * occurred during this process
     */
    private fun observeOneTimeUpdates(owner: LifecycleOwner, func: Location.() -> Unit): BlockExecution {
        val blockExecution = BlockExecution()

        runnable = Runnable {
            locationProvider.locationLiveData.removeObservers(owner)
            locationProvider.locationLiveData.observeOnce(owner) { result ->
                when (result) {
                    is LocusResult.Success -> {
                        func(result.location)
                    }
                    is LocusResult.Failure -> {
                        blockExecution(result.error)
                        logError(result.error)
                    }
                }
                locationProvider.stopContinuousLocation()
            }
        }
        return blockExecution
    }

    /**
     * Initiates permission request by starting [LocusActivity] which
     * is responsible to handle permission model and location settings resolution
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
     * Initiates permission request by starting [LocusActivity] which
     * is responsible to handle permission model and location settings resolution
     * @param context Context is the Android Context used to start the [LocusActivity]
     */
    private fun initPermissionRequestForBackgroundUpdates(context: Context) {
        if (!isRequestingPermission.getAndSet(true)) {
            val intent = Intent(context, LocusActivity::class.java).apply {
                if (options.shouldResolveRequest) {
                    putExtra(Constants.INTENT_EXTRA_CONFIGURATION, options)
                    putExtra(Constants.INTENT_EXTRA_IS_BACKGROUND, true)
                }
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            showPermissionNotification(context, pendingIntent)
        } else {
            logDebug("A request is already ongoing")
        }
    }

    /**
     * Displays location permission notification when location permission is not granted and
     * background location updates is requested.
     * @param context Context is the Android context
     * @param pendingIntent PendingIntent will be used to open permission activity
     */
    private fun showPermissionNotification(context: Context, pendingIntent: PendingIntent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel("permission_channel", "Permission Channel", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        with(NotificationCompat.Builder(context, "permission_channel")) {
            setContentTitle("Require Location Permission")
            setContentText("This feature requires location permission to access device location. Please allow to access device location")
            setSmallIcon(R.drawable.ic_location_on)
            addAction(NotificationCompat.Action.Builder(0, "Grant", pendingIntent).build())
            setPriority(NotificationManager.IMPORTANCE_HIGH)
            setAutoCancel(true)
            manager.notify(865, build())
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
        if (!::locationProvider.isInitialized) {
            locationProvider = LocationProvider(context)
        }
    }

    /**
     * Stops the ongoing location retrieval process.
     *
     * Note that this only stops location updates, it cannot stop
     * ongoing permission request.
     *
     * @param owner LifecycleOwner is the same  Lifecycle owner that
     * is used to start these location updates
     */
    fun stopTrackingLocation(owner: LifecycleOwner) {
        locationProvider.locationLiveData.removeObservers(owner)
        locationProvider.stopContinuousLocation()
    }

    /**
     * Stops the ongoing location retrieval process.
     * Note that this only stops location updates, it cannot stop ongoing permission request.
     */
    fun stopObservingLocation() {
        locationProvider.stopContinuousLocation()
    }

    /**
     * Stops the ongoing location retrieval process.
     * Note that this only stops location updates, it cannot stop ongoing permission request.
     */
    fun stopBackgroundLocation() {
        locationProvider.stopContinuousLocation()
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
            val isBackground = intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_BACKGROUND, false)
            isRequestingPermission.set(false)
            runnable?.run()
            runnable = null
            when (status) {
                Constants.GRANTED -> {
                    initLocationProvider(context)
                    if (isBackground) {
                        locationProvider.startBackgroundLocationUpdates(
                            context.applicationContext,
                            options.locationRequest
                        )
                    } else {
                        locationProvider.startContinuousLocation(options.locationRequest)
                    }
                    logDebug("Permission granted")
                }
                else -> {
                    locationProvider.locationLiveData.postValue(LocusResult.Failure(Throwable(status)))
                    logDebug(status)
                }
            }
            LocalBroadcastManager.getInstance(context).unregisterReceiver(permissionBroadcastReceiver)
        }
    }
}

