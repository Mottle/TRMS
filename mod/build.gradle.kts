import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.142"
}

val modId = providers.gradleProperty("mod_id").get()
val modVersion = providers.gradleProperty("mod_version").get()
val modGroupId = providers.gradleProperty("mod_group_id").get()
val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val neoVersion = providers.gradleProperty("neo_version").get()

group = modGroupId
version = modVersion

base {
    archivesName.set(modId)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

neoForge {
    version = neoVersion

    runs {
        create("client") {
            client()
            gameDirectory.set(layout.projectDirectory.dir("runs/client"))
        }

        // Dedicated Windows test-client run. Keep the quick-play arguments in
        // the ModDevGradle model so DevLaunch's required arguments remain intact.
        create("clientQuickPlay") {
            client()
            gameDirectory.set(layout.projectDirectory.dir("runs/client"))
            programArgument("--quickPlayMultiplayer")
            programArgument("127.0.0.1:25565")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    // This is ModDevGradle's mapped Minecraft-aware test mode. It adds the
    // NeoForge/Minecraft classpath and launch configuration to the test source set.
    // The test class itself is owned by the TRMS mod's game classloader.
    unitTest {
        enable()
        testedMod.set(mods.named(modId))
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

// The player installs only this Mod JAR. Embed the side-neutral common classes
// so no third runtime artifact or external classpath entry is required.
tasks.named<Jar>("jar") {
    dependsOn(commonJar)
    from({ zipTree(commonJar.get().archiveFile.get().asFile) }) {
        exclude("META-INF/MANIFEST.MF")
    }
}

// ModDev's isolated game test classloader does not consume resources from a
// test-fixtures dependency JAR. Copy the one common fixture into this test
// output while keeping `common` as its sole source of truth.
tasks.named<ProcessResources>("processTestResources") {
    dependsOn(":common:processTestFixturesResources")
    from(commonTestFixtureResources)
}

val replaceProperties = mapOf(
    "minecraft_version" to minecraftVersion,
    "neo_version" to neoVersion,
    "neo_version_range" to providers.gradleProperty("neo_version_range").get(),
    "loader_version_range" to providers.gradleProperty("loader_version_range").get(),
    "mod_id" to modId,
    "mod_name" to providers.gradleProperty("mod_name").get(),
    "mod_version" to modVersion,
    "mod_authors" to providers.gradleProperty("mod_authors").get(),
    "mod_description" to providers.gradleProperty("mod_description").get()
)

tasks.processResources {
    inputs.properties(replaceProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }
}
