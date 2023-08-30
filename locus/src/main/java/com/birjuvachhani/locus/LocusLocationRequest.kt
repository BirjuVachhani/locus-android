package com.birjuvachhani.locus

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.android.gms.location.LocationRequest as GMSLocationRequest
import com.huawei.hms.location.LocationRequest as HMSLocationRequest

/**
 * [LocusLocationRequest]
 */
@Parcelize
sealed class LocusLocationRequest : Parcelable {

    /**
     * This data class holds an instance of [HMSLocationRequest] also extends [LocusLocationRequest].
     */
    data class LocusHMSLocationRequest(val locationRequest: HMSLocationRequest) :
        LocusLocationRequest()

    /**
     * This data class holds an instance of [GMSLocationRequest] also extends [LocusLocationRequest].
     */
    data class LocusGMSLocationRequest(val locationRequest: GMSLocationRequest) :
        LocusLocationRequest()

    companion object {

        /**
         * [checkAvailableService]
         */
        fun LocusLocationRequest?.checkAvailableService(
            onGMSAvailable: () -> Unit,
            onHMSAvailable: () -> Unit,
            onNoServiceValid: () -> Unit = {
                logError("The Location Request is null, and No service is available.")
            },
        ) = when (this) {
            is LocusGMSLocationRequest -> onGMSAvailable()
            is LocusHMSLocationRequest -> onHMSAvailable()
            null -> onNoServiceValid()
        }
    }
}

/**
 * This enum class defines which services is available, Google mobile service or Huawei Mobile
 * service or none of them.
 */
enum class AvailableService {
    HMS, GMS, NONE
}
