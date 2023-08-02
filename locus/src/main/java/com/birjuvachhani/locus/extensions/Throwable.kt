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

package com.birjuvachhani.locus.extensions

import com.birjuvachhani.locus.Constants

/*
 * Created by Birju Vachhani on 06 February 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

// Extension property to check if the error caused because the user denied permission or not
val Throwable.isDenied get() = this.message == Constants.DENIED

// Extension property to check if the error caused because the user permanently denied permission or not
val Throwable.isPermanentlyDenied get() = this.message == Constants.PERMANENTLY_DENIED

// Extension property to check if the error caused because the location settings resolution failed or not
val Throwable.isSettingsResolutionFailed get() = this.message == Constants.RESOLUTION_FAILED

// Extension property to check if the error caused because the user denied enabling location or not
val Throwable.isSettingsDenied get() = this.message == Constants.LOCATION_SETTINGS_DENIED

// Extension property to check if the error caused because of some Fatal Exception or not
val Throwable.isFatal get() = !isDenied && !isPermanentlyDenied && !isSettingsDenied && !isSettingsResolutionFailed
