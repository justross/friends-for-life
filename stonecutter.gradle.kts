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