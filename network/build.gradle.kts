plugins {
    id("aevum.library")

    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

dependencies {
    // region Internal Modules
    implementation(projects.db)
    implementation(projects.common)
    // endregion

    // region Logging
    implementation(libs.timber)
    // endregion

    // region Networking
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.base)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    // endregion

    // region Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav)
    // endregion
}