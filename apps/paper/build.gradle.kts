plugins {
    id("com.gradleup.shadow")
}

val paperVersion = "26.3.build.235-stable"

dependencies {
    "compileOnly"("io.papermc.paper:paper-api:$paperVersion")
    "implementation"(project(":runtime-js"))
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("spectra-paper")
    archiveClassifier.set("")
    // Truffle discovers languages through service files; they must be merged, not overwritten.
    mergeServiceFiles()
    // Shadow 9 drops duplicate entries before transformers run unless they are let through; without this only
    // the first jar's service file survives and Truffle cannot find the regex engine.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    // GraalJS ships as named modules; module descriptors are meaningless once shaded into a plugin jar.
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
}

tasks.named("assemble") { dependsOn("shadowJar") }
