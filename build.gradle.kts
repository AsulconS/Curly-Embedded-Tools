import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

plugins {
    application
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.kotlin.jvm")
    id("rpc") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
}

subprojects {
    apply(plugin = "org.jetbrains.intellij.platform.module")
    apply(plugin = "rpc")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2026.1.3")

        pluginModule(implementation(project(":shared")))
        pluginModule(implementation(project(":frontend")))
        pluginModule(implementation(project(":backend")))
        pluginModule(implementation(project(":language")))

        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.BOTH
}

/**
 * A plain, monolithic sandbox for exercising the language support.
 *
 * `runIde` inherits `splitMode = true` from above, which makes the sandbox present itself as a
 * remote-dev host; the marketplace then resolves the "for JetBrains Client" companion of anything you
 * install there. That is how a plain `Python Community Edition` install also drags in
 * `python-frontend-plugin`, whose `intellij.grid.charts.impl` module id collides with the bundled Data
 * Editor Support — the conflict disables the database/persistence chain and takes this plugin with it.
 *
 * Nothing in the three languages is split-mode-specific, so test them here instead. Use `runIde` only
 * when the split-mode RPC demo is what you are actually working on.
 */
intellijPlatformTesting {
    runIde {
        register("runIdeLanguages") {
            splitMode = false
            sandboxDirectory = layout.buildDirectory.dir("sandbox-languages")
        }
    }
}
