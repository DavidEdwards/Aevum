package dae.aevum.build_logic

import org.gradle.api.Plugin
import org.gradle.api.Project

class LibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.run {
            apply("aevum.ktlint")
            apply("com.android.library")
            // kotlin-android is bundled with AGP 9+
            withPlugin("com.android.library") {
                target.baseSetup()
                target.tasks.register("aevumUnitTest") {
                    dependsOn("testDebugUnitTest")
                }
            }
        }
    }
}
