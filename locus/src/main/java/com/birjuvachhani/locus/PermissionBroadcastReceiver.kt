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

import androidx.lifecycle.Observer

/*
 * Created by Birju Vachhani on 16 September 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Receives results related to permission model
 */
class PermissionObserver(private val onResult: (Throwable?) -> Unit) : Observer<String> {

    override fun onChanged(value: String) {
        logDebug("Received Permission broadcast")
        isRequestingPermission.set(false)
        when (value) {
            Constants.GRANTED -> {
                logDebug("Permission granted")
                onResult(null)
            }
            else -> {
                logDebug(value)
                onResult(Throwable(value))
            }
        }
        permissionLiveData.removeObserver(this)
    }

}