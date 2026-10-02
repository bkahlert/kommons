# Library Dependencies (PR 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring every library dependency of Kommons to its current line (kotlinx-datetime 0.8, Ktor 3.6, Mordant 3.1, kotlin-logging 8, Kotest 5.9.1, kotlinx-serialization 1.11, JUnit 5.14, ICU4J 77.1, plexus-utils 3.6.2, npm bumps), adapt the code the bumps force, and register the linuxArm64 target that the old versions blocked.

**Architecture:** One catalog bump, then one task per library with source changes, each gated by compiling the modules it touches; the whole-build compile, the Yarn lock and the test run come once everything is adapted. kommons-time's `Instant`/`Clock` API moves to `kotlin.time` (the intended public API break of 3.0). kommons-logging-core's `by KotlinLogging` delegate keeps its shape on the `io.github.oshai` package. kommons-test replaces Kotest's internal `bestName()` with an own expect/actual helper. The Spring Boot 2.7.6 stack (Logback 1.2.11, logstash 7.2, Spring Boot) stays untouched for PR 4.

**Tech Stack:** Gradle 9.7.0, Kotlin Multiplatform 2.4.20, Kotlin/Native (linuxX64, linuxArm64, mingwX64, macosX64, macosArm64), Kotlin/JS (Node, browser), GitHub Actions.

**Spec:** [docs/superpowers/specs/2026-10-02-modernization-design.md](../specs/2026-10-02-modernization-design.md), section "PR 3: library dependencies", decisions 2, 8, 9 and the "Target versions" table.

Deviations from the spec's PR 3 text:

- **SLF4J stays at 1.7.36; the 2.0 bump moves to PR 4.** Logback stays at 1.2.11 until PR 4 (spec decision 11), and Logback 1.2 has no SLF4J 2 service provider: with `slf4j-api` 2.0 on the classpath every Logback-backed module binds to the NOP logger (kommons-logging-core's logger-name tests would see `NOP`, kommons-logging-logback's context tests would fail on a `ClassCastException`). kotlin-logging 8 runs on SLF4J 1 or 2, so nothing in PR 3 needs the bump.
- The `bestName()` replacement is an `internal expect fun` with three actuals (JVM, JS, native), not a single private function: `KClass.qualifiedName` throws on Kotlin/JS, and the JVM error messages keep their qualified names only with a platform-specific implementation.
- The `KLogger.name` test helper in kommons-logging-core's JS test goes: `KLogger` has had a `name` member since kotlin-logging 5, and the member shadows the extension.

## Global Constraints

- Versions (exact, verified on Maven Central and npm on 2026-10-02): kotlinx-datetime 0.8.0, kotlinx-serialization 1.11.0, Ktor 3.6.0, Mordant 3.1.0, kotlin-logging `io.github.oshai` 8.0.4, Kotest 5.9.1, JUnit Jupiter 5.14.4, JUnit Platform 1.14.4, ICU4J 77.1, plexus-utils 3.6.2, npm xregexp 5.1.3, npm @stdlib/string-next-grapheme-cluster-break 0.2.3. Unchanged: SLF4J 1.7.36, Logback 1.2.11, logstash-logback-encoder 7.2, Spring Boot 2.7.6, jsr305 3.0.2, Kotlin 2.4.20, Dokka 2.2.0, nebula 21.1.4, vanniktech 0.37.0.
- Every bumped JVM jar is Java 8 bytecode (class version 52, checked on 2026-10-02 for all of the above including `mordant-jvm-ffm`); decision 1 holds without changes to `jvmBytecodeTarget`.
- Every bumped multiplatform library publishes linuxArm64 (klibs on Central checked for Ktor, Mordant, Kotest, kotlinx-datetime, kotlinx-serialization).
- The build runs on JDK 17 through the wrapper script `$SCRATCH/gradlew17`. `./gradlew` below means that script. `--tests` applies only to the task named right before it.
- `./gradlew help --warning-mode all` prints zero deprecation lines; configuration cache stays on. No `by getting/creating`, no `project` at execution time.
- Local test command: `allTests test -PosArchOnly=true -x jsBrowserTest`. Karma cannot start Firefox on this machine; browser tests run only in CI. Baseline at the branch point: 971 JVM tests, 0 failures.
- `-PosArchOnly=true` registers only the host native target (macosArm64 here); a build without it registers all five after Task 7 and cross-compiles them on macOS.
- Commits follow Conventional Commits (`build:`, `refactor(scope):`, `test:`, `ci:`), lowercase description, no AI attribution trailers. The `Instant` and kotlin-logging changes carry `BREAKING CHANGE:` footers.
- Worktree `.claude/worktrees/build-library-dependencies`, branch `build/library-dependencies` from `origin/master` 1fab951b. The main checkout holds the author's uncommitted work: never touch it. The Bash sandbox refuses compound commands that mention `git` or expand a command name from a variable; use plain commands and scratch scripts.
- Intermediate commits (Task 1 alone) do not compile; the branch compiles from Task 6 on, as PR 1 did. CI runs on the final state.

## Review Focus

Inputs and conditions the spec implies but no task's tests exercise by default; each has a pinning check in the task named.

