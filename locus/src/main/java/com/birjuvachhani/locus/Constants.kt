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

/*
 * Created by Birju Vachhani on 06 February 2019
 * Copyright © 2020 locus-android. All rights reserved.
 */

/**
 * Holds all the constants used for the lib
 * */
internal object Constants {
    const val PERMISSION_NOTIFICATION_ID = 865
    internal const val DENIED = "denied"
    internal const val GRANTED = "granted"
    internal const val PERMANENTLY_DENIED = "permanently_denied"
    internal const val RESOLUTION_FAILED = "resolution_failed"
    internal const val LOCATION_SETTINGS_DENIED = "location_settings_denied"
    internal const val INTENT_EXTRA_CONFIGURATION = "request"
    internal const val INTENT_EXTRA_IS_SINGLE_UPDATE = "isSingleUpdate"
    internal const val INTENT_EXTRA_IS_BACKGROUND = "is_background"
    internal const val INTENT_EXTRA_PERMISSION_RESULT = "permission_result"
}