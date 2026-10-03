# Changelog

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

- kommons-logging-logback, kommons-logging-spring-boot, kommons-logging-spring-boot-starter and the Spring Boot
  sample. Spring Boot 4 provides their features: `logging.structured.format.console` (or `.file`) `=logstash` (or
  `ecs`, `gelf`) replaces the `json` preset, `logging.file.name` the file log, Boot's default layout the `spring`
  preset, a `logging.pattern.console` the `minimal` preset, and SLF4J's fluent `addKeyValue` the
  `StructuredArguments`. An application that keeps the `logging.preset.*` properties gets Boot's default logging
  without an error. logstash-logback-encoder leaves the dependency set; outside Spring Boot, a `logback.xml` of your
  own replaces the shipped configuration, with logstash-logback-encoder for JSON.

### Fixed

- kommons-logging-core on Kotlin/JS: a logger delegated inside a method was named `protoOf` on V8 (Chrome, Node),
  because Kotlin 2 emits methods as `protoOf(C).m = function () {}` and V8's `stack` string names such a frame
  `protoOf.m`. The name is read from V8's structured stack trace now and is the class again. The 3.0.0 note that V8
  yields the class held only under source-map-support, which the test runner loads.
- kommons-time: `LocalDate.toMomentString` returned the neighbouring date for a date 30 or more days away whose UTC
  offset differs from today's (daylight saving time, historical offset changes); it now formats the date itself.
- kommons-time: `Instant.toLocalDateString` on the JVM used the default time zone of its first call for the rest of the
  process; it now reads the default time zone on every call.
- kommons-exec: `Process.pid` was `null` on Java 16 and later, where the JDK denies reflective access to its process
  implementation; it now calls the public `Process.pid()` and keeps reading Java 8's private field.


## [3.0.0] - 2026-10-03

Kommons 3.0 moves the build, the dependencies and the publishing to current versions. The library API changes in two
places, kommons-time's `Instant`/`Clock` and kommons-logging-core's `KotlinLogging` package; everything else is a
dependency, platform floor or output-format change. Consumers on Spring Boot 2.x, SLF4J 1.7 or Ktor 2 cannot upgrade
without moving first; see "Changed".

### Changed

- JVM bytecode floors: Java 8 for all libraries, Java 11 for kommons-uri (as before), Java 17 for the Spring stack
  (kommons-logging-logback, kommons-logging-spring-boot, kommons-logging-spring-boot-starter).
- kommons-time: `Instant` and `Clock` are `kotlin.time.Instant` and `kotlin.time.Clock` (kotlinx-datetime 0.8, an `api`
  dependency). `InstantAsEpochMillisecondsSerializer`, `InstantAsEpochSecondsSerializer` and the `Clock.Companion`
  extensions target them. kotlinx-datetime 0.8 also renames `LocalDate.dayOfMonth`/`monthNumber` to `day`/`month.number`
  in your own code.
- kommons-logging-core: the `by KotlinLogging` delegate comes from `io.github.oshai.kotlinlogging` (kotlin-logging 8,
  lambda-only API) instead of `mu`.
- SLF4J 2.0.20: consumers need an SLF4J 2 provider, for example Logback 1.3 or later (the last Java 8 line). Logback 1.2
  binds to the NOP logger, and Spring Boot 2.7 fails on `StaticLoggerBinder`.
- The Spring stack requires Spring Boot 4 (Spring Framework 7; built and tested against 4.1.1) and Java 17. The
  auto-configuration is registered
  through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and the environment
  post-processor through the `org.springframework.boot.EnvironmentPostProcessor` key, so a Spring Boot 2.x application
  gets neither: no error, Boot's default logging instead of the presets and the JSON file.
- kommons-logging-logback's JSON preset uses logstash-logback-encoder 9 on Jackson 3 (package `tools.jackson`) and
  Logback 1.5. Custom `StructuredArgument` implementations port `writeTo` to `tools.jackson.core.JsonGenerator`; JSON
  `@timestamp` values carry microseconds. `Logback.clearSystemProperties()` clears Spring Boot 4's
  `LoggingSystemProperty` and `RollingPolicySystemProperty` names. The spring console preset follows Boot 4's layout
  (ISO-8601 timestamp with offset, application name in brackets when set).
