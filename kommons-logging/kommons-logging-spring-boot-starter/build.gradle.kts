plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Spring Boot Starter for Kommons Logging: Spring Boot"

kotlin {

    jvm {
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(project(":kommons-logging:kommons-logging-core"))
                api(project(":kommons-logging:kommons-logging-logback"))
                api(project(":kommons-logging:kommons-logging-spring-boot"))
                api("org.springframework.boot:spring-boot-starter-logging")
            }
        }
    }
}
