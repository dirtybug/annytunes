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

    signingConfigs {
        create("release") {
            val passFile1 = rootProject.file("release/keystore-pass.txt")
            val passFile2 = rootProject.file("keystore-pass.txt")
            val passFile3 = file("keystore-pass.txt")
            val passFromFile = when {
                passFile1.exists() -> passFile1.readText().trim()
                passFile2.exists() -> passFile2.readText().trim()
                passFile3.exists() -> passFile3.readText().trim()
                else -> null
            }
            val finalPassword = passFromFile
                ?: (project.findProperty("keystorePassword") as? String)
                ?: System.getenv("KEYSTORE_PASSWORD")

            val candidateFiles = listOf(
                rootProject.file("release/key.jks"),
                rootProject.file("release/release.keystore"),
                file("release.keystore"),
                file("release/release.keystore")
            )
            val storeF = candidateFiles.firstOrNull { it.exists() }
            val alias = (project.findProperty("keyAlias") as? String)
                ?: System.getenv("KEY_ALIAS")
                ?: "key0"

            if (storeF != null && !finalPassword.isNullOrBlank()) {
                storeFile = storeF
                storePassword = finalPassword
                keyAlias = alias
                keyPassword = finalPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            }
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