plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-android-extensions")
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")

    defaultConfig {
        applicationId("com.genovich.remembertaps")
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode(1)
        versionName("1.0")

        testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")
    }

    buildTypes {
        named("release") {
            minifyEnabled(false)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to arrayOf("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.0")

    implementation("androidx.activity:activity-ktx:1.2.0-alpha07")
    implementation("androidx.appcompat:appcompat:1.3.0-alpha01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-rc1")
    implementation("androidx.core:core-ktx:1.5.0-alpha01")
    implementation("androidx.recyclerview:recyclerview:1.2.0-alpha05")
    implementation("com.google.android.material:material:1.3.0-alpha02")

    val arrowVersion = "0.10.4"
    implementation("io.arrow-kt:arrow-fx:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    kapt("io.arrow-kt:arrow-meta:$arrowVersion")

    testImplementation("junit:junit:4.13")
    androidTestImplementation("androidx.test.ext:junit:1.1.2-rc03")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0-rc03")
}