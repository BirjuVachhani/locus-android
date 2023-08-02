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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/*
 * Created by Birju Vachhani on 06 February 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * LiveData extension to observer it in a null safe way
 * */
fun <T : Any> LiveData<T?>.watch(owner: LifecycleOwner, func: (T) -> Unit) {
    this.observe(owner) { result -> result?.apply(func) }
}

/**
 * Observes LiveData for only one time in lifecycle aware way and then removes the observer
 * @receiver LiveData<T?>
 * @param lifecycleOwner LifecycleOwner
 * @param func Function1<T, Unit> will be called upon getting data in observer
 */
fun <T : Any> LiveData<T?>.observeOnce(lifecycleOwner: LifecycleOwner, func: (T) -> Unit) {
    observe(lifecycleOwner, object : Observer<T?> {
        override fun onChanged(value: T?) {
            if (value != null) {
                func(value)
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
        override fun onChanged(value: T?) {
            if (value != null) {
                func(value)
                removeObserver(this)
            }
        }
    })
}
