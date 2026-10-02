import com.bkahlert.kommons.gradle.jvmBytecodeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Spring Boot auto-configuration for Kommons Logging: Logback"

jvmBytecodeTarget(17)

val springBootVersion = libs.versions.spring.boot.get()

kotlin {

    jvm {
        apply(plugin = "org.jetbrains.kotlin.kapt")
    }

    sourceSets {
        jvmMain.dependencies {
            configurations["kapt"].dependencies.add(
                project.dependencies.create("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
            )

            api(project(":kommons-core"))
            api(project(":kommons-io"))
            api(project(":kommons-logging:kommons-logging-core"))
            api(project(":kommons-logging:kommons-logging-logback"))
            api(project(":kommons-text"))

            implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")

            // resolves 'warning: unknown enum constant When.MAYBE'
            compileOnly(libs.jsr305)
        }
        jvmTest.dependencies {
            implementation("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion") // configuration metadata testing
            implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion") { because("LogFileWebEndpoint testing") }
            implementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") { because("output capturing") }
        }
    }
}

tasks {
    // makes sure an eventually existing additional-spring-configuration-metadata.json is copied to resources,
    // see https://docs.spring.io/spring-boot/docs/2.7.3/reference/html/configuration-metadata.html
    withType<KotlinJvmCompile>().configureEach { inputs.files(withType<ProcessResources>()) }
}
