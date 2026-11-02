// Public, dependency-free API for plugins that build on Spectra. Plugins depend on it with compileOnly and
// declare `depend: [Spectra]`; the Spectra plugin provides the implementation at runtime.
plugins {
    `maven-publish`
}

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "api"
            from(components["java"])
        }
    }
}
