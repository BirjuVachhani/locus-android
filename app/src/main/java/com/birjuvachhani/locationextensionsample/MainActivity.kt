package com.birjuvachhani.locationextensionsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.birjuvachhani.locationextension.getCurrentLocation
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startTracking(v: View) {
        getCurrentLocation(
            { location ->
                count++
                tvLatitude.text = location.latitude.toString()
                tvLongitude.text = location.longitude.toString()
                tvCount.text = count.toString()
                Log.e("MAINACTIVITY", "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
            },
            { isDenied, t ->
                tvError.text = "Error: isDenied: " + isDenied + " throwable: " + t?.message
                Log.e("MAINACTIVITY", "Error: isDenied: ${isDenied} throwable: ${t?.message}")
            })
    }
}
