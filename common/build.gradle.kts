plugins {
    id("aevum.library")

    alias(libs.plugins.ksp)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt)
}

dependencies {
    implementation(libs.timber)

    // region Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.nav)
    // endregion
}