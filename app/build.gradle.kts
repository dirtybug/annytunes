plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.app.anytunes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.annytunes"
        minSdk = 31
        targetSdk = 36
        versionCode = 3
        versionName = "1.3"


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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("com.github.felHR85:UsbSerial:6.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}