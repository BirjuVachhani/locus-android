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

import com.google.android.gms.location.LocationRequest

/*
 * Created by Birju Vachhani on 07 February 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Data class to store location related configurations which includes dialog messages and instance of LocationRequest
 * class.
 * */
@LocusMarker
class Configuration internal constructor() {

    companion object {
        internal const val INTERVAL_IN_MS = 1000L
        internal const val FASTEST_INTERVAL_IN_MS = 1000L
        internal const val MAX_WAIT_TIME_IN_MS = 1000L
    }

    var rationaleText: String =
        "Location permission is required in order to use this feature properly.Please grant the permission."
    var rationaleTitle: String = "Location permission required!"
    var blockedTitle: String = "Location Permission Blocked"
    var blockedText: String =
        "Location permission is blocked. Please allow permission from settings screen to use this feature"
    internal var locationRequest: LocationRequest = LocationRequest()

    init {
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = INTERVAL_IN_MS
        locationRequest.fastestInterval = FASTEST_INTERVAL_IN_MS
        locationRequest.maxWaitTime = MAX_WAIT_TIME_IN_MS
    }

    /**
     * Create an instance of LocationRequest class
     * @param func is a LocationRequest's lambda receiver which provide a block to configure LocationRequest
     * */
    fun request(func: (@LocusMarker LocationRequest).() -> Unit) {
        locationRequest = LocationRequest().apply(func)
    }


}