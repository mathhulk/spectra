val graalVersion = "25.4.4.1.1"

dependencies {
    "api"(project(":core"))
    "api"("org.graalvm.polyglot:polyglot:$graalVersion")
    // org.graalvm.polyglot:js is a POM-only aggregator of these two; depending on the jars directly keeps
    // shading plugins from trying to unpack a .pom file.
    "implementation"("org.graalvm.js:js-language:$graalVersion")
    "implementation"("org.graalvm.truffle:truffle-runtime:$graalVersion")
    "testImplementation"(testFixtures(project(":core")))
}
