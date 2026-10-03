# Removal of the Logback and Spring Boot logging modules: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete `kommons-logging-logback`, `kommons-logging-spring-boot`, `kommons-logging-spring-boot-starter` and `kommons-logging-spring-boot-sample` with every build, CI and documentation reference to them, leaving `kommons-logging-core` and the rest of the build green.

**Architecture:** A pure removal. The four module directories go, together with their `include` lines, their catalog entries, the kapt property only they used, and the CI invocation of the sample's `test` task. `kommons-bom` derives its constraints from the project list and the `kommons` aggregate depends on `kommons-logging-core` only, so neither changes.

**Tech Stack:** Gradle 9.7 with the configuration cache, Kotlin 2.4 multiplatform, GitHub Actions. Gradle needs `JAVA_HOME` pointing at a JDK 17 (`/Users/bkahlert/Library/Java/JavaVirtualMachines/corretto-17.0.10/Contents/Home` on the author's machine); the shell default is JDK 8 and fails with "Gradle requires JVM 17 or later". Run one Gradle build at a time and always pass `-PosArchOnly=true`.

**Spec:** [docs/superpowers/specs/2026-10-03-remove-spring-logging-design.md](../specs/2026-10-03-remove-spring-logging-design.md)

## Global Constraints

- `kommons-logging-core`, the `kommons-logging` parent directory, `buildSrc`, `kommons-bom`, `kommons`, `release.yml`, `build-custom.yml` and `test-report.yml` are not edited.
- `logback-classic` stays in the catalog at `1.5.38`; `kommons-exec` and `kommons-logging-core` keep it as a test dependency.
- The CI matrix keeps its JDK 17, 21 and 25 rows.
- Released CHANGELOG sections (`[3.0.0]` and older), the modernization spec and its plans are not edited.
- Commits follow Conventional Commits, header at most 72 characters, no AI attribution trailers. The removal is one commit; it ships as a minor version, so the header carries no `!` and there is no `BREAKING CHANGE` footer.
- Never commit on `main`; the work happens on `feat/remove-spring-logging`.

## Review Focus

1. A jvm CI job runs `./gradlew jvmTest test`; with the sample gone no project has a `test` task and Gradle fails on the unknown task. Task 2 removes the `test` argument from `build.yml`; Task 3 confirms it on the pull request's CI.
2. `kommons-logging-core` and `kommons-exec` run their JVM tests with `logback-classic` as the only SLF4J provider; if the catalog entry went too, the test log would show `No SLF4J providers were found`. Task 2 keeps the entry and greps the test output.
3. `kommons-bom` builds its constraints from every subproject with a `publish` task; a stale reference would appear as a removed artifact in the generated POM (`:kommons-bom:generatePomFileForMavenPublication`, then `grep artifactId` on `kommons-bom/build/publications/maven/pom-default.xml`). Task 2 checks the list.
4. A leftover catalog alias (`libs.spring.boot.bom`, `libs.plugins.spring.boot`, `libs.plugins.kotlin.spring`, `libs.logstash.logback.encoder`) referenced from a surviving build file fails configuration. Task 2 runs `help --warning-mode all` first, which configures every project.
5. A Markdown link to a deleted directory. Task 2 greps the surviving Markdown for the four module names.

---

### Task 1: Branch and design documents

**Files:**
- Create: `docs/superpowers/plans/2026-10-03-remove-spring-logging.md` (this file, already written)
- Create: `docs/superpowers/specs/2026-10-03-remove-spring-logging-design.md` (already written, untracked)

- [ ] **Step 1: Create the branch from main**

Run from the worktree root:

```bash
git fetch origin main
git switch -c feat/remove-spring-logging origin/main
git status --short
```

Expected: the two untracked design documents and nothing else.

- [ ] **Step 2: Commit the design documents**

```bash
git add docs/superpowers/specs/2026-10-03-remove-spring-logging-design.md docs/superpowers/plans/2026-10-03-remove-spring-logging.md
git commit -m "docs(spec): add the spring logging removal design and plan"
```

---

### Task 2: Remove the modules and every reference to them

**Files:**
- Delete: `kommons-logging/kommons-logging-logback/`, `kommons-logging/kommons-logging-spring-boot/`, `kommons-logging/kommons-logging-spring-boot-starter/`, `kommons-logging/kommons-logging-spring-boot-sample/`
- Modify: `settings.gradle.kts:28-31`
- Modify: `gradle/libs.versions.toml:11,19,40,57,75,76`
- Modify: `gradle.properties:1`
- Modify: `.github/workflows/build.yml:67`
- Modify: `kommons-logging/README.md:5-13`
- Modify: `README.md:15`
- Modify: `CHANGELOG.md:11-12`

**Interfaces:**
- Consumes: nothing from Task 1 beyond the branch.
- Produces: the commit Task 3 pushes.

- [ ] **Step 1: Delete the four module directories**

```bash
git rm -r -q kommons-logging/kommons-logging-logback kommons-logging/kommons-logging-spring-boot kommons-logging/kommons-logging-spring-boot-starter kommons-logging/kommons-logging-spring-boot-sample
ls kommons-logging
```

Expected: `README.md` and `kommons-logging-core` only. If `ls` shows a leftover `build` directory inside a deleted module, remove it with `rm -rf`; it is ignored by git.

- [ ] **Step 2: Drop the include lines**

In `settings.gradle.kts` delete these four lines, keeping `include("kommons-logging:kommons-logging-core")`:

```kotlin
include("kommons-logging:kommons-logging-logback")
include("kommons-logging:kommons-logging-spring-boot")
include("kommons-logging:kommons-logging-spring-boot-starter")
include("kommons-logging:kommons-logging-spring-boot-sample")
```

- [ ] **Step 3: Drop the catalog entries**

In `gradle/libs.versions.toml` delete these lines (and the blank line that would otherwise double):

```toml
logstash-logback-encoder = "9.0"
spring-boot = "4.1.1" # https://docs.spring.io/spring-boot/4.1/
logstash-logback-encoder = { module = "net.logstash.logback:logstash-logback-encoder", version.ref = "logstash-logback-encoder" }
spring-boot-bom = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }
kotlin-spring = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

`logback-classic = "1.5.38"` and its library entry stay. The `[plugins]` section keeps `kotlin-serialization`.

- [ ] **Step 4: Drop the kapt property**

`gradle.properties` line 1, `kapt.include.compile.classpath=false`, configured kapt, which only `kommons-logging-spring-boot` applied. Delete the line. The spec does not list this file; record the deviation in the pull request description.

- [ ] **Step 5: Stop invoking the test task in CI**

In `.github/workflows/build.yml` delete the line

```yaml
          ${{ matrix.target == 'jvm' && 'test' || '' }}
```

so the `run:` block reads:

```yaml
        run: >
          ./gradlew ${{ matrix.target }}Test
          ${{ matrix.extra-tasks || '' }}
          ${{ matrix.test-jdk && format('-PtestJdk={0}', matrix.test-jdk) || '' }}
          -Pkotlin.tests.individualTaskReports=true
          ${{ github.event.inputs.additional-gradle-args }}
```

Run `actionlint .github/workflows/build.yml` if `actionlint` is installed; expected: no output.

- [ ] **Step 6: Trim the logging family README**

Replace lines 5 to 13 of `kommons-logging/README.md` (from `**Kommons Logging** is a family` through the sample sentence) with:

```markdown
**Kommons Logging** is the home of the following Kotlin library:

- [Kommons Logging: Core](kommons-logging-core) … Convenience features for [Kotlin Logging](https://github.com/MicroUtils/kotlin-logging)
  and [SLF4J](https://www.slf4j.org/)
```

Keep the line break and indentation of the two-line bullet as it is today.

- [ ] **Step 7: Update the root README bullet**

In `README.md` replace

```markdown
- [Kommons Logging](kommons-logging) … for simple logging *(only **logging-core** included by default)*
```

with

```markdown
- **[Kommons Logging](kommons-logging) … for simple logging**
```

The bold marks modules included in the `kommons` aggregate; `kommons-logging-core` is.

- [ ] **Step 8: Add the CHANGELOG entry**

In `CHANGELOG.md`, under `## [Unreleased]`, replace the empty `### Removed` block with:

```markdown
### Removed

- kommons-logging-logback, kommons-logging-spring-boot, kommons-logging-spring-boot-starter and the Spring Boot
  sample. Spring Boot 4 provides their features: `logging.structured.format.console` (or `.file`) `=logstash` (or
  `ecs`, `gelf`) replaces the `json` preset, `logging.file.name` the file log, Boot's default layout the `spring`
  preset, a `logging.pattern.console` the `minimal` preset, and SLF4J's fluent `addKeyValue` the
  `StructuredArguments`. An application that keeps the `logging.preset.*` properties gets Boot's default logging
  without an error. logstash-logback-encoder leaves the dependency set; outside Spring Boot, a `logback.xml` of your
  own replaces the shipped configuration, with logstash-logback-encoder for JSON.
```

- [ ] **Step 9: Configure every project**

```bash
export JAVA_HOME=/Users/bkahlert/Library/Java/JavaVirtualMachines/corretto-17.0.10/Contents/Home
./gradlew help --warning-mode all -PosArchOnly=true 2>&1 | grep -ci 'deprecat'
```

Expected: `0`, and the build succeeds. A failure here names the surviving file that still references a removed alias or project.

- [ ] **Step 10: Build and test**

```bash
./gradlew assemble jvmTest -PosArchOnly=true 2>&1 | grep -E 'FAILED|BUILD|What went wrong' -A2
grep -rl 'No SLF4J providers were found' --include='*.xml' kommons-exec/build/test-results kommons-logging/kommons-logging-core/build/test-results; echo "slf4j provider check done"
```

Expected: `BUILD SUCCESSFUL`, no `FAILED` line, and the grep prints only `slf4j provider check done`.

- [ ] **Step 11: Check the BOM and the leftovers**

```bash
./gradlew :kommons-bom:generatePomFileForMavenPublication -PosArchOnly=true
grep artifactId kommons-bom/build/publications/maven/pom-default.xml
grep -rn 'kommons-logging-logback\|kommons-logging-spring-boot\|logstash\|spring-boot' --include='*.kts' --include='*.toml' --include='*.yml' --include='*.properties' --include='*.md' . 2>/dev/null | grep -v '/build/\|docs/superpowers/'
grep -n '^## \[3.0.0\]' CHANGELOG.md
```

Expected: the POM grep lists the surviving modules only (`kommons`, `kommons-core`, `kommons-debug`, `kommons-exec`, `kommons-io`, `kommons-kaomoji`, `kommons-logging-core`, `kommons-test`, `kommons-text`, `kommons-time`, `kommons-uri`) and no removed artifact. The second prints CHANGELOG hits only: the new `[Unreleased]` lines above the `## [3.0.0]` heading (whose line number the third command prints) and lines in the released sections below it, which stay as they are. No hit in any other file. `kommons-logging/kommons-logging-core/README.md` mentions `@SpringBootApplication` in its logger-naming example, which is not matched by the pattern and stays.

- [ ] **Step 12: Commit**

```bash
git add -A
git status --short
git commit -F - <<'EOF'
feat(logging): remove the logback and spring boot modules

Spring Boot 4 provides what the four modules did: structured JSON
logging, file logging and key-value pairs through SLF4J's fluent API.
Boot's formatter does not render logstash-logback-encoder's structured
arguments, so the json preset and Boot's structured logging overlapped
without composing. kommons-logging-core stays.
EOF
git log --oneline -2
```

Expected: `git status --short` before the commit lists only the intended files (deleted module files, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `.github/workflows/build.yml`, `kommons-logging/README.md`, `README.md`, `CHANGELOG.md`).

---

### Task 3: Pull request and CI

**Files:** none.

- [ ] **Step 1: Push and open the pull request**

Write the body to a scratch file first (outside the repository), then:

```bash
git push -u origin feat/remove-spring-logging
gh pr create --base main --title "feat(logging): remove the logback and spring boot modules" --body-file <scratch>/pr-b-body.md
```

Body sections, in the repository's style: `## What changes` (the deletions, the catalog, the CI `test` argument, the kapt property as a deviation from the spec, the READMEs, the CHANGELOG with the migration), `## Verified locally (Apple silicon, JDK 17 build)` (the counts from Task 2 steps 9 to 11), `## Release` (ships as a minor version: the easy configuration of JSON logging has become Spring Boot functionality). No AI attribution footer.

- [ ] **Step 2: Watch CI**

```bash
gh pr checks <number> --watch --interval 60
```

Expected: every `build` job green, including the three `jvm` rows on JDK 17, 21 and 25, which prove the `test` argument removal. If a job is red, read its log with `gh run view <run-id> --log-failed` before changing anything.
