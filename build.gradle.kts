// Top-level build file
//
// AGP 9.0 introduced built-in Kotlin support (android.builtInKotlin=true).
// AGP bundles KGP 2.2.10; we override it here to stay on 2.2.21 until
// Hilt ships a kotlin-metadata-jvm that supports metadata 2.3.0.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.2.21-2.0.5")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    // kotlin.android and kotlin.compose are intentionally NOT listed here.
    // AGP 9 built-in Kotlin (android.builtInKotlin=true) replaces kotlin.android.
    // The Compose compiler plugin is wired automatically when the Compose
    // build feature is enabled under built-in Kotlin.
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.junit5) apply false
    alias(libs.plugins.kover) apply false
}
