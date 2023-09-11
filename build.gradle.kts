buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Configure the Maven repository address for the HMS Core SDK.
        maven { url = uri("https://developer.huawei.com/repo/")}
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
        classpath("com.huawei.agconnect:agcp:1.6.3.300")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Configure the Maven repository address for the HMS Core SDK.
        maven { url = uri("https://developer.huawei.com/repo/")}
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}