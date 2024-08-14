@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("aevum.library")
    id("aevum.compose")

    alias(libs.plugins.compose.compiler)
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

    // region Compose
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.activity)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material2)
    implementation(libs.material3)
    implementation(libs.compose.nav)
    implementation(libs.compose.icons)

    implementation(libs.accompanist.permissions)
    // endregion

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // region Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.hilt.nav)
    // endregion

    testImplementation(libs.bundles.testing.jvm)
}