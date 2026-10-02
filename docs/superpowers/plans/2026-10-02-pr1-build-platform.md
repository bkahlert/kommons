# Build Platform (PR 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the Kommons build from Gradle 7.3 / Kotlin 1.8.10 / JDK 8 to Gradle 9.7.0 / Kotlin 2.4.20 / a JDK 17 toolchain, with Dokka 2.2, the configuration cache, and CI workflows that run on current GitHub Actions, while every library still ships Java 8 bytecode.

**Architecture:** All build logic stays in `buildSrc` precompiled script plugins. A single buildSrc function `jvmBytecodeTarget(version)` makes the bytecode level an explicit, per-module decision (8 default, 11 for kommons-uri, 17 for the Spring stack) while the build itself runs on a JDK 17 toolchain. Native source-set wiring moves to the Kotlin default hierarchy template. Dependency versions other than the build platform (KGP, Dokka, nebula) do not change in this PR; Spring Boot stays at 2.7.6 but is consumed as a BOM instead of through its Gradle plugin, which does not run on Gradle 9.

**Tech Stack:** Gradle 9.7.0 (Kotlin DSL, version catalog, buildSrc), Kotlin Multiplatform 2.4.20 (JVM, JS, Linux/Windows/macOS native), Dokka 2.2.0, nebula.release 21.1.4, Foojay toolchain resolver 1.0.0, GitHub Actions.

**Spec:** [docs/superpowers/specs/2026-10-02-modernization-design.md](../specs/2026-10-02-modernization-design.md), sections "PR 1: build platform" and "Decisions" 1, 3, 7, 9, 12.

This plan covers PR 1 only. PRs 2 to 5 get their own plans after PR 1 is merged, because their code changes depend on what the Kotlin 2.4 compiler and the Gradle 9 upgrade surface.

Two deviations from the spec's PR 1 text, both to keep PR 1 to the build platform:

- The vanniktech publishing plugin is added to buildSrc in PR 2, where it is first used, not here.
- The Spring Boot sample drops the Spring Boot Gradle plugin instead of keeping it: the 2.7.x plugin does not run on Gradle 9, and bumping it means Spring Boot 4, which is PR 4. The sample keeps compiling and testing against the 2.7.6 BOM; `bootRun` returns in PR 4.

## Global Constraints

- Library bytecode stays at JVM 1.8. kommons-uri is 11. `kommons-logging-logback`, `kommons-logging-spring-boot`, `kommons-logging-spring-boot-starter` and `kommons-logging-spring-boot-sample` are 17.
- The build runs on JDK 17 (`jvmToolchain(17)`); every Gradle command in this plan runs with `JAVA_HOME` pointing at a JDK 17.
- Gradle 9.7.0 exactly: it is the top of KGP 2.4.20's documented range (7.6.3 to 9.7.0).
- Kotlin 2.4.20, language and api version 2.4, progressive mode on, explicit API mode on.
- Project repositories reduce to `mavenCentral()` declared in settings. No `mavenLocal`, no OSSRH snapshot repository, no `google()`, no `gradlePluginPortal()` in project repositories.
- Native targets: linuxX64, linuxArm64, mingwX64, macosX64, macosArm64. No Apple mobile targets.
- No dependency versions other than Kotlin, Dokka and nebula.release change in this PR. Spring Boot stays 2.7.6, Logback 1.2.11, Kotest 5.5.4, and so on.
- Gradle 9's "test sources exist but no tests were discovered" check stays enabled.
- Commits follow Conventional Commits (`build:`, `ci:`, `test:`, `refactor:`), lowercase description, no AI attribution trailers.
- Do not touch the author's uncommitted changes on `master`. Work in a separate worktree.

## Review Focus

Inputs and conditions the spec implies but no task's tests exercise by default; each has a pinning check in the task named.

1. A consumer on Java 8 loads a library jar: class files must be version 52, otherwise `UnsupportedClassVersionError` at runtime. Task 6 checks every published jvm jar.
2. `-PtestJdk=21` must launch the test JVM on JDK 21, not silently stay on 17. Task 7 step 6 checks the executor command line.
3. `-PosArchOnly=true` on an Apple-silicon host must register exactly one native target (macosArm64) and still provide `nativeMain`. Task 1 step 15 checks the task list.
4. Expanded Logback appender resources must contain the include text and no leftover `${includes[...]}` template markers, otherwise Logback fails at runtime with an unresolved property. Task 5 step 6 checks the processed resource.
5. The sample's `banner.txt` must still expand `${project["name"]}` and `${project["version"]}` after the `project` object is replaced by a map. Task 5 step 7 checks the processed banner.

---

## Preparation

- [ ] **Step 1: Create a worktree on a fresh branch from master**

The author's working tree on `master` carries uncommitted work. Use a worktree so it stays untouched.

```bash
cd /Users/bkahlert/Development/com.bkahlert/kommons
git worktree add ../kommons-pr1 -b build/gradle-9-kotlin-2-4 master
cd ../kommons-pr1
```

If the spec/plan branch `docs/modernization-spec` is not merged yet, cherry-pick nothing; the plan is read from the main checkout.

