plugins {
    base
}

allprojects {
    group = "moe.liar.trms"
}

tasks.register("verifyTrms") {
    group = "verification"
    description = "Builds and tests the shared TRMS contract, Extension, and client Mod artifacts."
    dependsOn(":common:build", ":extension:build", ":mod:build")
}
