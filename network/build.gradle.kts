@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("aevum.library")
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
}