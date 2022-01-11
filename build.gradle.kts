plugins {
    id("maven-publish")
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

publishing {
    publications {
        create<MavenPublication>("locus-android") {
            groupId = "com.github.BirjuVachhani"
            artifactId = "locus-android"
            artifact("locus/build/outputs/aar/locus-release.aar")
        }
    }
}