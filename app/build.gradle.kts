plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.app.anytunes"
    compileSdk = 35

    val appVersionName = (project.findProperty("versionName") as? String)
        ?: System.getenv("APP_VERSION_NAME")
        ?: "1.3.0"
    val appVersionCode = (project.findProperty("versionCode") as? String)?.toIntOrNull()
        ?: System.getenv("APP_VERSION_CODE")?.toIntOrNull()
        ?: 13

    defaultConfig {
        applicationId = "com.app.annytunes"
        minSdk = 31
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
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