- kommons-test: `KommonsTestDisplayNameGenerator` needs JUnit 5.12 or later. kommons-test is tested on JUnit 5.14 and
  exercised on JUnit 6 by the Spring modules' test suites.
- kommons-text (native) and kommons-kaomoji: Mordant 3.1 as an `api` dependency (from 2.0.0-beta9). On the JVM Mordant 3
  brings `mordant-jvm-jna` (JNA 5.19.1), `mordant-jvm-ffm` and `mordant-jvm-graal-ffi`.
- kommons-uri, and kommons-test which exposes it: Ktor 3.6 (`ktor-http`, `ktor-utils`) as an `api` dependency (from
  2.2.3). Applications on Ktor 2 move to Ktor 3 first; Ktor 2 and Ktor 3 artifacts do not mix on one classpath.
- kommons-debug: Kotlin 2 compiles lambdas through `invokedynamic`, so un-annotated lambdas render as `Function`.
- kommons-logging-core on Kotlin/JS: in Firefox a logger delegated inside a method is named `<global>` (Firefox cannot
  name Kotlin 2 methods; V8 still yields the class).
- Native targets: linuxArm64 added (cross-compiled, not run in CI). macosX64 is deprecated by Kotlin/Native and stays
  published without tests.
- Publishing: Maven Central through the Central Portal, from one macOS job of the release workflow.
- Build: Gradle 9.7, Kotlin 2.4.20, Dokka 2.2, JDK 17 toolchain compiling down to the floors above.
  Consumers need Kotlin 2.4 or later; Kotlin 2.3 reads the artifacts but needs `@OptIn(ExperimentalTime::class)` for
  `kotlin.time.Instant`.
- Dependencies: kotlinx-datetime 0.8.0, kotlinx-serialization 1.11.0, Ktor 3.6.0, Mordant 3.1.0, kotlin-logging 8.0.4,
  SLF4J 2.0.20, Logback 1.5.38 (Spring Boot managed), logstash-logback-encoder 9.0, Spring Boot 4.1.1, JUnit 5.14.4,
  Kotest 5.9.1, ICU4J 77.1, plexus-utils 3.6.2, npm xregexp 5.1.3 and @stdlib/string-next-grapheme-cluster-break 0.2.3.

### Removed

