plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.securetreasure"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.securetreasure"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }


}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Compose
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Optional animations
    implementation("com.airbnb.android:lottie-compose:6.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Encryption helper (Javax)
    implementation("org.bouncycastle:bcprov-jdk15to18:1.77")
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")

    // In app/build.gradle.kts


        // ... (your other libraries like core-ktx, compose, etc.)

        // Encryption helper (Javax)
        implementation("org.bouncycastle:bcprov-jdk15to18:1.77")

        // --- ADD THESE LINES FOR FIREBASE ---
        // Add the Firebase Bill of Materials (BoM)
        implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

        // Add the dependencies for the services you need
        implementation("com.google.firebase:firebase-firestore-ktx")     // For Firestore Database
        implementation("com.google.firebase:firebase-messaging-ktx")   // For Push Notifications
        implementation("com.google.firebase:firebase-functions-ktx")   // For Cloud Functions
        // ------------------------------------

}
