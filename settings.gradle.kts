pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

enableFeaturePreview("VERSION_CATALOGS")

rootProject.name = "kommons"

include("kommons")
include("kommons-core")
include("kommons-debug")
include("kommons-exec")
include("kommons-io")
//include("kommons-logging")
include("kommons-test")
include("kommons-text")
