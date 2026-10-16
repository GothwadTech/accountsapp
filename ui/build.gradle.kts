plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}
android {
    namespace = "com.gothwad.accounts.ui"
    compileSdk = 36
    defaultConfig { minSdk = 26; consumerProguardFiles("consumer-rules.pro") }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true }
    publishing { singleVariant("release") { withSourcesJar() } }
}
kotlin { jvmToolchain(17) }
dependencies {
    api(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
}
afterEvaluate {
    publishing { publications { register<MavenPublication>("release") {
        from(components["release"])
        groupId = "com.github.GothwadTech.accountsapp"
        artifactId = "ui"
        version = providers.gradleProperty("version").getOrElse("0.1.0-SNAPSHOT")
    } } }
}
