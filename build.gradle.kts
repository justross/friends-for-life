plugins {
//    id("fabric-loom") version "1.3.+"
    id("dev.isxander.modstitch.base") version "0.5.12"
}

val releaseVersion = "1.4.0"

fun prop(name: String, consumer: (prop: String) -> Unit) {
    (findProperty(name) as? String?)
        ?.let(consumer)
}

val minecraft = property("deps.minecraft") as String;

modstitch {
    minecraftVersion = minecraft

    // Alternatively use stonecutter.eval if you have a lot of versions to target.
    // https://stonecutter.kikugie.dev/stonecutter/guide/setup#checking-versions
    javaTarget = when (minecraft) {
        "1.20.1" -> 17
        "1.21.1" -> 21
        "1.21.10" -> 21
        else -> throw IllegalArgumentException("Please store the java version for ${property("deps.minecraft")} in build.gradle.kts!")
    }

    // If parchment doesn't exist for a version yet you can safely
    // omit the "deps.parchment" property from your versioned gradle.properties
    parchment {
        prop("deps.parchment") { mappingsVersion = it }
    }

    // This metadata is used to fill out the information inside
    // the metadata files found in the templates folder.
    metadata {
        modId = "friendsforlife"
        modName = "Friends For Life"
        modVersion = releaseVersion
        modGroup = "net.arcana"
        modAuthor = "conbo, jjorge"
        modDescription = "Take your pets on your next adventure with this QoL mod!"

        fun <K, V> MapProperty<K, V>.populate(block: MapProperty<K, V>.() -> Unit) {
            block()
        }

        replacementProperties.populate {
            // You can put any other replacement properties/metadata here that
            // modstitch doesn't initially support. Some examples below.
            put("mod_issue_tracker", "https://github.com/justross/friends-for-life/issues")
            put(
                "pack_format", when (property("deps.minecraft")) {
                    "1.20.1" -> 15
                    "1.21.1" -> 34
                    "1.21.10" -> 46
                    else -> throw IllegalArgumentException("Please store the resource pack version for ${property("deps.minecraft")} in build.gradle.kts! https://minecraft.wiki/w/Pack_format")
                }.toString()
            )
        }
    }

    // Fabric Loom (Fabric)
    loom {
        // It's not recommended to store the Fabric Loader version in properties.
        // Make sure it's up to date.
        fabricLoaderVersion = "0.17.1"

        // Configure loom like normal in this block.
        configureLoom {
            if (property("deps.minecraft") == "1.21.10") {
                accessWidenerPath.set(
                    file("src/main/resources/friendsforlife.accesswidener")
                )
            }
            runs {
                maybeCreate("client").apply {
                    property("fabric.development", "true")
                    programArgs("--username", "TestPlayer")
                }

                maybeCreate("server").apply {
                    property("fabric.development", "true")
                }
            }
        }
    }

    // ModDevGradle (NeoForge, Forge, Forgelike)
    moddevgradle {
        enable {
            prop("deps.forge") { forgeVersion = it }
            prop("deps.neoform") { neoFormVersion = it }
            prop("deps.neoforge") { neoForgeVersion = it }
            prop("deps.mcp") { mcpVersion = it }
        }

        // Configures client and server runs for MDG, it is not done by default
        defaultRuns()

        // This block configures the `neoforge` extension that MDG exposes by default,
        // you can configure MDG like normal from here
        configureNeoforge {
            runs.all {
                disableIdeRun()
            }
        }
    }

    mixin {
        // You do not need to specify mixins in any mods.json/toml file if this is set to
        // true, it will automatically be generated.
        addMixinsToModManifest = true

        configs.register("friendsforlife")

        // Most of the time you won't ever need loader specific mixins.
        // If you do, simply make the mixin file and add it like so for the respective loader:
        // if (isLoom) configs.register("friendsforlife-fabric")
        // if (isModDevGradleRegular) configs.register("friendsforlife-neoforge")
        // if (isModDevGradleLegacy) configs.register("friendsforlife-forge")
    }
}

// Stonecutter constants for mod loaders.
// See https://stonecutter.kikugie.dev/stonecutter/guide/comments#condition-constants
var constraint: String = name.split("-")[1]
stonecutter {
    consts(
        "fabric" to constraint.equals("fabric"),
        "neoforge" to constraint.equals("neoforge"),
        "forge" to constraint.equals("forge"),
        "vanilla" to constraint.equals("vanilla")
    )
}

// All dependencies should be specified through modstitch's proxy configuration.
// Wondering where the "repositories" block is? Go to "stonecutter.gradle.kts"
// If you want to create proxy configurations for more source sets, such as client source sets,
// use the modstitch.createProxyConfigurations(sourceSets["client"]) function.
dependencies {
    modstitch.loom {
        modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api_version")}")

        modstitchModImplementation("io.wispforest:owo-lib:${property("deps.owo_version")}")
//        modstitchModImplementation("com.terraformersmc:modmenu:${property("deps.modmenu_version")}")
        annotationProcessor("io.wispforest:owo-lib:${property("deps.owo_version")}")
    }

    modstitch.moddevgradle {
        modstitchModImplementation("io.wispforest:owo-lib-neoforge:${property("deps.owo_version")}")
        modstitchModImplementation("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+e0539096d1")
        // modstitchModImplementation("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308dedd1")
    }

    // Anything else in the dependencies block will be used for all platforms.
}

