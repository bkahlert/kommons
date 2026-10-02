# Publishing (PR 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish Kommons to Maven Central through the Central Portal from a single macOS release job, replacing the OSSRH and GitHub Packages publishing that stopped working when OSSRH shut down.

**Architecture:** `kommons-publishing-conventions` applies `com.vanniktech.maven.publish`, which owns the Portal upload, signing and the per-target javadoc jars; the convention keeps only the POM, the snapshot rule and the `RELEASE_VERSION` override. The BOM applies the same convention and is recognised as a `java-platform`. `release.yml` shrinks to one `macos-latest` publish job (Kotlin/Native cross-compiles every target from macOS) plus the unchanged draft-release job.

**Tech Stack:** Gradle 9.7.0 (Kotlin DSL, buildSrc, configuration cache on), Kotlin Multiplatform 2.4.20, Dokka 2.2.0, nebula.release 21.1.4, com.vanniktech.maven.publish 0.37.0, GitHub Actions.

**Spec:** [docs/superpowers/specs/2026-10-02-modernization-design.md](../specs/2026-10-02-modernization-design.md), section "PR 2: publishing" and decisions 4, 5, 6.

Deviations from the spec's PR 2 text, each forced by the plugin or by the workflow:

- The plugin's property is `mavenCentralAutomaticPublishing`, not `mavenCentralAutomaticRelease`. `publishToMavenCentral()` without arguments reads it (and `mavenCentralDeploymentValidation`, default `VALIDATED`) as its defaults, so `-PmavenCentralAutomaticPublishing=false` overrides `gradle.properties`.
- `KotlinMultiplatform(javadocJar, sourcesJar = true)` is a deprecated constructor in 0.37.0; `SourcesJar.Sources()` is the default, so only `javadocJar` is passed.
- `release.yml` gets a third input, `automatic-release` (boolean, default on). The spec wants the first 3.0.0 run to pass `-PmavenCentralAutomaticPublishing=false`, and a dispatch input is the only way to pass it.
- The BOM keeps its published name "Kommons Bill of Materials" with a one-line `pom { name }` override; the convention would otherwise call it "Kommons Bom".
- The snapshot check now runs after the `RELEASE_VERSION` override, so a `-SNAPSHOT` release version is treated as a snapshot.

## Global Constraints

- Gradle 9.7.0, Kotlin 2.4.20, Dokka 2.2.0, nebula.release 21.1.4, vanniktech 0.37.0 (released 2026-06-21; minimum Gradle 9.0, KGP 2.2; tested up to Gradle 9.7.0-milestone-1 and KGP 2.4.0). No other dependency version changes in this PR.
- The build runs on JDK 17. Every `./gradlew` command in this plan runs through the wrapper script from Preparation step 2, which sets `JAVA_HOME` to Corretto 17.
- Configuration cache stays on. `./gradlew help --warning-mode all` prints zero deprecation lines. No `by getting/creating/registering`, no `project` access at execution time.
- Credentials are Gradle properties: `mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey`, `signingInMemoryKeyId`, `signingInMemoryKeyPassword`. CI provides them as `ORG_GRADLE_PROJECT_<property>` with the camelCase property part; the GitHub secrets are stored uppercased (`ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME` and so on).
- `signingKey`, `signingPassword`, `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `GITHUB_ACTOR`/`GITHUB_TOKEN` for GitHub Packages: gone from build and workflow.
- `-SNAPSHOT` versions disable `Sign` and `DokkaGenerateTask` tasks. `RELEASE_VERSION` from the environment overrides the nebula-inferred version.
- Local versions are nebula dev versions (`2.9.0-dev.N+build.<branch>.<sha>`), not snapshots, so Dokka and signing are active locally. Never export `mavenCentralUsername`/`mavenCentralPassword` locally.
- Native targets stay linuxX64, mingwX64, macosX64, macosArm64. `-PosArchOnly=true` registers only the host target.
- Commits follow Conventional Commits (`build:`, `ci:`, `docs:`), lowercase description, no AI attribution trailers. Branch `build/central-portal-publishing` from `origin/master` (f9bafb04); the local `master` is stale.
- Do not touch the main checkout at `/Users/bkahlert/Development/com.bkahlert/kommons`: it holds the author's uncommitted work. Work in the worktree only.

## Review Focus

Inputs and conditions the spec implies but no task's tests exercise by default; each has a pinning check in the task named.

1. The BOM must still constrain every published module after its publication changes from `Bom` to the plugin's `maven`: a BOM that lists 13 modules silently drops a version for consumers. Task 2 step 4 counts 14 `<dependency>` entries.
2. Every POM must carry a non-empty `<description>`, or the Portal rejects the whole deployment. Task 2 step 4 checks every generated POM.
3. A `-SNAPSHOT` version without any signing key must publish unsigned instead of failing with "no configured signatory". Task 2 step 6.
4. The workflow must map the uppercased secrets to camelCase env names; `ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME` would be ignored by Gradle and the release would fail at the upload. Task 3 step 2.
5. The single macOS job must produce linuxX64 and mingwX64 artifacts, otherwise decision 5 does not hold and consumers on those platforms get nothing. Task 2 step 5.

---

## Preparation

- [ ] **Step 1: Create a worktree on a fresh branch from origin/master**

Use `EnterWorktree` with name `build+central-portal-publishing` and branch `build/central-portal-publishing` based on `origin/master`. Fallback without the tool:

```bash
cd /Users/bkahlert/Development/com.bkahlert/kommons
git fetch origin
git worktree add ../kommons-pr2 -b build/central-portal-publishing origin/master
```

Check inside the worktree: `git log --oneline -1` prints `f9bafb04 build(logging): take spring versions from the spring boot bom`.

- [ ] **Step 2: Gradle wrapper on JDK 17**

The shell's default JDK is Liberica 8. Create `<scratchpad>/gradlew17` with the worktree path filled in and make it executable:

```bash
#!/usr/bin/env bash
# Purpose: Run the kommons Gradle wrapper on JDK 17 from any directory.
# Usage:   gradlew17 <gradle arguments>...

