# Changelog

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

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

[unreleased]: https://github.com/bkahlert/kommons-test/compare/v2.4.0...HEAD

[2.4.0]: https://github.com/bkahlert/kommons-test/compare/v2.3.1...v2.4.0

[2.3.1]: https://github.com/bkahlert/kommons-test/compare/v2.3.0...v2.3.1

[2.3.0]: https://github.com/bkahlert/kommons-test/compare/v2.2.0...v2.3.0

[2.2.0]: https://github.com/bkahlert/kommons-test/compare/v2.1.0...v2.2.0

[2.1.0]: https://github.com/bkahlert/kommons-test/compare/v2.0.0...v2.1.0

[2.0.0]: https://github.com/bkahlert/kommons-test/compare/v1.0.0...v2.0.0
