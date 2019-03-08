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

package com.birjuvachhani.locationextension

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer

/*
 * Created by Birju Vachhani on 06 February 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * LiveData extension to observer it in a null safe way
 * */
fun <T : Any> LiveData<T?>.watch(owner: LifecycleOwner, func: (T) -> Unit) {
    this.observe(owner, Observer { result ->
        result?.apply(func)
    })
}

val Throwable.isDenied: Boolean
    get() = this.message?.let { message ->
        message == Constants.DENIED
    } ?: false

val Throwable.isPermanentlyDenied: Boolean
    get() = this.message?.let { message ->
        message == Constants.PERMANENTLY_DENIED
    } ?: false

/**
 * Extension Function for initializing [MutableLiveData] with some initial value
 * @param data is the initial value
 * */
internal fun <T> MutableLiveData<T>.initWith(data: T): MutableLiveData<T> = this.apply {
    value = data
}