1. The wire format of `InstantAsEpochMilliseconds`/`InstantAsEpochSeconds` (serial names and `Long` encoding) must not change with the `Instant` type, or persisted JSON of consumers stops decoding. Task 2 step 7 adds a descriptor test; the existing encode/decode tests run in Task 6.
2. `by KotlinLogging` must yield the same logger names as before on JVM and JS, or consumers' log configuration stops matching. The existing `JvmDelegateKtTest`/`JsDelegateKtTest` pin them; Task 6 step 3 runs them, and step 4 greps the JVM test log for `No SLF4J providers`, which would mean the names came from the NOP logger.
3. `bestName()` must stay fully qualified on JVM and readable on JS, or every kommons-test failure message degrades. Task 5 tests both.
4. SLF4J binding in tests: `slf4j-simple` 1.7.36 keeps binding with the bumped libraries (kotlin-logging 8 drops its own SLF4J dependency). Task 6 step 4.
5. linuxArm64 must produce a klib for every native module, not only compile for one. Task 7 step 5 counts them (7 modules).

---

## Preparation

- [ ] **Step 1: Worktree and wrapper**

The worktree exists at `.claude/worktrees/build-library-dependencies` on branch `build/library-dependencies` (base `1fab951b docs: add publishing implementation plan`). `$SCRATCH/gradlew17` already points at it. Check:

```bash
git log --oneline -1
./gradlew help --warning-mode all 2>&1 | grep -ci deprecat
```

Expected: `1fab951b …` and `0`.

- [ ] **Step 2: Commit this plan**

```bash
git add docs/superpowers/plans/2026-10-02-pr3-library-dependencies.md
git commit -m "docs: add library dependencies implementation plan"
```

---

### Task 1: Version catalog and build files

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `kommons-test/build.gradle.kts`
- Modify: `kommons-exec/build.gradle.kts`

**Interfaces:**
- Produces: catalog aliases unchanged except `libs.kotest.common` (deleted); `libs.kotlin.logging` now resolves to `io.github.oshai:kotlin-logging`. Tasks 2 to 6 compile against these versions.

- [ ] **Step 1: Versions**

Replace the `[versions]` section of `gradle/libs.versions.toml` with:

```toml
[versions]
dokka = "2.2.0"
icu = "77.1"
kotlin = "2.4.20"
kotlin-logging = "8.0.4"
kotlinx-datetime = "0.8.0"
kotest = "5.9.1"
kotlinx-serialization = "1.11.0"
ktor = "3.6.0"
logback-classic = "1.2.11"
logstash-logback-encoder = "7.2"
jvm = "1.8"
jsr305 = "3.0.2"
junit-jupiter = "5.14.4"
junit-platform = "1.14.4"
mordant = "3.1.0"
nebula-release = "21.1.4"
slf4j = "1.7.36"
spring-boot = "2.7.6" # https://docs.spring.io/spring-boot/docs/2.7.6/
stdlib-js = "0.2.3"
plexus = "3.6.2"
xregexp = "5.1.3"
vanniktech-maven-publish = "0.37.0"
```

- [ ] **Step 2: Libraries**

In `[libraries]` replace

```toml
kotlin-logging = { module = "io.github.microutils:kotlin-logging", version.ref = "kotlin-logging" }
```

with

```toml
kotlin-logging = { module = "io.github.oshai:kotlin-logging", version.ref = "kotlin-logging" }
```

and delete the line

```toml
kotest-common = { module = "io.kotest:kotest-common", version.ref = "kotest" }
```

- [ ] **Step 3: kommons-test and kommons-exec build files**

In `kommons-test/build.gradle.kts` delete

```kotlin
            implementation(libs.kotest.common) // because("mpp.bestName")
```

In `kommons-exec/build.gradle.kts` delete

```kotlin
            implementation(libs.kotlin.logging)
```

(kommons-exec has no source that uses it: `grep -rn "KotlinLogging\|mu\." kommons-exec/src` is empty.)

- [ ] **Step 4: Configure**

```bash
./gradlew help --warning-mode all 2>&1 | grep -ciE "deprecat"
```

Expected: `0` and `BUILD SUCCESSFUL` in the full output. Compilation is not expected to succeed before Task 6.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml kommons-test/build.gradle.kts kommons-exec/build.gradle.kts
git commit -m "build: upgrade library dependencies