- [ ] **Step 2: Point every Gradle invocation at JDK 17**

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
"$JAVA_HOME/bin/java" -version
```

Expected: `openjdk version "17.…"`. Every `./gradlew` command below assumes this `JAVA_HOME` in the same shell.

---

### Task 1: Make the build configure on Gradle 9.7.0 and Kotlin 2.4.20

The build cannot be verified in smaller increments: Gradle 9 rejects KGP 1.8, and KGP 2.4 rejects Gradle 7.3. All configuration changes land together and are verified by `./gradlew help`.

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle.properties`
- Modify: `gradle/libs.versions.toml`
- Modify: `buildSrc/settings.gradle.kts`
- Modify: `buildSrc/build.gradle.kts`
- Create: `buildSrc/src/main/kotlin/com/bkahlert/kommons/gradle/JvmBytecodeTarget.kt`
- Modify: `buildSrc/src/main/kotlin/kotlin-conventions.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/kommons-jvm-conventions.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/kommons-js-conventions.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/kommons-native-conventions.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts` (Kotlin fixes only; Dokka wiring is Task 4)
- Modify: `kommons-time/build.gradle.kts`
- Modify: `kommons-uri/build.gradle.kts`
- Modify: `kommons-logging/kommons-logging-logback/build.gradle.kts`
- Modify: `kommons-logging/kommons-logging-spring-boot/build.gradle.kts`
- Modify: `kommons-logging/kommons-logging-spring-boot-starter/build.gradle.kts`
- Modify: `kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts`

**Interfaces:**
- Produces: `fun Project.jvmBytecodeTarget(version: Int)` in package `com.bkahlert.kommons.gradle`; the Gradle property `osArchOnly` (unchanged name); the convention plugin ids `kotlin-conventions`, `kommons-jvm-conventions`, `kommons-js-conventions`, `kommons-native-conventions`, `kommons-publishing-conventions` and the three `kommons-multiplatform-*-library-conventions` (unchanged).
- Produces: catalog aliases `libs.plugins.kotlin.serialization`, `libs.plugins.kotlin.spring`, `libs.spring.boot.bom`.

- [ ] **Step 1: Gradle wrapper properties**

Replace the content of `gradle/wrapper/gradle-wrapper.properties` with:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.7.0-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

The wrapper jar and scripts are refreshed in step 17 once the build configures.

- [ ] **Step 2: settings.gradle.kts**

Replace the file with:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // PREFER_PROJECT (the default) on purpose: the Kotlin/JS plugin registers its own
    // Node.js and Yarn download repositories on the root project.
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kommons"

include("kommons")
include("kommons-bom")
include("kommons-core")
include("kommons-debug")
include("kommons-exec")
include("kommons-io")
include("kommons-kaomoji")
include("kommons-logging:kommons-logging-core")
include("kommons-logging:kommons-logging-logback")
include("kommons-logging:kommons-logging-spring-boot")
include("kommons-logging:kommons-logging-spring-boot-starter")
include("kommons-logging:kommons-logging-spring-boot-sample")
include("kommons-test")
include("kommons-text")
include("kommons-time")
include("kommons-uri")
```

- [ ] **Step 3: root build.gradle.kts**

Replace the file with:

```kotlin
plugins {
    id("com.netflix.nebula.release")
}
```

- [ ] **Step 4: gradle.properties**

Replace the file with:

```properties
kapt.include.compile.classpath=false
kotlin.code.style=official
org.gradle.caching=true
org.gradle.jvmargs=-Dfile.encoding=UTF-8 -Xmx3g -XX:MaxMetaspaceSize=756m
```

The three removed properties (`kotlin.native.cacheKind.linuxX64=none`, `kotlin.native.ignoreDisabledTargets=true`, `kotlin.js.generate.executable.default=false`) are only re-added, one at a time with a comment naming the error they fix, if steps 16 or Task 2 fail without them.

- [ ] **Step 5: version catalog**

In `gradle/libs.versions.toml`:

In `[versions]` change:

```toml
dokka = "2.2.0"
kotlin = "2.4.20"
nebula-release = "21.1.4"
```

Delete the line `spring-dependency-management = "1.1.0"`.

In `[libraries]` delete the `spring-dependency-management-gradle-plugin` entry and add:

```toml
spring-boot-bom = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }
```

Append a new section:

```toml
[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-spring = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
```

Leave every other version untouched. `spring-boot-gradle-plugin` stays in the catalog (PR 4 uses it again) but leaves the buildSrc classpath in the next step.

- [ ] **Step 6: buildSrc settings and build**

Replace `buildSrc/settings.gradle.kts` with:

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

Replace `buildSrc/build.gradle.kts` with:

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.nebula.release.plugin)
}
```

- [ ] **Step 7: the bytecode-target helper**

Create `buildSrc/src/main/kotlin/com/bkahlert/kommons/gradle/JvmBytecodeTarget.kt`:

```kotlin
package com.bkahlert.kommons.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Compiles the JVM target of this multiplatform project to bytecode of the given Java [version] (8, 11, 17, ...)
 * for Kotlin and Java sources alike, independent of the JDK toolchain the build runs on.
 *
 * Both have to agree, otherwise the Kotlin Gradle plugin's JVM target validation fails the build.
 */
fun Project.jvmBytecodeTarget(version: Int) {
    val javaVersion = JavaVersion.toVersion(version)
    extensions.configure<KotlinMultiplatformExtension> {
        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
            }
        }
    }
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}
```

`JavaVersion.toVersion(8).toString()` is `"1.8"`, which is what `JvmTarget.fromTarget` expects; 11 and 17 map to `"11"` and `"17"`.

- [ ] **Step 8: kotlin-conventions**

Replace `buildSrc/src/main/kotlin/kotlin-conventions.gradle.kts` with:

```kotlin
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
```

- [ ] **Step 9: kommons-jvm-conventions**

Replace `buildSrc/src/main/kotlin/kommons-jvm-conventions.gradle.kts` with:

```kotlin
import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kotlin-conventions")
}

kotlin {
    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}

jvmBytecodeTarget(8)

// `-PtestJdk=21` runs the JVM tests on that JDK instead of the build toolchain; CI uses it for 21 and 25.
val testJdk: String? by project
testJdk?.let { version ->
    val launcher = the<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(version))
    }
    tasks.withType<Test>().configureEach {
        javaLauncher.set(launcher)
    }
}
```

- [ ] **Step 10: kommons-js-conventions**

Replace `buildSrc/src/main/kotlin/kommons-js-conventions.gradle.kts` with:

```kotlin
plugins {
    id("kotlin-conventions")
}

kotlin {
    js {
        browser {
            testTask {
                testLogging.showStandardStreams = true
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "10000"
                }
            }
        }
    }
}
```

The `yarn.ignoreScripts = false` line is dropped. If Task 2 prints `warning Ignored scripts due to flag.`, re-add it as:

```kotlin
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().ignoreScripts = false
}
```

- [ ] **Step 11: kommons-native-conventions**

Replace `buildSrc/src/main/kotlin/kommons-native-conventions.gradle.kts` with:

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("kotlin-conventions")
}

