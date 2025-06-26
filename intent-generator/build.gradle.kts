plugins {
    id("java-library")
    id("com.google.devtools.ksp")
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11 //Optimal
    targetCompatibility = JavaVersion.VERSION_11 //Optimal
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11 //Optimal
    }
}

dependencies {
    testImplementation(libs.junit)

    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-2.0.1")
    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("com.squareup:kotlinpoet-ksp:1.14.2")

//    implementation("com.google.auto.service:auto-service:1.1.1")
//    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}