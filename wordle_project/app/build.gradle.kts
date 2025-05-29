plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.wordle_project"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wordle_project"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas".toString(),
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
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
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    testImplementation (libs.robolectric.v48)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.okhttp)
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime.android)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    androidTestImplementation(libs.room.testing)
    testImplementation(libs.core)
    testImplementation(libs.ext.junit)
    runtimeOnly(libs.room.runtime)
    testImplementation(libs.junit)
    annotationProcessor(libs.room.compiler)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation (libs.junit)
    testImplementation (libs.mockito.core)
    testImplementation (libs.robolectric)
    androidTestImplementation (libs.junit.v115)
    androidTestImplementation (libs.espresso.core.v351)
}