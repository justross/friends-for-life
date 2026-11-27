plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.20.1-fabric"

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) { 
    group = "project"
    ofTask("build")
}

// Builds and publishes every version to Modrinth and CurseForge. See PUBLISHING.md.
stonecutter registerChiseled tasks.register("chiseledPublish", stonecutter.chiseled) {
    group = "publishing"
    ofTask("publishMods")
}

// Starts a real server with every built jar (and Forge 1.20.1 through Connector) and checks the mod works.
// See "Smoke tests" in build.gradle.kts.
stonecutter registerChiseled tasks.register("chiseledSmokeTest", stonecutter.chiseled) {
    group = "verification"
    ofTask("smokeTest")
}

// Lets only one smoke test server run at a time (they share a port, and memory), even in parallel builds
abstract class OneAtATime : BuildService<BuildServiceParameters.None>
gradle.sharedServices.registerIfAbsent("friendsforlifeSmokeTest", OneAtATime::class) { maxParallelUsages.set(1) }

// Runs the 1.20.1 Fabric jar on Forge through Sinytra Connector, in a Prism Launcher instance. See build.gradle.kts.
stonecutter registerChiseled tasks.register("runForgeConnector", stonecutter.chiseled) {
    group = "friendsforlife"
    versions { _, version -> version.project == "1.20.1-fabric" }
    ofTask("forgeConnectorLaunch")
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.fabricmc.net/")
        // Owo
        maven("https://maven.wispforest.io")
        maven("https://maven.wispforest.io/releases")
        maven {
            name = "Terraformers"
            url = uri("https://maven.terraformersmc.com/")
        }
    }
}