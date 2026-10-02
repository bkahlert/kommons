# Spring Boot and Logging Stack (PR 4) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the four Spring modules (`kommons-logging-logback`, `-spring-boot`, `-spring-boot-starter`, `-spring-boot-sample`) from Spring Boot 2.7.6 to Spring Boot 4.1.1 with Boot-managed Logback 1.5.38 and logstash-logback-encoder 9.0, adapt registration, binding and tests to Boot 4, and bring the three logging-stack test suites back into CI.

**Architecture:** One catalog and build-file task first, then one task per module in dependency order (logback, spring-boot, sample), each gated by its own test suite, then CI and the whole host suite. The Spring Boot BOM stays applied with `platform(...)` (never `enforcedPlatform`), so Gradle's highest-version rule keeps Kotlin at 2.4.20 and SLF4J at 2.0.20 while taking Logback 1.5.38, Jackson 3.1.5 and JUnit 6.0.3 from the BOM. kommons-test stays on JUnit 5.14.4; the Spring modules' suites running on JUnit 6 is accepted and doubles as a compatibility check. Logback 1.5 processes `<include>` and `<define>` in document order, so `base.xml` and the Logback XML resources need no change; the PR 3 CI exclusion of the three suites is lifted in the last task.

**Tech Stack:** Gradle 9.7.0, Kotlin 2.4.20 (kapt for the configuration processor), Spring Boot 4.1.1 (Spring Framework 7.0.9), Logback 1.5.38, logstash-logback-encoder 9.0 (Jackson 3, `tools.jackson`), JUnit 6.0.3 on the Spring modules' test classpaths, GitHub Actions.

**Spec:** [docs/superpowers/specs/2026-10-02-modernization-design.md](../specs/2026-10-02-modernization-design.md), section "PR 4: Spring Boot and logging stack", decisions 1, 11 and 12, and the "Target versions" table.

Deviations from the spec's PR 4 text, found while checking the Boot 4.1.1 jars (all verified on Maven Central on 2026-10-02):

- **`LoggingSystemProperties` has no String constants in Boot 4.** They moved to the enum `org.springframework.boot.logging.LoggingSystemProperty` (`environmentVariableName`). `Logback.clearSystemProperties()` reflected over the constants and would silently clear nothing; it iterates the enum now (Task 2). Tests that used the constants in annotations get a test-side holder with the literal names, pinned against the enum.
- **`MetadataStore` is package-private in Boot 4's configuration processor.** `ConfigurationMetadataIntegrationTest` reads `META-INF/spring-configuration-metadata.json` through the public `JsonMarshaller` instead and loses its fake `ProcessingEnvironment` (Task 3).
- **`ColorConverter` lives in a `@NullMarked` package.** `ConsolePretendingColorConverter.transform` overrides with non-null parameters (Task 2).
- **The sample gets the `org.springframework.boot` Gradle plugin back** at 4.1.1 (PR 1 removed it at 2.7 and deferred the restore to this PR); `spring-boot-starter-web` is a deprecated alias in Boot 4 and becomes `spring-boot-starter-webmvc`; `management.endpoint.shutdown.enabled` no longer exists and becomes `management.endpoint.shutdown.access: unrestricted` (Task 4). The spec's `-Xannotation-default-target=param-property` is not added: no constructor property in the four modules carries an annotation.
- **The spring-boot module's `TestConfig` and the sample rely on the new `AutoConfiguration.imports` file**, exactly as the spec says; in addition the `spring.factories` key for the environment post-processor changes to `org.springframework.boot.EnvironmentPostProcessor` (the old `org.springframework.boot.env` interface is deprecated for removal).
- **Two test expectations change with Boot 4's defaults:** the spring preset's timestamp is ISO-8601 with offset (`2026-10-02T21:00:00.000+02:00`), and `LoggingPresetTest` clears the MDC it fills, which PR 3 found leaking into `StructuredArgumentsTest` on Logback 1.3+.

## Global Constraints

