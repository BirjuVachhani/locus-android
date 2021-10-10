plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
    id("kotlin-parcelize")
}

var group = "com.github.BirjuVachhani"

android {
    compileSdk = 31
    defaultConfig {
        minSdk = 16
        targetSdk = 31
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
    compileOnly("androidx.appcompat:appcompat:1.4.0-beta01")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    compileOnly("com.google.android.gms:play-services-location:18.0.0")
    implementation("androidx.activity:activity-ktx:1.3.1")
}
