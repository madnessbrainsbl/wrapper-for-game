plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.fmguides.fmfdef.dexsingle"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fmguides.fmfdef.dexsingle"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testFunctionalTest = false
        testHandleProfiling = false
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
        isCoreLibraryDesugaringEnabled = true
    }
    buildToolsVersion = "35.0.0"
}


dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.android.gms:play-services-base:18.7.0")
    implementation("com.google.android.gms:play-services-tflite-java:16.4.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.firebase:firebase-core:21.1.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
}
