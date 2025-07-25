// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.devtools.ksp") version "2.1.20-2.0.1" apply false
    alias(libs.plugins.android.library) apply false
    kotlin("plugin.serialization") version "2.1.20"
}

buildscript {
    repositories {
        mavenCentral()
    }
}