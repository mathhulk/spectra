plugins {
    id("com.gradleup.shadow")
}

val paperVersion = "26.3.build.235-stable"

val typegenTool: Configuration by configurations.creating
val typegenClasspath: Configuration by configurations.creating
val paperApiSources: Configuration by configurations.creating { isTransitive = false }

dependencies {
    "compileOnly"("io.papermc.paper:paper-api:$paperVersion")
    "implementation"(project(":runtime-js"))
    "implementation"(project(":typegen"))
    typegenTool(project(":typegen"))
    typegenClasspath("io.papermc.paper:paper-api:$paperVersion")
    paperApiSources("io.papermc.paper:paper-api:$paperVersion:sources")
}

val typegenLauncher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }
val generatedResources = layout.buildDirectory.dir("generated/typegen")

/** Parameter names and Javadoc for Paper API, bundled so types generated on a server are documented. */
val generateDocsIndex by tasks.registering(JavaExec::class) {
    description = "Extracts Paper API parameter names and docs into a compact index."
    javaLauncher.set(typegenLauncher)
    classpath = typegenTool
    mainClass.set("com.mathhulk.spectra.typegen.Main")
    inputs.files(paperApiSources)
    val output = generatedResources.map { it.file("spectra/paper-api-docs.tsv.gz") }
    outputs.file(output)
    argumentProviders.add(CommandLineArgumentProvider {
        paperApiSources.files.flatMap { listOf("--sources", it.absolutePath) } +
            listOf("--docs-out", output.get().asFile.absolutePath)
    })
}

/** Declarations for Paper API alone, for publishing with the SDK as a fallback. */
val generatePaperTypes by tasks.registering(JavaExec::class) {
    description = "Generates TypeScript declarations for Paper API."
    javaLauncher.set(typegenLauncher)
    classpath = typegenTool
    mainClass.set("com.mathhulk.spectra.typegen.Main")
    inputs.files(typegenClasspath, paperApiSources)
    val output = layout.buildDirectory.file("types/paper.d.ts")
    outputs.file(output)
    argumentProviders.add(CommandLineArgumentProvider {
        val api = typegenClasspath.files.first { it.name.startsWith("paper-api-") }
        listOf("--classpath", typegenClasspath.asPath, "--jar", api.absolutePath) +
            paperApiSources.files.flatMap { listOf("--sources", it.absolutePath) } +
            listOf("--out", output.get().asFile.absolutePath)
    })
}

sourceSets["main"].resources.srcDir(generatedResources)

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateDocsIndex)
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("spectra-paper")
    archiveClassifier.set("")
    // Truffle discovers languages through service files; they must be merged, not overwritten. Shadow 9 drops
    // duplicates before transformers run unless they are let through.
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    // GraalJS ships as named modules; module descriptors are meaningless once shaded into a plugin jar.
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
}

tasks.named("assemble") { dependsOn("shadowJar") }