kotlinx-datetime 0.8.0, kotlinx-serialization 1.11.0, Ktor 3.6.0,
Mordant 3.1.0, kotlin-logging 8.0.4 (io.github.oshai), Kotest 5.9.1,
JUnit 5.14.4 / Platform 1.14.4, ICU4J 77.1, plexus-utils 3.6.2, npm
xregexp 5.1.3 and @stdlib/string-next-grapheme-cluster-break 0.2.3.
All JVM jars are Java 8 bytecode. SLF4J stays at 1.7.36 until Logback
moves off 1.2 with Spring Boot 4: Logback 1.2 has no SLF4J 2 provider.
kotest-common goes (kommons-test gets its own bestName), as does the
unused kotlin-logging dependency of kommons-exec. The code adaptations
follow in the next commits."
```

---

### Task 2: kommons-time on `kotlin.time.Instant` and `Clock`

**Files:**
- Modify: `kommons-time/src/commonMain/kotlin/com/bkahlert/kommons/time/time.kt`
- Modify: `kommons-time/src/commonMain/kotlin/com/bkahlert/kommons/time/strings.kt`
- Modify: `kommons-time/src/commonMain/kotlin/com/bkahlert/kommons/time/InstantAsEpochMillisecondsSerializer.kt`
- Modify: `kommons-time/src/commonMain/kotlin/com/bkahlert/kommons/time/InstantAsEpochSecondsSerializer.kt`
- Modify: `kommons-time/src/jvmMain/kotlin/com/bkahlert/kommons/time/JvmTime.kt`
- Modify: `kommons-time/src/jsMain/kotlin/com/bkahlert/kommons/time/JsTime.kt`
- Modify: `kommons-time/src/nativeMain/kotlin/com/bkahlert/kommons/time/NativeTime.kt`
- Modify: `kommons-test/src/commonMain/kotlin/com/bkahlert/kommons/test/time.kt`
- Modify (tests, imports only): `kommons-time/src/commonTest/kotlin/com/bkahlert/kommons/time/TimeKtTest.kt`, `…/InstantAsEpochMillisecondsSerializerTest.kt`, `…/InstantAsEpochSecondsSerializerTest.kt`, `kommons-time/src/jvmTest/kotlin/com/bkahlert/kommons/time/JvmTimeKtTest.kt`, `kommons-test/src/commonTest/kotlin/com/bkahlert/kommons/test/TimeKtTest.kt`, `kommons-io/src/jvmTest/kotlin/com/bkahlert/kommons/io/PathsKtTest.kt`

**Interfaces:**
- Produces: `com.bkahlert.kommons.time.invoke` as `kotlin.time.Clock.Companion.invoke(now: () -> kotlin.time.Instant): kotlin.time.Clock`; `InstantAsEpochMillisecondsSerializer`/`InstantAsEpochSecondsSerializer` as `KSerializer<kotlin.time.Instant>`; `Now: kotlin.time.Instant`. kommons-test's `Clock.Companion.fixed` consumes `invoke`.

What stays on `kotlinx.datetime`: `LocalDate`, `TimeZone`, `FixedOffsetTimeZone`, `UtcOffset`, `DateTimeUnit`, `daysUntil`, `plus`, `toLocalDateTime`, `todayIn`, `toJavaLocalDate`, `toJSDate`, `Date.toKotlinInstant()`. kotlinx-datetime 0.7+ provides them for `kotlin.time.Instant`/`Clock`.

- [ ] **Step 1: time.kt**

Replace the two imports

```kotlin
import kotlinx.datetime.Clock
```

```kotlin
import kotlinx.datetime.Instant
```

with (keeping the import block sorted: `kotlin.time.*` imports sit after the `kotlinx.datetime.*` ones in this file)

```kotlin
import kotlin.time.Clock
import kotlin.time.Instant
```

Nothing else in the file changes: `Clock.Companion.invoke`, `Clock.System.now()`, `Clock.System.todayIn(...)` and `Instant.toLocalDateTime(...)` all exist for the `kotlin.time` types.

- [ ] **Step 2: strings.kt and the two serializers**

In each of `strings.kt`, `InstantAsEpochMillisecondsSerializer.kt` and `InstantAsEpochSecondsSerializer.kt` replace

```kotlin
import kotlinx.datetime.Instant
```

with

```kotlin
import kotlin.time.Instant
```

(in the serializers the `kotlin.time.Instant` import goes after the `kotlinx.serialization.*` imports). The descriptor serial names `com.bkahlert.kommons.time.InstantAsMillisecondsSerializer` and `com.bkahlert.kommons.time.InstantAsSecondsSerializer` stay as they are.

- [ ] **Step 3: JvmTime.kt**

Replace

```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinInstant
```

with

```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
```

and add to the `kotlin.time.*` imports at the end of the import block:

```kotlin
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
```

- [ ] **Step 4: JsTime.kt**

Replace

```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJSDate
import kotlinx.datetime.toKotlinInstant
import kotlin.js.Date
import kotlin.time.Duration
```

with

```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJSDate
import kotlinx.datetime.toKotlinInstant
import kotlin.js.Date
import kotlin.time.Duration
import kotlin.time.Instant
```

and replace

```kotlin
private fun LocalDate.toJSDate() = Date(year, monthNumber - 1, dayOfMonth, 0, 0, 0, 0)
```

with

```kotlin
private fun LocalDate.toJSDate() = Date(year, month.number - 1, day, 0, 0, 0, 0)
```

- [ ] **Step 5: NativeTime.kt**

Replace

```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
```

with

```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
```

and replace `append(dayOfMonth)` with `append(day)`.

- [ ] **Step 6: kommons-test time.kt and the test imports**

In `kommons-test/src/commonMain/kotlin/com/bkahlert/kommons/test/time.kt` replace

```kotlin
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
```

with

```kotlin
import kotlin.time.Clock
import kotlin.time.Instant
```

Apply the same two-line replacement in `kommons-time/src/commonTest/.../TimeKtTest.kt` (it also imports `kotlinx.datetime.LocalDate` and `kotlinx.datetime.TimeZone`; those stay) and in `kommons-test/src/commonTest/.../TimeKtTest.kt`. In the two serializer tests replace `import kotlinx.datetime.Instant` with `import kotlin.time.Instant`. In `kommons-time/src/jvmTest/.../JvmTimeKtTest.kt` replace `import kotlinx.datetime.Clock` and `import kotlinx.datetime.toJavaInstant` with `import kotlin.time.Clock` and `import kotlin.time.toJavaInstant`. In `kommons-io/src/jvmTest/.../PathsKtTest.kt` replace `import kotlinx.datetime.toKotlinInstant` with `import kotlin.time.toKotlinInstant`. Keep every import block sorted (`kotlin.*` after `kotlinx.*`, as the files do today). `kommons-time/src/jsTest/.../JsTimeKtTest.kt` keeps `kotlinx.datetime.toKotlinInstant` (it converts a `Date`).

- [ ] **Step 7: Pin the serializers' wire format**

Append to the class in `kommons-time/src/commonTest/kotlin/com/bkahlert/kommons/time/InstantAsEpochMillisecondsSerializerTest.kt` (the file imports `io.kotest.matchers.shouldBe` and `kotlin.test.Test` already):

```kotlin
    @Test fun descriptor() = testAll {
        InstantAsEpochMillisecondsSerializer.descriptor should {
            it.serialName shouldBe "com.bkahlert.kommons.time.InstantAsMillisecondsSerializer"
            it.kind shouldBe PrimitiveKind.LONG
        }
    }
