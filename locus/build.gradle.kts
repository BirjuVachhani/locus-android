plugins {
    id("com.android.library")
    kotlin("android")
    id("com.github.dcendents.android-maven")
    kotlin("android.extensions")
}

var group = "com.github.BirjuVachhani"

android {
    compileSdkVersion(31)
    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(31)
        versionCode = 4
        versionName = "4.0.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    compileOnly("androidx.appcompat:appcompat:1.4.0-alpha03")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    compileOnly("com.google.android.gms:play-services-location:18.0.0")
    implementation("androidx.activity:activity-ktx:1.3.1")
}
