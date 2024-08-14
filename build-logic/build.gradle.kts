plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.pluginForGradle)
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.compilerEmbeddable)
    implementation(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        create("library") {
            id = "aevum.library"
            implementationClass = "dae.aevum.build_logic.LibraryPlugin"
        }
        create("app") {
            id = "aevum.app"
            implementationClass = "dae.aevum.build_logic.AppPlugin"
        }
        create("compose") {
            id = "aevum.compose"
            implementationClass = "dae.aevum.build_logic.ComposePlugin"
        }
        create("ktlint") {
            id = "aevum.ktlint"
            implementationClass = "dae.aevum.build_logic.KtlintPlugin"
        }
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}