// `-PosArchOnly=true` registers only the host's native target, which keeps local builds short.
// Source-set wiring (nativeMain, linuxMain, appleMain, ...) comes from Kotlin's default hierarchy template.
val osArchOnly: Boolean? by project

kotlin {
    if (osArchOnly == true) {
        val hostOs = System.getProperty("os.name")
        val hostArch = System.getProperty("os.arch")
        when {
            hostOs == "Mac OS X" -> if (hostArch == "aarch64") macosArm64() else macosX64()
            hostOs == "Linux" -> if (hostArch == "aarch64") linuxArm64() else linuxX64()
            hostOs.startsWith("Windows") -> mingwX64()
            else -> throw GradleException("Host OS $hostOs is not supported in Kotlin/Native.")
        }
    } else {
        linuxX64()
        linuxArm64()
        mingwX64()
        macosX64()
        macosArm64()
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        compilerOptions {
            optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
}
```

- [ ] **Step 12: publishing conventions, Kotlin-only fixes**

In `buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts`:

Replace

```kotlin
plugins {
    signing
    id("maven-publish")
    id("nebula.release")
}
```

with

```kotlin
plugins {
    signing
    id("maven-publish")
    id("com.netflix.nebula.release")
}
```

Replace

```kotlin
            name.set(project.name.split("-").joinToString(" ") { it.capitalize() })
```

with

```kotlin
            name.set(project.name.split("-").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } })
```

Leave the Dokka imports, the `dokkaPlugin` dependency and the `javadocJar` task as they are for now; Task 4 replaces them. If `./gradlew help` in step 16 fails on the `DokkaTask` import, do Task 4 step 1 first and come back.

- [ ] **Step 13: kommons-time and kommons-uri**

In `kommons-time/build.gradle.kts` replace the plugins block with:

```kotlin
plugins {
    id("kommons-multiplatform-library-conventions")
    alias(libs.plugins.kotlin.serialization)
}
```

Replace `kommons-uri/build.gradle.kts` with:

```kotlin
import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kommons-multiplatform-library-conventions")
    alias(libs.plugins.kotlin.serialization)
}

description = "Kommons URI is a Kotlin Multiplatform Library for handling (Data) URIs."

// java.net.spi.URLStreamHandlerProvider needs Java 9+
jvmBytecodeTarget(11)

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(platform(libs.kotlinx.serialization.bom.get()))
                api("org.jetbrains.kotlinx:kotlinx-serialization-core")

                api(platform(libs.ktor.bom.get()))
                api("io.ktor:ktor-http")
                api("io.ktor:ktor-utils")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
                implementation(project(":kommons-test"))
            }
        }
    }
}
```

- [ ] **Step 14: Spring module build files**

`kommons-logging/kommons-logging-logback/build.gradle.kts`: add the import and the call directly below the `description` line; the rest of the file is unchanged in this task (Task 5 rewrites `buildLogbackAppenders`).

```kotlin
import com.bkahlert.kommons.gradle.jvmBytecodeTarget
```

```kotlin
// depends on the spring-boot jar (ColorConverter), which is Java 17 bytecode
jvmBytecodeTarget(17)
```

`kommons-logging/kommons-logging-spring-boot/build.gradle.kts`: replace the file with:

```kotlin
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

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val springBootVersion = libs.versions.spring.boot.get()

        val jvmMain by getting {
            dependencies {
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
    withType<KotlinJvmCompile>().configureEach { inputs.files(withType<ProcessResources>()) }
}
```

The only functional change is `project.dependencies.create(...)` replacing the internal `DefaultExternalModuleDependency` constructor.

`kommons-logging/kommons-logging-spring-boot-starter/build.gradle.kts`: replace the file with:

```kotlin
import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Spring Boot Starter for Kommons Logging: Spring Boot"

jvmBytecodeTarget(17)

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(platform(libs.spring.boot.bom.get()))
                api(project(":kommons-logging:kommons-logging-core"))
                api(project(":kommons-logging:kommons-logging-logback"))
                api(project(":kommons-logging:kommons-logging-spring-boot"))
                api("org.springframework.boot:spring-boot-starter-logging")
            }
        }
    }
}
```

`kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts`: replace the file with:

```kotlin
plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.spring)
}

