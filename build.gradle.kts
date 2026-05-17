// Top-level build file
//
// kotlin.android and kotlin.compose are intentionally NOT applied here.
// AGP 9.0 built-in Kotlin (enabled by default) owns Kotlin compilation.
// The Compose compiler plugin is wired automatically via buildFeatures.compose = true.
// KSP 2.3.1+ supports AGP 9 built-in Kotlin (added in ksp#2674).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.junit5) apply false
    alias(libs.plugins.kover) apply false
}
