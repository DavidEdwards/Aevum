plugins {
    id("aevum.library")
    id("aevum.compose")

    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

dependencies {
    // region Internal Modules
    implementation(projects.db)
    implementation(projects.domain)
    implementation(projects.common)
    // endregion

    // region Logging
    implementation(libs.timber)
    // endregion

    // Paging
    implementation(libs.paging.compose)
    // endregion

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // region Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav)
    // endregion

    testImplementation(libs.bundles.testing.jvm)
}
