import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Spring Boot Starter for Kommons Logging: Spring Boot"

jvmBytecodeTarget(17)

kotlin {
    sourceSets {
        jvmMain.dependencies {
            api(project.dependencies.platform(libs.spring.boot.bom))
            api(project(":kommons-logging:kommons-logging-core"))
            api(project(":kommons-logging:kommons-logging-logback"))
            api(project(":kommons-logging:kommons-logging-spring-boot"))
            api("org.springframework.boot:spring-boot-starter-logging")
        }
    }
}
