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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/*
 * Created by Birju Vachhani on 06 February 2019
 * Copyright © 2020 locus-android. All rights reserved.
 */

/**
 * LiveData extension to observer it in a null safe way
 * */
fun <T : Any> LiveData<T?>.watch(owner: LifecycleOwner, func: (T) -> Unit) {
    this.observe(owner, Observer { result ->
        result?.apply(func)
    })
}

/**
 * Observes LiveData for only one time in lifecycle aware way and then removes the observer
 * @receiver LiveData<T?>
 * @param lifecycleOwner LifecycleOwner
 * @param func Function1<T, Unit> will be called upon getting data in observer
 */
fun <T : Any> LiveData<T?>.observeOnce(lifecycleOwner: LifecycleOwner, func: (T) -> Unit) {
    observe(lifecycleOwner, object : Observer<T?> {
        override fun onChanged(t: T?) {
            if (t != null) {
                func(t)
                removeObserver(this)
            }
        }
    })
}

/**
 * Observes LiveData for only one time in and then removes the observer
 * @receiver LiveData<T?>
 * @param func Function1<T, Unit> will be called upon getting data in observer
 */
fun <T : Any> LiveData<T?>.observeOnce(func: (T) -> Unit) {
    observeForever(object : Observer<T?> {
        override fun onChanged(t: T?) {
            if (t != null) {
                func(t)
                removeObserver(this)
            }
        }
    })
}

// Extension property to check if the error caused because the user denied permission or not
val Throwable.isDenied get() = this.message == Constants.DENIED

// Extension property to check if the error caused because the user permanently denied permission or not
val Throwable.isPermanentlyDenied get() = this.message == Constants.PERMANENTLY_DENIED

// Extension property to check if the error caused because the location settings resolution failed or not
val Throwable.isSettingsResolutionFailed get() = this.message == Constants.RESOLUTION_FAILED

// Extension property to check if the error caused because the user denied to enable location or not
val Throwable.isSettingsDenied get() = this.message == Constants.LOCATION_SETTINGS_DENIED

// Extension property to check if the error caused because of some Fatal Exception or not
val Throwable.isFatal get() = !isDenied && !isPermanentlyDenied && !isSettingsDenied && !isSettingsResolutionFailed