// ModDevGradle 2.0.74 (pinned by Modstitch 0.5.12) ships NeoFormRuntime 1.0.13, which can't build
// NeoForge 1.21.10's Minecraft artifacts. Targets that need a newer one set deps.neoform_runtime.
findProperty("deps.neoform_runtime")?.let { nfrtVersion ->
    plugins.withId("net.neoforged.moddev") {
        extensions.getByType<net.neoforged.nfrtgradle.NeoFormRuntimeExtension>().version.set(nfrtVersion.toString())
    }
}

// Jars are named friendsforlife-<version>+<target>.jar, e.g. friendsforlife-1.4.0+1.21.10-neoforge.jar
// (AbstractArchiveTask, not Jar: Loom's remapJar extends org.gradle.jvm.tasks.Jar, which withType<Jar> misses)
tasks.withType<AbstractArchiveTask>().configureEach {
    archiveVersion.set("$releaseVersion+${project.name}")
}

// ---------------------------------------------------------------------------------------------
// Publishing to Modrinth and CurseForge. See PUBLISHING.md.
//   Dry run, all versions:  ./gradlew chiseledPublish -Ppublish.dry_run=true
//   Publish, all versions:  MODRINTH_TOKEN=... CURSEFORGE_TOKEN=... ./gradlew chiseledPublish
// ---------------------------------------------------------------------------------------------
val loader = project.name.substringAfterLast('-')
val publishDryRun = (findProperty("publish.dry_run") as String?).toBoolean()
val modrinthId = (findProperty("publish.modrinth_id") as String?).orEmpty()
val curseforgeId = (findProperty("publish.curseforge_id") as String?).orEmpty()
val publishMcVersions = (property("publish.minecraft_versions") as String).split(",").map { it.trim() }

/** The first "## " section of CHANGELOG.md, without its heading. */
fun latestChangelog(): String {
    val lines = rootProject.file("CHANGELOG.md").readLines()
    val start = lines.indexOfFirst { it.startsWith("## ") }
    if (start < 0) return ""
    val end = lines.drop(start + 1).indexOfFirst { it.startsWith("## ") }.let { if (it < 0) lines.size else start + 1 + it }
    return lines.subList(start + 1, end).joinToString("\n").trim()
}

// mod-publish-plugin is bundled (and pinned to 0.8.4) by Modstitch, so it is applied here rather than in plugins {}
apply(plugin = "me.modmuss50.mod-publish-plugin")

configure<me.modmuss50.mpp.ModPublishExtension> {
    file.set(modstitch.finalJarTask.flatMap { it.archiveFile })
    displayName.set("Friends For Life $releaseVersion ($minecraft ${if (loader == "neoforge") "NeoForge" else "Fabric"})")
    version.set("$releaseVersion+${project.name}")
    changelog.set(providers.provider { latestChangelog() })
    type.set(
        when ((findProperty("publish.release_type") as String?) ?: "beta") {
            "release" -> STABLE
            "alpha" -> ALPHA
            else -> BETA
        }
    )
    modLoaders.add(loader)
    dryRun.set(publishDryRun)

    modrinth {
        projectId.set(modrinthId)
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.addAll(publishMcVersions)
        requires("owo-lib")
        if (loader == "fabric") requires("fabric-api")
    }

    curseforge {
        projectId.set(curseforgeId)
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        minecraftVersions.addAll(publishMcVersions)
        clientRequired.set(true)
        serverRequired.set(true)
        requires("owo-lib")
        if (loader == "fabric") requires("fabric-api")
    }
}

// Fail before uploading anything if the IDs or tokens are missing (skipped for dry runs).
val publishRequirements = mapOf(
    "publishModrinth" to listOf(
        "publish.modrinth_id in gradle.properties" to { modrinthId.isNotBlank() },
        "MODRINTH_TOKEN environment variable" to { !System.getenv("MODRINTH_TOKEN").isNullOrBlank() },
    ),
    "publishCurseforge" to listOf(
        "publish.curseforge_id in gradle.properties" to { curseforgeId.isNotBlank() },
        "CURSEFORGE_TOKEN environment variable" to { !System.getenv("CURSEFORGE_TOKEN").isNullOrBlank() },
    ),
)
tasks.matching { it.name in publishRequirements }.configureEach {
    val requirements = publishRequirements.getValue(name)
    doFirst {
        if (publishDryRun) return@doFirst
        val missing = requirements.filterNot { it.second() }.map { it.first }
        if (missing.isNotEmpty()) throw GradleException("Can't run $name, missing: ${missing.joinToString()}")
    }
}
