plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystoreFile = System.getenv("LUCENT_KEYSTORE_FILE")
val releaseStorePassword = System.getenv("LUCENT_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("LUCENT_KEY_ALIAS")
val releaseKeyPassword = System.getenv("LUCENT_KEY_PASSWORD")
val hasStableReleaseSigning = listOf(
    releaseKeystoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

// GitHub Actions supplies the workflow run number so each build has a monotonic
// versionCode. Local builds retain a safe development fallback.
val lucentVersionCode = System.getenv("LUCENT_VERSION_CODE")
    ?.toIntOrNull()
    ?.takeIf { it in 1..2_100_000_000 }
    ?: 1
val lucentVersionName = System.getenv("LUCENT_VERSION_NAME")
    ?.takeIf { it.isNotBlank() }
    ?: "0.1.0"

android {
    namespace = "com.tejas.liquidplayer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tejas.liquidplayer"
        minSdk = 23
        targetSdk = 36
        versionCode = lucentVersionCode
        versionName = lucentVersionName
    }

    signingConfigs {
        if (hasStableReleaseSigning) {
            create("stableRelease") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasStableReleaseSigning) {
                signingConfig = signingConfigs.getByName("stableRelease")
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("production") {
            dimension = "environment"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")

    // The actual Liquid Glass effect library requested for this project.
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")

    // VLC's playback engine, independent from this app's custom UI.
    implementation("org.videolan.android:libvlc-all:3.7.7")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
