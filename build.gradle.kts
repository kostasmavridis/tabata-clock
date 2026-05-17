// Top-level build file
//
// kotlin.android is NOT applied: AGP 9 built-in Kotlin owns compilation.
// kotlin.compose IS still required: AGP built-in Kotlin does not auto-wire
// the Compose compiler — it must be applied explicitly.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.junit5) apply false
    alias(libs.plugins.kover) apply false
}