export JAVA_HOME=/Users/bkahlert/Library/Java/JavaVirtualMachines/corretto-17.0.10/Contents/Home
cd "<worktree>" && exec ./gradlew "$@"
```

Every `./gradlew` below means this script. `<scratchpad>` is the session scratchpad directory; `$SCRATCH` in shell snippets stands for it.

- [ ] **Step 3: Baseline**

```bash
./gradlew help --warning-mode all 2>&1 | grep -ci "deprecat"
```

Expected: `0`.

---

### Task 1: Publishing convention on the vanniktech plugin

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `buildSrc/build.gradle.kts`
- Modify: `buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts`
- Modify: `gradle.properties`
- Modify: `kommons-bom/build.gradle.kts`

**Interfaces:**
- Consumes: Dokka task `dokkaGeneratePublicationHtml` (applied by `kotlin-conventions`), the module `description` set in each module's build script, nebula's `project.version`.
- Produces: per published module the tasks `publishToMavenCentral`, `publishAndReleaseToMavenCentral`, `publishToMavenLocal`, `<publication>DokkaJavadocJar`; the Gradle properties `mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey`, `signingInMemoryKeyId`, `signingInMemoryKeyPassword`, `mavenCentralAutomaticPublishing`; the environment variable `RELEASE_VERSION`. Task 3's workflow and Task 4's README rely on exactly these names.

- [ ] **Step 1: Version catalog**

In `gradle/libs.versions.toml`, append to `[versions]`:

```toml
vanniktech-maven-publish = "0.37.0"
```

Append to the `# Gradle plugins` group in `[libraries]`:

```toml
vanniktech-maven-publish-plugin = { module = "com.vanniktech:gradle-maven-publish-plugin", version.ref = "vanniktech-maven-publish" }
```

- [ ] **Step 2: buildSrc classpath**

Replace the `dependencies` block of `buildSrc/build.gradle.kts` with:

```kotlin
dependencies {
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.nebula.release.plugin)
    implementation(libs.vanniktech.maven.publish.plugin)
}
```

- [ ] **Step 3: The convention**

Replace `buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts` with:

