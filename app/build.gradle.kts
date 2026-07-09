plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}

android {
    namespace = "com.kaeru.app"
    compileSdk {
        version = release(36)
        packaging {
            resources {
                excludes += "META-INF/INDEX.LIST"
                excludes += "META-INF/DEPENDENCIES"
                excludes += "META-INF/LICENSE"
                excludes += "META-INF/LICENSE.txt"
                excludes += "META-INF/license.txt"
                excludes += "META-INF/NOTICE"
                excludes += "META-INF/NOTICE.txt"
                excludes += "META-INF/notice.txt"
                excludes += "META-INF/ASL2.0"
            }
        }
    }

    defaultConfig {
        applicationId = "com.kaeru.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 70000
        versionName = "7.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    implementation("com.google.api-client:google-api-client-android:2.9.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.43.0")
    implementation("io.github.koalaplot:koalaplot-core:0.11.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.3")
    implementation("com.kizitonwose.calendar:compose:2.10.1")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:5.3.2")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation(libs.androidx.ui.graphics)
    kapt("androidx.room:room-compiler:2.8.4")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.materialkolor:material-kolor:4.1.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("org.jsoup:jsoup:1.22.1")
    implementation("androidx.compose.material3:material3:1.5.0-alpha17")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}