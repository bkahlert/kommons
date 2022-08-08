pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kommons"

include("kommons-debug")
include("kommons-exec")
include("kommons-test")
include("kommons-text")
