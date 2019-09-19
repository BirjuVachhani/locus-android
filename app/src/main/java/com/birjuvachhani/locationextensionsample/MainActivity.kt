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

package com.birjuvachhani.locationextensionsample

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.observe
import com.birjuvachhani.locus.Locus
import com.google.android.gms.location.LocationRequest
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val TAG = this::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val request = LocationRequest.create()
        Intent(this, MainActivity::class.java).apply {
            putExtra("request", request)
        }
        Locus.setLogging(true)
    }

    fun getSingleUpdate(v: View) {
        Locus.getCurrentLocation(this) {
            Toast.makeText(this, "loc: ${it.location} error: ${it.error}", Toast.LENGTH_LONG).show()
        }
    }

    private fun onLocationUpdate(location: Location) {
        btnStart.isEnabled = false
        btnStop.isEnabled = true
        llLocationData.visibility = View.VISIBLE
        tvNoLocation.visibility = View.GONE
        tvLatitude.text = location.latitude.toString()
        tvLongitude.text = location.longitude.toString()
        tvTime.text = getCurrentTimeString()
        tvNoLocation.text = "No Updates Available"
        Log.e(TAG, "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
    }

    private fun onError(error: Throwable?) {
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        tvLatitude.text = ""
        tvLongitude.text = ""
        tvNoLocation.text = error?.message
        llLocationData.visibility = View.INVISIBLE
        Log.e(TAG, "Error: ${error?.message}")
    }

    /**
     * Returns current time string in 'HH:MM:SS' format.
     * */
    private fun getCurrentTimeString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.HOUR_OF_DAY)} : ${calendar.get(Calendar.MINUTE)} : ${calendar.get(
            Calendar.SECOND
        )}"
    }

    fun startUpdates(v: View) {
        Locus.configure {
            enableBackgroundUpdates = scBackground.isChecked
            forceBackgroundUpdates = scForceBackground.isChecked
            shouldResolveRequest = scResolveSettings.isChecked
        }
        Locus.startLocationUpdates(this).observe(this) { result ->
            result.location?.let(::onLocationUpdate) ?: onError(result.error)
        }
    }

    fun stopUpdates(v: View) {
        Locus.stopLocationUpdates()
        btnStop.isEnabled = false
        btnStart.isEnabled = true
        llLocationData.visibility = View.INVISIBLE
        tvNoLocation.visibility = View.VISIBLE
        tvNoLocation.text = "No Updates Available"
    }
}
