plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.accessai.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.accessai.sample"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = providers.gradleProperty("VERSION_NAME").get()
        // Empty = run without a server (LocalRulesBackend). Override with -PaccessaiUrl=https://…
        buildConfigField("String", "ACCESSAI_URL", "\"${providers.gradleProperty("accessaiUrl").orElse("").get()}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":accessai-compose"))
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
