import java.io.DataOutputStream
import java.io.IOException
import java.util.Collections
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipFile

plugins {
//    id("fabric-loom") version "1.3.+"
    id("dev.isxander.modstitch.base") version "0.5.12"
}

val releaseVersion = "1.4.0"
val fabricLoader = "0.17.1" // also used by the smoke test server

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
        fabricLoaderVersion = fabricLoader

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

// owo-lib's published POM can list older endec versions than the ones it was compiled against and bundles
// (owo-lib-neoforge 0.12.28+1.21.10: POM says endec 0.1.8, jar bundles 0.1.11). In dev runs Gradle's older
// copies win and owo crashes with NoSuchMethodError, so targets that need it pin them to the bundled versions.
// Format: deps.endec_versions=<artifact>:<version>,...
prop("deps.endec_versions") { spec ->
    val pinned = spec.split(",").associate { it.trim().substringBefore(":") to it.trim().substringAfter(":") }
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.wispforest" || requested.group == "io.wispforest.endec") {
                pinned[requested.name]?.let { useVersion(it) }
            }
        }
    }
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

// Ship only this loader's metadata. A Fabric jar that also contains META-INF/mods.toml is loaded by Forge as a
// (broken) native mod instead of being handed to Sinytra Connector, which is how the 1.20.1 Fabric jar runs on Forge.
tasks.named<Jar>("jar") {
    when (project.name.substringAfterLast('-')) {
        "fabric" -> exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
        "neoforge" -> exclude("fabric.mod.json", "META-INF/mods.toml")
    }
}

// ---------------------------------------------------------------------------------------------
// Publishing to Modrinth and CurseForge. See PUBLISHING.md.
//   Dry run, all versions:  ./gradlew chiseledPublish -Ppublish.dry_run=true
//   Publish, all versions:  ./gradlew chiseledPublish   (tokens in secrets.properties, which git ignores)
// ---------------------------------------------------------------------------------------------
val loader = project.name.substringAfterLast('-')
val publishDryRun = (findProperty("publish.dry_run") as String?).toBoolean()

// API tokens: read from secrets.properties in the project root (git-ignored; copy secrets.properties.example),
// falling back to the MODRINTH_TOKEN / CURSEFORGE_TOKEN environment variables.
val secrets = Properties().apply {
    rootProject.file("secrets.properties").takeIf { it.isFile }?.reader()?.use { load(it) }
}
fun token(key: String, envVar: String): String =
    secrets.getProperty(key)?.trim().takeUnless { it.isNullOrEmpty() } ?: System.getenv(envVar).orEmpty().trim()
val modrinthToken = token("modrinth.token", "MODRINTH_TOKEN")
val curseforgeToken = token("curseforge.token", "CURSEFORGE_TOKEN")
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
        accessToken.set(modrinthToken)
        minecraftVersions.addAll(publishMcVersions)
        requires("owo-lib")
        if (loader == "fabric") requires("fabric-api")
    }

    curseforge {
        projectId.set(curseforgeId)
        accessToken.set(curseforgeToken)
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
        "modrinth.token in secrets.properties" to { modrinthToken.isNotBlank() },
    ),
    "publishCurseforge" to listOf(
        "publish.curseforge_id in gradle.properties" to { curseforgeId.isNotBlank() },
        "curseforge.token in secrets.properties" to { curseforgeToken.isNotBlank() },
    ),
)
// Building a target on its own, when it isn't the active Stonecutter version, produces a jar with no classes.
// Refuse to upload one (also on dry runs).
val publishedJar = modstitch.finalJarTask.flatMap { it.archiveFile }
tasks.matching { it.name in publishRequirements }.configureEach {
    val requirements = publishRequirements.getValue(name)
    doFirst {
        val jar = publishedJar.get().asFile
        val hasClasses = ZipFile(jar).use { it.getEntry("net/arcana/friendsforlife/Init.class") != null }
        if (!hasClasses) throw GradleException(
            "${jar.name} contains no mod classes. Publish with ./gradlew chiseledPublish, or make ${project.name} the active Stonecutter version first."
        )
        if (publishDryRun) return@doFirst
        val missing = requirements.filterNot { it.second() }.map { it.first }
        if (missing.isNotEmpty()) throw GradleException("Can't run $name, missing: ${missing.joinToString()}")
    }
}

