package com.birjuvachhani.locationextension

import android.location.Location
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.location.LocationRequest

/**
 * Created by Birju Vachhani on 09-11-2018.
 */

class GeoLocation(val activity: AppCompatActivity, func: LocationOptions.() -> Unit = {}) {

    private var options = LocationOptions()

    private object Defaults {
        internal const val INTERVAL_IN_MS = 1000L
        internal const val FASTEST_INTERVAL_IN_MS = 1000L
        internal const val MAX_WAIT_TIME_IN_MS = 1000L
        internal const val NUMBER_OF_UPDATES = 1
    }

    init {
        options.locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        options.locationRequest.interval = Defaults.INTERVAL_IN_MS
        options.locationRequest.fastestInterval = Defaults.FASTEST_INTERVAL_IN_MS
        options.locationRequest.numUpdates = Defaults.NUMBER_OF_UPDATES
        options.locationRequest.maxWaitTime = Defaults.MAX_WAIT_TIME_IN_MS
        options(func)
    }

    fun options(func: LocationOptions.() -> Unit) {
        val locationOptions = LocationOptions()
        func.invoke(locationOptions)
        locationOptions.request.invoke(locationOptions.locationRequest)
        this.options = locationOptions
    }

    fun getCurrentLocation(success: (Location) -> Unit) {
        getLocationHelper(activity).apply {
            startLocationProcess(options, success, {}, true)
        }
    }

    fun getCurrentLocation(
        success: (Location) -> Unit,
        failure: (LocationError) -> Unit
    ) {
        getLocationHelper(activity).apply {
            startLocationProcess(options, success, failure, true)
        }
    }

    fun listenForLocation(success: (Location) -> Unit) {
        getLocationHelper(activity).apply {
            startLocationProcess(options, success, {}, false)
        }
    }

    fun listenForLocation(
        success: (Location) -> Unit,
        failure: (error: LocationError) -> Unit
    ) {
        getLocationHelper(activity).apply {
            startLocationProcess(options, success, failure, false)
        }
    }

    private fun getLocationHelper(activity: AppCompatActivity): LocationHelper {
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

    fun stopTrackingLocation() {
        getLocationHelper(activity).stopContinuousLocation()
    }
}

data class LocationOptions(
    var rationaleText: String = "Location permission is required in order to use this feature properly.Please grant the permission.",
    var blockedText: String = "Location permission is blocked. Please allow permission from settings screen to use this feature",
    var request: LocationRequest.() -> Unit = {},
    internal var locationRequest: LocationRequest = LocationRequest()
)