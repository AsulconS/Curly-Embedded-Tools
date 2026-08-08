# Curly Embedded Tools

[![Twitter Follow](https://img.shields.io/badge/follow-%40JBPlatform-1DA1F2?logo=twitter)](https://twitter.com/JBPlatform)
[![Developers Forum](https://img.shields.io/badge/JetBrains%20Platform-Join-blue)][jb:forum]

## Overview

This repository implements a modular IntelliJ Platform plugin. It uses content modules as a unit of functionality that the plugin consists of. Content modules are split into:

- `frontend` - UI code
- `backend` - stateful business logic
- `shared`

Frontend communicates with the backend through RPC.

This structure allows to:

- separate UI code from business logic
- implement features in a way they work natively in **[split mode][docs:remote-dev]** just like in the ordinary monolithic IDE
- keep the plugin code cleaner

## Demo Functionality

The sample plugin adds a `ModularPlugin` tool window with a chat-style UI implemented with the Swing framework.

## Plugin structure

A generated project contains the following content structure:

```
.
├── .run/                   Predefined Run/Debug Configurations
├── backend/                Backend module – business logic
│   ├── build.gradle.kts    Backend dependencies
│   └── src/main/
│       ├── kotlin/         Kotlin production sources
│       └── resources/      Curly.Embedded.Tools.backend.xml
├── frontend/               Frontend module – UI and presentation
│   ├── build.gradle.kts    Frontend dependencies
│   └── src/main/
│       ├── kotlin/         Kotlin production sources
│       └── resources/      Curly.Embedded.Tools.frontend.xml
├── shared/                 Shared module – cross-boundary contracts
│   ├── build.gradle.kts    Shared dependencies
│   └── src/main/
│       ├── kotlin/         Kotlin production sources
│       └── resources/      Curly.Embedded.Tools.shared.xml
├── gradle/
│   ├── wrapper/            Gradle Wrapper
│   └── libs.versions.toml  Version catalog
├── src
│   └── main
│       └── resources/
│           └── META-INF/   Plugin configuration file and logo
├── .gitignore              Git ignoring rules
├── build.gradle.kts        Root build – assembles the final plugin
├── gradle.properties       Gradle configuration properties
├── gradlew                 *nix Gradle Wrapper script
├── gradlew.bat             Windows Gradle Wrapper script
└── settings.gradle.kts     Gradle project settings
```

> [!NOTE]
> To use Java in your plugin, create the appropriate `/src/main/java` directory within the desired module.

The plugin logo is placed in `src/main/resources/META-INF/pluginIcon.svg`. See [Plugin Logo][docs:logo] for more information and logo requirements.

### Module Layout

- `root project` assembles the final plugin, declares the main IntelliJ Platform dependency, enables split mode, and includes the `shared`, `frontend`, and `backend` plugin modules in the final distribution.
- `shared` contains contracts that both sides must understand: RPC interfaces, DTOs, serializers, and shared model types. Put a cross-boundary API here.
- `frontend` contains UI-only code and presentation logic: the tool window registration, Swing UI, view models, and the frontend adapter that talks to the backend via RPC.
- `backend` contains project-level services and business logic: access to project, file system, and external processes, message creation, response generation, and the RPC implementation exposed to the frontend.

## Build script

The root [build.gradle.kts][file:build.gradle.kts] assembles the final plugin and applies the following Gradle plugins:

| Plugin                            | Description                                                                      |
|-----------------------------------|----------------------------------------------------------------------------------|
| `org.jetbrains.kotlin.jvm`        | Adds Kotlin support                                                              |
| `org.jetbrains.changelog`         | Simplifies patching the [CHANGELOG.md][file:CHANGELOG.md] file                   |
| `org.jetbrains.intellij.platform` | The [IntelliJ Platform Gradle Plugin][docs:intellij-platform-gradle-plugin-docs] |

The `intellijPlatform` dependencies block selects the IDE to compile against:

```kotlin
intellijIdea("2025.3.5")
```

See [Target Versions][docs:target-version] for more information.

The `intellijPlatform` dependencies block also contains a dependency on the platform testing framework:

```kotlin
testFramework(TestFrameworkType.Platform)
```

See [Testing][docs:testing] for more information

## Plugin configuration files

The root [plugin.xml][file:plugin.xml] file located in `src/main/resources/META-INF` provides general information about the plugin, its dependencies, and references the per-module plugin descriptors.

Each module ships its own plugin descriptor in its `src/main/resources/` directory:

- `Curly.Embedded.Tools.backend.xml` – registers backend extensions and services
- `Curly.Embedded.Tools.frontend.xml` – registers frontend extensions and tool windows
- `Curly.Embedded.Tools.shared.xml` – registers shared extensions and interfaces

You can read more about plugin configuration files in the [Plugin Configuration File][docs:plugin.xml] section of our documentation.

### Plugin ID and name

Generated plugin ID and name may require adjustment.

These values are generated based on _Group ID_ and _Artifact ID_ provided in the IDE Plugin wizard. It is recommended to review `<id>` and `<name>` elements in the plugin.xml file, and adjust them if needed.

Please note that Gradle properties `rootProject.name` and `project.group` don't need to match the `<id>` and `<name>` elements. There is no IntelliJ Platform-related reason they should as they serve different functions.

## Remote Development Ready Architecture

The demo is intentionally split so that the UI stays frontend-only and the business logic stays backend-only. This ensures optimal UX in the remote development scenario where the IDE has separate frontend and backend
processes. This is what we call **Split Mode**.

A high-level overview of the plugin structure:

- a UI for a chat with an AI assistant natively rendered in the frontend IDE in split mode
- data transfer between the frontend and backend via RPC
- RPC implementation in the backend IDE is capable of touching any backend entities and APIs like a file system

A more detailed explanation of how it is implemented:

1. The frontend registers the tool window and creates `ChatViewModel`.
2. `ChatViewModel` depends on the frontend-facing `ChatRepositoryApi` abstraction instead of directly depending on backend services.
3. `FrontendChatRepositoryModel` implements that abstraction by calling the shared `ChatRepositoryRpcApi` and collecting the backend message `Flow`.
4. The shared module defines `ChatRepositoryRpcApi` plus the DTOs used to cross the RPC boundary.
5. The backend registers `BackendRpcApiProvider`, which exposes `BackendChatRepositoryRpcApi` as the RPC implementation.
6. `BackendChatRepositoryRpcApi` resolves the backend project from `ProjectId` and delegates to `BackendChatRepositoryModel`.
7. `BackendChatRepositoryModel` owns the mutable message list and the demo response generation logic.

This separation keeps the frontend focused on rendering, local UI state, and interaction handling, while the backend owns project-scoped state and logic that should execute on the backend side in split mode.

## Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug configurations* that expose corresponding Gradle tasks:

| Configuration name               | Description                                                                                                                                            |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run IDE with Plugin (Frontend)   | Runs [`:runIdeFrontend`][docs:intellij-platform-gradle-plugin-runIde] IntelliJ Platform Gradle Plugin task. Use the *Debug* icon for plugin debugging. |
| Run IDE with Plugin (Backend)    | Runs [`:runIdeBackend`][docs:intellij-platform-gradle-plugin-runIde] IntelliJ Platform Gradle Plugin task. Use the *Debug* icon for plugin debugging.  |
| Run IDE with Plugin (Split Mode) | Runs both *Run IDE (Backend)* and *Run IDE (Frontend)* configurations simultaneously to launch the plugin in split mode.                               |

> [!NOTE]
> You can find the logs from the running task in the `idea.log` tab.

## IDE compatibility

The `<idea-version>` element is **generated** by `patchPluginXml` from
`intellijPlatform { pluginConfiguration { ideaVersion { … } } }` — editing it in
[plugin.xml][file:plugin.xml] by hand has no effect, the generated value wins.

```
<idea-version since-build="261" />     ← 2026.1 and every later release
```

`sinceBuild` is pinned at `261` instead of being inherited from the compile target, so bumping
`intellijIdea(…)` cannot silently drop users on an older branch. `untilBuild` is set to
`provider { null }`, which emits **no upper bound** — that is what lets a build compiled against 2026.1
install into 2026.2 and beyond. Add an upper bound only against a known incompatibility: a stale one
locks users out for a whole release cycle and can only be lifted by publishing a new version.

Compiling against one branch and running on a later one is checked with the JetBrains Plugin Verifier —
the same tool Marketplace moderation runs:

```bash
./gradlew verifyPlugin     # downloads the IDEs listed in pluginVerification.ides
```

> [!NOTE]
> A **"Not compatible with the version of your running IDE"** banner on the Marketplace page usually
> means the plugin is still in moderation, not that `idea-version` is wrong: an unapproved plugin has no
> published build, and that banner is how the page renders "nothing installable for you". Confirm with
> `https://plugins.jetbrains.com/api/plugins/<id>` — check `approve` and `hasUnapprovedUpdate` — before
> changing any metadata. The plugin page is JS-rendered, so fetching the HTML tells you nothing.

## Signing the plugin

`signPlugin` reads its credentials from the `intellijPlatform { signing { … } }` block in the root
[build.gradle.kts][file:build.gradle.kts], which accepts two sources — a local PKCS#12 keystore, or three
environment variables for CI. The keystore wins whenever `certificate/keystore.p12` is present.

### Local setup

The whole `certificate/` directory is gitignored. Pick a password, export it as `PRIVATE_KEY_PASSWORD`,
and generate a 4096-bit key with a self-signed 365-day chain:

```bash
mkdir -p certificate
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096
openssl req -new -x509 -days 365 -key private.pem -out certificate/chain.crt \
  -subj "/CN=AsulconS/O=AsulconS/emailAddress=adrian.r.bedregal@gmail.com"
openssl pkcs12 -export -inkey private.pem -in certificate/chain.crt \
  -name curly-embedded-tools -out certificate/keystore.p12 -passout pass:"$PRIVATE_KEY_PASSWORD"
rm private.pem
```

> [!IMPORTANT]
> Use a **PKCS#12 keystore**, not the `privateKeyFile` route shown in [Plugin Signing][docs:signing].
> Marketplace ZIP Signer 0.1.43 hands encrypted PEM keys to Bouncy Castle without registering its
> security provider, so on a stock JDK an `openssl genpkey -aes-256-cbc` key fails with
> `Cannot find any provider supporting AES/CBC/PKCS7Padding`, and a traditional-format one fails with
> `PBKDF-OpenSSL SecretKeyFactory not available`. `privateKeyFile` therefore only works with an
> *unencrypted* key. A keystore is read through plain JCA and stays password-protected at rest.
>
> Keep the password ASCII and free of whitespace — it is passed to the signer as a command-line
> argument. A stray `\r` from `openssl rand -base64 … | tr -d '\n'` surfaces as the misleading
> `keystore password was incorrect / Password is not ASCII`.

Then sign, and verify the result against the chain:

```bash
export PRIVATE_KEY_PASSWORD='…'
./gradlew signPlugin
./gradlew verifyPluginSignature
```

`signPlugin` writes `build/distributions/Curly-Embedded-Tools-<version>-signed.zip` alongside the
unsigned archive; `buildPlugin` is unaffected. Run the two tasks in **separate invocations** —
`verifyPluginSignature` consumes `signPlugin`'s output without declaring the dependency, so requesting
both at once fails Gradle's implicit-dependency validation.

### CI setup

Export the PEM text directly (the values are multi-line; base64-encode them if your secret store cannot
hold newlines, and decode before exporting):

| Variable | Contents |
|---|---|
| `CERTIFICATE_CHAIN` | contents of `chain.crt` |
| `PRIVATE_KEY` | contents of an **unencrypted** `private.pem` |
| `PRIVATE_KEY_PASSWORD` | key password — omit for an unencrypted key |

## Publishing the plugin

> [!TIP]
> Make sure to follow all guidelines listed in [Publishing a Plugin][docs:publishing] to follow all recommended and required steps.

Releasing to [JetBrains Marketplace](https://plugins.jetbrains.com) uses the `publishPlugin` task, configured
by the `intellijPlatform { publishing { … } }` block in the root [build.gradle.kts][file:build.gradle.kts]:

```bash
export PUBLISH_TOKEN='…'          # https://plugins.jetbrains.com/author/me/tokens
export PRIVATE_KEY_PASSWORD='…'   # publishPlugin uploads the *signed* archive
./gradlew publishPlugin
```

> [!IMPORTANT]
> The **first** upload of a plugin has to go through the [upload form](https://plugins.jetbrains.com/plugin/upload);
> `publishPlugin` only publishes updates to a plugin Marketplace already knows. `<id>` in
> [plugin.xml][file:plugin.xml] is what ties the two together and cannot change afterwards.

`publishPlugin` pulls `signPlugin` into the task graph — a `PUBLISH_TOKEN` on its own is not enough, the
signing key has to be reachable as well. See [Signing the plugin](#signing-the-plugin).

### Release channel

The channel is derived from the version's pre-release label, so it tracks `version` in
[gradle.properties][file:gradle.properties] instead of being pinned:

| `version` | Channel | Who sees it |
|---|---|---|
| `1.0.0` | `default` | every IDE |
| `1.0.0-SNAPSHOT` | `snapshot` | only users who added the channel as a custom repository |
| `2.1.7-alpha.3` | `alpha` | idem |

At the committed `1.0.0` this publishes to `default`, visible to every IDE — add a pre-release label to
route a build to an opt-in channel instead. Marketplace refuses a second artifact with the same version, so
recovering from a wrong-channel upload costs a version number: check the table before publishing.

## License

Licensed under the [Apache License, Version 2.0](./LICENSE).

Portions of this repository — the Gradle build configuration, the split-mode module layout, and the
`frontend`/`backend`/`shared` chat sample — derive from the Apache-2.0 licensed
[IntelliJ Platform plugin template](https://github.com/JetBrains/intellij-platform-plugin-template),
Copyright 2000-2021 JetBrains s.r.o. See [NOTICE](./NOTICE) for attributions.

## Useful links

- [IntelliJ Platform SDK Plugin SDK][docs]
- [IntelliJ Platform Gradle Plugin Documentation][docs:intellij-platform-gradle-plugin-docs]
- [IntelliJ Platform Explorer][jb:ipe]
- [JetBrains Marketplace Quality Guidelines][jb:quality-guidelines]
- [IntelliJ Platform UI Guidelines][jb:ui-guidelines]
- [JetBrains Marketplace Paid Plugins][jb:paid-plugins]
- [IntelliJ SDK Code Samples][gh:code-samples]
- [Remote Development / Split Mode][docs:remote-dev]

[docs]: https://plugins.jetbrains.com/docs/intellij
[docs:logo]: https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html?from=IJPluginReadmeFile
[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html?from=IJPluginReadmeFile
[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginReadmeFile
[docs:signing]: https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginReadmeFile
[docs:remote-dev]: https://plugins.jetbrains.com/docs/intellij/plugin-content-modules.html?from=IJPluginReadmeFile
[docs:target-version]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html?from=IJPluginReadmeFile#target-versions
[docs:testing]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html?from=IJPluginReadmeFile#testing
[docs:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html?from=IJPluginReadmeFile
[docs:intellij-platform-gradle-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html?from=IJPluginReadmeFile#runIde
[docs:intellij-platform-gradle-plugin-verifyPlugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html?from=IJPluginReadmeFile#verifyPlugin

[file:build.gradle.kts]: ./build.gradle.kts
[file:CHANGELOG.md]: ./CHANGELOG.md
[file:gradle.properties]: ./gradle.properties
[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml

[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples
[gh:intellij-platform-gradle-plugin]: https://github.com/JetBrains/intellij-platform-gradle-plugin

[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks

[jb:github]: https://github.com/JetBrains/.github/blob/main/profile/README.md
[jb:forum]: https://platform.jetbrains.com/
[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html
[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html
[jb:ipe]: https://jb.gg/ipe
[jb:ui-guidelines]: https://jetbrains.github.io/ui