description = "Spring Boot sample application for Kommons Logging: Spring Boot"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":kommons-logging:kommons-logging-spring-boot-starter"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation(project(":kommons-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

tasks.processResources {
    doLast {
        copy {
            from(layout.projectDirectory.file("src/main/resources/banner.txt"))
            into(layout.buildDirectory.dir("resources/main"))
            expand("project" to project)
        }
    }
}
```

This drops the Spring Boot Gradle plugin (2.7.x does not run on Gradle 9) and with it `bootRun`/`bootJar` for the sample; PR 4 restores the plugin at 4.1. The banner `doLast` block is kept verbatim here and rewritten in Task 5. If Kotlin 2.4.20 reports `-Xannotation-default-target=param-property` as already the default, remove that argument.

- [ ] **Step 15: Configure the build**

```bash
./gradlew help --warning-mode all
```

Expected: `BUILD SUCCESSFUL`. Work through failures in this order, each time re-running the command:

- Any `Unresolved reference` in buildSrc: fix the import or API name against the KGP 2.4.20 / Dokka 2.2.0 / Gradle 9.7 javadoc; do not fall back to `@Suppress`.
- A failure mentioning `DokkaTask`: do Task 4 step 1 now.
- `Inconsistent JVM-target compatibility detected for tasks 'compileJava' (17) and 'compileKotlinJvm' (1.8)`: `jvmBytecodeTarget` is not being applied to that module; check the module applies a `kommons-*-conventions` plugin that includes `kommons-jvm-conventions`.
- A failure about Node.js or Yarn repositories: the settings-level repositories mode is interfering; confirm `dependencyResolutionManagement` has no `repositoriesMode` set.
- A failure that one of the three removed `gradle.properties` entries fixed: re-add exactly that one with a comment naming the error.

Then check the host-only switch:

```bash
./gradlew tasks --all -PosArchOnly=true | grep -E '^(macosArm64|macosX64|linuxX64|linuxArm64|mingwX64)Test' | sort -u
```

Expected on an Apple-silicon Mac: exactly one line, `macosArm64Test …`.

- [ ] **Step 16: Record deprecation warnings**

```bash
./gradlew help --warning-mode all 2>&1 | grep -iE 'deprecat' | sort -u
```

Expected: no line mentioning `kommons`, `buildSrc` or a file under this repository. Warnings from third-party plugins are recorded in the PR description, not fixed here.

- [ ] **Step 17: Refresh the wrapper scripts**

```bash
./gradlew wrapper --gradle-version 9.7.0 --distribution-type bin
./gradlew --version | head -3
```

Expected: `Gradle 9.7.0` and an updated `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`.

- [ ] **Step 18: Commit**

```bash
git add gradle/wrapper gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml buildSrc kommons-time/build.gradle.kts kommons-uri/build.gradle.kts kommons-logging/*/build.gradle.kts
git commit -m "build: upgrade to gradle 9.7 and kotlin 2.4.20

Gradle 9 needs JDK 17 to run, so the build now uses a JDK 17 toolchain
while libraries keep compiling to Java 8 bytecode (kommons-uri 11, the
Spring modules 17) via jvmBytecodeTarget. Native source sets come from
the default hierarchy template. Project repositories move to settings
and shrink to Maven Central. The Spring Boot Gradle plugin is replaced
by the Spring Boot BOM because 2.7.x does not run on Gradle 9."
```

---

### Task 2: Compile every target

**Files:**
- Modify: whatever the compiler reports, typically under `kommons-*/src/**/*.kt`
- Modify: `kotlin-js-store/yarn.lock` (regenerated)

**Interfaces:**
- Consumes: the configured build from Task 1.

- [ ] **Step 1: Regenerate the Yarn lock file**

KGP's bundled npm tooling (Karma, Mocha, webpack) changed version with Kotlin 2.4, so the lock file no longer matches and `kotlinStoreYarnLock` would fail the build.

```bash
./gradlew kotlinUpgradeYarnLock
git diff --stat kotlin-js-store/yarn.lock
```

Expected: the lock file changed.

- [ ] **Step 2: Compile on the host**

```bash
./gradlew assemble -PosArchOnly=true --continue
```

Expected after fixes: `BUILD SUCCESSFUL`. Fix each compiler error with the stdlib replacement, not with a suppression:

| Error | Replacement |
|---|---|
| `Char.toInt()` deprecated | `Char.code` |
| `Number.toChar()` (on anything but `Int`) deprecated | `.toInt().toChar()` |
| `String.toUpperCase()` / `toLowerCase()` | `uppercase()` / `lowercase()` |
| opt-in required for `kotlinx.cinterop.*` | already covered by Task 1 step 11; if it still fires, the file is not in a native source set the convention configures |
| unresolved `kotlin.js.*` legacy API | the `kotlin.js` IR equivalent named in the error message |

Any error that is not a one-line stdlib replacement goes into the PR description under "needs a decision" and is fixed with the smallest change that keeps behaviour. Do not change public API signatures in this PR.

- [ ] **Step 3: Check Yarn and Karma warnings**

```bash
./gradlew :kommons-core:jsBrowserTest -PosArchOnly=true 2>&1 | grep -iE 'ignored scripts|deprecat' | sort -u
```

Expected: no `Ignored scripts due to flag` line. If it appears, apply the `YarnRootExtension` snippet from Task 1 step 10. If KGP prints a deprecation for `useKarma`, record the suggested replacement in the PR description; do not change the DSL in this task.

- [ ] **Step 4: Commit**

```bash
git add -A -- kotlin-js-store ':(glob)kommons*/src/**/*.kt' buildSrc
git commit -m "build: fix compilation on kotlin 2.4

Replaces APIs that Kotlin 2.4 rejects with their stdlib equivalents
and regenerates the Yarn lock file for the plugin's npm tooling."
```

---

### Task 3: Run the test suite on the host

**Files:**
- Modify: whatever fails, under `kommons-*/src/**Test/**/*.kt` or the production code it tests

**Interfaces:**
- Consumes: the compiling build from Task 2.

- [ ] **Step 1: Run all tests on the host**

```bash
./gradlew allTests test -PosArchOnly=true --continue
```

`allTests` covers every multiplatform module (JVM, JS Node and browser, host native); `test` covers the Spring Boot sample, whose task is not named `jvmTest` and therefore never ran in the old CI command.

Expected after fixes: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Triage failures**

For each failing test decide, in this order:

1. The test asserted behaviour of the old compiler or old Gradle (for example reflection names, `toString` of lambdas, exception messages from the stdlib): adjust the expectation and say so in the commit body.
2. Gradle reports `No test executed` / "no tests were discovered" for a module with test sources: the JUnit Platform engine is missing from that module's test runtime classpath. Add `implementation(project(":kommons-test"))` to its test source set; do not set `failOnNoDiscoveredTests = false`.
3. Production behaviour changed under Kotlin 2.4 (for example inference picked another overload): fix the production code so the existing test passes.

Anything that does not fit goes into the PR description under "needs a decision".

- [ ] **Step 3: Commit**

```bash
git add -A -- ':(glob)kommons*/src/**/*.kt' ':(glob)kommons*/build.gradle.kts'
git commit -m "test: adapt tests to kotlin 2.4 and gradle 9"
```

Skip the commit if nothing changed.

---

### Task 4: Dokka 2.2 and the javadoc jar

**Files:**
- Modify: `buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts`

**Interfaces:**
- Produces: task `javadocJar` per published module, built from `dokkaGeneratePublicationHtml`. PR 2 replaces this wiring with the vanniktech plugin's `JavadocJar.Dokka("dokkaGeneratePublicationHtml")`.

- [ ] **Step 1: Replace the Dokka v1 wiring**

In `buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts`:

Replace

```kotlin
import org.jetbrains.dokka.gradle.DokkaTask
```

with

```kotlin
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
```

Replace

```kotlin
    tasks.withType<DokkaTask>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
```

with

```kotlin
    tasks.withType<DokkaGenerateTask>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
```

Delete

```kotlin
val dokkaPlugin by configurations
dependencies { dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.7.10") }
```

Replace

```kotlin
val javadocJar by tasks.registering(Jar::class) {
    description = "Generates a JavaDoc JAR using Dokka"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    tasks.named<DokkaTask>("dokkaHtml").also { dokkaHtml ->
        dependsOn(dokkaHtml)
        from(dokkaHtml.get().outputDirectory)
    }
}
```

with

```kotlin
val javadocJar by tasks.registering(Jar::class) {
    description = "Generates a JavaDoc JAR using Dokka"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaGeneratePublicationHtml"))
}
```

`from(taskProvider)` wires the task dependency and uses the task's outputs; no `dependsOn` is needed.

- [ ] **Step 2: Build one javadoc jar**

The local version is a nebula dev version (`…-dev.0.uncommitted+…`), not `-SNAPSHOT`, so Dokka is enabled.

```bash
./gradlew :kommons-core:javadocJar -PosArchOnly=true
unzip -l kommons-core/build/libs/kommons-core-*-javadoc.jar | grep -cE 'index\.html|navigation\.html'
```

Expected: `BUILD SUCCESSFUL` and a count of at least `1`.

- [ ] **Step 3: Commit**

```bash
git add buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts
git commit -m "build: migrate to dokka 2.2

The javadoc jar is built from dokkaGeneratePublicationHtml. The
versioning plugin is dropped: nothing ever configured olderVersionsDir,
so it never produced anything."
```

---

### Task 5: Configuration cache

**Files:**
- Modify: `gradle.properties`
- Modify: `kommons-logging/kommons-logging-logback/build.gradle.kts`
- Modify: `kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts`
- Modify: `kommons-text/build.gradle.kts`

**Interfaces:**
- Consumes: `jvmProcessResources` task of kommons-logging-logback, `processResources` of the sample.

- [ ] **Step 1: Enable the cache**

Append to `gradle.properties`:

```properties
org.gradle.configuration-cache=true
```

- [ ] **Step 2: Logback appenders as resource filtering**

In `kommons-logging/kommons-logging-logback/build.gradle.kts` replace the whole `tasks { … }` block (from `tasks {` to its closing brace) with:

```kotlin
// The appender XML files are templates that embed the files from `includes/` via `${includes["<name>"]}`.
val loggingDirectory = "com/bkahlert/kommons/logging/logback"
val includes: Map<String, String> = layout.projectDirectory.dir("src/jvmMain/resources/$loggingDirectory/includes").asFile
    .listFiles { file -> file.extension == "xml" }.orEmpty()
    .associate { it.nameWithoutExtension to it.readText() }

tasks.named<ProcessResources>("jvmProcessResources") {
    filteringCharset = "UTF-8"
    filesMatching("$loggingDirectory/appenders/*.xml") {
        expand("includes" to includes)
    }
}
```

- [ ] **Step 3: Sample banner as resource filtering**

In `kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts` replace

```kotlin
tasks.processResources {
    doLast {
        copy {
            from(layout.projectDirectory.file("src/main/resources/banner.txt"))
            into(layout.buildDirectory.dir("resources/main"))
            expand("project" to project)
        }
    }
}
```

with

```kotlin
tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("banner.txt") {
        // banner.txt reads `${project["name"]}` and `${project["version"]}`
        expand("project" to mapOf("name" to project.name, "version" to project.version.toString()))
    }
}
```

- [ ] **Step 4: Unicode generator without project access at execution time**

In `kommons-text/build.gradle.kts` replace the `tasks { … }` block with:

```kotlin
val generateUnicodeData by tasks.registering {
    group = "build"
    description = "Regenerates src/nativeMain/.../UnicodeData.kt from the Unicode Character Database"
    val sourceFile = layout.projectDirectory.file("src/nativeMain/kotlin/com/bkahlert/kommons/text/UnicodeData.kt").asFile
    doLast {
        val generated = Unicode.UnicodeData.generate(sourceFile)
        logger.lifecycle("Generated $generated")
    }
}
```

`logger` inside `doLast` is the task's logger, `sourceFile` is captured as a plain `File`.

- [ ] **Step 5: Store and reuse the cache**

```bash
./gradlew help 2>&1 | grep -E 'Configuration cache entry (stored|reused)|problems were found'
./gradlew help 2>&1 | grep -E 'Reusing configuration cache|Configuration cache entry reused'
```

Expected: first command prints `Configuration cache entry stored.`, second prints a reuse line. If the first reports problems, open `build/reports/configuration-cache/**/configuration-cache-report.html` and sort:

- A problem in a file under this repository: fix it.
- A problem inside `com.netflix.nebula`, `org.jetbrains.kotlin` or `org.jetbrains.dokka`: set `org.gradle.configuration-cache=false` in `gradle.properties` with the comment `# <plugin> <version> is not configuration-cache compatible: <first problem line>` and continue; the steps below still apply.

Then run the full host suite with the cache:

```bash
./gradlew allTests test -PosArchOnly=true
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Check the expanded appender**

```bash
./gradlew :kommons-logging:kommons-logging-logback:jvmProcessResources -PosArchOnly=true
f=kommons-logging/kommons-logging-logback/build/processedResources/jvm/main/com/bkahlert/kommons/logging/logback/appenders/console-json-appender.xml
grep -c 'includes\[' "$f"; grep -c 'LoggingEventCompositeJsonEncoder' "$f"
```

Expected: `0` then `1` (the json-encoder include was inlined, no template marker is left).

- [ ] **Step 7: Check the expanded banner**

```bash
./gradlew :kommons-logging:kommons-logging-spring-boot-sample:processResources
grep -c 'kommons-logging-spring-boot-sample' kommons-logging/kommons-logging-spring-boot-sample/build/resources/main/banner.txt
grep -c 'project\[' kommons-logging/kommons-logging-spring-boot-sample/build/resources/main/banner.txt
```

Expected: `1` then `0`.

- [ ] **Step 8: Commit**

```bash
git add gradle.properties kommons-logging/kommons-logging-logback/build.gradle.kts kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts kommons-text/build.gradle.kts
git commit -m "build: enable the configuration cache

The ad-hoc copy blocks in doLast (Logback appender templates, sample
banner, Unicode generator) accessed the project at execution time and
are now declarative task configuration."
```

If the cache had to stay off, the header is `build: make ad-hoc tasks configuration-cache compatible` and the body names the blocking plugin.

---

### Task 6: Verify the bytecode levels

**Files:** none modified.

**Interfaces:**
- Consumes: the jvm jars built by `assemble`.

- [ ] **Step 1: Build all jvm jars**

```bash
./gradlew jvmJar -PosArchOnly=true
./gradlew :kommons-logging:kommons-logging-spring-boot-sample:jar
```

- [ ] **Step 2: Read the class file version of each jar**

```bash
for jar in $(find . -path '*/build/libs/*' -name '*-jvm-*.jar' -not -name '*-sources.jar' -not -name '*-javadoc.jar') \
           kommons-logging/kommons-logging-spring-boot-sample/build/libs/kommons-logging-spring-boot-sample-*.jar; do
  cls=$(unzip -Z1 "$jar" | grep '\.class$' | grep -v module-info | head -1)
  [ -n "$cls" ] || continue
  major=$(unzip -p "$jar" "$cls" | od -An -tu1 -j6 -N2 | awk '{print $1*256+$2}')
  printf '%-4s %s\n' "$major" "$jar"
done | sort
```

Expected:

| Major | Modules |
|---|---|
| 52 | kommons, kommons-core, kommons-debug, kommons-exec, kommons-io, kommons-kaomoji, kommons-test, kommons-text, kommons-time, kommons-logging-core |
| 55 | kommons-uri |
| 61 | kommons-logging-logback, kommons-logging-spring-boot, kommons-logging-spring-boot-starter, kommons-logging-spring-boot-sample |

Paste the output into the PR description. Any other number means `jvmBytecodeTarget` is not applied to that module; fix in its build file and re-run.

---

### Task 7: CI workflows

**Files:**
- Modify: `.github/workflows/build.yml`
- Modify: `.github/workflows/build-custom.yml`
- Modify: `.github/workflows/test-report.yml`
- Modify: `.github/workflows/release.yml` (action bumps only; PR 2 rewrites it)

**Interfaces:**
- Consumes: Gradle property `testJdk` (Task 1 step 9), task names `<target>Test`, `allTests`, `test`.

- [ ] **Step 1: build.yml**

Replace the file with:

```yaml
name: build

on:
  push:
    branches: ['master']
    tags: ['v*']
    paths-ignore: ['**.md']
  pull_request:
    branches: ['master']
    paths-ignore: ['**.md']
  workflow_dispatch:
    inputs:
      additional-gradle-args:
        description: "Additional Gradle arguments to add to the command line"
        required: false
        default: '--info --tests "com.bkahlert.kommons.logging.*"'

env:
  GRADLE_OPTS: -Dorg.gradle.jvmargs="-Dfile.encoding=UTF-8 -Xmx3g -XX:MaxMetaspaceSize=756m -XX:+HeapDumpOnOutOfMemoryError"

concurrency:
  group: ${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  build:
    strategy:
      fail-fast: false
      matrix:
        include:
          # JVM: the build always runs on JDK 17; `test-jdk` selects the JDK the tests run on.
          - { os: ubuntu-latest, target: jvm, test-jdk: 17 }
          - { os: ubuntu-latest, target: jvm, test-jdk: 21 }
          - { os: ubuntu-latest, target: jvm, test-jdk: 25 }
          - { os: windows-latest, target: jvm, test-jdk: 17 }
          - { os: macos-latest, target: jvm, test-jdk: 17 }

          - { os: ubuntu-latest, target: js }

          - { os: ubuntu-latest, target: linuxX64 }
          - { os: ubuntu-24.04-arm, target: linuxArm64 }
          - { os: windows-latest, target: mingwX64 }
          - { os: macos-latest, target: macosArm64 }

    runs-on: ${{ matrix.os }}

    steps:
      - name: Checkout
        uses: actions/checkout@v7

      - name: Set up JDK 17
        uses: actions/setup-java@v6
        with:
          java-version: 17
          distribution: temurin

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Test ${{ matrix.target }}${{ matrix.test-jdk && format(' on JDK {0}', matrix.test-jdk) || '' }}
        shell: bash
        run: >
          ./gradlew ${{ matrix.target }}Test
          ${{ matrix.target == 'jvm' && 'test' || '' }}
          ${{ matrix.test-jdk && format('-PtestJdk={0}', matrix.test-jdk) || '' }}
          -Pkotlin.tests.individualTaskReports=true
          ${{ github.event.inputs.additional-gradle-args }}

      - name: Upload test reports
        uses: actions/upload-artifact@v7
        if: success() || failure()
        with:
          name: test-reports--os-${{ matrix.os }}--target-${{ matrix.target }}--java-${{ matrix.test-jdk || 17 }}
          if-no-files-found: error
          path: ./**/build/test-results/**/*.xml
```

The `branch` input of the old `workflow_dispatch` is dropped: the old job never used it (its checkout had no `ref`). `test` is added for the jvm target so the Spring Boot sample's tests run in CI for the first time.

- [ ] **Step 2: build-custom.yml**

Replace the `steps` of the `build` job and the `java-version` default:

```yaml
      java-version:
        description: "JDK version"
        required: true
        default: '17'
```

```yaml
    steps:
      - name: Checkout
        uses: actions/checkout@v7
        with:
          fetch-depth: 0
          ref: ${{ github.event.inputs.branch }}

      - name: Set up JDK ${{ github.event.inputs.java-version }}
        uses: actions/setup-java@v6
        with:
          java-version: ${{ github.event.inputs.java-version }}
          distribution: ${{ github.event.inputs.java-distribution }}

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Build
        shell: bash
        run: ./gradlew ${TARGET}Test -Pkotlin.tests.individualTaskReports=true

      - name: Upload test reports
        uses: actions/upload-artifact@v7
        if: success() || failure()
        with:
          name: test-reports--os-${{ github.event.inputs.os }}--target-${{ github.event.inputs.target }}--java-${{ github.event.inputs.java-version }}
          if-no-files-found: error
          path: ./**/build/test-results/**/*.xml

      - uses: actions/upload-artifact@v7
        with:
          name: kommons--os-${{ github.event.inputs.os }}--target-${{ github.event.inputs.target }}--java-${{ github.event.inputs.java-version }}
          if-no-files-found: error
          path: |
            ./**/build/dokka
            ./**/build/libs
            ./**/build/publications
            ./**/build/reports
            ./**/build/test-results
```

Everything above `steps` other than the default stays as is.

- [ ] **Step 3: test-report.yml**

Replace `uses: dorny/test-reporter@v1` with `uses: dorny/test-reporter@v3`. Nothing else changes.

- [ ] **Step 4: release.yml action bumps**

Apply with sed, then review the diff:

```bash
sed -i '' \
  -e 's#actions/checkout@v3#actions/checkout@v7#g' \
  -e 's#actions/setup-java@v3#actions/setup-java@v6#g' \
  -e 's#uses: gradle/gradle-build-action@v2#uses: gradle/actions/setup-gradle@v6#g' \
  -e 's#softprops/action-gh-release@v1#softprops/action-gh-release@v3#g' \
  -e "s#default: '8'#default: '17'#" \
  -e "s#default: 'zulu'#default: 'temurin'#" \
  .github/workflows/release.yml
git diff --stat .github/workflows/release.yml
```

Expected: only `uses:` lines and the two defaults changed. The OSSRH publish tasks stay; they are dead until PR 2 replaces the workflow.

- [ ] **Step 5: Lint the workflows**

```bash
command -v actionlint >/dev/null || brew install actionlint
actionlint .github/workflows/*.yml
```

Expected: no output.

- [ ] **Step 6: Check that `-PtestJdk` really changes the test JVM**

```bash
./gradlew :kommons-core:jvmTest -PosArchOnly=true -PtestJdk=21 --tests 'com.bkahlert.kommons.JvmPlatformTest' --info 2>&1 \
  | grep -oE "Starting process 'Gradle Test Executor [0-9]+'.*Command: [^ ]+" | head -1
```

Expected: a path containing `21` (for example `…/corretto-21.0.2/Contents/Home/bin/java` or a Foojay-provisioned `…/jdks/…-21…/bin/java`), not a JDK 17 path.

- [ ] **Step 7: Commit**

```bash
git add .github/workflows
git commit -m "ci: update actions and test matrix for gradle 9

gradle-build-action is archived; setup-gradle replaces it. The JDK
matrix becomes a test-JDK matrix (17 everywhere, 21 and 25 on Ubuntu)
because the build itself is pinned to a JDK 17 toolchain. Adds
linuxArm64 and macosArm64 jobs, drops the untestable macosX64 job, and
runs the Spring Boot sample's tests, which the old jvmTest-only
command skipped."
```

---

### Task 8: Pull request

**Files:** none modified.

- [ ] **Step 1: Push and open the PR**

```bash
git push -u origin build/gradle-9-kotlin-2-4
gh pr create --base master --title "build: upgrade build platform to gradle 9.7 and kotlin 2.4" --body-file - <<'PR'
Implements PR 1 of docs/superpowers/specs/2026-10-02-modernization-design.md.

## What changes
- Gradle 7.3 → 9.7.0, Kotlin 1.8.10 → 2.4.20, Dokka 1.7.10 → 2.2.0, nebula.release 16 → 21.
- JDK 17 toolchain for the build; bytecode stays Java 8 for libraries, 11 for kommons-uri, 17 for the Spring modules (`jvmBytecodeTarget`).
- Default hierarchy template for native source sets; linuxArm64 added.
- Repositories move to settings and shrink to Maven Central.
- Spring Boot 2.7.6 is consumed as a BOM; its Gradle plugin does not run on Gradle 9 and returns with 4.1 in PR 4. The sample loses `bootRun` until then.
- Configuration cache: <enabled | off because …>.
- CI: current actions, test-JDK matrix 17/21/25, linuxArm64 and macosArm64 jobs, sample tests now run.

## Bytecode check (Task 6)
<paste the table>

## Third-party deprecation warnings (Task 1 step 16)
<paste>

## Needs a decision
<list or "none">

BREAKING CHANGE: the Spring Boot modules (kommons-logging-logback,
kommons-logging-spring-boot, kommons-logging-spring-boot-starter) now
require Java 17; kommons-logging-spring-boot-sample no longer provides
bootRun until PR 4.
PR
```

- [ ] **Step 2: Wait for the matrix**

```bash
gh pr checks --watch
```

Expected: every matrix job green. A red job is fixed on the branch with a commit of the matching type; the PR is merged only when all ten jobs pass.

- [ ] **Step 3: Clean up the worktree after merge**

```bash
cd /Users/bkahlert/Development/com.bkahlert/kommons
git worktree remove ../kommons-pr1
git branch -d build/gradle-9-kotlin-2-4
```
