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

import android.util.Log

/*
 * Created by Birju Vachhani on 19 July 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * Used to determine whether the logging is enabled or not
 */
internal var isLoggingEnabled: Boolean = false

/**
 * Used internally to log error type of log messages
 * @receiver T allows this method to access the class name of the calling class
 * @param log String is the actual log message
 */
internal inline fun <reified T : Any> T.logError(log: String) {
    if (isLoggingEnabled) {
        Log.e(this::class.java.simpleName, log)
    }
}

/**
 * Used internally to log error type of log messages
 * @receiver T allows this method to access the class name of the calling class
 * @param throwable Throwable is the actual error that needs to be logged
 */
internal inline fun <reified T : Any> T.logError(throwable: Throwable) {
    if (isLoggingEnabled) {
        Log.e(this::class.java.simpleName, throwable.message.toString())
    }
}

/**
 * Used internally to log debug type of log messages
 * @receiver T allows this method to access the class name of the calling class
 * @param log String is the actual log message
 */
internal inline fun <reified T : Any> T.logDebug(log: String) {
    if (isLoggingEnabled) {
        Log.d(this::class.java.simpleName, log)
    }
}