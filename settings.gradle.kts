plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "spectra"

include(":protocol", ":core", ":runtime-js", ":paper")

project(":protocol").projectDir = file("packages/protocol")
project(":core").projectDir = file("packages/core")
project(":runtime-js").projectDir = file("packages/runtime-js")
project(":paper").projectDir = file("apps/paper")
