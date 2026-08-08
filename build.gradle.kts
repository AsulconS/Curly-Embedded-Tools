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

        // Marketplace ZIP Signer CLI, resolved for the `signPlugin` task.
        zipSigner()
    }
}

intellijPlatform {
    projectName = "Curly-Embedded-Tools"
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.BOTH

    /**
     * Plugin signing — https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
     *
     * Two credential sources, in the order the Marketplace ZIP Signer consults them:
     *
     *  - **A PKCS#12 keystore** in the gitignored `certificate/` directory, for local signing. The
     *    documented `openssl genpkey -aes-256-cbc` → `privateKeyFile` route does *not* work: signer
     *    0.1.43 hands encrypted PEM to Bouncy Castle without ever registering its security provider,
     *    so a stock JDK fails on `AES/CBC/PKCS7Padding` (PKCS#8) or `PBKDF-OpenSSL` (traditional PEM).
     *    Only an unencrypted key survives that path. A keystore is read through plain JCA instead,
     *    which is what keeps the key password-protected at rest. See README for the generation steps.
     *  - **Three environment variables** holding PEM text, for CI, where the secret store — not a
     *    passphrase — is what protects the key. The keystore wins whenever its file is present, and
     *    on CI it is not.
     *
     * `keyStore` is only wired when the file exists: `certificateChainFile`/`privateKeyFile` are
     * `@InputFile`s that fail task validation on a missing path, and an absent keystore must leave the
     * environment-variable route intact rather than break it.
     */
    signing {
        val certificateDir = layout.projectDirectory.dir("certificate")
        val chainFile = certificateDir.file("chain.crt")
        val keyStoreFile = certificateDir.file("keystore.p12")
        val keyPassword = providers.environmentVariable("PRIVATE_KEY_PASSWORD")

        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = keyPassword

        // Also read by `verifyPluginSignature`, which checks the signed archive against this chain.
        if (chainFile.asFile.exists()) certificateChainFile = chainFile

        if (keyStoreFile.asFile.exists()) {
            keyStore = keyStoreFile
            keyStoreType = "PKCS12"
            keyStoreKeyAlias = "curly-embedded-tools"
            keyStorePassword = keyPassword
        }
    }

    /**
     * Publishing — https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html
     *
     * `publishPlugin` uploads the *signed* archive, so it pulls `signPlugin` into the task graph and
     * inherits its credentials: `PUBLISH_TOKEN` alone is not enough, the signing key has to be
     * reachable too. Tokens are issued at https://plugins.jetbrains.com/author/me/tokens.
     *
     * The release channel is derived from the version's pre-release label rather than pinned, because
     * Marketplace refuses a second artifact with the same version — recovering from a wrong-channel
     * upload means burning a version number. `1.0.0-SNAPSHOT` therefore lands in a `snapshot` channel
     * that users must add as a custom repository, `2.1.7-alpha.3` in `alpha`, and only a label-free
     * `1.0.0` reaches `default`, the channel every IDE sees.
     *
     * Marketplace also requires the *first* upload of a plugin to go through the web UI; `publishPlugin`
     * only ever publishes updates to an already-registered plugin.
     */
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("version").map { version ->
            listOf(version.substringAfter('-', "").substringBefore('.').lowercase().ifEmpty { "default" })
        }
    }
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
