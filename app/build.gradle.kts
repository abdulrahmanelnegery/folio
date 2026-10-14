plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.folio.reader"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.folio.reader"
        minSdk = 26
        targetSdk = 35
        versionCode = 29
        versionName = "0.11.2"
    }

    signingConfigs {
        create("release") {
            // Every scheme: v1 for old Android, v2 and v3 (which lets the key be rotated later) and v4 for streamed installs.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
            val store = System.getenv("FOLIO_KEYSTORE")
            if (store != null) {
                storeFile = file(store)
                storePassword = System.getenv("FOLIO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FOLIO_KEY_ALIAS") ?: "folio"
                keyPassword = System.getenv("FOLIO_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("FOLIO_KEYSTORE") != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "org/bouncycastle/pqc/**") }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.pdfbox.android)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
}
