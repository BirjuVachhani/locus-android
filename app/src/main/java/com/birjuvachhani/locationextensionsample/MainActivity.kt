package com.birjuvachhani.locationextensionsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.birjuvachhani.locationextension.GeoLocation
import com.google.android.gms.location.LocationRequest

class MainActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName

    var count = 0
    private val geoLocation = GeoLocation(this) {
        request = {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 1000
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startTracking(v: View) {
        geoLocation.listenForLocation({ location ->
            Log.e(TAG, "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
        }, {
            Log.e(TAG, "isDenied: ${it.isPermissionDenied}\t Error: ${it.throwable?.message}")
        })

    }

    override fun onPause() {
        super.onPause()
        geoLocation.stopTrackingLocation()
    }
}
