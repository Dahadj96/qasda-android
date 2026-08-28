import java.util.Properties

// The API key is read from local.properties (git-ignored) rather than written
// here. It is not a user secret - it identifies the client app, and anyone can
// pull it out of an APK - but a key committed to the repo is a key that has to
// be rotated in public when it changes, and it just changed once already when
// the backend moved off the VPS.
//
// Put these in local.properties, which Android Studio already keeps out of git:
//   QASDA_API_KEY_DEV=...
//   QASDA_API_KEY_PROD=...
// Without them the build still falls back to the placeholder, which the server
// answers with 401 - a clear failure rather than a confusing one.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun qasdaKey(name: String): String =
    localProps.getProperty(name) ?: System.getenv(name) ?: "dev-local-key-change-me"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "pro.qasdatrip.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "pro.qasdatrip.app"
        minSdk = 26              // Android 8. Below that is under 3% of Algerian devices.
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // The same app-level key the web bundle carries. Not a user secret —
        // it identifies the client, and the server treats it that way.
        buildConfigField("String", "API_BASE", "\"https://qasdatrip.pro\"")
        buildConfigField("String", "API_KEY", "\"${qasdaKey("QASDA_API_KEY_PROD")}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE", "\"https://dev.qasdatrip.pro\"")
            buildConfigField("String", "API_KEY", "\"${qasdaKey("QASDA_API_KEY_DEV")}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coil.compose)
    testImplementation(libs.junit)
}
