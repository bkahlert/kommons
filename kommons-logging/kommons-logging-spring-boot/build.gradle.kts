import com.bkahlert.kommons.gradle.jvmBytecodeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Spring Boot auto-configuration for Kommons Logging: Logback"

jvmBytecodeTarget(17)

kotlin {

    jvm {
        apply(plugin = "org.jetbrains.kotlin.kapt")
    }

    sourceSets {
        jvmMain.dependencies {
            // Spring artifact versions come from the Spring Boot BOM (see also the kapt block below).
            implementation(project.dependencies.platform(libs.spring.boot.bom))

            api(project(":kommons-core"))
            api(project(":kommons-io"))
            api(project(":kommons-logging:kommons-logging-core"))
            api(project(":kommons-logging:kommons-logging-logback"))
            api(project(":kommons-text"))

            implementation("org.springframework.boot:spring-boot-autoconfigure")

            // resolves 'warning: unknown enum constant When.MAYBE'
            compileOnly(libs.jsr305)
        }
        jvmTest.dependencies {
            implementation("org.springframework.boot:spring-boot-configuration-processor") // configuration metadata testing
            implementation("org.springframework.boot:spring-boot-starter-actuator") { because("LogFileWebEndpoint testing") }
            implementation("org.springframework.boot:spring-boot-starter-test") { because("output capturing") }
        }
    }
}

dependencies {
    "kapt"(platform(libs.spring.boot.bom))
    "kapt"("org.springframework.boot:spring-boot-configuration-processor")
}

tasks {
    // makes sure an eventually existing additional-spring-configuration-metadata.json is copied to resources,
    // see https://docs.spring.io/spring-boot/docs/2.7.3/reference/html/configuration-metadata.html
    withType<KotlinJvmCompile>().configureEach { inputs.files(withType<ProcessResources>()) }
}