- GitHub Packages publishing.
- `kotest-common` as a direct dependency of kommons-test (Kotest's assertion modules still pull it in).
- `io.spring.dependency-management` from the Spring modules' builds; Gradle's `platform(...)` manages the Spring Boot
  BOM.


## [2.8.0] - 2023-03-05

### Added

- Uri.resolve / Uri.resolveTo
    - Example
      ```kotlin
      Uri("http://a/b/c/d;p?q").resolve("../g")
      // returns
      Uri("http://a/b/g")
      ```
- DataUri factory for textual data

### Changed

- upgrade to
    - Kotlin 1.8.10
    - Ktor 2.2.3
    - Kotlinx.serialization to 1.5.0
- moved time-related feature to separate kommons-time module

### Fixed

- `toMomentString` for negative dates

## [2.7.0] - 2023-02-09

### Changed

- cleanup dependencies
- change implementation dependency of SLF4J to api dependency
- kommons-uri:
    - parse and serialize [Uniform Resource Identifiers (RFC3986)](https://www.rfc-editor.org/rfc/rfc3986)
    - support for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
    - support for [data URIs (RFC2397)](https://www.rfc-editor.org/rfc/rfc2397) with an
      appropriate [URL stream handler provider](https://docs.oracle.com/javase/9/docs/api/java/net/spi/URLStreamHandlerProvider.html)
      registered on the JVM

### Fixed

- logger name computation in Firefox

## [2.6.0] - 2023-01-14

### Added

- Kommons Bill of Materials
- Clock builder
- Clock.fixed

### Changed

- Upgrade to Kotlin 1.8.0

- Kommons Test's dynamic test builders `testing`/`testingAll` now lazily
  build tests as they're consumed by JUnit

### Fixed

- Add ValueRange.toString

## [2.5.0] - 2022-12-14

### Added

#### Kotlin-specific structured logging

```kotlin
data class Bar(val bar: Int) {
    override fun toString(): String = "bar-$bar"
}

logger.info("Successfully created {}", array(Bar(1), Bar(2)))
```

```json
{
    "@timestamp": "2022-12-14T14:51:57.583+01:00",
    "level": "INFO",
    "message": "Successfully created bars=[bar-1, bar-2]",
    "bars": [
        {
            "bar": 1
        },
        {
            "bar": 2
        }
    ]
}
```

### Changed

- chore: upgrade to Kotlin 1.7.21

## [2.4.1] - 2022-12-11

### Changed

- chore: upgrade to Gradle 7.6

### Fixed

- fix: add back `js.Date` extension functions

## [2.4.0] - 2022-12-09

### Added

- Native for ...
    - Linux x64
    - MinGW x64
    - macOS x64
    - macOS ARM 64
- and the following modules ...
    - Kommons Core
    - Kommons Kaomoji
    - Kommons Test
    - Kommons Text

## [2.3.1] - 2022-10-26

### Fixed

- catch InaccessibleObjectException when attempting to compute PID

## [2.3.0] - 2022-10-22

### Added

- Kotest JSON assertions API dependency
- CommandLine constructor to invoke the main method of class using Java
-

### Fixed

- Fix SyncExecutor to always empty and close the output and error stream

## [2.2.0] - 2022-10-11

### Added

- add [Japanese style emoticon](https://en.wikipedia.org/wiki/Emoticon#Japanese_style) constants such as `(つ◕౪◕)つ━☆ﾟ.*･｡ﾟ`
- add [kommons-exec](kommons-exec)

### Removed

- removed `kommons-exec-deprecated`

## [2.1.0] - 2022-09-08

### Added

- simple `pluralize()` extension function
- `Kommons Logging: Core`: get Logback or Kotlin Logger logger easily
- `Kommons Logging: Logback`: configure logging using system properties `CONSOLE_LOG_PRESET` and `FILE_LOG_PRESET`
- `Kommons Logging: Spring Boot`: configure logging using application properties `logging.preset.console` and `logging.preset.file`

### Changed

- Set `junit.jupiter.execution.parallel.config.dynamic.factor` to 2.
- Display "PascalCaseNestedTests" as "pascal case nested tests".

### Fixed

- Set JS test timeout to same as JUnit tests.

## [2.0.0] - 2022-08-15

### Changed

- migrated Kommons 1.x.x to this Gradle multi-project
    - [Kommons 1.x.x Changelog](https://github.com/bkahlert/kommons/compare/v1.0.0...v1.6.0)
- migrated Kommons Debug 0.x.x to this Gradle multi-project
    - [Kommons Debug 0.x.x Changelog](https://github.com/bkahlert/kommons-debug/compare/v0.1.0...v0.14.0)
- migrated Kommons Test 0.x.x to this Gradle multi-project
    - [Kommons Test 0.x.x Changelog](https://github.com/bkahlert/kommons-test/compare/v0.1.0...v0.4.4)

[unreleased]: https://github.com/bkahlert/kommons/compare/v3.0.0...HEAD

[3.0.0]: https://github.com/bkahlert/kommons/compare/v2.8.0...v3.0.0

[2.8.0]: https://github.com/bkahlert/kommons/compare/v2.7.0...v2.8.0

[2.7.0]: https://github.com/bkahlert/kommons/compare/v2.6.0...v2.7.0

[2.6.0]: https://github.com/bkahlert/kommons/compare/v2.5.0...v2.6.0

[2.5.0]: https://github.com/bkahlert/kommons/compare/v2.4.1...v2.5.0

[2.4.1]: https://github.com/bkahlert/kommons/compare/v2.4.0...v2.4.1

[2.4.0]: https://github.com/bkahlert/kommons/compare/v2.3.1...v2.4.0

[2.3.1]: https://github.com/bkahlert/kommons/compare/v2.3.0...v2.3.1

[2.3.0]: https://github.com/bkahlert/kommons/compare/v2.2.0...v2.3.0

[2.2.0]: https://github.com/bkahlert/kommons/compare/v2.1.0...v2.2.0

[2.1.0]: https://github.com/bkahlert/kommons/compare/v2.0.0...v2.1.0

[2.0.0]: https://github.com/bkahlert/kommons/compare/v1.0.0...v2.0.0
