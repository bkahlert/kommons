pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // PREFER_PROJECT (the default) on purpose: the Kotlin/JS plugin registers its own
    // Node.js and Yarn download repositories on the root project.
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kommons"

include("kommons")
include("kommons-bom")
include("kommons-core")
include("kommons-debug")
include("kommons-exec")
include("kommons-io")
include("kommons-kaomoji")
include("kommons-logging:kommons-logging-core")
include("kommons-test")
include("kommons-text")
include("kommons-time")
include("kommons-uri")
