// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // This is your app plugin
    id("com.android.application") version "8.7.2" apply false

    // This is your Kotlin plugin
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false

    // ADD THIS LINE for Google Services
    id("com.google.gms.google-services") version "4.4.0" apply false
}