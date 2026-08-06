# Horizon Versioning

Horizon uses one project version for server jars, local Maven publications, API
artifacts, and extension/plugin development examples:

```text
<minecraftVersion>-<series>.<patch>[-SNAPSHOT]
```

For the current Tiramisu development line this is:

```text
26.1.2-tiramisu.8-SNAPSHOT
```

`mcVersion` tracks the upstream Minecraft/Canvas-compatible version. `horizonSeries`
is the Horizon release line name, and `horizonPatch` is Horizon's own compatibility
or feature patch number within that line. Set `horizonSnapshot=false` only for a
released build.

The version fields live in `gradle.properties`:

```properties
mcVersion = 26.1.2
horizonSeries = tiramisu
horizonPatch = 8
horizonSnapshot = true
```

Build numbers and Git commits are build metadata, not Maven versions. They are written
to the server jar manifest as `Build-Number`, `Git-Commit`, `Canvas-Commit`, and
`Folia-Commit`. This keeps dependency coordinates stable while still making a built
jar traceable.

Publish local API artifacts with:

```bash
./gradlew :horizon-api:publishToMavenLocal :horizon-extension:api:publishToMavenLocal :horizon-gradle-plugin:publishToMavenLocal
```

Publish the matching Horizon development bundle when extension projects need a
Mojmap/NMS compile-time classpath:

```bash
./gradlew :horizon-server:publishToMavenLocal -PpublishDevBundle
```

With `publishDevBundle` present, this publishes both the `dev-bundle` Gradle-module
publication and the local-only runnable server publication. To publish them separately:

```bash
./gradlew :horizon-server:publishDevBundlePublicationToMavenLocal -PpublishDevBundle
./gradlew :horizon-server:publishRunnableServerPublicationToMavenLocal
```

The runnable-server publication also contains a `dev` classifier with the remapped
Mojmap server jar. NMS/plugin projects that need CraftBukkit or server internals
should consume `moe.liar.horizon:horizon-server:<version>:dev` from Maven Local;
they must not reference `horizon-server/build/libs` directly.

The development bundle relies on Gradle module metadata and intentionally omits Maven
POM dependencies. The runnable-server POM remains an independent valid publication.

Extension and plugin projects should depend on the matching version, for example:

```kotlin
compileOnly("moe.liar.horizon:horizon-extension-api:26.1.2-tiramisu.8-SNAPSHOT")
compileOnly("moe.liar.horizon:horizon-api:26.1.2-tiramisu.8-SNAPSHOT")
```
