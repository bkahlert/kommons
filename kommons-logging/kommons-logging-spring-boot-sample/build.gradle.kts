import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring") version @Suppress("DSL_SCOPE_VIOLATION") libs.versions.kotlin.asProvider().get()
//    kotlin("kapt")
}

description = "Spring Boot sample application for Kommons Logging: Spring Boot"

repositories {
    mavenCentral()
}

dependencies {
    // implementation("com.bkahlert.kommons:kommons-logging-spring-boot-starter:2.6.0")
    implementation(project(":kommons-logging:kommons-logging-spring-boot-starter"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
//    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // testImplementation("com.bkahlert.kommons:kommons-test:2.6.0")
    testImplementation(project(":kommons-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.set(freeCompilerArgs.get() + "-Xjsr305=strict")
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

@Suppress("UnstableApiUsage")
tasks.withType<ProcessResources>().configureEach {
    doLast {
        copy {
            from(layout.projectDirectory.file("src/main/resources/banner.txt"))
            into(layout.buildDirectory.dir("resources/main"))
            expand("project" to project)
        }
    }
}
