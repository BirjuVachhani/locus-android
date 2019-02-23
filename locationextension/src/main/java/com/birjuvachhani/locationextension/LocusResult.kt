/*
 * Copyright 2019 Birju Vachhani (https://github.com/BirjuVachhani)
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

package com.birjuvachhani.locationextension

import android.location.Location

/**
 * Created by Birju Vachhani on 05/02/19.
 */

/**
 * Represents states of LocusResult library
 * */
sealed class LocusResult {

    /**
     * Represents success state for retrieving location
     * */
    data class Success internal constructor(val location: Location) : LocusResult()

    /**
     * Represents failure state for location process
     * */
    data class Failure internal constructor(val error: Throwable) : LocusResult()

}