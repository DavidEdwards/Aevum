@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("aevum.library")
}

dependencies {
    implementation(libs.timber)

    implementation(projects.db)
    implementation(projects.network)
    implementation(projects.common)

    implementation(libs.javax.inject)

    implementation(libs.room.runtime)
}