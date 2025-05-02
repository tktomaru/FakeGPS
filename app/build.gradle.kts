import java.io.FileInputStream
import java.util.Properties

val apikeyPropsFile = rootProject.file("local.properties")
val apikeyProps = Properties().apply {
    if (apikeyPropsFile.exists()) {
        load(FileInputStream(apikeyPropsFile))
    }
}

configurations.configureEach {
    exclude(group = "com.intellij", module = "annotations")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "jp.tukutano.fakegps"
    compileSdk = 35

    defaultConfig {
        applicationId = "jp.tukutano.fakegps"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["googleMapsKey"] = apikeyProps["MAPS_API_KEY"] as? String ?: ""
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    kapt("androidx.room:room-compiler:2.5.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.1.0")

}