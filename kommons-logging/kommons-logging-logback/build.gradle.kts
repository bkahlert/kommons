import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons Logging Logback is a Kotlin Library for configuring Logback with nothing but system properties, and provides support for JSON"

// depends on the spring-boot jar (ColorConverter), which is Java 17 bytecode
jvmBytecodeTarget(17)

val springBootVersion = libs.versions.spring.boot.get()

kotlin {
    sourceSets {
        jvmMain.dependencies {
            api(project(":kommons-core"))
            api(project(":kommons-io"))
            api(project(":kommons-text"))
            api(project(":kommons-logging:kommons-logging-core"))
            implementation("org.springframework.boot:spring-boot:$springBootVersion") { because("ColorConverter") }
            api(libs.logback.classic)
            api(libs.logstash.logback.encoder)
        }
        jvmTest.dependencies {
            implementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") { because("output capturing") }
        }
    }
}

// The appender XML files are templates that embed the files from `includes/` via `${includes["<name>"]}`.
val loggingDirectory = "com/bkahlert/kommons/logging/logback"
val includes: Map<String, String> = layout.projectDirectory.dir("src/jvmMain/resources/$loggingDirectory/includes").asFile
    .listFiles { file -> file.extension == "xml" }.orEmpty()
    .associate { it.nameWithoutExtension to it.readText() }

tasks.named<ProcessResources>("jvmProcessResources") {
    filteringCharset = "UTF-8"
    filesMatching("$loggingDirectory/appenders/*.xml") {
        expand("includes" to includes)
    }
}
