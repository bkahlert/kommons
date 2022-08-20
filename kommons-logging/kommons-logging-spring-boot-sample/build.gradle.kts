import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring") version "1.7.10"

    // TODO delete
    kotlin("kapt")
}

description = "Spring Boot Sample Application for Kommons Logging"

// TODO delete
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

dependencies {
    implementation(project(":kommons-logging:kommons-logging-spring-boot-starter"))

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // TODO delete
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(project(":kommons-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }

    withType<Test> { useJUnitPlatform() }
}


// TODO delete
tasks {
    // makes sure an eventually existing additional-spring-configuration-metadata.json is copied to resources,
    // see https://docs.spring.io/spring-boot/docs/2.7.1/reference/html/configuration-metadata.html
    withType<KotlinCompile> { @Suppress("UnstableApiUsage") inputs.files(withType<ProcessResources>()) }
}