// ---------------------------------------------------------------------------------------------
// Forge 1.20.1 through Sinytra Connector, which runs this Fabric jar on Forge (1.20.1-fabric only).
//   ./gradlew runForgeConnector   (registered in stonecutter.gradle.kts, works whichever version is active)
// Builds the jar, installs it with Connector, Forgified Fabric API and owo-lib into a Prism Launcher instance
// (created if missing), and launches it with a debugger listening on localhost:5005.
// Optional overrides (e.g. in ~/.gradle/gradle.properties): connector.prism (launcher command),
// connector.prism_data (Prism data folder), connector.instance (instance folder name), connector.debug_port.
// ---------------------------------------------------------------------------------------------
prop("connector.forge") { forgeVersion ->
    // The maven.modrinth artifacts come from the Modrinth repository that Modstitch already adds
    val connectorMods = configurations.create("connectorMods") {
        isTransitive = false
        isCanBeConsumed = false
    }
    (property("connector.mods") as String).split(",").forEach { dependencies.add(connectorMods.name, it.trim()) }

    val home = System.getProperty("user.home")
    val prismData = file((findProperty("connector.prism_data") as String?) ?: "$home/.local/share/PrismLauncher")
    val instanceName = (findProperty("connector.instance") as String?) ?: "FFL Forge 1.20.1 Connector"
    // Prism's instance folder is configurable (Settings > Launcher > Folders); relative paths are inside its data folder
    val prismInstances = prismData.resolve("prismlauncher.cfg").takeIf { it.isFile }?.readLines()
        ?.firstOrNull { it.startsWith("InstanceDir=") }?.substringAfter("=")?.trim()?.takeIf { it.isNotEmpty() }
        ?.let { prismData.resolve(it) } ?: prismData.resolve("instances")
    val instanceDir = prismInstances.resolve(instanceName)
    val debugPort = (findProperty("connector.debug_port") as String?) ?: "5005"
    val debugArgs = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$debugPort"
    val modJar = modstitch.finalJarTask.flatMap { it.archiveFile }

    val prepare = tasks.register("forgeConnectorPrepare") {
        group = "friendsforlife"
        description = "Installs this jar and the Connector stack into the Forge 1.20.1 Prism instance"
        dependsOn(modstitch.finalJarTask)
        inputs.files(connectorMods)
        doLast {
            val jar = modJar.get().asFile
            if (ZipFile(jar).use { it.getEntry("net/arcana/friendsforlife/Init.class") == null }) throw GradleException(
                "${jar.name} contains no mod classes. Use ./gradlew runForgeConnector, or make ${project.name} the active Stonecutter version first."
            )
            val mods = instanceDir.resolve(".minecraft/mods").apply { mkdirs() }

            // Prism settings: created once (Prism edits the file afterwards), debug args kept in place
            val cfg = instanceDir.resolve("instance.cfg")
            if (!cfg.exists()) {
                cfg.writeText(
                    """
                    [General]
                    ConfigVersion=1.2
                    InstanceType=OneSix
                    name=$instanceName
                    iconKey=default
                    notes=Managed by ./gradlew runForgeConnector in Friends For Life. Mods are replaced on each run.
                    """.trimIndent() + "\n"
                )
            }
            val lines = cfg.readLines().filterNot { it.startsWith("OverrideJavaArgs=") || it.startsWith("JvmArgs=") }
            cfg.writeText((lines + "OverrideJavaArgs=true" + "JvmArgs=$debugArgs").joinToString("\n") + "\n")
            instanceDir.resolve("mmc-pack.json").writeText(
                """
                {
                    "components": [
                        { "uid": "net.minecraft", "version": "$minecraft", "important": true },
                        { "uid": "net.minecraftforge", "version": "$forgeVersion" }
                    ],
                    "formatVersion": 1
                }
                """.trimIndent() + "\n"
            )

            // Replace the jars installed last time; mods added to the folder by hand are left alone
            val installed = mods.resolve(".friendsforlife-installed")
            if (installed.exists()) installed.readLines().forEach { mods.resolve(it).delete() }
            val jars = connectorMods.files + jar
            jars.forEach { it.copyTo(mods.resolve(it.name), overwrite = true) }
            installed.writeText(jars.joinToString("\n") { it.name } + "\n")
            logger.lifecycle("Installed into ${mods.path}:\n  " + jars.joinToString("\n  ") { it.name })
        }
    }

    tasks.register("forgeConnectorLaunch") {
        group = "friendsforlife"
        description = "Launches Friends For Life on Forge $minecraft through Sinytra Connector (Prism Launcher)"
        dependsOn(prepare)
        doLast {
            val prism = (findProperty("connector.prism") as String?)?.split(" ")
                ?: System.getenv("PATH").orEmpty().split(File.pathSeparator)
                    .map { File(it, "prismlauncher") }.firstOrNull { it.canExecute() }?.let { listOf(it.path) }
                ?: File("$home/Applications").listFiles()
                    ?.firstOrNull { it.name.startsWith("PrismLauncher") && it.name.endsWith(".AppImage") }?.let { listOf(it.path) }
                ?: throw GradleException("Prism Launcher not found. Set connector.prism in ~/.gradle/gradle.properties.")
            val log = layout.buildDirectory.file("connector/prism.log").get().asFile.apply { parentFile.mkdirs() }
            ProcessBuilder(prism + listOf("--launch", instanceName))
                .redirectErrorStream(true).redirectOutput(log).start()
            logger.lifecycle("Launching '$instanceName' through Prism (output: ${log.path}).")
            logger.lifecycle("Attach a debugger to localhost:$debugPort, e.g. IDEA's 'Attach to Forge 1.20.1 (Connector)'.")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Smoke tests: start a real dedicated server with the built jar and the dependencies players install, then check
// that the mod works. The 1.20.1 Fabric jar is also tested on Forge 1.20.1 through Sinytra Connector.
//   All targets:  ./gradlew chiseledSmokeTest
//   One target:   ./gradlew :1.21.1-neoforge:smokeTest   (only for the active Stonecutter version)
// Servers are installed once into .gradle/smoke-test; each run starts from a fresh world. Logs of each run go to
// build/smoke-test/. A run fails if the server doesn't start, a mod item is missing, a crash report is written,
// or the log shows mixin/dependency failures or errors from this mod.
// ---------------------------------------------------------------------------------------------
val smokeMods = configurations.create("smokeTestMods") {
    isTransitive = false
    isCanBeConsumed = false
}
when (loader) {
    "fabric" -> {
        dependencies.add(smokeMods.name, "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api_version")}")
        dependencies.add(smokeMods.name, "io.wispforest:owo-lib:${property("deps.owo_version")}")
    }
    "neoforge" -> dependencies.add(smokeMods.name, "io.wispforest:owo-lib-neoforge:${property("deps.owo_version")}")
}

class SmokeServer(
    val label: String,
    val dir: File,
    val install: (java: String) -> Unit,
    val command: (java: String) -> List<String>,
    val mods: () -> Collection<File>,
)

val smokeRoot = rootProject.file(".gradle/smoke-test/servers")
val argsFile = if (System.getProperty("os.name").startsWith("Windows")) "win_args.txt" else "unix_args.txt"

fun smokeDownload(url: String, to: File) {
    to.parentFile.mkdirs()
    val part = File(to.path + ".part")
    uri(url).toURL().openStream().use { input -> part.outputStream().use { input.copyTo(it) } }
    if (!part.renameTo(to)) throw GradleException("Couldn't move $part to $to")
}

fun smokeExec(dir: File, command: List<String>, log: File) {
    val process = ProcessBuilder(command).directory(dir).redirectErrorStream(true).redirectOutput(log).start()
    if (process.waitFor() != 0) throw GradleException("${command.joinToString(" ")} failed, see $log")
}

/** A server installed by a Forge/NeoForge installer, started through its generated JVM args file. */
fun installerServer(label: String, key: String, installerUrl: String, libraryPath: String, mods: () -> Collection<File>): SmokeServer {
    val dir = smokeRoot.resolve(key)
    return SmokeServer(label, dir,
        install = { java ->
            if (!dir.resolve("$libraryPath/$argsFile").isFile) {
                val installer = dir.resolve("installer.jar")
                smokeDownload(installerUrl, installer)
                smokeExec(dir, listOf(java, "-jar", installer.path, "--installServer"), dir.resolve("install.log"))
            }
        },
        command = { java -> listOf(java, "-Xmx2G", "@$libraryPath/$argsFile", "nogui") },
        mods = mods,
    )
}

val smokeServers = buildList {
    when (loader) {
        "fabric" -> {
            val dir = smokeRoot.resolve("fabric-$minecraft-$fabricLoader")
            add(SmokeServer("Fabric $minecraft", dir,
                install = {
                    val launcher = dir.resolve("fabric-server-launch.jar")
                    if (!launcher.isFile) {
                        @Suppress("UNCHECKED_CAST")
                        val installers = groovy.json.JsonSlurper()
                            .parse(uri("https://meta.fabricmc.net/v2/versions/installer").toURL()) as List<Map<String, Any>>
                        val installer = installers.first { it["stable"] == true }["version"]
                        smokeDownload("https://meta.fabricmc.net/v2/versions/loader/$minecraft/$fabricLoader/$installer/server/jar", launcher)
                    }
                },
                command = { java -> listOf(java, "-Xmx2G", "-jar", "fabric-server-launch.jar", "nogui") },
                mods = { smokeMods.files },
            ))
        }
        "neoforge" -> {
            val version = property("deps.neoforge") as String
            add(installerServer("NeoForge $minecraft", "neoforge-$version",
                "https://maven.neoforged.net/releases/net/neoforged/neoforge/$version/neoforge-$version-installer.jar",
                "libraries/net/neoforged/neoforge/$version") { smokeMods.files })
        }
    }
    // Forge through Connector, for targets that set it up (see the Connector section above)
    val connectorMods = configurations.findByName("connectorMods")
    val forge = findProperty("connector.forge") as String?
    if (connectorMods != null && forge != null) {
        val version = "$minecraft-$forge"
        add(installerServer("Forge $minecraft (Connector)", "forge-$version",
            "https://maven.minecraftforge.net/net/minecraftforge/forge/$version/forge-$version-installer.jar",
            "libraries/net/minecraftforge/forge/$version") { connectorMods.files })
    }
}

val smokeItems = listOf("collar", "pet_roster", "ocarina") + listOf(
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
).map { "${it}_ocarina" }

// Two pet records written into the world before it starts, to test how loaded copies of a pet are told apart
// (PetData#generation). Pet P was summoned twice (count 2) and is wolf ...0b; pet Q has a pre-1.4.0 record (count 0)
// whose entity never loads.
fun smokeUuid(n: Int) = UUID(0L, n.toLong())
fun smokeUuidNbt(n: Int) = "[I;0,0,0,$n]"
val smokePetP = smokeUuid(1)
val smokePetQ = smokeUuid(2)

/** Minimal NBT writer, enough for the pet records. */
class Nbt(val type: Int, val write: (DataOutputStream) -> Unit)
fun nbtInt(v: Int) = Nbt(3) { it.writeInt(v) }
fun nbtLong(v: Long) = Nbt(4) { it.writeLong(v) }
fun nbtString(v: String) = Nbt(8) { it.writeUTF(v) }
fun nbtUuid(uuid: UUID) = Nbt(11) { out ->
    out.writeInt(4)
    listOf(uuid.mostSignificantBits, uuid.leastSignificantBits).forEach {
        out.writeInt((it ushr 32).toInt()); out.writeInt(it.toInt())
    }
}
fun nbtCompound(vararg entries: Pair<String, Nbt>) = Nbt(10) { out ->
    entries.forEach { (key, value) -> out.writeByte(value.type); out.writeUTF(key); value.write(out) }
    out.writeByte(0)
}
fun nbtList(vararg items: Nbt) = Nbt(9) { out ->
    out.writeByte(10); out.writeInt(items.size); items.forEach { it.write(out) }
}

fun smokePetRecord(pet: UUID, entity: UUID, generation: Int) = nbtCompound(
    "FriendsForLifeEntityUUID" to nbtUuid(pet), "EntityUUID" to nbtUuid(entity), "Generation" to nbtInt(generation),
    "OwnerUuid" to nbtUuid(smokeUuid(99)), "OwnerName" to nbtString("SmokeTest"), "collarItem" to nbtCompound(),
    "x" to nbtInt(0), "y" to nbtInt(-60), "z" to nbtInt(0), "tameDate" to nbtLong(0), "homePos" to nbtLong(0),
    "homeDim" to nbtString(""), "EntityData" to nbtCompound("id" to nbtString("minecraft:wolf")),
    "FollowMode" to nbtString("COMPANION"),
)

fun writeSmokePets(worldDir: File) {
    val file = worldDir.resolve("data/friends_for_life_tracked_pets.dat").apply { parentFile.mkdirs() }
    val root = nbtCompound("data" to nbtCompound(
        "pets" to nbtList(smokePetRecord(smokePetP, smokeUuid(11), 2), smokePetRecord(smokePetQ, smokeUuid(98), 0)),
        "recreatedPets" to nbtList(),
    ))
    DataOutputStream(GZIPOutputStream(file.outputStream())).use { out ->
        out.writeByte(10); out.writeUTF(""); root.write(out)
    }
}

fun smokeWolf(x: Int, entity: Int, pet: UUID, generation: Int?): String {
    val petInts = "[I;0,0,0,${pet.leastSignificantBits}]"
    val stamp = generation?.let { ",FriendsForLifeGeneration:$it" } ?: ""
    return "summon minecraft:wolf $x -60 6 {UUID:${smokeUuidNbt(entity)},FriendsForLifeEntityUUID:$petInts$stamp}"
}

// Console commands run once the server is up. Placing the items proves they're registered; summoning and killing
// pets runs the mod's mixins and entity load/unload handling; the wolves at the end test telling pet copies apart.
val smokeCommands = buildList {
    add("forceload add 0 0")
    add("setblock 0 -60 0 minecraft:chest")
    smokeItems.forEach { add("item replace block 0 -60 0 container.0 with friendsforlife:$it") }
    listOf("wolf", "cat", "parrot", "horse", "llama").forEachIndexed { i, mob -> add("summon minecraft:$mob ${2 + i} -60 2") }
    add("kill @e[type=!minecraft:player]")

    add(smokeWolf(2, 10, smokePetP, 1)) // older summon than the record: must be removed
    add(smokeWolf(3, 11, smokePetP, 2)) // the pet: must stay
    add(smokeWolf(4, 12, smokePetP, 2)) // same summon while the pet is loaded: duplicate, removed after 2 ticks
    add(smokeWolf(5, 20, smokePetQ, null)) // unstamped copy (pre-1.4.0) of a pet whose entity isn't loaded: adopted
    add("execute if entity ${smokeUuid(10)} run say FFL-SMOKE old copy survived")
    add("execute unless entity ${smokeUuid(11)} run say FFL-SMOKE current copy removed")
    add("execute if entity ${smokeUuid(12)} run say FFL-SMOKE duplicate survived")
    add("execute unless entity ${smokeUuid(20)} run say FFL-SMOKE adopted copy removed")
    // A collared Companion pet is invulnerable through vanilla's flag, but saves it as false (read through the save
    // code by data get); other entities keep their saved flag
    add("damage ${smokeUuid(11)} 1")
    add("data get entity ${smokeUuid(11)} Invulnerable")
    add("summon minecraft:wolf 6 -60 6 {UUID:${smokeUuidNbt(30)},Invulnerable:1b}")
    add("data get entity ${smokeUuid(30)} Invulnerable")
    add("save-all flush")
}

val smokeProblemPatterns = listOf(
    Regex("(?i)mixin apply failed|InvalidMixinException|MixinApplyError|MixinTransformerError"),
    Regex("Missing or unsupported mandatory dependencies|Incompatible mods? found|Could not execute entrypoint"),
    Regex("/(ERROR|FATAL)].*(friendsforlife|net\\.arcana)"),
    Regex("^\\s+at net\\.arcana\\."),
    Regex("Unknown item 'friendsforlife:"),
)

fun runSmokeServer(server: SmokeServer, java: String, jar: File, log: File): List<String> {
    server.dir.mkdirs()
    server.install(java)
    listOf("world", "logs", "crash-reports", "mods").forEach { server.dir.resolve(it).deleteRecursively() }
    writeSmokePets(server.dir.resolve("world"))
    val modsDir = server.dir.resolve("mods").apply { mkdirs() }
    (server.mods() + jar).forEach { it.copyTo(modsDir.resolve(it.name)) }
    server.dir.resolve("eula.txt").writeText("eula=true\n")
    server.dir.resolve("server.properties").writeText(
        listOf(
            "online-mode=false", "server-port=25599", "level-type=minecraft\\:flat", "generate-structures=false",
            "spawn-protection=0", "view-distance=4", "simulation-distance=4", "max-tick-time=-1", "sync-chunk-writes=false",
        ).joinToString("\n", postfix = "\n")
    )

    val output = Collections.synchronizedList(mutableListOf<String>())
    val started = AtomicBoolean(false)
    val process = ProcessBuilder(server.command(java)).directory(server.dir).redirectErrorStream(true).start()
    val reader = kotlin.concurrent.thread(isDaemon = true) {
        process.inputStream.bufferedReader().forEachLine { line ->
            output += line
            if ("Done (" in line) started.set(true)
        }
    }
    val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(10) // first run downloads a lot
    while (!started.get() && process.isAlive && System.nanoTime() < deadline) Thread.sleep(250)
    if (started.get()) {
        val console = process.outputStream.bufferedWriter()
        try {
            (smokeCommands + "stop").forEach {
                console.write(it); console.newLine(); console.flush()
                Thread.sleep(250)
            }
        } catch (e: IOException) {
            // The server stopped (crashed) while commands were being sent; the checks below report why
        }
        process.waitFor(2, TimeUnit.MINUTES)
    }
    if (process.isAlive) process.destroyForcibly().waitFor()
    reader.join(5000)
    val lines = synchronized(output) { output.toList() }
    log.parentFile.mkdirs()
    log.writeText(lines.joinToString("\n", postfix = "\n"))

    val problems = mutableListOf<String>()
    if (!started.get()) problems += "the server didn't finish starting"
    server.dir.resolve("crash-reports").listFiles()?.forEach { problems += "crash report: ${it.path}" }
    if (started.get()) {
        val placed = lines.count { "Replaced a slot" in it }
        if (placed != smokeItems.size) problems += "only $placed of ${smokeItems.size} mod items could be placed"
        if (lines.none { "Removing an old copy of pet $smokePetP" in it }) problems += "an old copy of a pet wasn't removed"
        if (lines.none { "Removing a duplicate of pet $smokePetP" in it }) problems += "a duplicate of a pet wasn't removed"
        problems += lines.filter { "FFL-SMOKE" in it && "say " !in it }
        if (lines.none { "Target is invulnerable" in it }) problems += "a collared Companion pet wasn't invulnerable"
        val savedFlags = lines.mapNotNull { Regex("has the following entity data: (\\d)b").find(it)?.groupValues?.get(1) }
        if (savedFlags != listOf("0", "1")) {
            problems += "saved Invulnerable flags were $savedFlags, expected [0, 1] (collared pet, then another wolf)"
        }
    }
    problems += lines.filter { line -> smokeProblemPatterns.any { it.containsMatchIn(line) } }.distinct().take(25)
    return problems
}

val smokeJava = javaToolchains.launcherFor { languageVersion.set(modstitch.javaTarget.map { JavaLanguageVersion.of(it) }) }
val smokeJar = modstitch.finalJarTask.flatMap { it.archiveFile }

tasks.register("smokeTest") {
    group = "verification"
    description = "Starts a server with this jar on " + smokeServers.joinToString { it.label } + " and checks the mod works"
    dependsOn(modstitch.finalJarTask)
    gradle.sharedServices.registrations.findByName("friendsforlifeSmokeTest")?.let { usesService(it.service) }
    doLast {
        val jar = smokeJar.get().asFile
        if (ZipFile(jar).use { it.getEntry("net/arcana/friendsforlife/Init.class") == null }) throw GradleException(
            "${jar.name} contains no mod classes. Use ./gradlew chiseledSmokeTest, or make ${project.name} the active Stonecutter version first."
        )
        val java = smokeJava.get().executablePath.asFile.path
        val failures = smokeServers.mapNotNull { server ->
            val log = layout.buildDirectory.file("smoke-test/${server.label.replace(Regex("[^A-Za-z0-9.]+"), "-")}.log").get().asFile
            logger.lifecycle("Smoke test: ${server.label}...")
            val start = System.nanoTime()
            val problems = runSmokeServer(server, java, jar, log)
            val seconds = (System.nanoTime() - start) / 1_000_000_000
            if (problems.isEmpty()) {
                logger.lifecycle("Smoke test: ${server.label} passed (${seconds}s)")
                null
            } else {
                "${server.label} failed (log: ${log.path}):\n" + problems.joinToString("\n") { "    $it" }
            }
        }
        if (failures.isNotEmpty()) throw GradleException("Smoke test\n" + failures.joinToString("\n"))
    }
}
