plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    id("kommons-test-jdk-conventions")
}

description = "Spring Boot sample application for Kommons Logging: Spring Boot"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":kommons-logging:kommons-logging-spring-boot-starter"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation(project(":kommons-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

tasks.processResources {
    filteringCharset = "UTF-8"
    // banner.txt reads `${project["name"]}` and `${project["version"]}`; resolved here at configuration time
    // because the configuration cache forbids Task.project inside the filesMatching action.
    val projectProperties = mapOf("name" to project.name, "version" to project.version.toString())
    filesMatching("banner.txt") {
        expand("project" to projectProperties)
    }
}