```kotlin
import com.vanniktech.maven.publish.JavaPlatform
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

plugins {
    id("com.vanniktech.maven.publish")
    id("com.netflix.nebula.release")
}

// The release workflow passes the version through the environment; otherwise nebula infers it from the Git history.
val releaseVersion: String? = System.getenv("RELEASE_VERSION")
if (releaseVersion != null) version = releaseVersion

val isSnapshot = version.toString().endsWith("-SNAPSHOT")
if (isSnapshot) {
    logger.lifecycle("Snapshot version: $version")
    tasks.withType<Sign>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
    tasks.withType<DokkaGenerateTask>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
}

mavenPublishing {
    // Automatic release and deployment validation follow the mavenCentralAutomaticPublishing and
    // mavenCentralDeploymentValidation Gradle properties, set in gradle.properties and overridable with -P.
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set(project.name.split("-").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } })
        // The module's build script sets its description after this convention has run. Without the deferral the
        // Kotlin Multiplatform publication's description stays empty, and Maven Central rejects the deployment.
        val mavenPom = this
        afterEvaluate { mavenPom.description.set(project.description) }
        url.set("https://github.com/bkahlert/kommons/tree/master/${project.name}")

        ciManagement {
            url.set("https://github.com/bkahlert/kommons/issues")
            system.set("GitHub")
        }

        developers {
            developer {
                id.set("bkahlert")
                name.set("Björn Kahlert")
                email.set("mail@bkahlert.com")
                url.set("https://bkahlert.com")
                timezone.set("Europe/Berlin")
            }
        }

        issueManagement {
            url.set("https://github.com/bkahlert/kommons/issues")
            system.set("GitHub")
        }

        licenses {
            license {
                name.set("MIT")
                url.set("https://github.com/bkahlert/kommons/blob/master/LICENSE")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/bkahlert/kommons")
            developerConnection.set("scm:git:https://github.com/bkahlert")
            url.set("https://github.com/bkahlert/kommons")
        }
    }
}

// Kotlin Multiplatform libraries publish every target with a Dokka javadoc jar; the BOM is a java-platform.
pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    mavenPublishing {
        configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))
    }
}
pluginManager.withPlugin("java-platform") {
    mavenPublishing {
        configure(JavaPlatform())
    }
}
```

What the plugin does with this (verified in the 0.37.0 sources):

- `publishToMavenCentral()` registers a `mavenCentral` repository that stages into `build/publishing/mavenCentral`, a build service that uploads all staged modules of the build as one Portal deployment at the end of the build, and the tasks `publishToMavenCentral` and `publishAndReleaseToMavenCentral`. For `-SNAPSHOT` versions the repository is the Portal snapshot repository instead.
- `signAllPublications()` applies `signing`, sets `required` to "not a snapshot", and calls `useInMemoryPgpKeys(signingInMemoryKeyId, signingInMemoryKey, signingInMemoryKeyPassword)` when `signingInMemoryKey` is present.
- `configure(KotlinMultiplatform(...))` adds a `<publication>DokkaJavadocJar` task and artifact to every KGP publication (`kotlinMultiplatform`, `jvm`, `js`, each native target) and keeps KGP's sources jars.
- `configure(JavaPlatform())` creates the `maven` publication from the `javaPlatform` component, without sources or javadoc jars.
- `pom {}` applies to every `MavenPublication` of the project, including ones created later.

If `./gradlew help` fails with "Cannot run Project.afterEvaluate(Action) when the project is already evaluated", replace the two description lines with `description.set(provider { project.description })` and note it in the commit body.

- [ ] **Step 4: gradle.properties**

Append to `gradle.properties`:

```properties
# Releases go to Maven Central as soon as the Central Portal has validated the deployment.
# -PmavenCentralAutomaticPublishing=false keeps the deployment for inspection at https://central.sonatype.com/publishing/deployments.
mavenCentralAutomaticPublishing=true
```

- [ ] **Step 5: The BOM**

Replace `kommons-bom/build.gradle.kts` with:

```kotlin
plugins {
    id("java-platform")
    id("kommons-publishing-conventions")
}

group = "com.bkahlert.kommons"
description = "Features for Kotlin™ Multiplatform You Didn't Know You Were Missing"

val bomProject = project

// Explicitly exclude subprojects that will never be published
// so that when configuring this project, we don't force their
// configuration and do unecessary work.
val excludeFromBom: List<String> = emptyList()
fun projectsFilter(candidateProject: Project) =
    excludeFromBom.all { !candidateProject.name.contains(it) } &&
        candidateProject.name != bomProject.name

// Declare that this subproject depends on all subprojects matching the filter
// When this subproject is configured, it will force configuration of all subprojects
// so that we can declare dependencies on them
rootProject.subprojects.filter(::projectsFilter).forEach { bomProject.evaluationDependsOn(it.path) }

dependencies {
    constraints {
        rootProject.subprojects.filter { project ->
            // Only declare dependencies on projects that will have publications
            projectsFilter(project) && project.tasks.findByName("publish")?.enabled == true
        }.forEach { api(project(it.path)) }
    }
}

mavenPublishing {
    pom {
        name.set("Kommons Bill of Materials")
    }
}
```

