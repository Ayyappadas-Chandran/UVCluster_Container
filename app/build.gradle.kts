plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.suprajit.uvcluster"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.suprajit.uvcluster"
        minSdk = 34
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    /*implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)*/
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    //implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // Car UI (non-system stub usable in Studio)
    implementation("androidx.car.app:app:1.4.0")

    // Optional safety (even though AAR/JAR already included)
    implementation("com.airbnb.android:lottie:3.4.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okio:okio:1.17.5")

    //For locally getting car and updateengine
    implementation(files("lib/android.car.jar"))
    implementation(files("lib/javalib.jar"))

    implementation(files("lib/sdp-android-1.1.1.aar"))
    implementation(files("lib/ssp-android-1.1.1.aar"))
    implementation(files("lib/media3-common-1.1.1.aar"))
    implementation(files("lib/media3-exoplayer-1.1.1.aar"))
    implementation(files("lib/media3-ui-1.1.1.aar"))
    implementation(files("lib/dotsindicator-4.3.aar"))

    //excluding profileinstaller, for emulator
    configurations.all {
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
    }
/*    implementation(files("lib/lottie-3.4.0.aar"))
    implementation(files("lib/gson-2.10.1.jar"))
    implementation(files("lib/okio-1.17.5.jar"))*/
}