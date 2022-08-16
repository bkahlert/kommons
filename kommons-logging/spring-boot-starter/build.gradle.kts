plugins {
    id("org.springframework.boot")
    kotlin("jvm")
    kotlin("plugin.spring")
}

description = "Spring Boot AutoConfiguration"

dependencies {
    api(project(":spring-boot-autoconfigure"))
}