```

and the matching test to `InstantAsEpochSecondsSerializerTest.kt`:

```kotlin
    @Test fun descriptor() = testAll {
        InstantAsEpochSecondsSerializer.descriptor should {
            it.serialName shouldBe "com.bkahlert.kommons.time.InstantAsSecondsSerializer"
            it.kind shouldBe PrimitiveKind.LONG
        }
    }
```

Add the imports each file is missing from: `com.bkahlert.kommons.test.testAll`, `io.kotest.matchers.should`, `kotlinx.serialization.descriptors.PrimitiveKind`. These tests run in Task 6 (kommons-time's tests need kommons-test, which compiles only after Task 5); they pass today and must still pass then.

- [ ] **Step 8: Compile kommons-time**

```bash
./gradlew :kommons-time:compileKotlinJvm :kommons-time:compileKotlinJs :kommons-time:compileKotlinMacosArm64 -PosArchOnly=true 2>&1 | grep -E "^e:|^w:|BUILD"
```

Expected: `BUILD SUCCESSFUL`, no `e:` lines. A `w:` about `ExperimentalTime` means the opt-in in `kotlin-conventions` is still needed and is fine. If `Clock.Companion` is reported unresolved, `kotlin.time.Clock` has no companion in this stdlib: then keep `kotlinx.datetime.Clock` out and turn `invoke` into a top-level `public fun Clock(now: () -> Instant): Clock`, update `kommons-test/.../time.kt` to call `Clock { now }`, and ledger the ruling.

- [ ] **Step 9: Commit**

```bash
git add kommons-time/src kommons-test/src/commonMain/kotlin/com/bkahlert/kommons/test/time.kt kommons-test/src/commonTest/kotlin/com/bkahlert/kommons/test/TimeKtTest.kt kommons-io/src/jvmTest/kotlin/com/bkahlert/kommons/io/PathsKtTest.kt
git commit -m "refactor(time): move instant and clock to kotlin.time

kotlinx-datetime 0.7 removed its own Instant and Clock in favour of the
standard library's. The epoch serializers, Now, the Clock factory and
the JVM and JS conversions follow; LocalDate.dayOfMonth and monthNumber
become day and month.number.

