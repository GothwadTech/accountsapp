plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.gothwad.accounts"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.gothwad.accounts"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
        manifestPlaceholders["gothwadRedirectScheme"] = "com.gothwad.accounts"
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":ui"))
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
}
