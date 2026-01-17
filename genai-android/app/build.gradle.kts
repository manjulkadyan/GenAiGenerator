import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.manjul.genai.videogenerator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.manjul.genai.videogenerator"
        minSdk = 24
        targetSdk = 35
        versionCode = 15
        versionName = "0.1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Load keystore properties if they exist
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use {
            keystoreProperties.load(it)
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                val storeFilePath = keystoreProperties.getProperty("storeFile")
                if (storeFilePath != null) {
                    storeFile = file(storeFilePath)
                }
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Apply signing config if keystore.properties exists
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField("Boolean", "STRICT_MODE_ENABLED", "false")
            buildConfigField("Boolean", "DEBUG", "false")
        }
        getByName("debug") {
            buildConfigField("Boolean", "STRICT_MODE_ENABLED", "true")
            buildConfigField("Boolean", "DEBUG", "true")
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
        buildConfig = true // needed for custom BuildConfig fields (STRICT_MODE_ENABLED, DEBUG_BUILD)
    }

    // Compose Compiler is now configured via the Gradle plugin (Kotlin 2.0+)
    // No need for composeOptions block anymore

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("com.google.android.material:material:1.12.0")

    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.code.gson:gson:2.11.0")
    
    // Google Play Billing - Updated to 8.0.0+ (required by Aug 31, 2025)
    implementation("com.android.billingclient:billing-ktx:8.1.0")
    
    // Google Play In-App Review
    implementation("com.google.android.play:review-ktx:2.0.2")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-datasource:1.4.1") // Includes CacheDataSource
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-database:1.4.1") // For cache database (SimpleCache)
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    
    // Room Database for caching
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    // Migrated from KAPT to KSP for Kotlin 2.0+ compatibility
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
