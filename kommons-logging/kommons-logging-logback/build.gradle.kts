plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons Logging Logback is a Kotlin Library for configuring Logback with nothing but system properties, and provides support for JSON"

kotlin {

    jvm {
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":kommons-core"))
                implementation(project(":kommons-io"))
                implementation(project(":kommons-logging:kommons-logging-core"))
                implementation("org.springframework.boot:spring-boot") { because("ColorConverter") }
                api(libs.logback.classic)
                api(libs.logstash.logback.encoder)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-test") { because("output capturing") }
            }
        }
    }
}

tasks {
    val buildLogbackAppenders by registering {
        group = "build"
        doLast {
            copy {
                val loggingDirectory = "com/bkahlert/kommons/logging/logback"
                val source = layout.projectDirectory.dir("src/jvmMain/resources/$loggingDirectory")
                val sourceIncludes = source.dir("includes")
                val templatedAppenders = source.dir("appenders")
                val builtAppenders = layout.buildDirectory.dir("processedResources/jvm/main/$loggingDirectory/appenders")

                from(templatedAppenders)
                into(builtAppenders)
                expand("includes" to checkNotNull(sourceIncludes.asFile.listFiles()).associate {
                    it.name.removeSuffix(".xml") to it.readText()
                })
            }
        }
    }

    @Suppress("UnstableApiUsage")
    withType<ProcessResources> {
        finalizedBy(buildLogbackAppenders)
    }
}
