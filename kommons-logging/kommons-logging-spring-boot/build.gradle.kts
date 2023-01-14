import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Spring Boot auto-configuration for Kommons Logging: Logback"

kotlin {

    jvm {
        apply(plugin = "org.jetbrains.kotlin.kapt")
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val springBootVersion = libs.versions.spring.boot.get()

        val jvmMain by getting {
            dependencies {
                configurations["kapt"].dependencies.add(
                    DefaultExternalModuleDependency("org.springframework.boot", "spring-boot-configuration-processor", springBootVersion)
                )

                implementation(project(":kommons-core"))
                implementation(project(":kommons-io"))
                implementation(project(":kommons-logging:kommons-logging-core"))
                implementation(project(":kommons-logging:kommons-logging-logback"))
                implementation(project(":kommons-text"))

                implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")

                // resolves 'warning: unknown enum constant When.MAYBE'
                compileOnly(libs.jsr305)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion") // configuration metadata testing
                implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion") { because("LogFileWebEndpoint testing") }
                implementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") { because("output capturing") }
            }
        }
    }
}

tasks {
    // makes sure an eventually existing additional-spring-configuration-metadata.json is copied to resources,
    // see https://docs.spring.io/spring-boot/docs/2.7.3/reference/html/configuration-metadata.html
    withType<KotlinJvmCompile>().configureEach { @Suppress("UnstableApiUsage") inputs.files(withType<ProcessResources>()) }
}
