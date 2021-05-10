# Releasing

[Maven Central Nexus](https://oss.sonatype.org/#welcome)  
[Sonatype Jira](https://issues.sonatype.org/secure/Dashboard.jspa)

New versions are released using the [Nebula Release Plugin](https://github.com/nebula-plugins/nebula-release-plugin) which
uses [grgit](https://github.com/ajoberstar/grgit) to interact with the Git repository.

**All examples assume version 2.1.0 was successfully released with you working on future version 2.2.0.**

## Version Types

|           	| Example                                            | Format                                                                    | Gradle Task            |
|-----------	|------------------------------------------------	|-----------------------------------------------------------------------	|----------------------	|
| Final        | 2.2.0                                            | &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;                                                | final                    |
| Candidate    | 2.2.0-rc.1                                        | &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;-rc.#                                            | candidate                |
| Snapshot    | 2.2.0-dev.2.uncommitted+d953d49<br>2.2.0-SNAPSHOT    | &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;-dev.#+&lt;hash&gt;<br>&lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;-SNAPSHOT    | devSnapshot<br>snapshot    |

## Cheat Sheet

### Print Current Version

```shell
./gradlew properties | grep ^version: | perl -pe 's/version:\s+//'
```

### Test Uncommited Changes on a Dependant Project

1. Change dependency in dependent project to `implementation("com.bkahlert:koodies-jvm:2.2.0-SNAPSHOT")`.
2. Publish changes to local Maven repository using

```shell
./gradlew snapshot publishToMavenLocal -x :dokkaHtml
```

```shell
./gradlew snapshot publishToMavenLocal -x :dokkaHtml  -x :javadoc -x publishJsPublicationToBintrayRepository -x publishJvmPublicationToBintrayRepository -x publishNativePublicationToBintrayRepository -x publishKotlinMultiplatformPublicationToBintrayRepository -x publishPackageToBintray -x publishAllPublicationsToGitHubPackagesRepository -x publishJsPublicationToGitHubPackagesRepository -x publishJvmPublicationToGitHubPackagesRepository -x publishNativePublicationToGitHubPackagesRepository -x publishKotlinMultiplatformPublicationToGitHubPackagesRepository -x publishJsPublicationToMavenCentralRepository -x publishJvmPublicationToMavenCentralRepository -x publishNativePublicationToMavenCentralRepository -x publishKotlinMultiplatformPublicationToMavenCentralRepository
```

.

### Release a Candidate

1. Commit your changes using `git commit`.
2. Release candidate using `./gradlew candidate -Prelease.scope=minor`.
   (increases with every call)

### Release a Final Version

1. Commit your changes using `git commit`.
2. Release final version
    - major: `./gradlew final -Prelease.scope=major publish`
    - minor: `./gradlew final -Prelease.scope=minor publish`
    - patch: `./gradlew final -Prelease.scope=patch publish`

### Increase Version Number

```shell
./gradlew <snapshot|devSnapshot|candidate|final> -Prelease.scope=<major|minor|patch>
./gradlew final -Prelease.scope=patch
```

Example—Increase major version: `./gradlew <snapshot|devSnapshot|candidate|final> -Prelease.scope=patch`

### Last Tag

Create a tag locally using `git tag v2.2.0` (leading `v`  is important) and `./gradlew -Prelease.useLastTag=true final` in a CI to make the latter use a
pre-defined version, e.g.

```shell
./gradlew -Prelease.useLastTag=true final publishAllPublicationsToMavenCentralRepository
./gradlew -Prelease.useLastTag=true final publishAllPublicationsToGitHubPackagesRepository
```

### Override Version

```shell
./gradlew -Prelease.version=2.3.0 final
```

```kotlin
enum class Features {
    FeatureA, FeatureB, FeatureC
}

val features = EnumSetBuilder.build<Features> {
    +Features.FeatureA + Features.FeatureC
}
```

```kotlin
fun buildList(init: ListBuilder<String>.() -> Unit) {
    val list = init.build()
}

buildList {
    +"element"
    +existingList
}
```

### Provide Credentials

It's recommended to provide credentials via system or environment properties. To do the latter, run:

```shell
export SONATYPE_NEXUS_USERNAME=<Sonatype Nexus Username>
export SONATYPE_NEXUS_PASSWORD=<Sonatype Nexus Password>

export GITHUB_USERNAME=<GitHub Username>
export GITHUB_TOKEN=<GitHub Token>
export GRGIT_USER=<GitHub Token>
```
