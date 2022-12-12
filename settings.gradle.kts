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
include("kommons-kaomoji")
include("kommons-logging:kommons-logging-core")
include("kommons-logging:kommons-logging-logback")
include("kommons-logging:kommons-logging-spring-boot")
include("kommons-logging:kommons-logging-spring-boot-starter")
include("kommons-logging:kommons-logging-spring-boot-sample")
include("kommons-test")
include("kommons-text")
