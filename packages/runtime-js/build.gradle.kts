val graalVersion = "25.4.4.1.1"

dependencies {
    "api"(project(":core"))
    "api"("org.graalvm.polyglot:polyglot:$graalVersion")
    "implementation"("org.graalvm.polyglot:js:$graalVersion")
}

dependencies {
    "testImplementation"(testFixtures(project(":core")))
}
