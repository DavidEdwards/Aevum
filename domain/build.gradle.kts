plugins {
    id("aevum.library")

    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

dependencies {
    implementation(libs.timber)

    // Paging for issues list
    implementation(libs.paging.runtime)

    implementation(projects.db)
    implementation(projects.network)
    implementation(projects.common)

    implementation(libs.javax.inject)

    implementation(libs.room.runtime)

    implementation(libs.datastore.preferences)

    // region Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav)
    // endregion
}