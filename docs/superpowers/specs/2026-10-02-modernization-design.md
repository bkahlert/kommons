# Kommons 3.0 modernization

Design for upgrading the build, dependencies, publishing and CI of Kommons to current versions and current best practice, released as 3.0.0.

Date: 2026-10-02. Status: design approved, implementation plan pending.

Kommons is company work: several ista microservices depend on it.

## Decisions

| # | Decision | Rationale |
|---|---|---|
| 1 | JVM bytecode floor is Kotlin's floor, 1.8, for all libraries. Exceptions: kommons-uri at 11 (needs `java.net.spi.URLStreamHandlerProvider`, as today), the four Spring modules (`kommons-logging-logback`, `-spring-boot`, `-spring-boot-starter`, `-spring-boot-sample`) at 17. | Kotlin 2.4.20 still defaults `jvmTarget` to 1.8 and ships a Java 8 stdlib. Every supported Spring Boot line needs JDK 17. `kommons-logging-logback` depends on the `spring-boot` jar for `ColorConverter`, so its floor is 17 whatever we declare. |
| 2 | ICU4J pinned to 77.1 and Kotest to 5.9.1. | Both latest lines are JDK 11 bytecode (class version 55, checked on the 6.2.5 assertion jars and the 78.3 jar). 77.1 is the last Java 8 ICU4J (Unicode 16); 5.9.1 the last Java 8 Kotest. Revisit on the next JVM floor change. |
| 3 | Build JDK 17 via `jvmToolchain(17)`, compiling down per decision 1. | Gradle 9 needs JDK 17 to run. |
| 4 | Publishing moves to the Central Portal through `com.vanniktech.maven.publish`. | OSSRH shut down 2025-06-30. The plugin is Portal-native and KMP-aware. |
| 5 | Release publishes from a single macOS job. | Kotlin/Native cross-compiles every klib in this project from macOS; the repo has no cinterop. Maven Central rejects duplicate publications from multi-host uploads. |
| 6 | GitHub Packages publishing is removed. | No workflow has ever published there; the README badge links to GitHub Releases. |
| 7 | Repositories reduce to `mavenCentral()` in settings. `mavenLocal`, the OSSRH snapshot repo, `google()` and `gradlePluginPortal()` leave the project repositories. | Dead or unused. Confirmed by the author. |
| 8 | kotlin-logging is migrated to `io.github.oshai`, not removed. | The `by KotlinLogging` delegate is kommons-logging-core's public API. |
| 9 | Native targets: add linuxArm64 in PR 3, compiled but not run in CI; keep macosX64 published but untested; no Apple mobile targets. | linuxArm64 is tier 2 and Kotlin/Native has no Linux ARM64 host, so it can only be cross-compiled. Ktor 2.2.3, Mordant 2.0.0-beta9 and Kotest 5.5.4 publish no linuxArm64 variants; the target needs PR 3's bumps (PR 1 found this). macosX64 is deprecated upstream and Intel runners disappear in 2027. |
| 10 | Five staged PRs, each green in CI before merge. | Keeps failures attributable to one change. |
| 11 | Spring Boot 4.1.x, with Boot-managed Logback 1.5.38 rather than Logback 1.6. | 3.5 left OSS support in June 2026. Overriding Boot's BOM for Logback buys nothing. |
| 12 | `io.spring.dependency-management` is dropped in favour of `platform(...)`. | Last released 2024-12; Gradle 9 compatibility unverified; Gradle's native BOM support suffices. |
| 13 | Binary-compatibility-validator API dumps are out of scope. | New scope; can follow separately. |

## Target versions

