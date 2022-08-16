import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm")
}

description = "Facilitated Logback configuration"

kotlin {
    explicitApi()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework:spring-core") { because("ResourcePatternResolver") }

    implementation("org.springframework.boot:spring-boot") { because("ColorConverter") }

    api("org.slf4j:slf4j-api")
    api("ch.qos.logback:logback-classic")
    api("net.logstash.logback:logstash-logback-encoder:7.1.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.platform:junit-platform-launcher")

    testImplementation(platform("io.strikt:strikt-bom:0.34.1"))
    testImplementation("io.strikt:strikt-core") { because("assertion lib") }
    testImplementation("io.strikt:strikt-jvm") { because("JVM specified assertion lib") }

    compileOnly("javax.servlet:javax.servlet-api")
}

tasks {
    withType<ProcessResources> {
        filesMatching("build.properties") {
            expand(project.properties)
        }
    }
    val buildLogbackAppenders by registering {
        group = "build"
        dependsOn(named("processResources"))
        doLast {
            copy {
                // TODO determine correct directory
                val loggingDirectory = "com/bkahlert/logging"
                val source = layout.projectDirectory.dir("src/main/resources/$loggingDirectory")
                val sourceIncludes = source.dir("includes")
                val templatedAppenders = source.dir("appenders")
                val builtAppenders = layout.projectDirectory.dir("resources/main/$loggingDirectory/appenders")

                from(templatedAppenders)
                into(builtAppenders)
                expand("includes" to checkNotNull(sourceIncludes.asFile.listFiles()).associate {
                    it.name.removeSuffix(".xml") to it.readText()
                })
//                filter(ReplaceTokens::class, includes.toMap().also { println(it) })
//                filter(
//                    ReplaceTokens::class,
//                    "includes.jansi.xml" to "foo",
//                    "includes.plain-ansi-encoder.xml" to "bar",
//                )
            }
        }
    }
}

/*

                <!-- reads the includess directory and stores each file's content in a property -->
                <!-- @see LogbackConfigurationCompilationAsserter.java for more information -->
                <groupId>org.codehaus.gmaven</groupId>
                <artifactId>gmaven-plugin</artifactId>
                <version>1.5</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>execute</goal>
                        </goals>
                        <phase>generate-resources</phase>
                        <configuration>
                            <source>final def assetsDirectory = "${basedir}/src/main/resources/com/bkahlert/logging/includess"
                            final def files = Optional.ofNullable(new File(assetsDirectory).listFiles()).orElseThrow()
                            files.each({ project.properties["includes." + it.getname] = it.getText() })</source>
                        </configuration>
                    </execution>
                </executions>
 */
