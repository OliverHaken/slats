plugins {
    id("com.android.application")
}

android {
    namespace = "com.slats.game"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.slats.game"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
