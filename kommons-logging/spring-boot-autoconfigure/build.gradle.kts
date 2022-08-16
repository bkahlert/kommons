plugins {
    id("org.springframework.boot")
    kotlin("jvm")
    kotlin("plugin.spring")
}

description = "Spring Boot AutoConfiguration"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":logback-logging"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.cloud:spring-cloud-context:3.1.1") { because("BootstrapApplicationListener") }

    implementation("org.springframework.boot:spring-boot-actuator") { because("LogFileWebEndpoint") }
    implementation("org.springframework.boot:spring-boot-configuration-processor") { because("MetadataStore") }
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.platform:junit-platform-launcher")

    testImplementation(platform("io.strikt:strikt-bom:0.34.1"))
    testImplementation("io.strikt:strikt-core") { because("assertion lib") }
    testImplementation("io.strikt:strikt-jvm") { because("JVM specified assertion lib") }
}
