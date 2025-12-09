plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services") version "4.4.1"
}

android {
    namespace = "com.example.citytripv2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.citytripv2"
        minSdk = 24
        targetSdk = 35
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- CRUCIAAL: Expliciete versies om de error te fixen ---
    // We dwingen hier oudere, stabiele versies af zodat Gradle niet de "alpha" versies pakt
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Compose Setup
    implementation(platform("androidx.compose:compose-bom:2024.05.00")) // Iets nieuwere BOM
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // --- JOUW VEREISTEN ---

    // 1. Navigatie
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 2. Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // 3. OSMDroid
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // 4. Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // 5. Extra's
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}