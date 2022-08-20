plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Spring Boot AutoConfiguration for Kommons Logging"

kotlin {

    jvm {
//        apply(plugin = "org.springframework.boot")
//        apply(plugin = "io.spring.dependency-management")
//        apply(plugin = "org.jetbrains.kotlin.plugin.spring")

        configurations {

//            compileOnly {
//                extendsFrom(configurations.annotationProcessor.get())
//            }
        }
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmMain by getting {
            dependencies {
//                kapt("org.springframework.boot:spring-boot-configuration-processor") { because("application properties metadata creation") }
                implementation(kotlin("reflect"))
                implementation(project(":kommons-core"))
                implementation(project(":kommons-debug"))

                api(project(":kommons-logging:kommons-logging-logback"))

                // TODO use only required dependency (spring framework context)
                implementation("org.springframework.boot:spring-boot-starter-web:2.7.2") { because("to provide functionality for endpoint implementations") }
                implementation("org.springframework.boot:spring-boot-autoconfigure:2.7.2")
                implementation("org.springframework.cloud:spring-cloud-context:3.1.1") { because("BootstrapApplicationListener") }

                implementation("org.springframework.boot:spring-boot-actuator:2.7.2") { because("LogFileWebEndpoint") }
                implementation("org.springframework.boot:spring-boot-configuration-processor:2.7.2") { because("MetadataStore") }
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.spring.boot.starter.test) // output capturing

                // TODO remove
                implementation("io.strikt:strikt-core:0.34.1") { because("assertion lib") }
                implementation("io.strikt:strikt-jvm:0.34.1") { because("JVM specified assertion lib") }
            }
        }
    }
}
//
//tasks {
//    // makes sure an eventually existing additional-spring-configuration-metadata.json is copied to resources,
//    // see https://docs.spring.io/spring-boot/docs/2.7.1/reference/html/configuration-metadata.html
//    withType<KotlinCompile> { @Suppress("UnstableApiUsage") inputs.files(withType<ProcessResources>()) }
//}