- Versions (exact, verified on Maven Central on 2026-10-02): Spring Boot 4.1.1 (`org.springframework.boot:spring-boot-dependencies` manages Logback 1.5.38, SLF4J 2.0.18, JUnit 6.0.3 through `org.junit:junit-bom`, Jackson 3.1.5 through `tools.jackson:jackson-bom`, Kotlin 2.3.21, Spring Framework 7.0.9), logstash-logback-encoder 9.0 (not BOM-managed; needs Jackson 3, Logback 1.5, Java 17), catalog `logback-classic = "1.5.38"` (used directly only by the BOM-less test classpaths of kommons-logging-core and kommons-exec). Unchanged: SLF4J 2.0.20 (wins over the BOM's 2.0.18), Kotlin 2.4.20 (wins over the BOM's 2.3.21 because the BOM is a `platform`, never an `enforcedPlatform`), JUnit 5.14.4 in the catalog (kommons-test's declared API; the Spring modules resolve 6.0.3), everything PR 3 set.
- Java: the four Spring modules compile to Java 17 (class version 61) through `jvmBytecodeTarget(17)` (library modules) and `jvmToolchain(17)` (sample), as PR 1 left them. No other module changes its bytecode floor. Spring Boot 4.1 needs Java 17 and supports up to Java 26; the sample's tests therefore run on the `-PtestJdk` 21 and 25 matrix rows from this PR on.
- The build runs on JDK 17 through the wrapper script `$SCRATCH/gradlew17`. `./gradlew` below means that script. `--tests` applies only to the Gradle task named right before it.
- `./gradlew help --warning-mode all` prints zero deprecation lines; configuration cache stays on. No `by getting/creating`, no `project` at execution time.
- Local test commands: the logging stack alone is `:kommons-logging:kommons-logging-logback:jvmTest :kommons-logging:kommons-logging-spring-boot:jvmTest :kommons-logging:kommons-logging-spring-boot-sample:test -PosArchOnly=true`; the host suite after Task 5 is `allTests test -PosArchOnly=true -x jsBrowserTest` without the three `-x` exclusions PR 3 added. Baseline at the branch point (PR 3's forced rerun): 923 JVM, 515 Node, 447 macosArm64 tests, 0 failures, with the logging stack's 53 JVM tests excluded (logback 36, spring-boot 15, sample 2).
- Gradle may restore stale Kotlin/JS outputs from the local build cache (PR 3 saw corrupted lone-surrogate literals); a JS test failing with `?` or U+FFFD where a surrogate is expected means a stale cache entry: delete that module's `build/` and rerun with `--no-build-cache`.
- Commits follow Conventional Commits (`build:`, `fix(logging):`, `refactor(logging):`, `test(logging):`, `docs(logging):`, `ci:`), lowercase description, no AI attribution trailers. The catalog commit carries a `BREAKING CHANGE:` footer (Spring Boot 4 and Java 17 for consumers of the Spring modules).
- Worktree `.claude/worktrees/build-spring-boot-4`, branch `build/spring-boot-4` from `origin/master` `ddd148fe`. The main checkout holds the author's uncommitted work: never touch it. The Bash sandbox refuses compound commands that mention `git` or expand a command name from a variable; use plain commands and scratch scripts.
- Intermediate commits: Task 1 alone does not compile (type-level `@ConstructorBinding` is gone in Boot 4); the branch compiles from Task 4 on. CI runs on the final state.

## Review Focus

Inputs and conditions the spec implies but no task's tests exercise by default; each has a pinning check in the task named.

1. `Logback.clearSystemProperties()` must still clear every system property Boot 4 writes (`LOG_FILE`, `CONSOLE_LOG_STRUCTURED_FORMAT`, ...) and kommons' two preset properties, or tests and consumers that reset logging between runs keep stale values. Task 2 step 1 adds `LogbackSystemPropertiesTest.clear_system_properties`, and `names_match_spring_boot` pins the test-side literals against the enum.
2. The spring preset must render Boot 4's default console layout (ISO-8601 timestamp with offset, two spaces before the level, `--- [thread]`, padded logger). Task 2 step 6 updates `shouldMatchSpringPreset` in both modules' `matchers.kt`; `LoggingPresetTest` and `LoggingReConfiguringEnvironmentPostProcessorTest` run it.
3. A consumer setting `logging.file.name` or `logging.file.path` with the actuator on the classpath must get the kommons log file from `LogFileWebEndpoint`. `LogFileIntegrationTest` (all three `LogFileProvider`s) pins it in Task 3.
4. `LoggingProperties` must bind by constructor without `@ConstructorBinding`, including the nested `PresetProperties` defaults when nothing is configured. `LoggingPropertiesTest.undefined_properties` and `application_properties` pin it in Task 3.
5. The JSON preset must keep producing logstash JSON on Boot 4 (not Boot's own structured logging), with MDC entries and without `baz` for a null MDC value. `shouldMatchJsonPreset` in `LoggingPresetTest`, `LoggingReConfiguringEnvironmentPostProcessorTest` and the sample's `should log to log file` pin it in Tasks 2 to 4.
6. Configuration metadata with the KDoc descriptions must still be generated by Boot 4's processor through kapt. `ConfigurationMetadataIntegrationTest` pins it in Task 3.

## Preparation

- [ ] **Step 1: Worktree and wrapper**

The worktree exists at `.claude/worktrees/build-spring-boot-4` on branch `build/spring-boot-4` (base `ddd148fe ci: link the linuxarm64 test binaries on the linuxx64 job`). `$SCRATCH/gradlew17` points at it. Check:

```bash
git log --oneline -1
./gradlew help --warning-mode all 2>&1 | grep -ci deprecat
```

Expected: `ddd148fe …` and `0`.

- [ ] **Step 2: Commit this plan**

```bash
git add docs/superpowers/plans/2026-10-02-pr4-spring-boot.md
git commit -m "docs: add spring boot implementation plan"
```

---

### Task 1: Version catalog, test-JDK convention and build files

**Files:**
- Modify: `gradle/libs.versions.toml:10-11,19,65,74-76`
- Create: `buildSrc/src/main/kotlin/kommons-test-jdk-conventions.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/kommons-jvm-conventions.gradle.kts:1-25`
- Modify: `kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts:1-26`
- Modify: `kommons-logging/kommons-logging-spring-boot/build.gradle.kts:47-51`

**Interfaces:**
- Produces: catalog aliases `libs.plugins.spring.boot` (`org.springframework.boot` 4.1.1), `libs.spring.boot.bom` (unchanged alias, now 4.1.1), `libs.logstash.logback.encoder` 9.0, `libs.logback.classic` 1.5.38; the precompiled script plugin `kommons-test-jdk-conventions` that every `Test` task of the applying project picks `-PtestJdk` up from. Tasks 2 to 4 compile against these versions; Task 5 relies on the sample following `-PtestJdk`.

- [ ] **Step 1: Catalog versions and the Boot plugin alias**

In `gradle/libs.versions.toml` replace

```toml
logback-classic = "1.3.16"
logstash-logback-encoder = "7.2"
```

with

```toml
logback-classic = "1.5.38"
logstash-logback-encoder = "9.0"
```

and

```toml
spring-boot = "2.7.6" # https://docs.spring.io/spring-boot/docs/2.7.6/
```

with

```toml
spring-boot = "4.1.1" # https://docs.spring.io/spring-boot/4.1/
```

Delete the unused library line

```toml
spring-boot-gradle-plugin = { module = "org.springframework.boot:spring-boot-gradle-plugin", version.ref = "spring-boot" }
```

and add to `[plugins]`, after `kotlin-spring`:

```toml
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

- [ ] **Step 2: Move the test-JDK launcher into its own convention**

Create `buildSrc/src/main/kotlin/kommons-test-jdk-conventions.gradle.kts`:

```kotlin
// `-PtestJdk=21` runs the tests on that JDK instead of the build toolchain; CI uses it for 21 and 25.
// Shared by the multiplatform conventions and the plain-JVM Spring Boot sample.
providers.gradleProperty("testJdk").orNull?.let { version ->
    val launcher = the<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(version))
    }
    tasks.withType<Test>().configureEach {
        javaLauncher.set(launcher)
    }
}
```

In `buildSrc/src/main/kotlin/kommons-jvm-conventions.gradle.kts` replace the whole file with

```kotlin
import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kotlin-conventions")
    id("kommons-test-jdk-conventions")
}

kotlin {
    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}

jvmBytecodeTarget(8)
```

(the former lines 17-25 moved verbatim into the new plugin).

- [ ] **Step 3: Sample build file**

Replace `kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts` lines 1-26 (plugins, dependencies, kotlin block, test block) with

```kotlin
plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    id("kommons-test-jdk-conventions")
}