| Dependency | From | To |
|---|---|---|
| Gradle | 7.3 | 9.7.0 (top of KGP 2.4.20's supported range; 9.8 only if the KGP matrix allows) |
| Kotlin | 1.8.10 | 2.4.20 |
| Dokka | 1.7.10 | 2.2.0 |
| nebula.release | 16.0.0 (`nebula.release`) | 21.1.4 (`com.netflix.nebula.release`) |
| com.vanniktech.maven.publish | new | 0.37.0 |
| Foojay toolchain resolver | new | latest 1.x |
| kotlinx-datetime | 0.4.0 | 0.8.0 |
| kotlinx-serialization | 1.5.0 | 1.11.0 |
| Ktor | 2.2.3 | 3.6.0 |
| Mordant | 2.0.0-beta9 | 3.1.0 |
| kotlin-logging | io.github.microutils 2.1.23 | io.github.oshai 8.0.4 |
| SLF4J | 1.7.36 | 2.0.20 |
| JUnit Jupiter / Platform | 5.9.0 / 1.9.0 | 5.14.4 |
| Kotest assertions | 5.5.4 | 5.9.1 (`kotest-common` removed) |
| ICU4J | 71.1 | 77.1 |
| plexus-utils | 3.4.2 | 3.6.2 |
| Spring Boot | 2.7.6 | 4.1.1 |
| spring-dependency-management | 1.1.0 | removed |
| Logback | 1.2.11 | 1.5.38 (Boot-managed) |
| logstash-logback-encoder | 7.2 | 9.0 |
| jsr305 | 3.0.2 | unchanged |
| npm xregexp | 5.1.0 | 5.1.3 |
| npm @stdlib/string-next-grapheme-cluster-break | 0.0.9 | 0.2.3 |
| actions/checkout | v3 | v7 |
| actions/setup-java | v3 (`adopt`) | v6 (`temurin`) |
| actions/upload-artifact | v3 | v7 |
| gradle/gradle-build-action | v2 | gradle/actions/setup-gradle v6 |
| softprops/action-gh-release | v1 | v3 |
| dorny/test-reporter | v1 | v3 |

Exact patch versions are taken at implementation time from Maven Central; the table fixes the lines.

## PR 1: build platform

Scope: Gradle, JDK, Kotlin, Dokka, convention plugins, catalog, CI workflows, and the code changes the compiler forces.

### Settings and root

- [settings.gradle.kts](../../../settings.gradle.kts): remove `enableFeaturePreview("VERSION_CATALOGS")`. Add `dependencyResolutionManagement { repositories { mavenCentral() } }` and the Foojay resolver convention plugin. `pluginManagement` keeps `mavenCentral()` and `gradlePluginPortal()`.
- [build.gradle.kts](../../../build.gradle.kts): drop the repositories block; apply `com.netflix.nebula.release`.
- [gradle.properties](../../../gradle.properties): keep `org.gradle.caching`, `org.gradle.jvmargs`, `kotlin.code.style`, `kapt.include.compile.classpath`. Add `org.gradle.configuration-cache=true`. Remove `kotlin.native.cacheKind.linuxX64`, `kotlin.native.ignoreDisabledTargets` and `kotlin.js.generate.executable.default` one at a time; keep only what still bites, with a comment saying why.
- Configuration cache: the three `copy {}` blocks inside `doLast` (Logback appenders in [kommons-logging-logback/build.gradle.kts](../../../kommons-logging/kommons-logging-logback/build.gradle.kts), banner expansion in [kommons-logging-spring-boot-sample/build.gradle.kts](../../../kommons-logging/kommons-logging-spring-boot-sample/build.gradle.kts), Unicode generation in [kommons-text/build.gradle.kts](../../../kommons-text/build.gradle.kts)) become declarative task configuration (`Copy`/`ProcessResources` inputs, or injected `FileSystemOperations`). If nebula.release or the Kotlin/JS tooling cannot run under the configuration cache, it stays off and the reason is recorded in `gradle.properties`.

### buildSrc

- [buildSrc/build.gradle.kts](../../../buildSrc/build.gradle.kts): add the vanniktech plugin; remove `spring-dependency-management`; keep Dokka, KGP, nebula, Spring Boot plugin.
- [kotlin-conventions.gradle.kts](../../../buildSrc/src/main/kotlin/kotlin-conventions.gradle.kts): `jvmToolchain(17)`; `kotlin { compilerOptions { languageVersion/apiVersion = KOTLIN_2_4; progressiveMode = true; optIn.addAll(...) } }` replaces the `KotlinCompilationTask` loop and the `languageSettings` block. Drop the `kotlin.RequiresOptIn` opt-in. Keep the remaining opt-ins until a compile proves them unused. Test task configuration unchanged; Gradle 9's "test sources but no tests discovered" check stays on.
- [kommons-jvm-conventions.gradle.kts](../../../buildSrc/src/main/kotlin/kommons-jvm-conventions.gradle.kts): add a buildSrc function `jvmTarget(version: Int)` (name indicative) that sets Kotlin `jvmTarget` and Java `targetCompatibility`/`release` together on the jvm target, so KGP's JVM-target validation passes with Java sources enabled by default. Default 1.8. kommons-uri calls it with 11, the Spring modules with 17; their per-module `KotlinJvmCompile` blocks are removed.
- [kommons-js-conventions.gradle.kts](../../../buildSrc/src/main/kotlin/kommons-js-conventions.gradle.kts): `js { browser { ... }; nodejs { ... } }` without `IR`; Karma on Firefox headless; Mocha timeout kept. Regenerate `kotlin-js-store/yarn.lock` with `kotlinUpgradeYarnLock`.
- [kommons-native-conventions.gradle.kts](../../../buildSrc/src/main/kotlin/kommons-native-conventions.gradle.kts): remove all manual `dependsOn` wiring; rely on the default hierarchy template. Register `linuxX64`, `mingwX64`, `macosX64`, `macosArm64`; `linuxArm64` follows in PR 3 (decision 9). Keep the `osArchOnly` property: it registers only the host target; the template still provides `nativeMain`/`nativeTest`. Add the `kotlinx.cinterop.ExperimentalForeignApi` opt-in for native source sets.
- Dokka: plugin v2 `dokka { }` DSL, task `dokkaGeneratePublicationHtml`. Remove the versioning plugin (never configured). Snapshot versions keep Dokka disabled as today.
- [kommons-publishing-conventions.gradle.kts](../../../buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts): only the Kotlin fixes here (`capitalize()` to `replaceFirstChar`); the rewrite is PR 2.

### Spring module build files

- Starter: stop applying the Spring Boot and dependency-management plugins; `api(platform(libs.spring.boot.bom))`.
- Sample: keep `org.springframework.boot` and `kotlin("plugin.spring")`; `implementation(platform(SpringBootPlugin.BOM_COORDINATES))`; add `-Xannotation-default-target=param-property`; `jvmToolchain(17)`.
- `kommons-logging-spring-boot`: kapt stays for `spring-boot-configuration-processor`; versions come from the BOM platform instead of `libs.versions.spring.boot`.
- All four call `jvmTarget(17)`.

### Catalog

- Versions per the table. Remove `kotest-common`, `spring-dependency-management`. Add `spring-boot-bom` (`org.springframework.boot:spring-boot-dependencies`) and the vanniktech plugin artifact. Plugin ids stay resolved through the buildSrc classpath.

### Compiler-forced code changes

- `Char.toInt()`/`Int.toChar()`: 11 sites move to `Char.code` and `Char(Int)`.
- Whatever else Kotlin 2.4 in progressive mode rejects. Expect type-inference adjustments, not redesigns. The `capitalize`/`decapitalize` calls in the modules are kommons-text's own API and stay.

### CI workflows

- [build.yml](../../../.github/workflows/build.yml): actions per the table; `setup-gradle` plus a separate `run: ./gradlew ...` step. `GRADLE_OPTS` reduces to the JVM args with heap dump on OOM. Keep `-Pkotlin.tests.individualTaskReports=true`, the concurrency group, `paths-ignore`, and the 30 s JUnit timeout under `CI=true`.
- A `testJdk` Gradle property sets the `Test` tasks' `javaLauncher` to that toolchain; Foojay provisions it. It covers the multiplatform modules only; the Spring Boot sample's `test` task stays on the build toolchain until PR 4, because Spring Boot 2.7 supports Java up to 19.
- Matrix:

  | Target | Runner | Test JDK |
  |---|---|---|
  | jvm | ubuntu-latest, windows-latest, macos-latest | 17 |
  | jvm | ubuntu-latest | 21, 25 |
  | js | ubuntu-latest | 17 |
  | linuxX64 | ubuntu-latest | 17 |
  | mingwX64 | windows-latest | 17 |
  | macosArm64 | macos-latest | 17 |

  No ARM Linux runner: Kotlin/Native has no Linux ARM64 host (on `ubuntu-24.04-arm` the plugin registers no `linuxArm64Test` task). Once PR 3 adds the target, the linuxX64 job cross-compiles and links it with `linuxArm64TestBinaries`; the binaries are never run in CI.

- [build-custom.yml](../../../.github/workflows/build-custom.yml): same action bumps; JDK default 17.
- [test-report.yml](../../../.github/workflows/test-report.yml): `dorny/test-reporter@v3`; artifact regex unchanged.
- [release.yml](../../../.github/workflows/release.yml): action bumps only here; the single-host rewrite is PR 2.

### QA for PR 1

- `./gradlew check` locally with `-PosArchOnly=true`; the full matrix green in CI.
- `javap -v` on one class per published jar: major version 52 for libraries, 55 for kommons-uri, 61 for the Spring modules. Recorded in the PR description.
- `./gradlew help` runs with the configuration cache stored and reused on the second invocation, or the property is off with a documented reason.

## PR 2: publishing

### Convention

- [kommons-publishing-conventions.gradle.kts](../../../buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts) applies `com.vanniktech.maven.publish` and `com.netflix.nebula.release`. `mavenPublishing { publishToMavenCentral(); signAllPublications(); configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"), sourcesJar = true)) }`. The existing POM block (name from project name, description via `afterEvaluate`, MIT, developer, scm, issue and CI management) moves into `mavenPublishing.pom { }` unchanged.
- Snapshot rule as today: `-SNAPSHOT` versions disable signing and Dokka tasks. `RELEASE_VERSION` from the environment still overrides the nebula-inferred version.
- Automatic Portal release is enabled with the plugin's own `mavenCentralAutomaticRelease` Gradle property in `gradle.properties`. The first 3.0.0 run passes `-PmavenCentralAutomaticRelease=false` so the deployment can be inspected and published in the Portal UI.
- GitHub Packages repository removed.
- [kommons-bom/build.gradle.kts](../../../kommons-bom/build.gradle.kts) applies the same convention; vanniktech handles `java-platform`. Its publishing, signing and POM blocks go; the constraints logic and the `publish`-task probe stay.

### Credentials

Gradle properties, provided in CI as `ORG_GRADLE_PROJECT_*` environment variables from repository secrets:

| Property | Content |
|---|---|
| `mavenCentralUsername` | Central Portal user token: generated username (not the display name) |
| `mavenCentralPassword` | Central Portal user token: generated password |
| `signingInMemoryKey` | ASCII-armored private key |
| `signingInMemoryKeyId` | last 8 hex digits of the key id |
| `signingInMemoryKeyPassword` | key passphrase |

The current `signingKey`/`signingPassword` properties and `OSSRH_*` secrets are retired.

Author actions before PR 2 can be validated end to end:

1. Sign in at central.sonatype.com with the former OSSRH account and confirm `com.bkahlert.kommons` is listed under Namespaces.
2. Generate a user token there.
3. Store the five secrets above in the GitHub repository.
   
### Release workflow

- One `macos-latest` job: checkout with `fetch-depth: 0`, JDK 17, setup-gradle, `./gradlew publishToMavenCentral`. Inputs reduce to `version` and `branch`.
- Draft-release job unchanged (CHANGELOG body, tag `v<version>`), with `softprops/action-gh-release@v3`. Step summary drops the Nexus close/release instructions.
- Tests are not part of the release job; `build.yml` covers them.

### Docs

- README "Development" gains a release paragraph: how a release is triggered and where to inspect a deployment.

### QA for PR 2

- `./gradlew publishToMavenLocal` with a throwaway signing key produces, for every published module including the BOM: module file, POM, main artifacts per target, sources jar, javadoc jar, and `.asc` for each.
- First real deployment with automatic release off validates in the Portal without errors.

## PR 3: library dependencies

- kotlinx-datetime 0.8: `Instant`, `Clock`, `toKotlinInstant`, `toJavaInstant` move to `kotlin.time` (stable in Kotlin 2.4, no opt-in). kommons-time's `InstantAsEpochMillisecondsSerializer`, `InstantAsEpochSecondsSerializer` and the `Clock.Companion` extensions now target `kotlin.time.Instant`/`Clock`. This is the intended public API break of 3.0. `dayOfMonth`/`monthNumber` become `day`/`month`. Tests expecting `Z` as the UTC zone id or parsing instants without seconds are adjusted.
- Mordant 3.1: `Terminal(TRUECOLOR).also { it.info.updateTerminalSize() }` becomes `updateSize()` at the two sites (kommons-text native, kommons-kaomoji common).
- kotlin-logging 8: package `mu` becomes `io.github.oshai.kotlinlogging` in `delegate.kt` and the four test files; `slf4j-api` stays declared explicitly in kommons-logging-core. Removed from kommons-exec.
- Kotest 5.9.1: `bestName()` replaced by a private helper in kommons-test; `kotest-common` dropped.
- Ktor 3.6, serialization 1.11, JUnit 5.14, plexus-utils 3.6.2, SLF4J 2.0, ICU4J 77.1, npm bumps: no source changes expected for the symbols in use; compile and tests decide.
- linuxArm64 (decision 9): register the target in [kommons-native-conventions.gradle.kts](../../../buildSrc/src/main/kotlin/kommons-native-conventions.gradle.kts) once Ktor 3.6, Mordant 3.1 and Kotest 5.9.1 are in; all three publish it. In [build.yml](../../../.github/workflows/build.yml) the linuxX64 job's command gains `linuxArm64TestBinaries`.

### QA for PR 3

- Existing kommons-time, kommons-text, kommons-kaomoji, kommons-logging-core, kommons-uri and kommons-test suites pass with adjusted expectations.
- New unit test for the `bestName` replacement.

## PR 4: Spring Boot and logging stack

- Auto-configuration registration: `LoggingConfiguration` moves from `spring.factories` to `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and is annotated `@AutoConfiguration`. The `EnvironmentPostProcessor` entry stays in `spring.factories`; the interface import moves to `org.springframework.boot`.
- Type-level `@ConstructorBinding` removed from `LoggingProperties` and the sample's `HelloWorldConfiguration`.
- logstash-logback-encoder 9 / Jackson 3: `StructuredArgumentsTest` imports `tools.jackson.core.JsonGenerator`. Production code uses only logstash markers and structured arguments.
- Logback 1.5.38 via Boot's BOM; the project's Logback XML uses no Janino conditionals, so no config change.
- Sample: Boot 4 starters for web and actuator; test starter. Its `test` task follows `-PtestJdk` like the multiplatform modules (the launcher wiring moves from `kommons-jvm-conventions` to a convention the sample applies too), so the JDK 21 and 25 matrix rows cover it from here on.

### QA for PR 4

- kommons-logging-logback, -spring-boot and the sample's existing tests pass (ApplicationContextRunner, output capture, configuration metadata, LogFileWebEndpoint).

## PR 5: release 3.0.0

- Version bump in the 16 Markdown files quoting 2.8.0.
- CHANGELOG, Changed: JVM floors (1.8 libraries, 11 kommons-uri, 17 Spring stack); `kotlin.time.Instant`/`Clock` in kommons-time; kotlin-logging package `io.github.oshai.kotlinlogging`; linuxArm64 added; macosX64 deprecated upstream; dependency versions. Removed: GitHub Packages publishing; `kotest-common` as a transitive dependency; `io.spring.dependency-management`.
- Release through the new workflow, first with automatic release off.

## Unknowns to settle during implementation

- Gradle 9.7.0 or 9.8.0, depending on KGP's published compatibility range at implementation time.
- Configuration-cache compatibility of nebula.release 21 and the Kotlin/JS tooling.
- Which of the three `kotlin.native.*`/`kotlin.js.*` properties still matter.
- The Karma DSL deprecation announced for KGP 2.4.20 and its replacement.
- Which opt-ins remain necessary.
- Whether `io.ktor.utils.io.core.toByteArray` still exists in Ktor 3.6 (fallback: stdlib `encodeToByteArray`).
- Whether any module trips Gradle 9's "no tests discovered" check.

## Out of scope

- The author's uncommitted working-tree changes on master, including the `CONSOLE_LOG_CHARSET` line that Boot 4's UTF-8 default makes redundant.
- Binary-compatibility-validator.
- Apple mobile targets, wasm targets.
- Removing the `kotlin-logging` and `kommons-core` dependencies from kommons-logging-core (the author's TODOs).
- Migrating buildSrc to an included build.

## Working conventions

- Branches per PR, named `build/...`, `ci/...`, `refactor/...` etc. per the commit type; never on master.
- Conventional Commits; the Instant and JVM-floor changes carry `BREAKING CHANGE` footers.
