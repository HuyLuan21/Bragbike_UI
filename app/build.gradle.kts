import java.util.Properties

// Đọc local.properties
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.bragbike"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bragbike"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject biến vào BuildConfig
        buildConfigField("String", "API_KEY", "\"${localProps["API_KEY"] ?: ""}\"")
        buildConfigField("String", "BASE_URL", "\"${localProps["BASE_URL"] ?: ""}\"")
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

    // Bắt buộc để dùng BuildConfig với AGP 8+
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("io.socket:socket.io-client:2.1.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")
}