The constraints logic and the `publish`-task probe are unchanged. The convention supplies snapshot handling, `RELEASE_VERSION`, signing, the Portal repository and the rest of the POM; the BOM's `pom {}` runs after the convention's and only overrides the name.

- [ ] **Step 6: Configure the build, twice**

```bash
./gradlew help --warning-mode all 2>&1 | tee "$SCRATCH/help-1.log" | grep -iE "deprecat|configuration cache"
./gradlew help 2>&1 | grep -i "configuration cache"
```

Expected: first run prints no deprecation line and `Configuration cache entry stored.`; second run prints `Reusing configuration cache.` A deprecation line from the vanniktech plugin itself is recorded for the PR description under "Third-party deprecation warnings" and is not fixed here.

- [ ] **Step 7: Check the task wiring**

```bash
./gradlew :kommons-core:tasks --all -PosArchOnly=true 2>/dev/null | grep -E "^(publishToMavenCentral|publishAndReleaseToMavenCentral|publishAllPublicationsToMavenCentralRepository|kotlinMultiplatformDokkaJavadocJar|jvmDokkaJavadocJar|jsDokkaJavadocJar|macosArm64DokkaJavadocJar|signKotlinMultiplatformPublication|javadocJar) "
./gradlew :kommons-bom:tasks --all -PosArchOnly=true 2>/dev/null | grep -E "^(publishToMavenCentral|publishMavenPublicationToMavenCentralRepository|signMavenPublication|generateMetadataFileForMavenPublication) "
```

