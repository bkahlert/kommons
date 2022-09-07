# Changelog

## [Unreleased]

### Added

- simple `pluralize()` extension function
- `Kommons Logging: Core`: get Logback or Kotlin Logger logger easily
- `Kommons Logging: Logback`: configure logging using system properties `CONSOLE_LOG_PRESET` and `FILE_LOG_PRESET`
- `Kommons Logging: Spring Boot`: configure logging using application properties `logging.preset.console` and `logging.preset.file`

### Changed

- Set `junit.jupiter.execution.parallel.config.dynamic.factor` to 2.
- Display "PascalCaseNestedTests" as "pascal case nested tests".

### Deprecated

*none*

### Removed

*none*

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

[unreleased]: https://github.com/bkahlert/kommons-test/compare/v2.0.0...HEAD

[2.0.0]: https://github.com/bkahlert/kommons-test/compare/v1.0.0...v2.0.0
