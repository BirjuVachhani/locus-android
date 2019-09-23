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

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.birjuvachhani.locus.Locus.config
import com.birjuvachhani.locus.Locus.locationProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Created by Birju Vachhani on 09 November 2018
 * Copyright © 2019 locus-android. All rights reserved.
 */

internal val isRequestingPermission = AtomicBoolean().apply {
    set(false)
}

/**
 * Holds Location updates
 */
internal var locationLiveData = MutableLiveData<LocusResult>()

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
 * @property config Configuration holds the configuration of the library
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
object Locus {
    private var config = Configuration()
    private lateinit var locationProvider: LocationProvider

    /**
     * Enables logging for this library
     *
     * Most of the events are logged on debug level and errors are logged on error level.
     *
     * @param shouldLog True to enable logs, false otherwise
     */
    fun setLogging(shouldLog: Boolean) {
        isLoggingEnabled = shouldLog
    }

    /**
     * creates Configuration object from user configuration
     *
     * @param func is a lambda receiver for Configuration which is used
     * to build Configuration object
     * */
    fun configure(func: Configuration.() -> Unit) {
        func(config)
    }

    /**
     * Starts location updates by verifying permissions and location settings
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
     * @return LiveData<LocusResult> which can be used to observe location updates
     */
    fun startLocationUpdates(context: Context): LiveData<LocusResult> {
        checkAndStartLocationUpdates(context.applicationContext)
        return locationLiveData
    }

    fun <T> startLocationUpdates(
        lifecycleOwnerContext: T,
        onResult: (LocusResult) -> Unit
    ) where T : Context, T : LifecycleOwner {
        locationLiveData.observe(lifecycleOwnerContext, Observer(onResult))
        checkAndStartLocationUpdates(lifecycleOwnerContext.applicationContext)
    }

    private fun checkAndStartLocationUpdates(
        context: Context,
        singleUpdate: ((LocusResult) -> Unit)? = null
    ) {
        val receiver = PermissionBroadcastReceiver {
            it?.let { error ->
                singleUpdate?.let {
                    it(LocusResult.error(error))
                } ?: locationLiveData.postValue(LocusResult.error(it))
            } ?: startUpdates(context, singleUpdate)
        }
        if (!getAllPermissions(config.enableBackgroundUpdates).all(context::hasPermission)) {
            // Doesn't have permission, start permission resolution
            startPermissionAndResolutionProcess(context, receiver, singleUpdate != null)
        } else if (config.shouldResolveRequest) {
            // has permissions, need to check for location settings
            checkLocationSettings(context) { isSatisfied ->
                if (isSatisfied) {
                    logDebug("Location settings are satisfied")
                    startUpdates(context, singleUpdate)
                } else {
                    logDebug("Location settings are not satisfied")
                    startPermissionAndResolutionProcess(context, receiver, singleUpdate != null)
                }
            }
        } else {
            // has permission but location settings resolution is disabled so start updates directly
            startUpdates(context, singleUpdate)
        }
    }

    private fun checkLocationSettings(context: Context, onResult: (Boolean) -> Unit) {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(config.locationRequest)
        builder.setAlwaysShow(true)
        val client = LocationServices.getSettingsClient(context)
        val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnSuccessListener {
            it?.locationSettingsStates
            onResult(true)
        }.addOnFailureListener { exception ->
            logError(exception)
            onResult(false)
        }
    }

    private fun startUpdates(context: Context, singleUpdate: ((LocusResult) -> Unit)? = null) {
        initLocationProvider(context.applicationContext)
        if (singleUpdate == null) {
            locationProvider.startUpdates(config.locationRequest)
        } else {
            locationProvider.getSingleUpdate(config.locationRequest, singleUpdate)
        }
    }

    private fun startPermissionAndResolutionProcess(
        context: Context,
        receiver: PermissionBroadcastReceiver,
        isOneTime: Boolean = false
    ) {
        if (isRequestingPermission.getAndSet(true)) {
            logDebug("A request is already ongoing")
            return
        }
        val intent = getLocationActivityIntent(context, isOneTime)
        LocalBroadcastManager
            .getInstance(context)
            .registerReceiver(receiver, IntentFilter(context.packageName))
        if (appIsInForeground(context)) {
            context.applicationContext.startActivity(intent)
        } else {
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            showPermissionNotification(context, pendingIntent)
        }
    }

    private fun getLocationActivityIntent(context: Context, isOneTime: Boolean) =
        Intent(context, LocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (config.shouldResolveRequest) {
                putExtra(Constants.INTENT_EXTRA_CONFIGURATION, config)
                putExtra(Constants.INTENT_EXTRA_IS_SINGLE_UPDATE, isOneTime)
            }
        }

    private fun appIsInForeground(context: Context): Boolean {
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.runningAppProcesses?.filter {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }?.any {
            it.pkgList.any { pkg -> pkg == context.packageName }
        } ?: false
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
     * @param func [@kotlin.ExtensionFunctionType] Function1<Location, Unit> provides
     * a success block which will grant access to retrieved location
     *
     * @return BlockExecution that can be used to handle exceptions and failures
     * during location retrieval process
     */
    fun getCurrentLocation(
        context: Context,
        func: (LocusResult) -> Unit
    ) {
        initLocationProvider(context.applicationContext)
        checkAndStartLocationUpdates(context.applicationContext, func)
    }

    /**
     * Displays location permission notification when location permission is not granted and
     * background location updates is requested.
     * @param context Context is the Android context
     * @param pendingIntent PendingIntent will be used to open permission activity
     */
    private fun showPermissionNotification(context: Context, pendingIntent: PendingIntent) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
        // TODO move channel creation to content provider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    "permission_channel",
                    "Permission Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            manager.createNotificationChannel(channel)
        }
        with(NotificationCompat.Builder(context, "permission_channel")) {
            setContentTitle("Require Location Permission")
            setContentText("This feature requires location permission to access device location. Please allow to access device location")
            setSmallIcon(R.drawable.ic_location_on)
            addAction(NotificationCompat.Action.Builder(0, "Grant", pendingIntent).build())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                priority = NotificationManager.IMPORTANCE_HIGH
            }
            setAutoCancel(true)
            manager.notify(Constants.PERMISSION_NOTIFICATION_ID, build())
        }
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
     * Note that this only stops location updates, it cannot stop ongoing permission request.
     */
    fun stopLocationUpdates() {
        if (::locationProvider.isInitialized) {
            locationProvider.stopUpdates()
        }
        // TODO check if any objects need to reset
    }

    /**
     * Resets location configs to default
     */
    fun setDefaultConfig() {
        config = Configuration()
    }
}