Expected: the first command lists the eight plugin tasks and no `javadocJar` (the convention's own task is gone); the second lists all four BOM tasks. If a module still has `OSSRH` or `GitHubPackages` in its task list (`grep -i ossrh`), the convention was not replaced.

- [ ] **Step 8: Commit**

```bash
git add gradle/libs.versions.toml buildSrc/build.gradle.kts buildSrc/src/main/kotlin/kommons-publishing-conventions.gradle.kts gradle.properties kommons-bom/build.gradle.kts
git commit -m "build: publish through the central portal with the vanniktech plugin

OSSRH shut down on 2025-06-30. The vanniktech plugin stages every
module of a build into one Central Portal deployment, signs with the
in-memory key from the signingInMemoryKey* properties and builds a
Dokka javadoc jar per target publication. The GitHub Packages
repository goes: no workflow ever published there. The BOM applies the
same convention and only keeps its constraints logic."
```

---

### Task 2: Local publishing QA

No files change in this task unless a step fails. Every step runs in the worktree with the throwaway key from step 1 in the environment, except step 6, which runs without it.

**Files:**
- Create (scratchpad only): `$SCRATCH/publish-env.sh`, `$SCRATCH/gnupg/`, `$SCRATCH/m2/`, `$SCRATCH/m2-all/`, `$SCRATCH/m2-snapshot/`

**Interfaces:**
- Consumes: the tasks and properties from Task 1.

- [ ] **Step 1: Throwaway signing key**

```bash
export GNUPGHOME="$SCRATCH/gnupg"; mkdir -p "$GNUPGHOME"; chmod 700 "$GNUPGHOME"
gpg --batch --pinentry-mode loopback --passphrase throwaway \
  --quick-generate-key "Kommons Throwaway <throwaway@example.invalid>" rsa4096 sign never
KEY_FPR=$(gpg --batch --list-secret-keys --with-colons | awk -F: '/^fpr/ {print $10; exit}')
{
  printf 'export ORG_GRADLE_PROJECT_signingInMemoryKeyId=%s\n' "${KEY_FPR: -8}"
  printf 'export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=throwaway\n'
  printf 'export ORG_GRADLE_PROJECT_signingInMemoryKey=%q\n' \
    "$(gpg --batch --pinentry-mode loopback --passphrase throwaway --export-secret-keys --armor "$KEY_FPR")"
} > "$SCRATCH/publish-env.sh"
```

Expected: `publish-env.sh` has three `export` lines; the key id is 8 hex digits. The following steps start with `source "$SCRATCH/publish-env.sh"`.

- [ ] **Step 2: Publish the host targets to a scratch Maven repository**

```bash
source "$SCRATCH/publish-env.sh"
./gradlew publishToMavenLocal -PosArchOnly=true -Dmaven.repo.local="$SCRATCH/m2" 2>&1 | tail -5
ls "$SCRATCH/m2/com/bkahlert/kommons" | wc -l
```

Gradle honours `maven.repo.local` for `publishToMavenLocal`. Expected: `BUILD SUCCESSFUL` and `45` artifact directories: 7 full multiplatform modules (`kommons`, `kommons-core`, `kommons-kaomoji`, `kommons-test`, `kommons-text`, `kommons-time`, `kommons-uri`) × 4 (root, `-jvm`, `-js`, `-macosarm64`) = 28, 2 jvm+js modules (`kommons-debug`, `kommons-logging-core`) × 3 = 6, 5 jvm modules (`kommons-exec`, `kommons-io`, `kommons-logging-logback`, `kommons-logging-spring-boot`, `kommons-logging-spring-boot-starter`) × 2 = 10, plus `kommons-bom`. If the directory is empty, the artifacts went to `~/.m2/repository`; use that path for the next steps and delete the dev version from it at the end.

- [ ] **Step 3: Every artifact directory is complete and signed**

```bash
M2="$SCRATCH/m2/com/bkahlert/kommons"
for dir in "$M2"/*/*/; do
  module=$(basename "$(dirname "$dir")")
  missing=""
  ls "$dir" | grep -q '\.module$' || missing="$missing module"
  ls "$dir" | grep -q '\.pom$' || missing="$missing pom"
  if [ "$module" != "kommons-bom" ]; then
    ls "$dir" | grep -q -- '-sources\.jar$' || missing="$missing sources"
    ls "$dir" | grep -q -- '-javadoc\.jar$' || missing="$missing javadoc"
  fi
  for f in "$dir"*; do
    case "$f" in *.asc|*.md5|*.sha1|*.sha256|*.sha512) continue ;; esac
    [ -f "$f.asc" ] || missing="$missing $(basename "$f").asc"
  done
  printf '%-48s %s\n' "$module" "${missing:-ok}"
done | tee "$SCRATCH/artifacts.txt"
grep -vc " ok$" "$SCRATCH/artifacts.txt"
```

Expected: 45 lines, last line `0`. Each line names the module file, POM, sources jar, javadoc jar (not for the BOM) and a `.asc` next to every file. Spot-check one javadoc jar is not empty:

```bash
unzip -l "$M2"/kommons-core-jvm/*/kommons-core-jvm-*-javadoc.jar | grep -cE 'index\.html|navigation\.html'
```

Expected: at least `1`.

- [ ] **Step 4: POM content**

```bash
grep -L "<description>" "$M2"/*/*/*.pom
grep -c "<dependency>" "$M2"/kommons-bom/*/kommons-bom-*.pom
grep -o "<name>[^<]*</name>" "$M2"/kommons-bom/*/kommons-bom-*.pom "$M2"/kommons-logging-core/*/kommons-logging-core-*.pom
```

Expected: no file listed (every POM has a description), `14` dependencies in the BOM (every module except the BOM itself and the Spring Boot sample), and the names `Kommons Bill of Materials` and `Kommons Logging Core`.

- [ ] **Step 5: All native targets from this host**

This mirrors the release job: one macOS host cross-compiles linuxX64 and mingwX64.

```bash
source "$SCRATCH/publish-env.sh"
./gradlew :kommons-core:publishToMavenLocal -Dmaven.repo.local="$SCRATCH/m2-all" 2>&1 | tail -3
ls "$SCRATCH/m2-all/com/bkahlert/kommons" | grep -E "kommons-core-(linuxx64|mingwx64|macosx64|macosarm64)$"
```

Expected: `BUILD SUCCESSFUL` and the four target directories, each with a `.klib`, `.module`, `.pom`, `-sources.jar`, `-javadoc.jar` and their `.asc` files (re-run the step 3 loop with `M2="$SCRATCH/m2-all/com/bkahlert/kommons"` to check).

- [ ] **Step 6: A snapshot publishes unsigned without any key**

Run in a shell that did not source `publish-env.sh`:

```bash
env -u ORG_GRADLE_PROJECT_signingInMemoryKey RELEASE_VERSION=3.0.0-SNAPSHOT \
  ./gradlew :kommons-core:publishToMavenLocal -PosArchOnly=true -Dmaven.repo.local="$SCRATCH/m2-snapshot" 2>&1 | grep -E "Snapshot version|BUILD"
ls "$SCRATCH/m2-snapshot/com/bkahlert/kommons/kommons-core/3.0.0-SNAPSHOT/"
```

Expected: `Snapshot version: 3.0.0-SNAPSHOT`, `BUILD SUCCESSFUL`, and a directory with `.module`, `.pom`, jars and no `.asc` file. The javadoc jars are empty because Dokka is disabled for snapshots; that is the existing rule.

- [ ] **Step 7: The Portal publish tasks under the configuration cache**

`publishToMavenLocal` never runs the plugin's `mavenCentral` repository tasks. They stage into `build/publishing/mavenCentral`; the upload happens at the end of the build from projects that `prepareMavenCentralPublishing` registered. That task fails without `mavenCentralUsername`, so it is excluded here: nothing gets registered, nothing is uploaded, and the staging half still runs.

```bash
source "$SCRATCH/publish-env.sh"
./gradlew publishAllPublicationsToMavenCentralRepository -PosArchOnly=true -x prepareMavenCentralPublishing 2>&1 | tee "$SCRATCH/central-dry-run.log" | tail -5
ls kommons-core/build/publishing/mavenCentral/com/bkahlert/kommons/kommons-core/*/
grep -ciE "configuration cache problem" "$SCRATCH/central-dry-run.log"
```

Expected: `BUILD SUCCESSFUL`, `Configuration cache entry stored.`, the staging directory holds the files from step 3 plus Gradle's checksum files, and the problem count is `0`. Any failure is a defect to fix in Task 1 (commit the fix as `build: …` with the error in the body).

- [ ] **Step 8: Clean up and record**

```bash
rm -rf "$SCRATCH/m2-snapshot"
```

Keep `m2`, `m2-all` and the logs until the PR is open. Paste the counts (45 directories, 0 incomplete, 14 BOM dependencies, 4 native targets) into the ledger; they go into the PR description in Task 5.

---

### Task 3: Release workflow

**Files:**
- Modify: `.github/workflows/release.yml`

**Interfaces:**
- Consumes: Gradle task `publishToMavenCentral`, property `mavenCentralAutomaticPublishing`, env `RELEASE_VERSION`, the five `ORG_GRADLE_PROJECT_*` properties from Task 1; repository secrets `ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME`, `ORG_GRADLE_PROJECT_MAVENCENTRALPASSWORD`, `ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEY`, `ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEYID`, `ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEYPASSWORD` (all exist).
- Produces: workflow `release` with inputs `version`, `branch`, `automatic-release`; Task 4's README describes exactly these.

- [ ] **Step 1: Rewrite release.yml**

Replace `.github/workflows/release.yml` with:

```yaml
name: release

on:
  workflow_dispatch:
    inputs:
      version:
        description: "Version of the release"
        required: true
        default: '3.0.0'
      branch:
        description: "Branch to release from"
        required: true
        default: 'master'
      automatic-release:
        description: "Publish to Maven Central as soon as the Central Portal has validated the deployment (off: publish it by hand in the Portal)"
        type: boolean
        default: true

env:
  RELEASE_VERSION: ${{ inputs.version }}
  GRADLE_OPTS: -Dorg.gradle.jvmargs="-Dfile.encoding=UTF-8 -Xmx3g -XX:MaxMetaspaceSize=756m -XX:+HeapDumpOnOutOfMemoryError"

permissions:
  contents: read

jobs:
  publish:
    # One host publishes every target: Kotlin/Native cross-compiles all of them from macOS,
    # and the Central Portal rejects a release whose coordinates arrive in more than one deployment.
    runs-on: macos-latest
    env:
      # GitHub stores secret names uppercased; Gradle's ORG_GRADLE_PROJECT_<property> mapping is case-sensitive.
      ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME }}
      ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.ORG_GRADLE_PROJECT_MAVENCENTRALPASSWORD }}
      ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEY }}
      ORG_GRADLE_PROJECT_signingInMemoryKeyId: ${{ secrets.ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEYID }}
      ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEYPASSWORD }}
    steps:
      - name: Checkout
        uses: actions/checkout@v7
        with:
          fetch-depth: 0
          ref: ${{ inputs.branch }}

      - name: Set up JDK 17
        uses: actions/setup-java@v6
        with:
          java-version: 17
          distribution: temurin

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Publish to the Central Portal
        run: ./gradlew publishToMavenCentral -PmavenCentralAutomaticPublishing=${{ inputs.automatic-release }}

  release-draft:
    needs: [publish]
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - name: Checkout
        uses: actions/checkout@v7
        with:
          fetch-depth: 0
          ref: ${{ inputs.branch }}

      - name: Draft release
        uses: softprops/action-gh-release@v3
        with:
          draft: true
          body_path: CHANGELOG.md
          name: v${{ inputs.version }}
          tag_name: v${{ inputs.version }}
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Print summary
        env:
          AUTOMATIC_RELEASE: ${{ inputs.automatic-release }}
        run: |
          {
            echo "## Release ${RELEASE_VERSION} prepared :rocket:"
            echo "To finish the release:"
            echo "- Go to the [Central Portal deployments](https://central.sonatype.com/publishing/deployments); the deployment id is in the log of the publish job"
            if [ "${AUTOMATIC_RELEASE}" = "true" ]; then
              echo "  - The deployment is published once validated; Maven Central shows it 10 to 30 minutes later"
            else
              echo "  - Check the validated deployment and hit \"Publish\""
            fi
            echo "- Go to [releases](https://github.com/bkahlert/kommons/releases)"
            echo "  - Find the draft for this release"
            echo "  - Edit it and clean up the release notes"
            echo "  - Hit \"Publish release\""
          } >> "$GITHUB_STEP_SUMMARY"
```

Tests are not part of this job; `build.yml` runs them on every push to master and every tag.

- [ ] **Step 2: Lint and check the secret mapping**

```bash
actionlint .github/workflows/release.yml
grep -cE "ORG_GRADLE_PROJECT_(mavenCentralUsername|mavenCentralPassword|signingInMemoryKey|signingInMemoryKeyId|signingInMemoryKeyPassword): \\$\{\{ secrets\.ORG_GRADLE_PROJECT_[A-Z]+ \}\}" .github/workflows/release.yml
grep -ciE "ossrh|signingKey|signingPassword|GITHUB_ACTOR|java-version:.*inputs|github\.event\.inputs" .github/workflows/release.yml
```

Expected: no actionlint output, `5`, `0`.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: release from one macos job to the central portal

The four OSSRH publish jobs become a single macos-latest job running
publishToMavenCentral; Kotlin/Native cross-compiles every target from
macOS and the Portal rejects coordinates split over several
deployments. The JDK inputs go (the build is pinned to a JDK 17
toolchain); an automatic-release input lets the first 3.0.0 run keep
the deployment for inspection in the Portal."
```

---

### Task 4: README release paragraph

**Files:**
- Modify: `README.md` (section "Development", after "Project structure")

**Interfaces:**
- Consumes: workflow inputs from Task 3, Gradle properties from Task 1.

- [ ] **Step 1: Add the release subsection**

In `README.md`, after the "Project structure" list (the line `- [buildSrc](buildSrc) … custom build logic`) and before `## Contributing`, insert:

```markdown

### Releasing

Releases are published to Maven Central through the [Central Portal](https://central.sonatype.com/) by the
[release workflow](.github/workflows/release.yml): open *Actions → release → Run workflow*, enter the version (for
example `3.0.0`) and the branch. One macOS job builds, signs and uploads every module as a single deployment; a second
job drafts the GitHub release with [CHANGELOG.md](CHANGELOG.md) as its body.

- *Publish automatically* (the default) releases the deployment as soon as the Portal has validated it. Switch it off to
  inspect the deployment first; it then waits under [Deployments](https://central.sonatype.com/publishing/deployments)
  until you hit *Publish*. The deployment id is in the log of the publish job.
- Published artifacts show up on Maven Central 10 to 30 minutes later. Clean up the drafted release notes and publish the
  GitHub release.

`./gradlew publishToMavenLocal` builds the same artifacts into `~/.m2/repository`. Versions other than `-SNAPSHOT` are
signed, which needs the `signingInMemoryKey`, `signingInMemoryKeyId` and `signingInMemoryKeyPassword` Gradle properties,
for example as `ORG_GRADLE_PROJECT_*` environment variables; see the
[plugin documentation](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets).
```

- [ ] **Step 2: Check the links resolve**

```bash
grep -oE "\]\((\.github/workflows/release\.yml|CHANGELOG\.md)\)" README.md | sort -u
ls .github/workflows/release.yml CHANGELOG.md
```

Expected: both links listed, both files exist.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: describe how a release is made"
```

---

### Task 5: Pull request

**Files:** none modified.

- [ ] **Step 1: Final local check**

```bash
./gradlew help --warning-mode all 2>&1 | grep -ci "deprecat"
git log --oneline origin/master..HEAD
```

Expected: `0`, and three commits (`build:`, `ci:`, `docs:`).

- [ ] **Step 2: Push and open the PR (ask the author before pushing)**

```bash
git push -u origin build/central-portal-publishing
gh pr create --base master --title "build: publish to maven central through the central portal" --body-file - <<'PR'
Implements PR 2 of docs/superpowers/specs/2026-10-02-modernization-design.md.

## What changes
- `kommons-publishing-conventions` applies `com.vanniktech.maven.publish` 0.37.0: Central Portal upload as one deployment per build, in-memory signing from `signingInMemoryKey*`, a Dokka javadoc jar per target publication. OSSRH and GitHub Packages repositories are gone.
- The BOM applies the same convention (`JavaPlatform`); only its constraints logic stays in its build script.
- `release.yml`: one `macos-latest` job runs `publishToMavenCentral`; inputs are `version`, `branch` and `automatic-release`. The draft-release job is unchanged; the step summary points at the Portal deployments instead of Nexus.
- `gradle.properties`: `mavenCentralAutomaticPublishing=true`.
- README: how a release is triggered and where to inspect a deployment.

## Verified locally (Apple silicon, JDK 17 build, throwaway GPG key)
- `publishToMavenLocal -PosArchOnly=true`: 45 artifact directories, every one with module file, POM, sources jar, javadoc jar (BOM: module file and POM) and a `.asc` per file.
- Every POM has a description; the BOM lists 14 modules.
- `:kommons-core:publishToMavenLocal` without `-PosArchOnly`: linuxX64, mingwX64, macosX64 and macosArm64 artifacts from this host.
- `RELEASE_VERSION=3.0.0-SNAPSHOT` without a key publishes unsigned.
- `publishToMavenCentral` without credentials stages every module under the configuration cache and fails only at the upload.
- `help --warning-mode all`: no deprecation lines.

## Deviations from the spec
- The plugin's property is `mavenCentralAutomaticPublishing` (the spec names it `mavenCentralAutomaticRelease`).
- `release.yml` has an `automatic-release` input so the first 3.0.0 run can keep the deployment for inspection; the spec reduces inputs to `version` and `branch` but also asks for that run.
- The BOM keeps its name "Kommons Bill of Materials" by a one-line POM override.

## Third-party deprecation warnings
<none, or the lines from Task 1 step 6>

## After merge
- Delete the retired secrets `ORG_GRADLE_PROJECT_SIGNINGKEY`, `ORG_GRADLE_PROJECT_SIGNINGPASSWORD`, `OSSRH_USERNAME`, `OSSRH_PASSWORD`.
- The first 3.0.0 release runs with *automatic-release* off and is published from the Portal UI (spec, "QA for PR 2").
PR
```

Fill in the counts from Task 2 and the deprecation section before submitting.

- [ ] **Step 3: Wait for CI**

```bash
gh pr checks --watch
```

Expected: all nine `build.yml` jobs green (the release workflow is dispatch-only and does not run). A red job is fixed on the branch with a commit of the matching type.

- [ ] **Step 4: Merge**

Merge commits are disabled on the repository and squash would fold the three commits into one; rebase keeps them:

```bash
gh pr merge --rebase
```

`deleteBranchOnMerge` removes the remote branch. Afterwards, from the main checkout, remove the worktree (`ExitWorktree`, or `git worktree remove ../kommons-pr2 && git branch -d build/central-portal-publishing`), and remind the author to delete the four retired secrets.
