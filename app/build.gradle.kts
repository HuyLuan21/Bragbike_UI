import java.util.Properties

// Đọc local.properties
val localProps = Properties()
val localPropsFile = rootProject.projectDir.resolve("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
} else {
    println("local.properties NOT FOUND at: ${localPropsFile.absolutePath}")
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
        
        // Mapbox Access Token từ local.properties
        val mapboxToken = localProps["MAPBOX_ACCESS_TOKEN"]?.toString() ?: ""
        resValue("string", "mapbox_access_token", mapboxToken)

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

    // Bắt buộc để dùng các tính năng nâng cao trong AGP 8+
    buildFeatures {
        buildConfig = true
        viewBinding = true
        resValues = true
    }

    configurations.all {
        resolutionStrategy {
            force("com.google.code.gson:gson:2.10.1")
        }
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
    implementation(libs.gson)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Mapbox
    implementation("com.mapbox.maps:android-ndk27:11.22.0")
    implementation(libs.mapbox.android)
    implementation(libs.mapbox.sdk.services)
    implementation(libs.mapbox.sdk.turf)
}