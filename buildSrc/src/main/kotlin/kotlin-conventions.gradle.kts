import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

group = "com.bkahlert.kommons"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    google()
    gradlePluginPortal() // tvOS builds need to be able to fetch a kotlin gradle plugin
}

kotlin {
    explicitApi()

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }

        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
            languageSettings.optIn("kotlin.experimental.ExperimentalTypeInference")
            languageSettings.progressiveMode = true // false by default
        }
    }
}

tasks.withType(KotlinCompilationTask::class).configureEach {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_1_8)
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    if (System.getenv("CI") == "true") {
        systemProperty("junit.jupiter.execution.timeout.testable.method.default", "30s")
    }

    filter {
        isFailOnNoMatchingTests = false
    }
}
