plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}
android {
    namespace = "com.gothwad.accounts.core"
    compileSdk = 36
    defaultConfig { minSdk = 26; consumerProguardFiles("consumer-rules.pro") }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }

    publishing { singleVariant("release") { withSourcesJar() } }
}
kotlin { jvmToolchain(17) }
dependencies {
    api(libs.coroutines)
    api(libs.okhttp)
    implementation(libs.browser)
    implementation(libs.serialization)
    implementation(libs.tink)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
afterEvaluate {
    publishing { publications { register<MavenPublication>("release") {
        from(components["release"])
        groupId = "com.github.GothwadTech.accountsapp"
        artifactId = "core"
        version = providers.gradleProperty("version").getOrElse("0.1.0-SNAPSHOT")
    } } }
}
