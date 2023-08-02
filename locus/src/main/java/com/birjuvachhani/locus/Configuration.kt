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

import android.os.Parcelable
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest as GMSLocationRequest
import com.huawei.hms.location.LocationRequest as HMSLocationRequest
import kotlinx.parcelize.Parcelize

/*
 * Created by Birju Vachhani on 07 February 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Data class to store location related configurations which includes dialog messages and instance of LocationRequest class.
 */
@LocusMarker
@Parcelize
data class Configuration(
    internal var locationRequest: LocusLocationRequest = getLocusLocationRequest(AvailableService.GMS),
    var shouldResolveRequest: Boolean = true,
    var enableBackgroundUpdates: Boolean = false,
    var forceBackgroundUpdates: Boolean = false,
) : Parcelable {

    companion object {
        internal const val INTERVAL_IN_MS = 1000L
        internal const val FASTEST_INTERVAL_IN_MS = 1000L
        internal const val MAX_WAIT_TIME_IN_MS = 1000L
    }

    /**
     * Create an instance of LocationRequest class
     * @param func is a LocationRequest's lambda receiver which provide a block to configure LocationRequest
     */
    fun request(func: (@LocusMarker GMSLocationRequest).() -> Unit) {
        locationRequest =
            LocusLocationRequest.LocusGMSLocationRequest(GMSLocationRequest.create().apply(func))
    }

}

/**
 * Gets back an instance of [LocusLocationRequest].
 *
 * @param availableService is an instance of [AvailableService].
 *
 * @return [LocusLocationRequest].
 */
internal fun getLocusLocationRequest(availableService: AvailableService): LocusLocationRequest =
    when (availableService) {
        AvailableService.HMS -> LocusLocationRequest.LocusHMSLocationRequest(getHMSLocationRequest())
        else -> LocusLocationRequest.LocusGMSLocationRequest(getGMSLocationRequest())
    }

/**
 * Gets the Google play service location request.
 */
internal fun getGMSLocationRequest(): GMSLocationRequest =
    GMSLocationRequest.Builder(Configuration.INTERVAL_IN_MS)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMinUpdateIntervalMillis(Configuration.FASTEST_INTERVAL_IN_MS)
        .setMaxUpdateDelayMillis(Configuration.MAX_WAIT_TIME_IN_MS).build()

/**
 * Gets the Huawei mobile service location request.
 */
internal fun getHMSLocationRequest(): HMSLocationRequest =
    HMSLocationRequest().setInterval(Configuration.INTERVAL_IN_MS)
        .setPriority(HMSLocationRequest.PRIORITY_HIGH_ACCURACY)
        .setFastestInterval(Configuration.FASTEST_INTERVAL_IN_MS)
        .setMaxWaitTime(Configuration.MAX_WAIT_TIME_IN_MS)
