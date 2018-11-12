package com.birjuvachhani.locationextension

import android.location.Location
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.location.LocationRequest

/**
 * Created by Birju Vachhani on 09-11-2018.
 */

class GeoLocation(val activity: AppCompatActivity, func: LocationOptions.() -> Unit = {}) {

    private var options = LocationOptions()

    init {
        options(func)
    }

    fun options(func: LocationOptions.() -> Unit) {
        val locationOptions = LocationOptions()
        func.invoke(locationOptions)
        locationOptions.request.invoke(locationOptions.locationRequest)
        this.options = locationOptions
    }

    fun getCurrentLocation(success: (Location) -> Unit = {}) {
        getLocationHelper(activity).apply {
            Handler().post {
                startLocationProcess(LocationOptions(), success, { _, _ -> }, true)
            }
        }
    }

    // Give location only once
    fun getCurrentLocation(
        success: (Location) -> Unit = {},
        failure: (isDenied: Boolean, t: Throwable?) -> Unit = { _, _ -> }
    ) {
        getLocationHelper(activity).apply {
            Handler().post {
                startLocationProcess(options, success, failure, true)
            }
        }
    }

    fun listenForLocation(
        success: (Location) -> Unit, failure: (isDenied: Boolean, t: Throwable?) -> Unit
    ) {
        getLocationHelper(activity).apply {
            Handler().post {
                startLocationProcess(options, success, failure, false)
            }
        }
    }

    private fun getLocationHelper(activity: AppCompatActivity): LocationHelper {
        val frag = activity.supportFragmentManager.findFragmentByTag(LocationHelper.TAG)
        return if (frag == null) {
            val mLocationHelper = LocationHelper.newInstance()
            activity.supportFragmentManager.beginTransaction()
                .add(mLocationHelper, LocationHelper.TAG)
                .commit()
            mLocationHelper
        } else {
            frag as LocationHelper
        }
    }

}

// Give location only once

class LocationOptions {
    var rationale: String = "This a rationale string!"
    var blocked: String = "Permission is blocked!"
    var request: LocationRequest.() -> Unit = {}
    internal var locationRequest = LocationRequest()

    companion object {
        fun build(func: LocationOptions.() -> Unit): LocationOptions {
            val locationOptions = LocationOptions()
            func.invoke(locationOptions)
            locationOptions.request.invoke(locationOptions.locationRequest)
            return locationOptions
        }
    }
}