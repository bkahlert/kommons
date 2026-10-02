plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.spring)
}

description = "Spring Boot sample application for Kommons Logging: Spring Boot"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":kommons-logging:kommons-logging-spring-boot-starter"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")

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
    doLast {
        copy {
            from(layout.projectDirectory.file("src/main/resources/banner.txt"))
            into(layout.buildDirectory.dir("resources/main"))
            expand("project" to project)
        }
    }
}
