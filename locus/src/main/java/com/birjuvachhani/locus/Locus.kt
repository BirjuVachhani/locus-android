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
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.birjuvachhani.locus.Locus.config
import com.birjuvachhani.locus.Locus.locationProvider
import com.birjuvachhani.locus.extensions.getAvailableService
import java.util.concurrent.atomic.AtomicBoolean
import com.google.android.gms.location.LocationServices as GMSLocationServices
import com.google.android.gms.location.LocationSettingsRequest as GMSLocationSettingsRequest
import com.huawei.hms.location.LocationServices as HMSLocationServices
import com.huawei.hms.location.LocationSettingsRequest as HMSLocationSettingsRequest

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
 * Holds permission results
 */
internal var permissionLiveData = MutableLiveData<String>()

/**
 * Marker class for Locus Extensions
 * */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY)
internal annotation class LocusMarker

/**
 * A helper class for location extension which provides
 * dsl extensions for getting location.
 *
 * The problem with the FusedLocationProviderClient is that when we provide
 * a LocationCallback instance to receive location updates, it stops receiving
 * updates after some time when app goes in background. This use case prevents
 * from getting location updates in services and while the app is in background.
 *
 * The preferable workaround is to use PendingIntent instead of using LocationCallback
 * to receive location updates. This way works perfectly even when the app is in background.
 * Therefore, We are not using LocationCallback approach to avoid any uncertain behaviour.
 *
 * @property config Holds the configuration of the library
 * @property locationProvider Used to retrieve location
 *
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
     * @param func Lambda receiver for Configuration which is used to build Configuration object
     * */
    fun configure(func: Configuration.() -> Unit) {
        func(config)
    }

    /**
     * Starts location updates by verifying permissions and location settings.
     * This method can be used anywhere, but it is intended to be used where you don't have
     * access to [Context] reference which implements [LifecycleOwner].
     * e.g. Service, IntentService, BroadcastReceiver. It also can be used for Fragments.
     * It returns [LiveData] instance which can be used to observe for location updates.
     * Also, any errors will be passed to this [LiveData] instance.
     *
     * This method internally handles runtime location permission which is needed
     * for Android M and above. It also handles rationale dialogs and permission
     * blocked dialogs also. These dialogs can be configured using [configure] method.
     * It also handles location resolution process for requested location settings.
     * It shows setting resolution dialog if needed and ask for user's permission to
     * change location settings.
     *
     * @param context Context is the Android context
     * @return LiveData<LocusResult> which can be used to observe location updates
     */
    @Throws(IllegalStateException::class)
    fun startLocationUpdates(context: Context): LiveData<LocusResult> {
        config.setLocationRequest(context.getAvailableService())
        assertMainThread("startLocationUpdates")
        checkAndStartLocationUpdates(context.applicationContext)
        return locationLiveData
    }

    /**
     * Starts location updates by verifying permissions and location settings.
     * This overloaded method is intended to be called from fragments, and they have access
     * to [Context] and they are [LifecycleOwner]. Instead of returning a [LiveData] instance,
     * it takes in a lambda block which will be invoked on [LiveData] events.
     *
     * This method internally handles runtime location permission which is needed
     * for Android M and above. It also handles rationale dialogs and permission
     * blocked dialogs also. These dialogs can be configured using [configure] method.
     * It also handles location resolution process for requested location settings.
     * It shows setting resolution dialog if needed and ask for user's permission to
     * change location settings.
     *
     * @param fragment Fragment from which this method is called
     * @param onResult Lambda block which is called upon receiving updates on [LiveData]
     */
    @Throws(IllegalStateException::class)
    fun startLocationUpdates(
        fragment: Fragment,
        onResult: (LocusResult) -> Unit,
    ) {
        assertMainThread("startLocationUpdates")
        locationLiveData.observe(fragment, Observer(onResult))
        startLocationUpdates(fragment.requireContext().applicationContext)
    }

    /**
     * Starts location updates by verifying permissions and location settings.
     * This overloaded method is intended to be called from the components which is ContextWrapper
     * and also implements [LifecycleOwner] interface.
     * e.g. Activities, LifecycleService, Application
     * Instead of returning a [LiveData] instance, it takes in a lambda block which will be
     * invoked on [LiveData] events.
     *
     * This method internally handles runtime location permission which is needed
     * for Android M and above. It also handles rationale dialogs and permission
     * blocked dialogs also. These dialogs can be configured using [configure] method.
     * It also handles location resolution process for requested location settings.
     * It shows setting resolution dialog if needed and ask for user's permission to
     * change location settings.
     *
     * @param lifecycleOwnerContext Instance of a class which itself is [Context] and implements [LifecycleOwner]
     * @param onResult Lambda block which is called upon receiving updates on [LiveData]
     */
    @Throws(IllegalStateException::class)
    fun <T> startLocationUpdates(
        lifecycleOwnerContext: T,
        onResult: (LocusResult) -> Unit,
    ) where T : Context, T : LifecycleOwner {
        assertMainThread("startLocationUpdates")
        locationLiveData.observe(lifecycleOwnerContext, Observer(onResult))
        startLocationUpdates(lifecycleOwnerContext.applicationContext)
    }

    /**
     * Checks if the method call is performed on main thread or not. throws exception if not on main thread
     * @param methodName Name of the method for which this check will be performed
     */
    private fun assertMainThread(methodName: String) {
        check(Looper.getMainLooper() == Looper.myLooper()) {
            "Cannot invoke $methodName on a background thread"
        }
    }

    private fun checkAndStartLocationUpdates(
        context: Context,
        singleUpdate: ((LocusResult) -> Unit)? = null,
    ) {
        val observer = PermissionObserver {
            it?.let { error ->
                singleUpdate?.let {
                    it(LocusResult.error(error))
                } ?: locationLiveData.postValue(LocusResult.error(it))
            } ?: startUpdates(context, singleUpdate)
        }
        when {
            !getAllPermissions(config.enableBackgroundUpdates).all(context::hasPermission) ->
                // Doesn't have permission, start permission resolution
                startPermissionAndResolutionProcess(context, observer, singleUpdate != null)

            config.shouldResolveRequest ->
                // has permissions, need to check for location settings
                checkLocationSettings(context) { isSatisfied ->
                    if (isSatisfied) {
                        logDebug("Location settings are satisfied")
                        startUpdates(context, singleUpdate)
                    } else {
                        logDebug("Location settings are not satisfied")
                        startPermissionAndResolutionProcess(context, observer, singleUpdate != null)
                    }
                }

            else ->
                // has permission but location settings resolution is disabled so start updates directly
                startUpdates(context, singleUpdate)
        }
    }

    /**
     * Determines whether the requested location settings are satisfied or not
     * @param context Context
     * @param onResult Function1<Boolean, Unit>
     */
    private fun checkLocationSettings(context: Context, onResult: (Boolean) -> Unit) {
        when (val locationRequest = config.locationRequest) {
            is LocusLocationRequest.LocusHMSLocationRequest -> {
                val builder = HMSLocationSettingsRequest.Builder()
                builder.addLocationRequest(locationRequest.locationRequest)
                builder.setAlwaysShow(true)
                val client = HMSLocationServices.getSettingsClient(context)
                val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
                locationSettingsResponseTask.addOnSuccessListener {
                    onResult(true)
                }.addOnFailureListener { exception ->
                    logError(exception)
                    onResult(false)
                }
            }

            is LocusLocationRequest.LocusGMSLocationRequest -> {
                val builder = GMSLocationSettingsRequest.Builder()
                builder.addLocationRequest(locationRequest.locationRequest)
                builder.setAlwaysShow(true)
                val client = GMSLocationServices.getSettingsClient(context)
                val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
                locationSettingsResponseTask.addOnSuccessListener {
                    onResult(true)
                }.addOnFailureListener { exception ->
                    logError(exception)
                    onResult(false)
                }
            }

            null -> {
                logError("The Location Request is null, and No service is available.")
                onResult(false)
            }
        }
    }

    /**
     * Responsible for starting location updates. Called after performing all the necessary checks and requests.
     * @param context Android Context
     * @param singleUpdate Callback to be invoked if this request is for single update only
     */
    private fun startUpdates(context: Context, singleUpdate: ((LocusResult) -> Unit)? = null) {
        initLocationProvider(context.applicationContext)
        if (singleUpdate == null) {
            when (val locusLocationRequest = config.locationRequest) {
                is LocusLocationRequest.LocusGMSLocationRequest -> {
                    locationProvider.startUpdates(locusLocationRequest.locationRequest)
                }

                is LocusLocationRequest.LocusHMSLocationRequest -> {
                    locationProvider.startUpdates(locusLocationRequest.locationRequest)
                }

                null -> {
                    logError("The Location Request is null, and No service is available.")
                }
            }
        } else {
            when (val locusLocationRequest = config.locationRequest) {
                is LocusLocationRequest.LocusGMSLocationRequest -> {
                    locationProvider.getSingleUpdate(
                        locusLocationRequest.locationRequest, singleUpdate
                    )
                }

                is LocusLocationRequest.LocusHMSLocationRequest -> {
                    locationProvider.getSingleUpdate(
                        locusLocationRequest.locationRequest, singleUpdate
                    )
                }

                null -> {
                    singleUpdate(LocusResult.error(error = Exception("The Location Request is null, and No service is available.")))
                    logError("The Location Request is null, and No service is available.")
                }
            }
        }
    }

    /**
     * Responsible to handle initialization and execution of location permission retrieval process
     * @param context Android Context
     * @param permissionObserver Observer instance that receives location permission results
     * @param isOneTime Determines whether this request is for single location updates or continuous updates
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startPermissionAndResolutionProcess(
        context: Context,
        permissionObserver: Observer<String>,
        isOneTime: Boolean = false,
    ) {
        if (isRequestingPermission.getAndSet(true)) {
            logDebug("A request is already ongoing")
            return
        }
        val intent = getLocationActivityIntent(context, isOneTime)
        permissionLiveData.observeForever(permissionObserver)
        if (appIsInForeground(context)) {
            context.applicationContext.startActivity(intent)
        } else {
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        flags
                    )
                } else {
                    PendingIntent.getActivity(context, 0, intent, flags)
                }
            showPermissionNotification(context, pendingIntent)
        }
    }

    /**
     * Provides intent configured to open location activity
     * @param context Android Context used to create Intent
     * @param isOneTime Boolean
     * @return Intent
     */
    private fun getLocationActivityIntent(context: Context, isOneTime: Boolean) =
        Intent(context, LocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Constants.INTENT_EXTRA_CONFIGURATION, config)
            putExtra(Constants.INTENT_EXTRA_IS_SINGLE_UPDATE, isOneTime)
        }

    /**
     * Utility function to check whether the app is in foreground or not
     * @param context Android Context used to get system services
     * @return Boolean True if the app is in foreground, false otherwise
     */
    private fun appIsInForeground(context: Context): Boolean {
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.runningAppProcesses?.filter {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }?.any {
            it.pkgList.any { pkg -> pkg == context.packageName }
        } ?: false
    }

    /**
     * Initiates location retrieval process to receive only one location update.
     *
     * This method internally handles runtime location permission which is needed
     * for Android M and above. It also handles rationale dialogs and permission
     * blocked dialogs also. These dialogs can be configured using [configure] method.
     * It also handles location resolution process for requested location settings.
     * It shows setting resolution dialog if needed and ask for user's permission to
     * change location settings.
     *
     * Note that this method doesn't handle any lifecycle events which means that the [onResult] lambda block will be called being unaware of the component lifecycle. This method is also able to deliver result while the app is in background so all the requirements of the user's current location can be satisfied with this method call
     *
     * @param context Android context object
     * @param onResult provides a success block which will grant access to retrieved location
     */
    fun getCurrentLocation(
        context: Context,
        onResult: (LocusResult) -> Unit,
    ) {
        config.setLocationRequest(context.getAvailableService())
        initLocationProvider(context.applicationContext)
        checkAndStartLocationUpdates(context.applicationContext, onResult)
    }

    /**
     * Displays location permission notification when location permission is not granted and
     * background location updates is requested.
     * @param context The Android context
     * @param pendingIntent Used to open permission activity
     */
    private fun showPermissionNotification(context: Context, pendingIntent: PendingIntent) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
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
     * @param context The Android Context used to initialize [LocationProvider]
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
    }

    /**
     * Resets location configs to default
     */
    fun setDefaultConfig() {
        config = Configuration()
    }
}

