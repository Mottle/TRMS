pluginManagement {
    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("moe\\.liar\\.horizon(\\..*)?")
            }
        }
        gradlePluginPortal()
        mavenCentral {
            content {
                excludeGroupByRegex("moe\\.liar\\.horizon(\\..*)?")
            }
        }
        maven("https://maven.neoforged.net/releases")
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
}

dependencyResolutionManagement {
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
        maven("https://maven.neoforged.net/releases")
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
}

rootProject.name = "trms"

include(":common", ":extension", ":mod")
