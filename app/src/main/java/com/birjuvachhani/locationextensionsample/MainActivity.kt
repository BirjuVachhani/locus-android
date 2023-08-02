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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.birjuvachhani.locationextensionsample.databinding.ActivityMainBinding
import com.birjuvachhani.locus.AvailableService
import com.birjuvachhani.locus.Locus
import com.birjuvachhani.locus.extensions.getAvailableService
import java.util.Calendar
import com.google.android.gms.location.LocationRequest as GSMLocationRequest
import com.huawei.hms.location.LocationRequest as HSMLocationRequest
/*
 * Created by Birju Vachhani on 18 September 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        private val TAG = this::class.java.simpleName
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        when (getAvailableService()) {
            AvailableService.HMS -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra("request", HSMLocationRequest().setInterval(1000))
                }
            }

            else -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra("request", GSMLocationRequest.Builder(1000).build())
                }
            }
        }

        Locus.setLogging(true)
    }

    fun getSingleUpdate(v: View) {
        Locus.getCurrentLocation(this) { result ->
            result.location?.let {
                binding.tvSingleUpdate.text = "${it.latitude}, ${it.longitude}"
                binding.tvSingleUpdate.visibility = View.VISIBLE
                binding.tvErrors.visibility = View.INVISIBLE
            } ?: run {
                binding.tvSingleUpdate.visibility = View.INVISIBLE
                binding.tvErrors.text = result.error?.message
                binding.tvErrors.visibility = View.VISIBLE
            }
        }
    }

    private fun onLocationUpdate(location: Location) {
        binding.btnStart.isEnabled = false
        binding.btnStop.isEnabled = true
        binding.llLocationData.visibility = View.VISIBLE
        binding.tvNoLocation.visibility = View.GONE
        binding.tvLatitude.text = location.latitude.toString()
        binding.tvLongitude.text = location.longitude.toString()
        binding.tvTime.text = getCurrentTimeString()
        binding.tvErrors.visibility = View.INVISIBLE
        Log.e(TAG, "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
    }

    private fun onError(error: Throwable?) {
        binding.btnStart.isEnabled = true
        binding.tvLatitude.text = ""
        binding.tvLongitude.text = ""
        binding.llLocationData.visibility = View.INVISIBLE
        Log.e(TAG, "Error: ${error?.message}")
        binding.tvErrors.text = error?.message
        binding.tvErrors.visibility = View.VISIBLE
    }

    /**
     * Returns current time string in 'HH:MM:SS' format.
     * */
    private fun getCurrentTimeString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.HOUR_OF_DAY)} : ${calendar.get(Calendar.MINUTE)} : ${
            calendar.get(
                Calendar.SECOND
            )
        }"
    }

    fun startUpdates(v: View) {
        Locus.configure {
            enableBackgroundUpdates = binding.scBackground.isChecked
            forceBackgroundUpdates = binding.scForceBackground.isChecked
            shouldResolveRequest = binding.scResolveSettings.isChecked
        }
        Locus.startLocationUpdates(this) { result ->
            result.location?.let(::onLocationUpdate)
            result.error?.let(::onError)
        }
    }

    fun stopUpdates(v: View) {
        Locus.stopLocationUpdates()
        binding.btnStop.isEnabled = true
        binding.btnStart.isEnabled = true
        binding.llLocationData.visibility = View.INVISIBLE
        binding.tvNoLocation.visibility = View.VISIBLE
        binding.tvSingleUpdate.visibility = View.INVISIBLE
    }

    fun startLocationService(v: View) {
        startService(Intent(this, LocationService::class.java))
        finish()
    }
}
