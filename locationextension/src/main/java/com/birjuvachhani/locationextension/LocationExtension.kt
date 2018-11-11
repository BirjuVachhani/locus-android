package com.birjuvachhani.locationextension

import android.location.Location
import android.os.Handler
import android.support.v7.app.AppCompatActivity

/**
 * Created by Birju Vachhani on 09-11-2018.
 */

// Give location only once
fun AppCompatActivity.getCurrentLocation(
    success: (Location) -> Unit, failure: (isDenied: Boolean, t: Throwable?) -> Unit
) {
    getLocationHelper(this).apply {
        Handler().post {
            startLocationProcess(success, failure, true)
        }
    }
}

fun AppCompatActivity.listenForLocation(
    success: (Location) -> Unit, failure: (isDenied: Boolean, t: Throwable?) -> Unit
) {
    getLocationHelper(this).apply {
        Handler().post {
            startLocationProcess(success, failure, false)
        }
    }
}

private fun getLocationHelper(activity: AppCompatActivity): LocationHelper {
    val frag = activity.supportFragmentManager.findFragmentByTag(LocationHelper.TAG)
    if (frag == null) {
        val mLocationHelper = LocationHelper.newInstance()
        activity.supportFragmentManager.beginTransaction()
            .add(mLocationHelper, LocationHelper.TAG)
            .commit()
        return mLocationHelper
    } else {
        return frag as LocationHelper
    }
}