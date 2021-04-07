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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.birjuvachhani.locus.Locus

/*
 * Created by Birju Vachhani on 23 September 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

internal class ServiceStopBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "ServiceStopBroadcastReceiver"
    }

    @SuppressLint("LongLogTag")
    override fun onReceive(context: Context, intent: Intent) {
        Log.e(TAG, "Received broadcast to stop location updates")
        Locus.stopLocationUpdates()
        context.stopService(Intent(context, LocationService::class.java))
    }
}