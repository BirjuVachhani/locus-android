package com.birjuvachhani.locationextensionsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.birjuvachhani.locationextension.GeoLocation
import com.google.android.gms.location.LocationRequest
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var count = 0
    private val geoLocation = GeoLocation(this) {
        rationale = "this is custom rationale"
        blocked = "this is custom permission blocked message"
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

        geoLocation.getCurrentLocation({ location ->
            count++
            tvLatitude.text = location.latitude.toString()
            tvLongitude.text = location.longitude.toString()
            tvCount.text = count.toString()
            Log.e("MAINACTIVITY", "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
        }, { isDenied, t ->

        })
    }
}
