plugins {
    id("com.android.application")
    id("com.chaquo.python")
}

android {
    namespace = "com.artemis.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.artemis.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.2"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
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

    sourceSets {
        getByName("main") {
            java.srcDir("../../packages/artemis-accessibility-helper/app/src/main/java")
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.12"
    }
}
