plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.anchornotes_team3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.anchornotes_team3"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Networking - Retrofit, OkHttp, Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Image loading - Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Markdown - Markwon for rendering and editing
    implementation("io.noties.markwon:core:4.6.2")
    
    // Color Picker - Material Color Picker Dialog
    implementation("com.github.dhaval2404:colorpicker:2.3")
    
    // Geofencing - Google Play Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Google Maps - Map display and Places API
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.libraries.places:places:3.3.0")

    // PDF Generation - iText7 for Android
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Core library desugaring for Java 8+ APIs on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // Testing - Local Unit Tests (JUnit + Robolectric + Mockito)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    
    // Testing - Instrumented Tests (Espresso)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0")
}