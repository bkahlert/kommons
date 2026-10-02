import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

group = "com.bkahlert.kommons"

kotlin {
    explicitApi()
    jvmToolchain(17)

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        progressiveMode.set(true)
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll(
            "kotlin.ExperimentalUnsignedTypes",
            "kotlin.time.ExperimentalTime",
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.experimental.ExperimentalTypeInference",
        )
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
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
