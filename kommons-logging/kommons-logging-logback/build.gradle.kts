plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons Logging Logback is a Kotlin Library for configuring Logback with nothing but system properties, and provides support for JSON"

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val springBootVersion = libs.versions.spring.boot.get()

        val jvmMain by getting {
            dependencies {
                api(project(":kommons-core"))
                api(project(":kommons-io"))
                api(project(":kommons-text"))
                api(project(":kommons-logging:kommons-logging-core"))
                implementation("org.springframework.boot:spring-boot:$springBootVersion") { because("ColorConverter") }
                api(libs.logback.classic)
                api(libs.logstash.logback.encoder)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") { because("output capturing") }
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
    withType<ProcessResources>().configureEach {
        finalizedBy(buildLogbackAppenders)
    }
}
