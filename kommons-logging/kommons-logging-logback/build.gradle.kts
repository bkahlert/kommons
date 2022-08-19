plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons Logging Logback is a Kotlin Library to facilitate the configuration of Logback."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))

                implementation(libs.spring.boot) // ColorConverter

                api(libs.slf4j.api)
                api(libs.logback.classic)
                api(libs.logstash.logback.encoder)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.spring.boot.starter.test) // output capturing

                // TODO remove
                implementation(project(":kommons-debug"))

                // TODO remove
                implementation("io.strikt:strikt-core:0.34.1") { because("assertion lib") }
                implementation("io.strikt:strikt-jvm:0.34.1") { because("JVM specified assertion lib") }

//                compileOnly("javax.servlet:javax.servlet-api")
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
