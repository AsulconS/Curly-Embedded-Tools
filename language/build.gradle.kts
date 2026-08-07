import org.jetbrains.intellij.platform.gradle.TestFrameworkType

dependencies {
    intellijPlatform {
        bundledModule("intellij.platform.lang")

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(libs.junit4)
}
