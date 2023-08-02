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

package com.birjuvachhani.locus

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.birjuvachhani.locus.extensions.getAvailableService
import com.google.android.gms.location.LocationResult as GMSLocationResult
import com.huawei.hms.location.LocationResult as HMSLocationResult

/*
 * Created by Birju Vachhani on 12 August 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Receives location broadcasts
 */
internal class LocationBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PROCESS_UPDATES =
            "com.birjuvachhani.locus.LocationProvider.LocationBroadcastReceiver.action.PROCESS_UPDATES"

        fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, LocationBroadcastReceiver::class.java)
            intent.action = ACTION_PROCESS_UPDATES
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getBroadcast(context, 0, intent, flags)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        logDebug("Received location update broadcast")
        intent ?: return
        when (context?.getAvailableService()) {
            AvailableService.HMS -> extractDataFromHMS(intent)
            else -> extractDataFromGMS(intent)
        }
    }

    /**
     * With the help of this method we will extract data With Huawei mobile service LocationResult.
     */
    private fun extractDataFromHMS(intent: Intent) {
        if (intent.action == ACTION_PROCESS_UPDATES && HMSLocationResult.hasResult(intent)) {
            HMSLocationResult.extractResult(intent).let { result ->
                if (result?.locations?.isNotEmpty() == true) {
                    result.lastLocation?.let {
                        logDebug("Huawei mobile service Received location $it")
                        locationLiveData.postValue(LocusResult.success(it))
                    }
                }
            }
        }
    }

    /**
     * With the help of this method we will extract data With Google mobile service LocationResult.
     */
    private fun extractDataFromGMS(intent: Intent) {
        if (intent.action == ACTION_PROCESS_UPDATES && GMSLocationResult.hasResult(intent)) {
            GMSLocationResult.extractResult(intent).let { result ->
                if (result?.locations?.isNotEmpty() == true) {
                    result.lastLocation?.let {
                        logDebug("Google mobile service Received location $it")
                        locationLiveData.postValue(LocusResult.success(it))
                    }
                }
            }
        }
    }
}