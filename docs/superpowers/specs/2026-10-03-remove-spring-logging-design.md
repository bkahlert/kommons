# Removal of the Logback and Spring Boot logging modules

Design for removing `kommons-logging-logback`, `kommons-logging-spring-boot`, `kommons-logging-spring-boot-starter` and
`kommons-logging-spring-boot-sample` from Kommons. `kommons-logging-core` stays.

Date: 2026-10-03. Status: design approved, implementation plan pending. The change ships as a minor version, not a
major one: the easy configuration of JSON logging has become Spring Boot functionality, so the removal takes nothing
away that Boot 4 does not provide. It lands on `main` under `[Unreleased]`.

## Why

- Spring Boot 4, which the modules require since 3.0.0, ships what they provided: structured JSON logging
  (`logging.structured.format.console=logstash`), file logging (`logging.file.name`) and key-value pairs through
  SLF4J's fluent API. Boot's formatter does not render logstash-logback-encoder's `StructuredArgument`, so the `json`
  preset and Boot's structured logging overlap without composing, and an application that sets Boot's property under
  kommons gets nothing, because kommons replaces Boot's Logback configuration.
- The modules carry the Spring Boot BOM, kapt with the configuration processor, three test suites that share
  `${java.io.tmpdir}/kommons.log` across test JVMs, and a `console: off` YAML value that Boot reads as `false`.

## Decisions

| # | Decision | Rationale |
|---|---|---|
| 1 | The four modules are deleted outright, with no deprecation release in between. | Their users had to move to Boot 4 for 3.0.0 and have the replacement built in. The release decision is separate from the change. |
| 2 | `kommons-logging-core` and the `kommons-logging` parent directory stay as they are. | Artifact coordinates and the `kommons` aggregate are unchanged; flattening the directory would churn history for no effect. |
| 3 | `logback-classic` stays in the catalog, at its current version, as the test-time SLF4J provider of `kommons-exec` and `kommons-logging-core`. | Nothing else provides SLF4J in those test JVMs. The version is no longer tied to a Boot BOM. |
| 4 | The CI matrix keeps its JDK 21 and 25 rows. | They were added for Boot 4 but exercise every library on current JDKs. |
| 5 | Released CHANGELOG sections, the modernization spec and its plans are not edited. | They are dated records. |

## Scope

### Deleted

- `kommons-logging/kommons-logging-logback`
- `kommons-logging/kommons-logging-spring-boot`
- `kommons-logging/kommons-logging-spring-boot-starter`
- `kommons-logging/kommons-logging-spring-boot-sample`

### Edited

- [settings.gradle.kts](../../../settings.gradle.kts): the four `include` lines.
- [gradle/libs.versions.toml](../../../gradle/libs.versions.toml): versions `logstash-logback-encoder` and
  `spring-boot`; libraries `logstash-logback-encoder` and `spring-boot-bom`; plugins `kotlin-spring` and `spring-boot`.
- [.github/workflows/build.yml](../../../.github/workflows/build.yml): the jvm jobs stop invoking the `test` task.
  The sample was the only project with one; with it gone, `./gradlew jvmTest test` fails on the unknown task.
- [kommons-logging/README.md](../../../kommons-logging/README.md): the family lists only `kommons-logging-core`; the
  sample sentence goes.
- [README.md](../../../README.md): the Kommons Logging bullet loses the aside about `logging-core` being the only
  module included by default and becomes bold, since the one remaining module is part of the `kommons` aggregate.
- [CHANGELOG.md](../../../CHANGELOG.md): the entry below under `[Unreleased]` / `Removed`.

### Unchanged

- `buildSrc`: `kommons-test-jdk-conventions` is applied by `kommons-jvm-conventions`, not only by the sample.
- `kommons-bom` derives its constraints from the project list; `kommons` depends on `kommons-logging-core` only.
- `kommons-logging-core`, including its README: the Spring Boot application there illustrates logger naming.
- `release.yml`, `build-custom.yml`, `test-report.yml`.

## CHANGELOG entry

Under `[Unreleased]`, `### Removed`:

> - kommons-logging-logback, kommons-logging-spring-boot, kommons-logging-spring-boot-starter and the Spring Boot
>   sample. Spring Boot 4 provides their features: `logging.structured.format.console` (or `.file`) `=logstash` (or
>   `ecs`, `gelf`) replaces the `json` preset, `logging.file.name` the file log, Boot's default layout the `spring`
>   preset, a `logging.pattern.console` the `minimal` preset, and SLF4J's fluent `addKeyValue` the
>   `StructuredArguments`. An application that keeps the `logging.preset.*` properties gets Boot's default logging
>   without an error. logstash-logback-encoder leaves the dependency set; outside Spring Boot, a `logback.xml` of your
>   own replaces the shipped configuration, with logstash-logback-encoder for JSON.

## Verification

- `./gradlew assemble -PosArchOnly=true` and `./gradlew jvmTest -PosArchOnly=true`: green.
- `./gradlew help --warning-mode all`: no deprecation output.
- `./gradlew :kommons-bom:generatePomFileForMavenPublication -PosArchOnly=true` and `grep artifactId kommons-bom/build/publications/maven/pom-default.xml`: the constraints name the surviving modules only, no removed artifact.
- `grep -rn -i spring --include='*.kt' --include='*.kts' --include='*.toml' --include='*.yml' --include='*.md' .`
  outside `build/`, `docs/superpowers` and the released CHANGELOG sections hits only the `kommons-logging-core`
  README example.
- The full CI matrix green on the pull request.

## Delivery

One branch, `feat/remove-spring-logging`, one commit:

```
feat(logging): remove the logback and spring boot modules

Spring Boot 4 provides what the four modules did: structured JSON
logging, file logging and key-value pairs through SLF4J's fluent API.
Boot's formatter does not render logstash-logback-encoder's structured
arguments, so the json preset and Boot's structured logging overlapped
without composing. kommons-logging-core stays.
```

No `!` and no `BREAKING CHANGE` footer: the removal ships as a minor version (see Status above). The pull request
title is the commit header; its description records that reasoning.