BREAKING CHANGE: kommons-time's Instant and Clock API now uses
kotlin.time.Instant and kotlin.time.Clock; InstantAsEpochMilliseconds
and InstantAsEpochSeconds serialize kotlin.time.Instant. Replace
kotlinx.datetime.Instant/Clock imports with kotlin.time."
```

---

### Task 3: kotlin-logging on `io.github.oshai`

**Files:**
- Modify: `kommons-logging/kommons-logging-core/src/commonMain/kotlin/com/bkahlert/kommons/logging/delegate.kt`
- Modify: `kommons-logging/kommons-logging-core/src/commonTest/kotlin/com/bkahlert/kommons/logging/fixtures.kt`
- Modify: `kommons-logging/kommons-logging-core/src/jvmTest/kotlin/com/bkahlert/kommons/logging/JvmDelegateKtTest.kt`
- Modify: `kommons-logging/kommons-logging-core/src/jsTest/kotlin/com/bkahlert/kommons/logging/JsDelegateKtTest.kt`
- Modify: `kommons-logging/kommons-logging-core/build.gradle.kts`

**Interfaces:**
- Produces: `operator fun io.github.oshai.kotlinlogging.KotlinLogging.provideDelegate(thisRef: Any?, property: KProperty<*>): Lazy<io.github.oshai.kotlinlogging.KLogger>` (same name, new package). `KLogger.name` is a member of the new `KLogger`.

- [ ] **Step 1: delegate.kt**

Replace

```kotlin
import mu.KLogger
import mu.KotlinLogging
```

with

```kotlin
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
```

and in the KDoc replace `[Kotlin Logging](https://github.com/MicroUtils/kotlin-logging)` with `[Kotlin Logging](https://github.com/oshai/kotlin-logging)`.

- [ ] **Step 2: fixtures.kt and JvmDelegateKtTest.kt**

In `fixtures.kt` replace `import mu.KotlinLogging` with `import io.github.oshai.kotlinlogging.KotlinLogging`. In `JvmDelegateKtTest.kt` replace

```kotlin
import mu.KLogger
import mu.KotlinLogging
```

with

```kotlin
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
```

placed before the `io.kotest.*` imports (sorted). The assertions on `logger.name` stay: `name` is a `KLogger` member now.

- [ ] **Step 3: JsDelegateKtTest.kt**

Replace

```kotlin
import com.bkahlert.kommons.debug.properties
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mu.KLogger
import mu.KotlinLogging
```

with

```kotlin
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
```

and delete the helper at the end of the file:

```kotlin
val KLogger.name: String
    get() = checkNotNull(properties["loggerName"] as? String) { "Failed to find logger name of $this" }
```

- [ ] **Step 4: Drop the test-only kommons-debug dependency**

In `kommons-logging/kommons-logging-core/build.gradle.kts` delete

```kotlin
        jsTest.dependencies {
            implementation(project(":kommons-debug"))
        }
```

(`properties` was its only use.) `api(libs.kotlin.logging)` and `api(libs.slf4j.api)` stay: kotlin-logging 5+ no longer brings SLF4J itself.

- [ ] **Step 5: Compile**

```bash
./gradlew :kommons-logging:kommons-logging-core:compileKotlinJvm :kommons-logging:kommons-logging-core:compileKotlinJs 2>&1 | grep -E "^e:|^w:|BUILD"
```

Expected: `BUILD SUCCESSFUL`, no `e:` lines. The tests compile and run in Task 6.

- [ ] **Step 6: Commit**

```bash
git add kommons-logging/kommons-logging-core
git commit -m "refactor(logging): migrate to the io.github.oshai kotlin-logging

The io.github.microutils line ended at 3.x. The by KotlinLogging
delegate keeps its shape on the new package; the JS test drops its
KLogger.name helper, a member since kotlin-logging 5.

BREAKING CHANGE: the by KotlinLogging delegate and the KLogger it
yields come from io.github.oshai.kotlinlogging; replace mu.* imports.
kotlin-logging 8 logs only through lambdas (the String overloads are
gone) and prints a startup line naming its logger factory."
```

---

### Task 4: Mordant 3

**Files:**
- Modify: `kommons-text/src/nativeMain/kotlin/com/bkahlert/kommons/text/NativeGrapheme.kt:58`
- Modify: `kommons-kaomoji/src/commonMain/kotlin/com/bkahlert/kommons/kaomoji/columns.kt:8`

**Interfaces:**
- Consumes: Mordant 3.1.0 `Terminal(ansiLevel)` and `Terminal.updateSize(): Size` (checked in the 3.1.0 sources; `TerminalInfo.updateTerminalSize()` is gone).

- [ ] **Step 1: The two terminal sites**

In `NativeGrapheme.kt` replace

```kotlin
internal val terminal: Terminal by lazy { Terminal(TRUECOLOR).also { it.info.updateTerminalSize() } }
```

with

```kotlin
internal val terminal: Terminal by lazy { Terminal(TRUECOLOR).also { it.updateSize() } }
```

In `columns.kt` replace

```kotlin
private val terminal: Terminal by lazy { Terminal(TRUECOLOR).also { it.info.updateTerminalSize() } }
```

with

```kotlin
private val terminal: Terminal by lazy { Terminal(TRUECOLOR).also { it.updateSize() } }
```

- [ ] **Step 2: Compile kommons-text and kommons-kaomoji for every host target**

```bash
./gradlew :kommons-kaomoji:assemble -PosArchOnly=true 2>&1 | grep -E "^e:|^w:|BUILD"
```

Expected: `BUILD SUCCESSFUL` (this compiles kommons-text's jvm, js and macosArm64 main too, and Dokka for both).

- [ ] **Step 3: Commit**

```bash
git add kommons-text/src/nativeMain/kotlin/com/bkahlert/kommons/text/NativeGrapheme.kt kommons-kaomoji/src/commonMain/kotlin/com/bkahlert/kommons/kaomoji/columns.kt
git commit -m "refactor: read the terminal size through mordant 3

Terminal.updateSize() replaces TerminalInfo.updateTerminalSize(). The
Mordant dependency of kommons-text (native) and kommons-kaomoji is api,
so consumers move from 2.0.0-beta9 to 3.1.0 with this release."
```

---

### Task 5: Own `bestName()` in kommons-test

**Files:**
- Create: `kommons-test/src/commonMain/kotlin/com/bkahlert/kommons/test/KClasses.kt`
- Create: `kommons-test/src/jvmMain/kotlin/com/bkahlert/kommons/test/KClasses.kt`
- Create: `kommons-test/src/jsMain/kotlin/com/bkahlert/kommons/test/KClasses.kt`
- Create: `kommons-test/src/nativeMain/kotlin/com/bkahlert/kommons/test/KClasses.kt`
- Modify: `kommons-test/src/commonMain/kotlin/com/bkahlert/kommons/test/rootCause.kt:10`
- Modify: `kommons-test/src/jvmMain/kotlin/com/bkahlert/kommons/test/junit/DynamicTestBuilder.kt:17`
- Test: `kommons-test/src/commonTest/kotlin/com/bkahlert/kommons/test/KClassesKtTest.kt`
- Test: `kommons-test/src/jvmTest/kotlin/com/bkahlert/kommons/test/JvmKClassesKtTest.kt`

**Interfaces:**
- Produces: `internal expect fun KClass<*>.bestName(): String` in package `com.bkahlert.kommons.test`, mirroring Kotest's: JVM `qualifiedName ?: java.name`, JS `simpleName ?: toString()`, native `qualifiedName ?: simpleName ?: toString()`.

kommons-test has no `jsMain` or `nativeMain` directory yet; create them. The default hierarchy template wires `nativeMain` for all native targets.

- [ ] **Step 1: Write the failing tests**

Create `kommons-test/src/commonTest/kotlin/com/bkahlert/kommons/test/KClassesKtTest.kt`:

```kotlin
package com.bkahlert.kommons.test

import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotBeBlank
import kotlin.test.Test

class KClassesKtTest {

    @Test fun best_name() = testAll {
        val result = String::class.bestName()
        result shouldEndWith "String"
    }

    @Test fun best_name_of_anonymous_object() = testAll {
        val result = (object {})::class.bestName()
        result.shouldNotBeBlank()
    }
}
```

Create `kommons-test/src/jvmTest/kotlin/com/bkahlert/kommons/test/JvmKClassesKtTest.kt`:

```kotlin
package com.bkahlert.kommons.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JvmKClassesKtTest {

    @Test fun best_name() = testAll {
        val result = String::class.bestName()
        result shouldBe "kotlin.String"
    }

    @Test fun best_name_of_anonymous_object() = testAll {
        val anonymous = object {}
        val result = anonymous::class.bestName()
        result shouldBe anonymous.javaClass.name
    }
}
```

- [ ] **Step 2: Point the two call sites at the new helper and run the tests to see them fail**

In `rootCause.kt` and `DynamicTestBuilder.kt` delete the line

```kotlin
import io.kotest.mpp.bestName
```

(`bestName` resolves within the package `com.bkahlert.kommons.test` once Step 3 exists; `DynamicTestBuilder.kt` is in `com.bkahlert.kommons.test.junit` and needs `import com.bkahlert.kommons.test.bestName` instead.)

```bash
./gradlew :kommons-test:compileTestKotlinJvm -PosArchOnly=true 2>&1 | grep -E "^e:|BUILD" | head -5
```

Expected: `BUILD FAILED` with `e: … Unresolved reference 'bestName'` (in `rootCause.kt`, `DynamicTestBuilder.kt` and the two tests). The failure is the RED step: the helper does not exist yet.

- [ ] **Step 3: Implement the helper**

Create `kommons-test/src/commonMain/kotlin/com/bkahlert/kommons/test/KClasses.kt`:

```kotlin
package com.bkahlert.kommons.test

import kotlin.reflect.KClass

/** Returns the most descriptive name this platform has for this class: qualified where available, simple otherwise. */
internal expect fun KClass<*>.bestName(): String
```

Create `kommons-test/src/jvmMain/kotlin/com/bkahlert/kommons/test/KClasses.kt`:

```kotlin
package com.bkahlert.kommons.test

import kotlin.reflect.KClass

internal actual fun KClass<*>.bestName(): String = qualifiedName ?: java.name
```

Create `kommons-test/src/jsMain/kotlin/com/bkahlert/kommons/test/KClasses.kt`:

```kotlin
package com.bkahlert.kommons.test

import kotlin.reflect.KClass

// KClass.qualifiedName is unsupported on Kotlin/JS and throws.
internal actual fun KClass<*>.bestName(): String = simpleName ?: toString()
```

Create `kommons-test/src/nativeMain/kotlin/com/bkahlert/kommons/test/KClasses.kt`:

```kotlin
package com.bkahlert.kommons.test

import kotlin.reflect.KClass

internal actual fun KClass<*>.bestName(): String = qualifiedName ?: simpleName ?: toString()
```

- [ ] **Step 4: Run the tests on every host target**

```bash
./gradlew :kommons-test:jvmTest --tests 'com.bkahlert.kommons.test.KClassesKtTest' --tests 'com.bkahlert.kommons.test.JvmKClassesKtTest' -PosArchOnly=true 2>&1 | grep -E "^e:|tests completed|BUILD"
./gradlew :kommons-test:jsNodeTest --tests 'com.bkahlert.kommons.test.KClassesKtTest' -PosArchOnly=true 2>&1 | grep -E "^e:|BUILD"
./gradlew :kommons-test:macosArm64Test --tests 'com.bkahlert.kommons.test.KClassesKtTest' -PosArchOnly=true 2>&1 | grep -E "^e:|BUILD"
```

Expected: three times `BUILD SUCCESSFUL`. kommons-test's test compilation also proves kommons-uri compiles against Ktor 3.6 and kommons-text against ICU4J 77.1 (both are dependencies). A Ktor compile error here is triaged per symbol: `io.ktor.utils.io.core.toByteArray` exists in 3.6.0 (`ktor-io/common/src/io/ktor/utils/io/core/Strings.kt`); any other missing symbol gets the nearest Ktor 3 replacement and a ledger ruling.

- [ ] **Step 5: Commit**

```bash
git add kommons-test/src
git commit -m "refactor(test): replace kotest's bestName with an own helper

io.kotest.mpp.bestName lives in kotest-common, an internal artifact.
The expect/actual helper keeps Kotest's per-platform behaviour:
qualified names on JVM and native, simple names on JS."
```

---

### Task 6: Whole build, Yarn lock, test suite

**Files:**
- Modify: `kotlin-js-store/yarn.lock` (regenerated)
- Modify: whatever the test run shows, under `kommons-*/src/**/*.kt`

**Interfaces:**
- Consumes: everything from Tasks 1 to 5.

- [ ] **Step 1: Compile every module for the host targets**

```bash
./gradlew assemble -PosArchOnly=true 2>&1 | tee "$SCRATCH/pr3-assemble.log" | grep -E "^e:|BUILD"
```

Expected: `BUILD SUCCESSFUL`, no `e:` lines. This covers kommons-uri (Ktor 3.6), kommons-text jvm (ICU4J 77.1), kommons-exec (plexus-utils 3.6.2) and the Spring modules (JUnit 5.14 through kommons-test).

- [ ] **Step 2: Regenerate the Yarn lock**

```bash
./gradlew kotlinUpgradeYarnLock 2>&1 | grep -E "BUILD|warning"
git diff --stat kotlin-js-store/yarn.lock
grep -E '^"?(xregexp|@stdlib/string-next-grapheme-cluster-break)@' kotlin-js-store/yarn.lock
```

Expected: `BUILD SUCCESSFUL`, the lock file changed, and the two entries name versions 5.1.3 and 0.2.3.

- [ ] **Step 3: Run the host test suite**

```bash
./gradlew allTests test -PosArchOnly=true -x jsBrowserTest --continue > "$SCRATCH/pr3-tests.log" 2>&1; echo "exit=$?"
grep -E "FAILED|BUILD" "$SCRATCH/pr3-tests.log" | head -40
```

Tally the JVM results from the JUnit XML (same method as PR 1):

```bash
find . -path '*/build/test-results/jvmTest/*.xml' -o -path '*/build/test-results/test/*.xml' | xargs grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' | awk -F'"' '{t+=$2; s+=$4; f+=$6; e+=$8} END {print "tests="t" skipped="s" failures="f" errors="e}'
```

Expected after fixes: `BUILD SUCCESSFUL`, at least 975 tests (971 plus the four new ones), `failures=0 errors=0`; `jsNodeTest` and `macosArm64Test` green for every module.

- [ ] **Step 4: SLF4J binding and kotlin-logging startup output**

```bash
grep -c "No SLF4J providers were found\|Failed to load class \"org.slf4j.impl.StaticLoggerBinder\"" "$SCRATCH/pr3-tests.log"
grep -m3 -i "kotlin-logging\|KotlinLogging" "$SCRATCH/pr3-tests.log"
```

Expected: `0`. The second grep may show kotlin-logging 8's startup line (it names the logger factory it picked); that is informational and goes into the PR description for the PR 5 CHANGELOG.

- [ ] **Step 5: Triage failures**

For each failing test decide, in this order:

1. The test asserted behaviour of the old library version (kotlinx-datetime formatting or parsing, Kotest message wording, Ktor encoding details, kotlin-logging names): adjust the expectation and say so in the commit body.
2. Production behaviour changed with the library and the test is right: fix the production code so the existing test passes.
3. A module trips Gradle's "no tests discovered" check because a Kotest or JUnit bump changed discovery: add the missing engine to that module's test source set; never disable the check.

Anything that does not fit goes into the PR description under "needs a decision".

- [ ] **Step 6: Zero deprecations**

```bash
./gradlew help --warning-mode all 2>&1 | grep -ciE "deprecat"
```

Expected: `0`.

- [ ] **Step 7: Commits**

```bash
git add kotlin-js-store/yarn.lock
git commit -m "build: regenerate the yarn lock for the npm bumps"
```

Then, if the triage changed tests:

```bash
git add -A -- ':(glob)kommons*/src/**/*.kt'
git commit -m "test: adapt tests to the upgraded libraries"
```

Production fixes from triage rule 2 get their own `fix(scope):` commit each, with the failing test named in the body.

---

### Task 7: linuxArm64 target

**Files:**
- Modify: `buildSrc/src/main/kotlin/kommons-native-conventions.gradle.kts:21-30`
- Modify: `.github/workflows/build.yml` (matrix row and run step)

**Interfaces:**
- Produces: Kotlin/Native target `linuxArm64` in every module applying `kommons-native-conventions`; Gradle tasks `linuxArm64TestBinaries`, `compileKotlinLinuxArm64`, publication `linuxArm64`. The release job (PR 2) picks the new publication up without changes.

- [ ] **Step 1: Register the target**

In `kommons-native-conventions.gradle.kts` replace

```kotlin
    } else {
        // linuxArm64 (spec decision 9) follows with the dependency bumps: Ktor 2.2.3, Mordant 2.0.0-beta9 and
        // Kotest 5.5.4 publish no linuxArm64 variants, so the target cannot compile before PR 3.
        linuxX64()
        mingwX64()
```

with

```kotlin
    } else {
        linuxX64()
        // Kotlin/Native has no Linux ARM64 host: CI cross-compiles and links this target but never runs its tests.
        linuxArm64()
        mingwX64()
```

The source-set opt-in prefixes (`linux`) already cover `linuxArm64Main`/`linuxArm64Test`.

- [ ] **Step 2: CI**

In `.github/workflows/build.yml` replace

```yaml
          - { os: ubuntu-latest, target: linuxX64 }
```

with

```yaml
          # linuxArm64 has no CI host; the linuxX64 job cross-compiles and links its test binaries without running them.
          - { os: ubuntu-latest, target: linuxX64, extra-tasks: linuxArm64TestBinaries }
```

and in the run step add a line after `${{ matrix.target == 'jvm' && 'test' || '' }}`:

```yaml
          ${{ matrix.extra-tasks || '' }}
```

so the step reads:

```yaml
        run: >
          ./gradlew ${{ matrix.target }}Test
          ${{ matrix.target == 'jvm' && 'test' || '' }}
          ${{ matrix.extra-tasks || '' }}
          ${{ matrix.test-jdk && format('-PtestJdk={0}', matrix.test-jdk) || '' }}
          -Pkotlin.tests.individualTaskReports=true
          ${{ github.event.inputs.additional-gradle-args }}
```

- [ ] **Step 3: Lint**

```bash
actionlint .github/workflows/build.yml && echo "actionlint: no findings"
```

Expected: `actionlint: no findings`.

- [ ] **Step 4: Cross-compile and link linuxArm64 on this host**

```bash
./gradlew linuxArm64TestBinaries 2>&1 | tee "$SCRATCH/pr3-linuxarm64.log" | grep -E "^e:|BUILD"
ls kommons-core/build/bin/linuxArm64/debugTest/
```

Expected: `BUILD SUCCESSFUL` (the first run downloads the Linux ARM64 toolchain) and a `test.kexe` in the directory.

- [ ] **Step 5: Every native module produces a linuxArm64 klib**

```bash
./gradlew assemble 2>&1 | grep -E "^e:|BUILD"
find . -path '*/build/classes/kotlin/linuxArm64/main/klib/*.klib' -not -path '*/buildSrc/*' | sort
```

Expected: `BUILD SUCCESSFUL` and seven klibs: `kommons`, `kommons-core`, `kommons-kaomoji`, `kommons-test`, `kommons-text`, `kommons-time`, `kommons-uri`.

- [ ] **Step 6: Commits**

```bash
git add buildSrc/src/main/kotlin/kommons-native-conventions.gradle.kts
git commit -m "build: add the linuxarm64 target

Ktor 3.6, Mordant 3.1 and Kotest 5.9.1 publish linuxArm64, which the
previous versions did not (spec decision 9)."
git add .github/workflows/build.yml
git commit -m "ci: link the linuxarm64 test binaries on the linuxx64 job

Kotlin/Native has no Linux ARM64 host, so the binaries are built and
linked but never run."
```

---

### Task 8: Pull request

**Files:** none modified.

- [ ] **Step 1: Final local check**

```bash
./gradlew help --warning-mode all 2>&1 | grep -ci "deprecat"
git log --oneline origin/master..HEAD
```

Expected: `0`; commits for the plan, the catalog, time, logging, Mordant, bestName, the Yarn lock, tests (if any), the target and CI.

- [ ] **Step 2: Push and open the PR**

```bash
git push -u origin build/library-dependencies
gh pr create --base master --title "build: upgrade library dependencies to current versions" --body-file - <<'PR'
Implements PR 3 of docs/superpowers/specs/2026-10-02-modernization-design.md.

## What changes
- kotlinx-datetime 0.8.0, kotlinx-serialization 1.11.0, Ktor 3.6.0, Mordant 3.1.0, kotlin-logging 8.0.4 (`io.github.oshai`), Kotest 5.9.1, JUnit 5.14.4 / Platform 1.14.4, ICU4J 77.1, plexus-utils 3.6.2, npm xregexp 5.1.3 and @stdlib/string-next-grapheme-cluster-break 0.2.3.
- kommons-time: `Instant` and `Clock` are `kotlin.time`'s (the intended 3.0 API break); `dayOfMonth`/`monthNumber` become `day`/`month.number`.
- kommons-logging-core: `by KotlinLogging` on `io.github.oshai.kotlinlogging`.
- kommons-test: own `bestName()` (expect/actual) instead of Kotest's internal `kotest-common`.
- Mordant 3: `Terminal.updateSize()`.
- linuxArm64 registered; the linuxX64 CI job links its test binaries (`linuxArm64TestBinaries`).

## Verified locally (Apple silicon, JDK 17 build)
- `assemble -PosArchOnly=true` and the full `assemble` (five native targets, seven linuxArm64 klibs) green.
- `allTests test -PosArchOnly=true -x jsBrowserTest`: <N> JVM tests / 0 failures, Node and macosArm64 tests green.
- `linuxArm64TestBinaries` links on macOS.
- No `No SLF4J providers were found` in the test log; `help --warning-mode all` prints no deprecation lines.
- Every bumped JVM jar is Java 8 bytecode (class version 52, checked on Maven Central).

## Deviations from the spec
- SLF4J stays at 1.7.36 until PR 4 brings Logback 1.5 with Spring Boot 4: Logback 1.2 has no SLF4J 2 provider, so SLF4J 2.0 now would bind every Logback-backed module to the NOP logger.
- `bestName()` is an `internal expect fun` with JVM, JS and native actuals (JS has no `qualifiedName`).

## CHANGELOG notes for PR 5
- `kotlin.time.Instant`/`Clock` in kommons-time; kotlin-logging package `io.github.oshai.kotlinlogging`; kotlin-logging 8 prints a startup line naming its logger factory and logs only through lambdas.
- Mordant 3.1.0 is an `api` dependency of kommons-text (native) and kommons-kaomoji: consumers move from 2.0.0-beta9.
- `kotest-common` is no longer a transitive dependency of kommons-test.
- linuxArm64 added.

## Needs a decision
<list or "none">

BREAKING CHANGE: kommons-time uses kotlin.time.Instant and
kotlin.time.Clock; the by KotlinLogging delegate comes from
io.github.oshai.kotlinlogging.
PR
```

Fill in the test count and the "Needs a decision" section before submitting.

- [ ] **Step 3: Wait for CI**

```bash
gh pr checks --watch
```

Expected: all nine `build.yml` jobs green, including the browser tests in the `js` job and the linuxArm64 link on the linuxX64 job. A red job is fixed on the branch with a commit of the matching type.

- [ ] **Step 4: Merge and clean up**

```bash
gh pr merge --rebase
```

Then remove the worktree (`ExitWorktree` with `remove`; the rebased commits are on master under new SHAs, so confirm the discard) and prune.