description = "Spring Boot sample application for Kommons Logging: Spring Boot"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":kommons-logging:kommons-logging-spring-boot-starter"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation(project(":kommons-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
```

The `tasks.processResources` block (banner expansion) stays as it is. `spring-boot-starter-web` is a deprecated alias in Boot 4; `-webmvc` is its replacement with the same contents (Tomcat, Jackson, MVC).

- [ ] **Step 4: Configuration-processor comment in the spring-boot module**

In `kommons-logging/kommons-logging-spring-boot/build.gradle.kts` replace

```kotlin
    // see https://docs.spring.io/spring-boot/docs/2.7.3/reference/html/configuration-metadata.html
```

with

```kotlin
    // see https://docs.spring.io/spring-boot/4.1/specification/configuration-metadata/annotation-processor.html
```

Nothing else changes in that file: kapt with `spring-boot-configuration-processor` is the documented way for Kotlin on Boot 4 too, and the test dependencies (`spring-boot-starter-actuator`, `spring-boot-starter-test`, `spring-boot-configuration-processor`) keep their names.

- [ ] **Step 5: Configuration and resolution gate**

```bash
./gradlew help --warning-mode all 2>&1 | grep -ciE "deprecat"
./gradlew :kommons-logging:kommons-logging-spring-boot-sample:dependencies --configuration testRuntimeClasspath -PosArchOnly=true 2>&1 | grep -oE "(org.springframework.boot:spring-boot:|ch.qos.logback:logback-classic:|net.logstash.logback:logstash-logback-encoder:|org.junit.jupiter:junit-jupiter-api:|tools.jackson.core:jackson-core:|org.jetbrains.kotlin:kotlin-stdlib:|org.slf4j:slf4j-api:)[^ ]*( -> [^ ]*)?" | sort -u
```

Expected: `0`; and one resolved line each ending in `spring-boot:4.1.1`, `logback-classic:1.5.38`, `logstash-logback-encoder:9.0`, `junit-jupiter-api:… -> 6.0.3`, `jackson-core:… -> 3.1.5`, `kotlin-stdlib:… -> 2.4.20` (or `:2.4.20`), `slf4j-api:… -> 2.0.20`. A `kotlin-stdlib` line ending in `2.3.21` means the BOM was applied as `enforcedPlatform` somewhere: stop and report.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml buildSrc/src/main/kotlin/kommons-test-jdk-conventions.gradle.kts buildSrc/src/main/kotlin/kommons-jvm-conventions.gradle.kts kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts kommons-logging/kommons-logging-spring-boot/build.gradle.kts
git commit -m "build: upgrade the spring stack to spring boot 4.1" -m "Spring Boot 4.1.1 with its managed Logback 1.5.38, Jackson 3.1.5 and
JUnit 6.0.3, logstash-logback-encoder 9.0 (Jackson 3, Logback 1.5,
Java 17). The sample gets the Spring Boot Gradle plugin back (PR 1
removed it at 2.7) and the web starter's Boot 4 name. The -PtestJdk
launcher wiring moves into kommons-test-jdk-conventions so the sample's
test task follows the JDK matrix like the multiplatform modules.

The catalog's logback-classic serves only the BOM-less test classpaths
of kommons-logging-core and kommons-exec and follows the BOM." -m "BREAKING CHANGE: kommons-logging-logback, kommons-logging-spring-boot
and kommons-logging-spring-boot-starter require Spring Boot 4 and
Java 17; the JSON preset depends on logstash-logback-encoder 9 on
Jackson 3 (tools.jackson) and Logback 1.5."
```

---

### Task 2: kommons-logging-logback on Boot 4, Logback 1.5 and logstash 9

**Files:**
- Create: `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/SpringLoggingSystemProperties.kt`
- Create: `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/logging/logback/LogbackSystemPropertiesTest.kt`
- Modify: `kommons-logging/kommons-logging-logback/src/jvmMain/kotlin/com/bkahlert/kommons/logging/logback/Logback.kt:27,106-118` (and the unused imports the change leaves)
- Modify: `kommons-logging/kommons-logging-logback/src/jvmMain/kotlin/com/bkahlert/kommons/logging/logback/ConsolePretendingColorConverter.kt:17-20`
- Modify: `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/logging/logback/LoggingPresetTest.kt:27-36,44-49`
- Modify: `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/logback/LogbackConfigurationExtension.kt:19,57`
- Modify: `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/logging/logback/StructuredArgumentsTest.kt:9,199-200`
- Modify: `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/matchers.kt:74-78`
- Modify: `kommons-logging/kommons-logging-spring-boot/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/matchers.kt:74-78` (byte-identical copy; same change)

**Interfaces:**
- Consumes: Task 1's versions.
- Produces: `com.bkahlert.kommons.test.logging.SpringLoggingSystemProperties` (test-only `object` with `const val LOG_FILE`, `LOG_LEVEL_PATTERN`, `LOG_DATEFORMAT_PATTERN`, `EXCEPTION_CONVERSION_WORD`), used by this module's tests only; `Logback.clearSystemProperties()` keeps its signature. The updated `shouldMatchSpringPreset` regex, which Task 3's tests run through the spring-boot module's copy of `matchers.kt`.

- [ ] **Step 1: Write the failing tests for the system-property clearing**

Create `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/SpringLoggingSystemProperties.kt`:

```kotlin
package com.bkahlert.kommons.test.logging

/**
 * Names of the system properties Spring Boot's `LoggingSystemProperty` writes.
 * Boot 4 has no String constants for them, and annotation arguments need constants.
 */
object SpringLoggingSystemProperties {
    const val LOG_FILE: String = "LOG_FILE"
    const val LOG_LEVEL_PATTERN: String = "LOG_LEVEL_PATTERN"
    const val LOG_DATEFORMAT_PATTERN: String = "LOG_DATEFORMAT_PATTERN"
    const val EXCEPTION_CONVERSION_WORD: String = "LOG_EXCEPTION_CONVERSION_WORD"
}
```

Create `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/logging/logback/LogbackSystemPropertiesTest.kt`:

```kotlin
package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.test.logging.SpringLoggingSystemProperties
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.springframework.boot.logging.LoggingSystemProperty

@Isolated
class LogbackSystemPropertiesTest {

    @Test fun clear_system_properties() {
        val names = LoggingSystemProperty.entries.map { it.environmentVariableName } +
            listOf(LoggingSystemProperties.CONSOLE_LOG_PRESET, LoggingSystemProperties.FILE_LOG_PRESET)
        names.forEach { System.setProperty(it, "set-by-test") }

        Logback.clearSystemProperties()

        names.forEach { System.getProperty(it).shouldBeNull() }
    }

    @Test fun names_match_spring_boot() {
        SpringLoggingSystemProperties.LOG_FILE shouldBe LoggingSystemProperty.LOG_FILE.environmentVariableName
        SpringLoggingSystemProperties.LOG_LEVEL_PATTERN shouldBe LoggingSystemProperty.LEVEL_PATTERN.environmentVariableName
        SpringLoggingSystemProperties.LOG_DATEFORMAT_PATTERN shouldBe LoggingSystemProperty.DATEFORMAT_PATTERN.environmentVariableName
        SpringLoggingSystemProperties.EXCEPTION_CONVERSION_WORD shouldBe LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.environmentVariableName
    }
}
```

- [ ] **Step 2: Compile the main source set, record the Boot 4 errors**

```bash
./gradlew :kommons-logging:kommons-logging-logback:compileKotlinJvm -PosArchOnly=true 2>&1 | grep -E "^e:|BUILD"
```

Expected: `BUILD FAILED` with `e:` lines in `Logback.kt` (`LoggingSystemProperties` has no accessible constants; the alias import no longer resolves to anything usable, or the reflection compiles but clears nothing) and in `ConsolePretendingColorConverter.kt` (`transform` overrides nothing: the supertype's parameters are non-null under `@NullMarked`). If the main source set compiles cleanly, go on anyway: step 4 is the behaviour gate.

- [ ] **Step 3: Production changes**

In `ConsolePretendingColorConverter.kt` replace

```kotlin
    protected override fun transform(event: ILoggingEvent?, `in`: String?): String {
        AnsiOutput.setConsoleAvailable(true)
        return super.transform(event, `in`)
    }
```

with

```kotlin
    protected override fun transform(event: ILoggingEvent, `in`: String): String {
        AnsiOutput.setConsoleAvailable(true)
        return super.transform(event, `in`)
    }
```

In `Logback.kt` replace the import

```kotlin
import org.springframework.boot.logging.LoggingSystemProperties as SpringLoggingSystemProperties
```

with

```kotlin
import org.springframework.boot.logging.LoggingSystemProperty
```

and replace

```kotlin
    /** Clears all system properties that can be used to configure logging. */
    public fun clearSystemProperties() {
        arrayOf(
            SpringLoggingSystemProperties::class,
            LoggingSystemProperties::class
        ).flatMap {
            it.java.declaredFields.filter { field ->
                Modifier.isPublic(field.modifiers) && Modifier.isStatic(field.modifiers) && field.type == String::class.java
            }
        }.forEach {
            System.clearProperty(it.get(null) as String)
        }
    }
```

with

```kotlin
    /** Clears all system properties that can be used to configure logging: Spring Boot's and kommons' preset properties. */
    public fun clearSystemProperties() {
        LoggingSystemProperty.entries.forEach { System.clearProperty(it.environmentVariableName) }
        System.clearProperty(LoggingSystemProperties.CONSOLE_LOG_PRESET)
        System.clearProperty(LoggingSystemProperties.FILE_LOG_PRESET)
    }
```

Remove the imports this leaves unused (`java.lang.reflect.Modifier`; keep `com.bkahlert.kommons.logging.LoggingSystemProperties`, which the new body uses). Compile:

```bash
./gradlew :kommons-logging:kommons-logging-logback:compileKotlinJvm -PosArchOnly=true 2>&1 | grep -E "^e:|^w:|BUILD"
```

Expected: `BUILD SUCCESSFUL`, no `e:`; a `w:` about an unused import means an import was missed.

- [ ] **Step 4: Adapt the tests to Boot 4 and logstash 9**

`LoggingPresetTest.kt`: replace the four imports

```kotlin
import org.springframework.boot.logging.LoggingSystemProperties.EXCEPTION_CONVERSION_WORD
import org.springframework.boot.logging.LoggingSystemProperties.LOG_DATEFORMAT_PATTERN
import org.springframework.boot.logging.LoggingSystemProperties.LOG_FILE
import org.springframework.boot.logging.LoggingSystemProperties.LOG_LEVEL_PATTERN
```

with

```kotlin
import com.bkahlert.kommons.test.logging.SpringLoggingSystemProperties.EXCEPTION_CONVERSION_WORD
import com.bkahlert.kommons.test.logging.SpringLoggingSystemProperties.LOG_DATEFORMAT_PATTERN
import com.bkahlert.kommons.test.logging.SpringLoggingSystemProperties.LOG_FILE
import com.bkahlert.kommons.test.logging.SpringLoggingSystemProperties.LOG_LEVEL_PATTERN
```

(keep the import block sorted: the `com.bkahlert.kommons.test.logging.*` imports sit with the other `com.bkahlert.kommons.test.logging` imports), add `import org.junit.jupiter.api.AfterEach`, and replace

```kotlin
    @BeforeEach
    fun setUp() {
        Logback.reset()
        MDC.put("foo", "bar")
        MDC.put("baz", null)
    }
```

with

```kotlin
    @BeforeEach
    fun setUp() {
        Logback.reset()
        MDC.put("foo", "bar")
        MDC.put("baz", null)
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }
```

`LogbackConfigurationExtension.kt`: replace `import org.springframework.boot.logging.LoggingSystemProperties` with `import com.bkahlert.kommons.test.logging.SpringLoggingSystemProperties` (sorted into the `com.bkahlert.kommons` block) and line 57's `LoggingSystemProperties.LOG_FILE` with `SpringLoggingSystemProperties.LOG_FILE`.

`StructuredArgumentsTest.kt`: replace `import com.fasterxml.jackson.core.JsonGenerator` with `import tools.jackson.core.JsonGenerator` (sorted: after the `org.*` imports) and

```kotlin
                override fun writeTo(generator: JsonGenerator) {
                    generator.writeNumberField("key", i++)
                }
```

with

```kotlin
                override fun writeTo(generator: JsonGenerator) {
                    generator.writeNumberProperty("key", i++)
                }
```

(logstash 9's `StructuredArgument.writeTo` takes `tools.jackson.core.JsonGenerator` and declares no checked exception; Jackson 3 renamed `writeNumberField` to `writeNumberProperty`.)

- [ ] **Step 5: Run the module's tests**

```bash
./gradlew :kommons-logging:kommons-logging-logback:jvmTest -PosArchOnly=true --rerun 2>&1 | grep -E "FAILED|tests completed|BUILD"
```

Expected: failures only in `LoggingPresetTest` cases that use the spring preset (`shouldMatchSpringPreset`: the timestamp now reads `2026-10-02T21:00:00.000+02:00`), and `LogbackSystemPropertiesTest` green. Any other failure is triaged per Task 5 step 4's rules before going on.

- [ ] **Step 6: Boot 4's console layout in the spring-preset matcher (both copies)**

In both `kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/matchers.kt` and `kommons-logging/kommons-logging-spring-boot/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/matchers.kt` replace

```kotlin
fun PrintedLogEntry.shouldMatchSpringPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}.\d{3} {2}INFO +\d* +--- \[.*] TestLogger + : $message with value
    """.trimIndent()
)
```

with

```kotlin
fun PrintedLogEntry.shouldMatchSpringPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}(?:[+-]\d{2}:\d{2}|Z) {2}INFO +\d* +--- \[.*] TestLogger + : $message with value
    """.trimIndent()
)
```

Boot 4's default `LOG_DATEFORMAT_PATTERN` is `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`; the rest of the default `CONSOLE_LOG_PATTERN` (`%5p`, PID, `--- `, application name and group in square brackets when set, `[%15.15t]`, correlation id, `%-40.40logger{39} : `) still matches the unchanged tail of the regex because the test processes set no application name. `shouldMatchCustomSpringPreset` sets its own date pattern and stays.

- [ ] **Step 7: Rerun the module's tests and the two BOM-less Logback consumers**

```bash
./gradlew :kommons-logging:kommons-logging-logback:jvmTest -PosArchOnly=true --rerun 2>&1 | grep -E "FAILED|tests completed|BUILD"
./gradlew :kommons-logging:kommons-logging-core:jvmTest :kommons-exec:jvmTest -PosArchOnly=true --rerun 2>&1 | grep -E "FAILED|tests completed|BUILD"
```

Expected: `BUILD SUCCESSFUL` twice, no `FAILED` lines. The JUnit XML under `kommons-logging/kommons-logging-logback/build/test-results/jvmTest/` sums to 38 tests (36 before plus the two new ones), `failures="0"`. The second command proves Logback 1.5.38 binds on the catalog-only test classpaths.

- [ ] **Step 8: Commits**

```bash
git add kommons-logging/kommons-logging-logback/src/jvmMain/kotlin/com/bkahlert/kommons/logging/logback/Logback.kt kommons-logging/kommons-logging-logback/src/jvmMain/kotlin/com/bkahlert/kommons/logging/logback/ConsolePretendingColorConverter.kt kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/logging/logback/LogbackSystemPropertiesTest.kt kommons-logging/kommons-logging-logback/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/SpringLoggingSystemProperties.kt
git commit -m "fix(logging): clear spring boot 4's logging system properties" -m "Spring Boot 4 has no String constants in LoggingSystemProperties; the
names live in the LoggingSystemProperty enum. Logback.clearSystemProperties
iterates the enum instead of reflecting over constants that no longer
exist, and clears kommons' two preset properties explicitly.
ConsolePretendingColorConverter.transform overrides with non-null
parameters: ColorConverter's package is @NullMarked.

Failing test: LogbackSystemPropertiesTest.clear_system_properties."
git add kommons-logging/kommons-logging-logback/src/jvmTest kommons-logging/kommons-logging-spring-boot/src/jvmTest/kotlin/com/bkahlert/kommons/test/logging/matchers.kt
git commit -m "test(logging): adapt the logback tests to spring boot 4 and logstash 9" -m "- shouldMatchSpringPreset expects Boot 4's ISO-8601 timestamp with
  offset (both copies of matchers.kt).
- StructuredArgumentsTest implements logstash 9's writeTo with Jackson 3's
  tools.jackson.core.JsonGenerator and writeNumberProperty.
- LoggingPresetTest and LogbackConfigurationExtension take the Boot
  system property names from a test-side holder; Boot 4 has no constants.
- LoggingPresetTest clears the MDC it fills, which leaked into
  StructuredArgumentsTest on Logback 1.3+."
```

---

### Task 3: kommons-logging-spring-boot on Boot 4

**Files:**
- Modify: `kommons-logging/kommons-logging-spring-boot/src/jvmMain/kotlin/com/bkahlert/kommons/logging/spring/LoggingConfiguration.kt`
- Create: `kommons-logging/kommons-logging-spring-boot/src/jvmMain/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `kommons-logging/kommons-logging-spring-boot/src/jvmMain/resources/META-INF/spring.factories`
- Modify: `kommons-logging/kommons-logging-spring-boot/src/jvmMain/kotlin/com/bkahlert/kommons/logging/spring/LoggingReConfiguringEnvironmentPostProcessor.kt:10`
- Modify: `kommons-logging/kommons-logging-spring-boot/src/jvmMain/kotlin/com/bkahlert/kommons/logging/spring/LoggingProperties.kt:10,16`
- Modify: `kommons-logging/kommons-logging-spring-boot/src/jvmTest/kotlin/com/bkahlert/kommons/logging/spring/ConfigurationMetadataIntegrationTest.kt`
- Modify: `kommons-logging/kommons-logging-spring-boot/src/jvmTest/kotlin/com/bkahlert/kommons/logging/spring/LoggingReConfiguringEnvironmentPostProcessorTest.kt:49-53`
- Modify: `kommons-logging/kommons-logging-spring-boot/README.md:45`

**Interfaces:**
- Consumes: Task 1's versions, Task 2's `matchers.kt` change in this module's test source set.
- Produces: the auto-configuration `com.bkahlert.kommons.logging.spring.LoggingConfiguration` registered through `AutoConfiguration.imports`, which the sample (Task 4) and every consumer discover; the environment post-processor registered under `org.springframework.boot.EnvironmentPostProcessor`.

- [ ] **Step 1: Registration files**

Create `kommons-logging/kommons-logging-spring-boot/src/jvmMain/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` with the single line

```
com.bkahlert.kommons.logging.spring.LoggingConfiguration
```

Replace the contents of `kommons-logging/kommons-logging-spring-boot/src/jvmMain/resources/META-INF/spring.factories` with

```
org.springframework.boot.EnvironmentPostProcessor=\
com.bkahlert.kommons.logging.spring.LoggingReConfiguringEnvironmentPostProcessor
```

(Boot 3+ ignores the `EnableAutoConfiguration` key; environment post-processors stay in `spring.factories`, under the interface's new fully qualified name.)

- [ ] **Step 2: Production code**

`LoggingConfiguration.kt`: replace the whole file with

```kotlin
package com.bkahlert.kommons.logging.spring

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Auto-configuration of the Logback logging framework using presets.
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties::class)
public open class LoggingConfiguration
```

(`@AutoConfiguration` is `proxyBeanMethods = false` by default.)

`LoggingReConfiguringEnvironmentPostProcessor.kt`: replace `import org.springframework.boot.env.EnvironmentPostProcessor` with `import org.springframework.boot.EnvironmentPostProcessor`, placed before `import org.springframework.boot.SpringApplication` (sorted import block). The KDoc at lines 19-25 keeps saying it is registered via `spring.factories`, which stays true.

`LoggingProperties.kt`: delete the import `import org.springframework.boot.context.properties.ConstructorBinding` and the annotation line `@ConstructorBinding`. Boot 4 binds a single-constructor Kotlin data class by constructor without it (and the annotation is constructor-only now).

- [ ] **Step 3: Compile**

```bash
./gradlew :kommons-logging:kommons-logging-spring-boot:compileKotlinJvm -PosArchOnly=true 2>&1 | grep -E "^e:|^w:|BUILD"
```

Expected: `BUILD SUCCESSFUL`, no `e:` lines, no unused-import `w:` lines; kapt runs the Boot 4 configuration processor (a `kaptKotlinJvm` task in the output).

- [ ] **Step 4: Read the configuration metadata through the public marshaller**

`ConfigurationMetadataIntegrationTest.kt` keeps its two tests and the two `ItemMetadata` extension properties (`kClass`, `sourceKClass`) unchanged. Replace the companion object

```kotlin
    companion object {
        val configuredProperties: List<ItemMetadata>
            get() = MetadataStore(FileReadOnlyProcessingEnvironment()).readMetadata().items
    }
```

with

```kotlin
    companion object {
        private const val METADATA_PATH = "META-INF/spring-configuration-metadata.json"

        val configuredProperties: List<ItemMetadata>
            get() = checkNotNull(Program.contextClassLoader.getResourceAsStream(METADATA_PATH)) { "$METADATA_PATH not found on the test classpath" }
                .use { JsonMarshaller().read(it).items }
    }
```

Delete the classes `FileReadOnlyProcessingEnvironment`, `ReadOnlyFiler` and `ReadOnlyFileObject` (everything after `sourceKClass`) and the imports they alone used: `org.springframework.boot.configurationprocessor.MetadataStore`, `java.io.InputStream`, `java.io.OutputStream`, `java.io.Reader`, `java.io.Writer`, `java.net.URI`, `java.net.URL`, `java.util.Locale`, `javax.annotation.processing.Filer`, `javax.annotation.processing.Messager`, `javax.annotation.processing.ProcessingEnvironment`, `javax.lang.model.SourceVersion`, `javax.lang.model.element.Element`, `javax.lang.model.util.Elements`, `javax.lang.model.util.Types`, `javax.tools.FileObject`, `javax.tools.JavaFileManager.Location`, `javax.tools.JavaFileObject`, `javax.tools.StandardLocation.CLASS_OUTPUT`. Add `import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller` (sorted after `ItemMetadata`). `MetadataStore` is package-private in Boot 4's processor; `JsonMarshaller.read(InputStream)` is the public reader of the same JSON and returns the same `ItemMetadata` items.

- [ ] **Step 5: MDC hygiene in the post-processor test**

In `LoggingReConfiguringEnvironmentPostProcessorTest.kt` replace

```kotlin
    @AfterEach
    fun tearDown() {
        Logback.clearSystemProperties()
        Logback.reset()
    }
```

with

```kotlin
    @AfterEach
    fun tearDown() {
        MDC.clear()
        Logback.clearSystemProperties()
        Logback.reset()
    }
```

(`org.slf4j.MDC` is already imported at line 31.)

- [ ] **Step 6: Run the module's tests**

```bash
./gradlew :kommons-logging:kommons-logging-spring-boot:jvmTest -PosArchOnly=true --rerun 2>&1 | grep -E "FAILED|tests completed|BUILD"
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' kommons-logging/kommons-logging-spring-boot/build/test-results/jvmTest/*.xml | awk -F'"' '{t+=$2; f+=$6; e+=$8} END {print "tests="t" failures="f" errors="e}'
```

Expected: `BUILD SUCCESSFUL`, `tests=15 failures=0 errors=0`: `LoggingPropertiesTest` (constructor binding without the annotation), `LogFileIntegrationTest` (all three providers including `LogFileWebEndpoint`, which Boot 4 still creates with the actuator starter, `management.endpoints.web.exposure.include=*` and a log-file property, no web server needed), `LoggingReConfiguringEnvironmentPostProcessorTest` (spring and JSON presets through the new `spring.factories` key), `ConfigurationMetadataIntegrationTest` (descriptions "Preset", "CONSOLE log"/"FILE log" from the Boot 4 processor). A `LogFileIntegrationTest` timeout at the 10 s default is a `@Slow` annotation on the class (`com.bkahlert.kommons.test.Slow`), not a change to the test body.

- [ ] **Step 7: README property names**

In `kommons-logging/kommons-logging-spring-boot/README.md` replace

```markdown
Spring Boot's logging configuration options like `file.path` and `logback.rollingpolicy.max-file-size` are still supported.
```

with

```markdown
Spring Boot's logging configuration options like `logging.file.path` and `logging.logback.rollingpolicy.max-file-size` are still supported.
```

- [ ] **Step 8: Commits**

```bash
git add kommons-logging/kommons-logging-spring-boot/src/jvmMain
git commit -m "fix(logging): register the auto-configuration the spring boot 3 way" -m "LoggingConfiguration is an @AutoConfiguration listed in
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports;
Boot 3+ no longer reads the EnableAutoConfiguration key of
spring.factories. The environment post-processor implements
org.springframework.boot.EnvironmentPostProcessor (the org.springframework.boot.env
interface is deprecated for removal) and is registered under that key.
LoggingProperties drops the type-level @ConstructorBinding, which Boot 4
allows on constructors only; a single-constructor data class binds by
constructor without it."
git add kommons-logging/kommons-logging-spring-boot/src/jvmTest
git commit -m "test(logging): read the configuration metadata through json marshaller" -m "Boot 4's MetadataStore is package-private; JsonMarshaller is the public
reader of META-INF/spring-configuration-metadata.json and yields the
same ItemMetadata. The fake ProcessingEnvironment, Filer and FileObject
go. LoggingReConfiguringEnvironmentPostProcessorTest clears the MDC it
fills."
git add kommons-logging/kommons-logging-spring-boot/README.md
git commit -m "docs(logging): name the spring boot logging properties in full"
```

---

### Task 4: Sample on Boot 4

**Files:**
- Modify: `kommons-logging/kommons-logging-spring-boot-sample/src/main/kotlin/com/bkahlert/kommons/logging/sample/helloworld/HelloWorldConfiguration.kt:4,20`
- Modify: `kommons-logging/kommons-logging-spring-boot-sample/src/main/resources/application.yml:9-10`
- Modify: `kommons-logging/kommons-logging-spring-boot-sample/README.md:24,33`

**Interfaces:**
- Consumes: Task 1's sample build file (Boot plugin, `-webmvc` starter, test-JDK convention), Task 3's auto-configuration registration (the sample's `@EnableAutoConfiguration` finds `LoggingConfiguration` through the imports file).
- Produces: the `bootJar` and `bootRun` tasks of `:kommons-logging:kommons-logging-spring-boot-sample`, which the README documents.

- [ ] **Step 1: Constructor binding and the actuator property**

`HelloWorldConfiguration.kt`: delete the import `import org.springframework.boot.context.properties.ConstructorBinding` and the annotation line `@ConstructorBinding` above `@ConfigurationProperties("hello-world")`.

`application.yml`: replace

```yaml
  endpoint:
    shutdown.enabled: true
```

with

```yaml
  endpoint:
    shutdown.access: unrestricted
```

(`management.endpoint.<id>.enabled` is gone from Boot 4's metadata; `access` takes `none`, `read_only` or `unrestricted`, and setting both fails at startup.)

- [ ] **Step 2: Compile, test on the build JDK and on JDK 25, build the boot jar**

```bash
./gradlew :kommons-logging:kommons-logging-spring-boot-sample:test -PosArchOnly=true --rerun 2>&1 | grep -E "FAILED|tests completed|BUILD"
./gradlew :kommons-logging:kommons-logging-spring-boot-sample:test -PosArchOnly=true -PtestJdk=25 --rerun 2>&1 | grep -E "FAILED|tests completed|BUILD|Provisioning|toolchain"
./gradlew :kommons-logging:kommons-logging-spring-boot-sample:bootJar -PosArchOnly=true 2>&1 | grep -E "^e:|BUILD"
ls kommons-logging/kommons-logging-spring-boot-sample/build/libs/
```

Expected: `BUILD SUCCESSFUL` three times, no `FAILED`; the second run provisions or uses a JDK 25 toolchain (Foojay), proving `kommons-test-jdk-conventions` reaches the sample's `test` task; the jar listing shows `kommons-logging-spring-boot-sample-<version>.jar` (boot jar) and `…-plain.jar`. The JUnit XML under `kommons-logging/kommons-logging-spring-boot-sample/build/test-results/test/` sums to 2 tests, 0 failures (`SampleSpringBootTest`: minimal console, JSON file; `SampleUnitTest`).

- [ ] **Step 3: README**

In `kommons-logging/kommons-logging-spring-boot-sample/README.md` the `bootRun` line (line 24) is true again and stays. Replace

```markdown
2. Uncomment the commented lines in the dependency block of [build.gradle.kts](build.gradle.kts) and remove the project dependencies.
```

with

```markdown
2. In [build.gradle.kts](build.gradle.kts) replace the project dependencies with `implementation("com.bkahlert.kommons:kommons-logging-spring-boot-starter:<version>")` and `testImplementation("com.bkahlert.kommons:kommons-test:<version>")`.
```

- [ ] **Step 4: Commits**

```bash
git add kommons-logging/kommons-logging-spring-boot-sample/src
git commit -m "fix(logging): adapt the sample to spring boot 4" -m "HelloWorldConfigurationProperties drops the type-level
@ConstructorBinding (constructor-only in Boot 4). The shutdown endpoint
is enabled through management.endpoint.shutdown.access, which replaced
the removed .enabled property."
git add kommons-logging/kommons-logging-spring-boot-sample/README.md
git commit -m "docs(logging): describe how to start from the sample" -m "The build file has no commented dependency lines; name the artifacts
to depend on instead."
```

---

### Task 5: CI, docs and the whole host suite

**Files:**
- Modify: `.github/workflows/build.yml:63-73`
- Modify: `kommons-logging/kommons-logging-logback/README.md:49-51`

**Interfaces:**
- Consumes: everything from Tasks 1 to 4.

- [ ] **Step 1: Lift the PR 3 exclusions**

In `.github/workflows/build.yml` delete the three comment lines above the test step

```yaml
      # The logging stack's suites are excluded until the Spring Boot upgrade: Spring Boot 2.7 has no SLF4J 2
      # support (it needs org.slf4j.impl.StaticLoggerBinder), and Logback 1.3 resolves <include> at parse time,
      # before the <define> elements that compute kommons' appender configuration paths have run.
```

and the three lines inside the run command

```yaml
          -x :kommons-logging:kommons-logging-logback:jvmTest
          -x :kommons-logging:kommons-logging-spring-boot:jvmTest
          -x :kommons-logging:kommons-logging-spring-boot-sample:test
```

so the step reads

```yaml
      - name: Test ${{ matrix.target }}${{ matrix.test-jdk && format(' on JDK {0}', matrix.test-jdk) || '' }}
        shell: bash
        run: >
          ./gradlew ${{ matrix.target }}Test
          ${{ matrix.target == 'jvm' && 'test' || '' }}
          ${{ matrix.extra-tasks || '' }}
          ${{ matrix.test-jdk && format('-PtestJdk={0}', matrix.test-jdk) || '' }}
          -Pkotlin.tests.individualTaskReports=true
          ${{ github.event.inputs.additional-gradle-args }}
```

Then:

```bash
actionlint .github/workflows/build.yml && echo "actionlint: no findings"
```

Expected: `actionlint: no findings`.

- [ ] **Step 2: Spring preset example in the logback README**

In `kommons-logging/kommons-logging-logback/README.md` replace the example block

```log
2022-08-29 00:08:52.917  INFO   --- [ool-1-worker-18] TestLogger                               : message
```

with

```log
2026-10-02T21:00:00.000+02:00  INFO 12345 --- [           main] TestLogger                               : message
```

(Boot 4's default layout: ISO-8601 timestamp with offset, level, PID, thread, logger.)

- [ ] **Step 3: Run the whole host suite**

```bash
./gradlew allTests test -PosArchOnly=true -x jsBrowserTest --continue > "$SCRATCH/pr4-tests.log" 2>&1; echo "exit=$?"
grep -E "FAILED|BUILD" "$SCRATCH/pr4-tests.log" | head -40
find . -path '*/build/test-results/jvmTest/*.xml' -o -path '*/build/test-results/test/*.xml' | xargs grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' | awk -F'"' '{t+=$2; s+=$4; f+=$6; e+=$8} END {print "tests="t" skipped="s" failures="f" errors="e}'
grep -c "No SLF4J providers were found\|StaticLoggerBinder" "$SCRATCH/pr4-tests.log"
./gradlew help --warning-mode all 2>&1 | grep -ciE "deprecat"
```

Expected: `BUILD SUCCESSFUL`, no `FAILED` lines; JVM `tests=` at least 978 (923 plus the logging stack's 53 plus the two new tests), `failures=0 errors=0`; `0` SLF4J binding problems; `0` deprecation lines. Node and macosArm64 results are unchanged from PR 3 (515 and 447 tests) since no multiplatform code changes here.

- [ ] **Step 4: Triage**

For each failing test decide, in this order:

1. The test asserted Boot 2.7, Logback 1.3 or logstash 7 behaviour (layout, property names, Jackson API): adjust the expectation and say so in the commit body.
2. Production behaviour changed with the library and the test is right: fix the production code so the existing test passes.
3. A Spring context test hits the 10 s default timeout on this machine: annotate the class `@Slow`; never raise the global timeout.
4. A JUnit 6 incompatibility in kommons-test surfaces only through the Spring modules (the one place JUnit 6 is on the classpath): fix kommons-test so it works on both lines, with a `fix(test):` commit naming the failing test.

Anything that does not fit goes into the PR description under "Needs a decision".

- [ ] **Step 5: Commits**

```bash
git add .github/workflows/build.yml
git commit -m "ci: run the logging stack tests again" -m "Spring Boot 4 runs on SLF4J 2 and Logback 1.5 resolves <include> in
document order, so the three suites PR 3 excluded pass again."
git add kommons-logging/kommons-logging-logback/README.md
git commit -m "docs(logging): show spring boot 4's console layout"
```

Triage changes get a `test(logging):` or `fix(scope):` commit each, with the failing test named in the body.

---

### Task 6: Pull request

**Files:** none modified.

- [ ] **Step 1: Final local check**

```bash
./gradlew help --warning-mode all 2>&1 | grep -ci "deprecat"
git log --oneline origin/master..HEAD
```

Expected: `0`; commits for the plan, the catalog, the logback fix and tests, the spring-boot fix, tests and docs, the sample fix and docs, CI and the README.

- [ ] **Step 2: Push and open the PR**

```bash
git push -u origin build/spring-boot-4
gh pr create --base master --title "build: upgrade the spring stack to spring boot 4.1" --body-file - <<'PR'
Implements PR 4 of docs/superpowers/specs/2026-10-02-modernization-design.md.

## What changes
- Spring Boot 4.1.1 with its managed Logback 1.5.38, Jackson 3.1.5 and JUnit 6.0.3; logstash-logback-encoder 9.0 (Jackson 3 `tools.jackson`, Logback 1.5, Java 17). The catalog's `logback-classic` follows the BOM for the two BOM-less test classpaths.
- kommons-logging-spring-boot: `LoggingConfiguration` is an `@AutoConfiguration` registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`; the environment post-processor implements `org.springframework.boot.EnvironmentPostProcessor`; `LoggingProperties` binds by constructor without the type-level `@ConstructorBinding`.
- kommons-logging-logback: `Logback.clearSystemProperties()` iterates Boot 4's `LoggingSystemProperty` enum (the String constants are gone); `ConsolePretendingColorConverter.transform` overrides with non-null parameters (JSpecify `@NullMarked`).
- Sample: the `org.springframework.boot` Gradle plugin is back at 4.1.1 (`bootRun`, `bootJar`), `spring-boot-starter-webmvc` replaces the deprecated `-web` alias, `management.endpoint.shutdown.access: unrestricted` replaces the removed `.enabled`; its `test` task follows `-PtestJdk` through the new `kommons-test-jdk-conventions`, so Boot 4.1 runs on JDK 21 and 25 in CI.
- CI runs the three logging-stack suites again (PR 3's exclusions lifted).

## Verified locally (Apple silicon, JDK 17 build)
- `allTests test -PosArchOnly=true -x jsBrowserTest`: <N> JVM tests / 0 failures (PR 3: 923 with the logging stack excluded), Node and macosArm64 unchanged.
- Sample tests on JDK 17 and `-PtestJdk=25`; `bootJar` builds.
- Resolved on the Spring modules' test classpaths: spring-boot 4.1.1, logback-classic 1.5.38, logstash-logback-encoder 9.0, junit-jupiter 6.0.3, jackson-core 3.1.5, kotlin-stdlib 2.4.20, slf4j-api 2.0.20.
- No `No SLF4J providers were found`/`StaticLoggerBinder` in the test log; `help --warning-mode all` prints no deprecation lines.

## Deviations from the spec
- `LoggingSystemProperties` has no String constants in Boot 4 (enum `LoggingSystemProperty`): production and tests adapt.
- `MetadataStore` is package-private: `ConfigurationMetadataIntegrationTest` reads the metadata through `JsonMarshaller`.
- `ConsolePretendingColorConverter` override non-null (`@NullMarked` package).
- The sample's Boot Gradle plugin is restored here (PR 1 deferred it); `-Xannotation-default-target=param-property` is not added (no annotated constructor property exists).
- JUnit 6.0.3 on the Spring modules' test classpaths via the BOM; kommons-test keeps 5.14.4 and is exercised on JUnit 6 by those suites.
- Spring preset timestamps are ISO-8601 with offset (Boot 3+ default); `LoggingPresetTest`/`LoggingReConfiguringEnvironmentPostProcessorTest` clear the MDC they fill.

## CHANGELOG notes for PR 5
- The Spring modules require Spring Boot 4 (Spring Framework 7) and Java 17; auto-configuration is discovered through `AutoConfiguration.imports`, so Boot 2.x applications no longer pick kommons-logging-spring-boot up.
- kommons-logging-logback's JSON preset depends on logstash-logback-encoder 9 (Jackson 3, package `tools.jackson`) and Logback 1.5; custom `StructuredArgument` implementations port their `writeTo` to `tools.jackson.core.JsonGenerator`.
- `Logback.clearSystemProperties()` clears Boot 4's property set (including `CONSOLE_LOG_STRUCTURED_FORMAT` and friends).
- Spring preset console output follows Boot 4's layout (ISO-8601 timestamp with offset, application name in brackets when set).
- The sample uses `spring-boot-starter-webmvc`.

## Needs a decision
- <list or "none">
- Noted, not changed: `application.yml` of the sample has `console: off`, which YAML 1.1 reads as the boolean `false`, so the console preset silently falls back to `spring`; quote it (`"off"`) if the console was meant to be off. Boot 4's own structured logging (`logging.structured.format.console=logstash`) overlaps the JSON preset; whether kommons keeps shipping logstash-logback-encoder or delegates to Boot is a 3.x roadmap question, not for this PR.

BREAKING CHANGE: kommons-logging-logback, kommons-logging-spring-boot and
kommons-logging-spring-boot-starter require Spring Boot 4 and Java 17;
the JSON preset uses logstash-logback-encoder 9 on Jackson 3 and
Logback 1.5.
PR
```

Fill in the test count and the "Needs a decision" section before submitting.

- [ ] **Step 3: Wait for CI**

```bash
gh pr checks --watch
```

Expected: all nine `build.yml` jobs green, including the sample's tests on the JDK 21 and 25 rows and the browser tests in the `js` job. A red job is fixed on the branch with a commit of the matching type.

- [ ] **Step 4: Merge and clean up**

```bash
gh pr merge --rebase
```

Then remove the worktree (`ExitWorktree` with `remove`; the rebased commits are on master under new SHAs, so confirm the discard) and prune.
