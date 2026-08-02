# Horizon Gradle Plugins

Horizon ships two development plugins for extension and plugin projects. They are
versioned with the server and API artifacts, for example
`26.1.2-tiramisu.7-SNAPSHOT`.

## Plugin Management

Use Maven local while Horizon artifacts are local-only. Configure your ordinary
dependency repositories in `settings.gradle.kts`. Horizon userdev disables
paperweight's generic Paper repository injection, but paperweight still adds the
exact Mache repository declared by the selected development bundle. Consequently,
do not use `RepositoriesMode.FAIL_ON_PROJECT_REPOS` in a userdev build.

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.canvasmc.io/releases")
        maven("https://maven.canvasmc.io/public")
    }
}
```

## Run Server

`moe.liar.horizon.run-server` creates a local run directory, writes `eula.txt`,
and starts a configured Horizon paperclip jar. Bukkit/Paper plugin jars are
passed to the server with Paper's modern `-add-plugin=<path>` argument instead
of being copied into `run/plugins`.

```kotlin
plugins {
    id("moe.liar.horizon.run-server") version "26.1.2-tiramisu.7-SNAPSHOT"
}

horizonRun {
    serverJar.set(layout.projectDirectory.file("server/horizon-paperclip-26.1.2-tiramisu.7-SNAPSHOT.jar"))
    runDirectory.set(layout.projectDirectory.dir("run"))
    acceptEula.set(true)
    jvmArgs.add("-Xmx4G")
    extensionJars.from(tasks.jar)
}
```

If `serverJar` is not set, the plugin looks for
`server/horizon-paperclip-<plugin version>.jar`.

Useful tasks:

```bash
./gradlew prepareHorizonRun
./gradlew installHorizonRunArtifacts
./gradlew runHorizonServer
```

Use `pluginJars.from(tasks.jar)` for Bukkit/Paper plugins instead of
`extensionJars`. `installHorizonRunArtifacts` is only needed when a jar must be
present in the run directory, such as Horizon extension jars in `run/extensions`.
Installed jars use Horizon-managed names like
`my-extension_HorizonRun_extension.jar`; old Horizon-managed jars are removed on
the next install, while manually placed jars are left alone.

For extension testing, run both tasks in order:

```bash
./gradlew installHorizonRunArtifacts runHorizonServer
```

## Userdev

`moe.liar.horizon.userdev` declares the matching Horizon dev bundle through the
`horizonDevBundle` configuration and delegates its setup to paperweight-userdev.
This prepares the Mojmap server and dependency classpath consumed by Java compile
tasks. Horizon targets Minecraft 26.1+, where server and API development no longer
needs the legacy plugin reobfuscation flow.

```kotlin
plugins {
    `java-library`
    id("moe.liar.horizon.userdev") version "26.1.2-tiramisu.7-SNAPSHOT"
}

horizonUserdev {
    horizonVersion.set("26.1.2-tiramisu.7-SNAPSHOT")
}

dependencies {
    compileOnly("moe.liar.horizon:horizon-extension-api:26.1.2-tiramisu.7-SNAPSHOT")
}
```

If `horizonVersion` is not set, the plugin uses its own published version. Set it
explicitly when testing from an included build or a custom plugin classpath.

Check the dev bundle with:

```bash
./gradlew resolveHorizonDevBundle
```
