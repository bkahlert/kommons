import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Spring Boot AutoConfiguration for Kommons Logging"

kotlin {

    jvm {
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")
        apply(plugin = "org.jetbrains.kotlin.kapt")
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                configurations["kapt"].dependencies.add(libs.spring.boot.configuration.processor)

                implementation(project(":kommons-core"))
                implementation(project(":kommons-debug"))
                implementation(project(":kommons-io"))
                implementation(project(":kommons-logging:kommons-logging-logback"))
                implementation(project(":kommons-text"))

                implementation("org.springframework.boot:spring-boot-autoconfigure")

                // TODO test
                implementation("org.springframework.cloud:spring-cloud-context:3.1.1") { because("BootstrapApplicationListener") }

                // resolves 'warning: unknown enum constant When.MAYBE'
                compileOnly(libs.jsr305)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.spring.boot.configuration.processor) // configuration metadata testing
                implementation("org.springframework.boot:spring-boot-starter-actuator") { because("LogFileWebEndpoint testing") }
                implementation("org.springframework.boot:spring-boot-starter-test") { because("output capturing") }
            }
        }
    }
}

tasks {
    // makes sure an eventually existing additional-spring-configuration-metadata.json is copied to resources,
    // see https://docs.spring.io/spring-boot/docs/2.7.1/reference/html/configuration-metadata.html
    withType<KotlinCompile> { @Suppress("UnstableApiUsage") inputs.files(withType<ProcessResources>()) }
}
