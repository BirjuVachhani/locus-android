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

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.birjuvachhani.locus.Locus

/*
 * Created by Birju Vachhani on 12 August 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

class TempService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.e("BIRJU", "Temp Service started")
        Handler().postDelayed({
            start()
        }, 5000)
        return START_STICKY
    }

    fun start() {
        Locus.configure {
            enableBackgroundUpdates = true
        }
        Locus.startLocationUpdates(this).observeForever { result ->
            result?.location?.let {
                Log.e(
                    "BIRJU Service",
                    "Received Background location in service: ${it.latitude}, ${it.longitude}"
                )
            } ?: Log.e("BIRJU Service", "Error: ${result?.error}")
        }
    }
}