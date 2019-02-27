# Locus-Android

[![License](https://img.shields.io/badge/License-Apache%202.0-2196F3.svg?style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)
[![language](https://img.shields.io/github/languages/top/BirjuVachhani/location-extension-android.svg?style=for-the-badge&colorB=f18e33)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=for-the-badge)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-16%2B-F44336.svg?style=for-the-badge)](https://android-arsenal.com/api?level=16)
[![Travis (.org) branch](https://img.shields.io/travis/BirjuVachhani/location-extension-android/master.svg?style=for-the-badge)](https://travis-ci.org/BirjuVachhani/location-extension-android)

Locus is a kotlin library for android which makes it very easy to retrieve location with just few lines of code. Everything including permission model and Location settings is handled internally which removes a lot of boilerplate code any developer have to write everytime. 

Please note that this library is still under development. Stable version of the library will be released soon. Stay toned!

## Gradle Dependency

* Add the JitPack repository to your project's build.gradle file

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

* Add the dependency in your app's build.gradle file

```
dependencies {
    implementation 'com.github.BirjuVachhani:locus-android:2.1.0'
}
```

![GitHub (pre-)release](https://img.shields.io/github/release-pre/birjuvachhani/location-extension-android.svg?style=for-the-badge&colorB=0091EA)

## Usage

### 1. Init GeoLocation Class:

#### Activity
```kotlin
private val locus = Locus(this)
```

#### Fragment
```kotlin
lateinit var locus: Locus

override fun onAttach(context: Context?) {
    super.onAttach(context)
    this.geoLocation = GeoLocation(requireActivity())
}
```
### 2. Retrieve Location

#### Retrieve Location Only Once
```kotlin
locus.getCurrentLocation { location ->
    Log.e(TAG, "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
}
```
To handle errors:

```kotlin
locus.getCurrentLocation({ location ->
    Log.e(TAG, "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
}, { error ->
    Log.e(TAG, "Permission Denied: ${error.isPermissionDenied}\tThrowable: ${error.throwable.message}")
})
```

#### Continuous Location
```kotlin
locus.listenForLocation { location ->
    Log.e(TAG, "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
}
```
To handle errors:

```kotlin
locus.listenForLocation({ location ->
    Log.e(TAG, "Latitude: ${location.latitude}\tLongitude: ${location.longitude}")
}, { error ->
    Log.e(TAG, "Permission Denied: ${error.isPermissionDenied}\tThrowable: ${error.throwable.message}")
})
```

### 3. Stop Receiving Location
```kotlin
locus.stopTrackingLocation()
```

So easy right? That's what we wanted to achieve: a hassle free location retrieval process.


## Custom Configuration

You can use default settings by just initializing the [GeoLocation](https://github.com/BirjuVachhani/locus-android/blob/master/locationextension/src/main/java/com/birjuvachhani/locationextension/LocationExtension.kt) Class like this:

```kotlin
private val locus = Locus(this)
```

To use custom [LocationRequest](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest) object and custom dialog messages, you can initialize [GeoLocation](https://github.com/BirjuVachhani/locus-android/blob/master/locationextension/src/main/java/com/birjuvachhani/locationextension/LocationExtension.kt) class like this:

```kotlin
private val geoLocation = Locus(this) {
    rationaleText = "This is custom rationale text"
    blockedText = "The permission is blocked and this is custom blocked message"
    request = {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 1000
        fastestInterval = 1000
    }
}
```

Here, **request** is provides a lambda block to configure [LocationRequest](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest).

Following are the default [LocationRequest](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest) configuration which will be used in not provided externally.

```kotlin
priority = LocationRequest.PRIORITY_HIGH_ACCURACY
interval = 1000L
fastestInterval = 1000L
maxWaitTime = 1000L
```

# License

```
   Copyright 2018 BirjuVachhani

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
