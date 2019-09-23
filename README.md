
![banner](https://github.com/BirjuVachhani/locus-android/blob/master/banner.svg)

# Locus-Android

[![License](https://img.shields.io/badge/License-Apache%202.0-2196F3.svg?style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)
[![language](https://img.shields.io/github/languages/top/BirjuVachhani/location-extension-android.svg?style=for-the-badge&colorB=f18e33)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=for-the-badge)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-16%2B-F44336.svg?style=for-the-badge)](https://android-arsenal.com/api?level=16)

Locus is a tiny kotlin library for android which makes it super very easy to retrieve location with just few lines of code. Everything including permission model and Location settings resolution is handled internally which removes a lot of boilerplate code any developer have to write every time.

| Current Version 	| 3.0.0-alpha02 	|
|-----------------	|---------------	|
| Platform        	| Android       	|
| Language        	| Kotlin        	|
| SDK Level       	| 16+           	|
| License         	| Apache 2.0    	|
| Size            	| 38 KB            	|

See [Wiki](https://github.com/BirjuVachhani/locus-android/wiki) for more information and configuration!

## Features

* Android Q support
* Completely written in Kotlin
* Easy Initialization
* Handles Permission Model
* No Boilerplate
* Built on Kotlin DSL
* Manifest Permission Check
* Life-Cycle Aware Location Updates
* Location Settings Check
* Location Settings Request
* Custom Location Options Configuration
* Custom Relation Dialog configuration
* Custom Permission Blocked Dialog configuration

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
    implementation 'com.github.BirjuVachhani:locus-android:3.0.1'
}
```

## Usage

See [Wiki](https://github.com/BirjuVachhani/locus-android/wiki) on how to get started with **Locus**.

### Pull Request
To generate a pull request, please consider following [Pull Request Template](https://github.com/BirjuVachhani/locus-android/blob/master/PULL_REQUEST_TEMPLATE.md).

### Issues
To submit an issue, please check the [Issue Template](https://github.com/BirjuVachhani/locus-android/blob/master/ISSUE_TEMPLATE.md).

Code of Conduct
---
[Code of Conduct](https://github.com/BirjuVachhani/locus-android/blob/master/CODE_OF_CONDUCT.md)

## Contribution

You are most welcome to contribute to this project!

Please have a look at [Contributing Guidelines](https://github.com/BirjuVachhani/locus-android/blob/master/CONTRIBUTING.md), before contributing and proposing a change.

# License

```
   Copyright © 2019 BirjuVachhani

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
