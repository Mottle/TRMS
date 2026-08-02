import org.gradle.api.tasks.Sync
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    id("moe.liar.horizon.userdev") version "26.1.2-tiramisu.7-SNAPSHOT"
    id("moe.liar.horizon.run-server") version "26.1.2-tiramisu.7-SNAPSHOT"
}

group = "moe.liar.horizon.trms"
val extensionVersion = providers.gradleProperty("extensionVersion").orElse("0.1.0-SNAPSHOT").get()
val requestedHorizonVersion = providers.gradleProperty("horizonVersion")
    .orElse("26.1.2-tiramisu.7-SNAPSHOT")
    .get()
version = extensionVersion

base {
    archivesName.set("trms-extension")
}

repositories {
    mavenLocal {
        content {
            includeGroupByRegex("moe\\.liar\\.horizon(\\..*)?")
        }
    }
    mavenCentral {
        content {
            excludeGroupByRegex("moe\\.liar\\.horizon(\\..*)?")
        }
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            excludeGroupByRegex("moe\\.liar\\.horizon(\\..*)?")
        }
    }
    maven("https://maven.canvasmc.io/releases") {
        content {
            excludeGroupByRegex("moe\\.liar\\.horizon(\\..*)?")
        }
    }
    maven("https://maven.canvasmc.io/public") {
        content {
            excludeGroupByRegex("moe\\.liar\\.horizon(\\..*)?")
        }
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly("moe.liar.horizon:horizon-extension-api:$requestedHorizonVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

horizonUserdev {
    horizonVersion.set(requestedHorizonVersion)
}

val horizonPaperclip by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add(horizonPaperclip.name, "moe.liar.horizon:horizon-server:$requestedHorizonVersion:paperclip")
}

val installHorizonPaperclip by tasks.registering(Sync::class) {
    group = "horizon"
    description = "Resolves the published Horizon paperclip for the TRMS test server."
    from(horizonPaperclip)
    into(layout.projectDirectory.dir("run-assets"))
    rename { "horizon-paperclip.jar" }
}

horizonRun {
    serverJar.set(layout.projectDirectory.file("run-assets/horizon-paperclip.jar"))
    runDirectory.set(layout.projectDirectory.dir("run/server"))
    acceptEula.set(true)
    jvmArgs.add("-Xmx2G")
    extensionJars.from(tasks.jar)
}

tasks.named("runHorizonServer") {
    dependsOn(installHorizonPaperclip, "installHorizonRunArtifacts")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.test {
    useJUnitPlatform()
}

val commonJar = project(":common").tasks.named<Jar>("jar")
val commonTestFixtureResources = project(":common").layout.buildDirectory.dir("resources/testFixtures")

// Horizon installs one Extension JAR. Embed the pure common library rather
// than relying on a neighboring server library or Horizon source checkout.
tasks.named<Jar>("jar") {
    dependsOn(commonJar)
    from({ zipTree(commonJar.get().archiveFile.get().asFile) }) {
        exclude("META-INF/MANIFEST.MF")
    }
}

tasks.named<ProcessResources>("processTestResources") {
    dependsOn(":common:processTestFixturesResources")
    from(commonTestFixtureResources)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("extension.toml") {
        expand("version" to extensionVersion)
    }
}
