package com.birjuvachhani.locationextensionsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.birjuvachhani.locationextension.GeoLocation
import com.google.android.gms.location.LocationRequest
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName

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
            tvLatitude.text = location.latitude.toString()
            tvLongitude.text = location.longitude.toString()
            tvError.text = ""
            tvTime.text = getCurrentTimeString()
            tvUpdateLabel.visibility = View.VISIBLE
            Log.e(TAG, "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
        }, { error ->
            tvLatitude.text = ""
            tvLongitude.text = ""
            tvError.text = "Permission Denied: ${error.isPermissionDenied}, Throwable: ${error.throwable?.message}"
            Log.e(TAG, "isDenied: ${error.isPermissionDenied}\t Error: ${error.throwable?.message}")
        })

    }

    fun getCurrentTimeString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.HOUR_OF_DAY)} : ${calendar.get(Calendar.MINUTE)} : ${calendar.get(Calendar.SECOND)}"
    }

    override fun onPause() {
        super.onPause()
        geoLocation.stopTrackingLocation()
    }
}
