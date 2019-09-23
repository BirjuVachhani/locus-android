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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/*
 * Created by Birju Vachhani on 16 September 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Receives local broadcasts related to permission model
 */
class PermissionBroadcastReceiver(private val onResult: (Throwable?) -> Unit) :
    BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        logDebug("Received Permission broadcast")
        val status = intent.getStringExtra(Constants.INTENT_EXTRA_PERMISSION_RESULT)
            ?: return
        isRequestingPermission.set(false)
        when (status) {
            Constants.GRANTED -> {
                logDebug("Permission granted")
                onResult(null)
            }
            else -> {
                logDebug(status)
                locationLiveData.postValue(LocusResult.error(Throwable(status)))
                onResult(Throwable(status))
            }
        }
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
    }
}