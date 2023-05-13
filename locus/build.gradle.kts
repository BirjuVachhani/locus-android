plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
    id("kotlin-parcelize")
}

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    compileOnly("com.google.android.material:material:1.4.0")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")
    compileOnly("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
}

publishing {
    publications {
        create<MavenPublication>("locus-android") {
            groupId = "com.github.BirjuVachhani"
            artifactId = "locus-android"
            version = "4.1.0"
            artifact("build/outputs/aar/locus-release.aar")
        }
    }
}