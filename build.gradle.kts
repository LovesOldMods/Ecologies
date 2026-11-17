import dev.kikugie.stonecutter.controller.task.StonecutterTaskMutexService

plugins {
    id("net.neoforged.moddev")
}

val modId = sc.properties["mod.id"]
val mcVersion = sc.current.version

version = "${sc.properties["mod.version"]}+$mcVersion"
group = sc.properties["mod.group"]
base.archivesName = modId

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://maven.minecraftforge.net/", "MinecraftForge", "com.github.glitchfiend")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    // Places the tundra biome into the overworld on both versions.
    implementation("com.github.glitchfiend:TerraBlender-neoforge:${sc.properties["deps.terrablender"]}")
}

// Files that only exist on one version (item model definitions, recipe
// formats, ...) live in src/main/resources-<version>/ next to the shared ones.
sourceSets.main {
    resources.srcDir(rootProject.file("src/main/resources-$mcVersion"))
}

neoForge {
    version = sc.properties["deps.neo_loader"]

    val parchment = sc.properties["deps.parchment"]
    if (parchment.isNotEmpty()) parchment {
        mappingsVersion = parchment
        minecraftVersion = sc.properties["deps.parchment_mc"]
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run/$mcVersion/client")
            client()
            // `-Pecologies.world=<name>` joins that singleplayer world straight away (for screenshots in tests).
            providers.gradleProperty("ecologies.world").orNull?.let { programArguments.addAll("--quickPlaySingleplayer", it, "--width", "1280", "--height", "720") }
        }
        register("server") {
            gameDirectory = rootProject.file("run/$mcVersion/server")
            server()
            programArgument("--nogui")
        }
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            logLevel = org.slf4j.event.Level.INFO
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "id" to modId,
            "name" to sc.properties["mod.name"],
            "version" to sc.properties["mod.version"],
            "minecraft" to sc.properties["mod.mc_compat"],
            "authors" to sc.properties["mod.authors"],
            "license" to sc.properties["mod.license"],
        )
        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
        usesService(StonecutterTaskMutexService.create(gradle, name))
    }

    withType<Jar> {
        val licenseName = "LICENSE-$modId" // plain string, so the configuration cache can store it
        from(rootProject.file("LICENSE")) { rename { licenseName } }
    }

    // `./gradlew :26.3:runServer -Pecologies.stdin` lets you type (or pipe) server commands.
    if (providers.gradleProperty("ecologies.stdin").isPresent) {
        withType<JavaExec>().matching { it.name == "runServer" }.configureEach { standardInput = System.`in` }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/<mod version>/"
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${sc.properties["mod.version"]}"))
    }
}
