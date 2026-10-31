plugins {
    id("net.fabricmc.fabric-loom")
}

base {
    archivesName.set("spectra-fabric")
}

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}

val minecraftVersion = property("minecraft_version") as String
val fabricLoaderVersion = property("fabric_loader_version") as String
val fabricApiVersion = property("fabric_api_version") as String
val rinkuVersion = property("rinku_version") as String

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraftVersion")
    "implementation"("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    "implementation"("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    // Browser backend (Chromium via JCEF). Spectra will move to its own fork; see apps/fabric/README.md.
    "implementation"("maven.modrinth:rinku:$rinkuVersion")
    // The wire protocol ships inside the mod jar.
    "implementation"(project(":protocol"))
    "include"(project(":protocol"))
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to project.version,
        "minecraft_version" to minecraftVersion,
        "fabric_loader_version" to fabricLoaderVersion,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}
