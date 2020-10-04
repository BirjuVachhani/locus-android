/*
 * Copyright © 2020 Birju Vachhani (https://github.com/BirjuVachhani)
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

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/*
 * Created by Birju Vachhani on 18 September 2019
 * Copyright © 2020 locus-android. All rights reserved.
 */

/**
 * Checks whether the app has location permission or not
 * @return true is the app has location permission, false otherwise.
 * */
internal fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Determines whether the permission rationale should be displayed or not
 */
internal fun Activity.shouldShowRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

/**
 * Holds normal location permissions
 */
internal val locationPermissions: Array<String> by lazy {
    arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
}

/**
 * Holds background location permission introduced in Android Q
 */
internal val backgroundPermission: Array<String>
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else arrayOf()


/**
 * Creates array of permissions
 * @param isBackground Determines whether the background location permission should be included or not
 */
internal fun getAllPermissions(isBackground: Boolean) =
    if (isBackground) locationPermissions + backgroundPermission else locationPermissions