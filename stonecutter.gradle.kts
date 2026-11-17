plugins {
    id("net.neoforged.moddev") version "2.0.148" apply false
}

stonecutter active "26.3"

stonecutter parameters {
    swaps["mod_version"] = "\"${properties["mod.version"]}\";"
    swaps["minecraft"] = "\"${current.version}\";"

    replacements {
        // ResourceLocation was renamed Identifier in 1.21.11.
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
