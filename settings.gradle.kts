pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // One shared source tree, built once per Minecraft version.
        versions("1.21.1", "26.3")
        // The version the code in git is written for; switch with the
        // "Set active project to ..." Gradle tasks, never by hand.
        vcsVersion = "26.3"
    }
}

rootProject.name = "Ecologies"
