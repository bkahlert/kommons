pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kommons"

include("kommons-core")
include("kommons-debug")
include("kommons-io")
include("kommons-exec")
include("kommons-test")
include("kommons-text")
