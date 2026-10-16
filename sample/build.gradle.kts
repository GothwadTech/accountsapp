plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.gothwad.accounts.sample"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.gothwad.accounts.sample"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
        manifestPlaceholders["gothwadRedirectScheme"] = "com.gothwad.accounts"
    }
    flavorDimensions += "host"
    productFlavors {
        create("alpha") { dimension = "host"; applicationIdSuffix = ".alpha"; manifestPlaceholders["gothwadRedirectScheme"] = "com.gothwad.accounts.sample.alpha" }
        create("beta") { dimension = "host"; applicationIdSuffix = ".beta"; manifestPlaceholders["gothwadRedirectScheme"] = "com.gothwad.accounts.sample.beta" }
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
