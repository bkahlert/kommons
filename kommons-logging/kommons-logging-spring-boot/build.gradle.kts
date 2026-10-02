import com.bkahlert.kommons.gradle.jvmBytecodeTarget

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

// kaptTest extends kapt; the test sources declare no configuration properties, and a test-side
// spring-configuration-metadata.json would shadow the main one on the test classpath.
configurations.named("kaptTest") {
    exclude(group = "org.springframework.boot", module = "spring-boot-configuration-processor")
}

// Boot's configuration processor cannot derive the logging.preset.* properties from LoggingProperties
// (Kotlin adds a no-arg constructor because every parameter has a default); they are declared in
// META-INF/additional-spring-configuration-metadata.json, which the processor merges into the generated
// metadata only if told where to look: under kapt its class output is build/tmp/kapt3/classes/main, so
// its own resources lookup fails.
extensions.configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension>("kapt") {
    arguments {
        arg(
            "org.springframework.boot.configurationprocessor.additionalMetadataLocations",
            layout.projectDirectory.dir("src/jvmMain/resources").asFile.path
        )
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptTask>().configureEach {
    inputs.file(layout.projectDirectory.file("src/jvmMain/resources/META-INF/additional-spring-configuration-metadata.json"))
}
