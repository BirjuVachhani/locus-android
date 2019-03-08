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

/*
 * Created by Birju Vachhani on 07 February 2019
 * Copyright © 2019 locus-android. All rights reserved.
 */

/**
 * handles failure execution calls
 * */
class BlockExecution internal constructor() {

    private var failureFunc: Throwable.() -> Unit = {}

    /**
     * provides block to be executed when there's a failure
     * */
    infix fun failure(func: Throwable.() -> Unit) {
        this.failureFunc = func
    }

    /**
     * invokes failure function
     * */
    internal operator fun invoke(throwable: Throwable) {
        failureFunc(throwable)
    }
}