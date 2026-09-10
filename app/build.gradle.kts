plugins {
    id("com.android.application")
}

android {
    namespace = "com.bazaverse.arcanewaves"
    compileSdk = 36

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("arcane-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.bazaverse.arcanewaves"
        minSdk = 23
        targetSdk = 36
        versionCode = 18
        versionName = "2.2.0"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
