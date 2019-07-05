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

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.birjuvachhani.locationextension.Locus
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName

    // create an instance of Locus class to use it later to retrieve location on the go.
    private val locus = Locus()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun stopTracking(v: View) {
        locus.stopTrackingLocation(this)
    }

    /**
     * Initiates location tracking process on button click
     * */
    fun startTracking(v: View) {

        locus.listenForLocation(this) {
            locationContainer.visibility = View.VISIBLE
            tvLatitude.text = latitude.toString()
            tvLongitude.text = longitude.toString()
            tvError.text = ""
            tvTime.text = getCurrentTimeString()
            Log.e(TAG, "Latitude: $latitude\tLongitude: $longitude")
        } failure {
            tvLatitude.text = ""
            tvLongitude.text = ""
            tvError.text = message
            Log.e(TAG, "Error: $message")
        }
    }

    /**
     * Returns current time string in 'HH:MM:SS' format.
     * */
    private fun getCurrentTimeString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.HOUR_OF_DAY)} : ${calendar.get(Calendar.MINUTE)} : ${calendar.get(Calendar.SECOND)}"
    }

    override fun onPause() {
        super.onPause()
        //stop receiving location when app is not in foreground.
        locus.stopTrackingLocation(this)
    }
}
