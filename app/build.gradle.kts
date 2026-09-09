plugins {
    id("com.android.application")
}

android {
    namespace = "com.bazaverse.arcanewaves"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bazaverse.arcanewaves"
        minSdk = 23
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
