plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
    id("kotlin-parcelize")
}

android {
    compileSdk = 33
    defaultConfig {
        minSdk = 19
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        buildConfig = false
    }
    namespace = "com.birjuvachhani.locus"
}

dependencies {
    compileOnly("com.google.android.material:material:1.9.0")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    compileOnly("com.huawei.hms:location:6.11.0.301")
    compileOnly("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("locus-android") {
                groupId = "com.github.BirjuVachhani"
                artifactId = "locus-android"
                version = "5.0.0"

                afterEvaluate {
                    artifact(tasks.getByName("bundleReleaseAar"))
                }
            }
        }
    }
}