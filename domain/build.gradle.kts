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