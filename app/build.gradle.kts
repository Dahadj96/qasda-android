import java.io.File
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

// The upload keystore and its passwords, by the same route and for a stronger
// reason: these ARE secrets. Whoever holds this file can publish an update
// that every existing install accepts as ours, and it cannot be rotated - an
// app signed by a different key is a different app to Android, and the only
// way back is a new listing.
//
//   QASDA_KEYSTORE=C:/Users/you/keys/qasda-upload.jks
//   QASDA_KEYSTORE_PASSWORD=...
//   QASDA_KEY_ALIAS=qasda
//   QASDA_KEY_PASSWORD=...
//
// With no keystore configured, a release build still runs and still shrinks -
// it just comes out unsigned, which is what a CI runner wants anyway. It is
// only installing on a phone or uploading to Play that needs the signature.
val keystoreFile: File? = (localProps.getProperty("QASDA_KEYSTORE") ?: System.getenv("QASDA_KEYSTORE"))
    ?.let { path -> File(path).takeIf(File::isAbsolute) ?: rootProject.file(path) }
    ?.takeIf(File::exists)

fun qasdaSecret(name: String): String =
    localProps.getProperty(name)
        ?: System.getenv(name)
        ?: error("$name is required to sign a release build. See app/build.gradle.kts.")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

android {
    namespace = "pro.qasdatrip.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "pro.qasdatrip.app"
        minSdk = 26              // Android 8. Below that is under 3% of Algerian devices.
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        // The same app-level key the web bundle carries. Not a user secret —
        // it identifies the client, and the server treats it that way.
        buildConfigField("String", "API_BASE", "\"https://qasdatrip.pro\"")
        buildConfigField("String", "API_KEY", "\"${qasdaKey("QASDA_API_KEY_PROD")}\"")
        // Public OAuth client ID, not a secret. Set after enabling Google auth.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps.getProperty("QASDA_GOOGLE_WEB_CLIENT_ID") ?: System.getenv("QASDA_GOOGLE_WEB_CLIENT_ID") ?: "917623534436-skkt7j0inhidudnderaao6j7idrc1p1p.apps.googleusercontent.com"}\"")
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = keystoreFile
                storePassword = qasdaSecret("QASDA_KEYSTORE_PASSWORD")
                keyAlias = qasdaSecret("QASDA_KEY_ALIAS")
                keyPassword = qasdaSecret("QASDA_KEY_PASSWORD")
            }
        }
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
            // With no upload keystore configured, fall back to the debug key
            // so a minified build can still be installed and exercised - which
            // is the only way to find out whether R8 broke anything. Play
            // rejects a debug-signed upload outright, so this cannot quietly
            // become a real release.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }

        // R8 is the difference between a build that works and a build that
        // ships, and the only way to know which one you have is to run it.
        //
        // This is `release` in every respect R8 can see - it copies it, so
        // the same minification, shrinking and keep rules apply - and points
        // at dev, so the shrunk app can be exercised against a server that
        // exists. The real domain spent the day serving a parked-domain page,
        // which is exactly the kind of thing that makes a release-build test
        // meaningless when it is the only environment you can aim at.
        //
        // Declared after `release` on purpose: initWith copies the block as
        // it stands, and the minification above is set inside it. Moved
        // earlier, this would silently copy an unminified release and prove
        // nothing at all.
        //
        // It cannot become a shipped build: the applicationId ends .staging.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            matchingFallbacks += listOf("release")
            buildConfigField("String", "API_BASE", "\"https://dev.qasdatrip.pro\"")
            buildConfigField("String", "API_KEY", "\"${qasdaKey("QASDA_API_KEY_DEV")}\"")
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

    // The app stores a few of core's own models on disk - the recent searches
    // - so it needs the serialization runtime itself rather than borrowing
    // core's, which is an implementation detail of that module.
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
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
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.google.identity)
    implementation(libs.coroutines.play.services)
    testImplementation(libs.junit)
}
