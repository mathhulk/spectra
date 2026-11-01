pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "spectra"

include(":api", ":protocol", ":core", ":runtime-js", ":paper", ":fabric")

project(":api").projectDir = file("packages/api")
project(":protocol").projectDir = file("packages/protocol")
project(":core").projectDir = file("packages/core")
project(":runtime-js").projectDir = file("packages/runtime-js")
project(":paper").projectDir = file("apps/paper")
project(":fabric").projectDir = file("apps/fabric")
