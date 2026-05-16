import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.junit5)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.kostasmavridis.tabataclock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kostasmavridis.tabataclock"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // Read signing config from environment variables (set by CI) or local.properties (local dev)
    val keystorePath     = System.getenv("KEYSTORE_PATH")
    val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
    val keyAlias         = System.getenv("KEY_ALIAS")
    val keyPassword      = System.getenv("KEY_PASSWORD")

    signingConfigs {
        if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                storeFile     = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Use the persistent keystore when available (CI), otherwise fall back to default debug signing
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null) signingConfig = releaseSigning
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null) signingConfig = releaseSigning
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/libdatastore_shared_counter.so"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.di.*",
                    "*_HiltModules*",
                    "*_Factory*",
                    "*.BuildConfig",
                    "*.ComposableSingletons*"
                )
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Unit tests (JVM)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}
