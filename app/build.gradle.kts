plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.germanbartoli.codexpulse"
    compileSdk = 37
    buildToolsVersion = "36.1.0"
    defaultConfig {
        applicationId = "com.germanbartoli.codexpulse"
        minSdk = 31
        targetSdk = 36
        versionCode = 8
        versionName = "0.5.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("publicacion") {
            val ruta = System.getenv("PULSE_KEYSTORE")
            if (ruta != null) {
                storeFile = file(ruta)
                storePassword = System.getenv("PULSE_STORE_PASSWORD")
                keyAlias = "codexpulse"
                keyPassword = System.getenv("PULSE_STORE_PASSWORD")
            }
        }
    }
    buildTypes {
        debug { if (System.getenv("PULSE_KEYSTORE") != null) signingConfig = signingConfigs.getByName("publicacion") }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("publicacion")
        }
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isReturnDefaultValues = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260719")
}
