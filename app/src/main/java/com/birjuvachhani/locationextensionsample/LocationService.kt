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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.birjuvachhani.locus.Locus
import com.birjuvachhani.locus.LocusResult

/*
 * Created by Birju Vachhani on 12 August 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

class LocationService : LifecycleService() {

    companion object {
        const val NOTIFICATION_ID = 787
        const val STOP_SERVICE_BROADCAST_ACTON =
            "com.birjuvachhani.locationextensionsample.ServiceStopBroadcastReceiver"
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private val manager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: throw Exception("No notification manager found")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.e("BIRJU", "Temp Service started")
        Handler(Looper.getMainLooper()).postDelayed({
            start()
        }, 2000)
        return START_STICKY
    }

    private fun start() {
        startForeground(NOTIFICATION_ID, getNotification())
        Locus.configure {
            enableBackgroundUpdates = true
        }
        Locus.startLocationUpdates(this).observe(this) { result ->
            manager.notify(NOTIFICATION_ID, getNotification(result))
        }
    }

    private fun getNotification(result: LocusResult? = null): Notification {
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: throw Exception("No notification manager found")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    "location",
                    "Location Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        return with(NotificationCompat.Builder(this, "location")) {
            setContentTitle("Location Service")
            result?.apply {
                location?.let {
                    setContentText("${it.latitude}, ${it.longitude}")
                } ?: setContentText("Error: ${error?.message}")
            } ?: setContentText("Trying to get location updates")
            setSmallIcon(R.drawable.ic_location)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            addAction(
                0,
                "Stop Updates",
                PendingIntent.getBroadcast(
                    this@LocationService,
                    0,
                    Intent(this@LocationService, ServiceStopBroadcastReceiver::class.java).apply {
                        action = STOP_SERVICE_BROADCAST_ACTON
                    },
                    flags
                )
            )
            build()
        }
    }
}