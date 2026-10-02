# Releasing

Releases are published to Maven Central through the [Central Portal](https://central.sonatype.com/).

## Publishing a release

1. Open *Actions → release → Run workflow* on GitHub ([release.yml](.github/workflows/release.yml)), enter the version
   (for example `3.0.0`) and the branch, and run it. One macOS job builds, signs and uploads every module as a single
   deployment; a second job drafts the GitHub release with [CHANGELOG.md](CHANGELOG.md) as its body.
2. With *Publish to Maven Central automatically* checked (the default), the deployment is published as soon as the
   Portal has validated it. Uncheck it to inspect the deployment first; it then waits under
   [Deployments](https://central.sonatype.com/publishing/deployments) until you hit *Publish*. The deployment id is in
   the log of the publish job.
3. Maven Central shows the artifacts 10 to 30 minutes after publishing. Clean up the drafted release notes and publish
   the GitHub release.

The workflow takes its credentials from the repository secrets `ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME` and
`ORG_GRADLE_PROJECT_MAVENCENTRALPASSWORD` (a Central Portal user token) and `ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEY`,
`ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEYID` and `ORG_GRADLE_PROJECT_SIGNINGINMEMORYKEYPASSWORD` (the ASCII-armored
signing key, the last eight hex digits of its id, and its passphrase).

## Publishing to Maven Local

A `-SNAPSHOT` version is neither signed nor documented, so it needs no key:

```shell
./gradlew snapshot publishToMavenLocal
```

Any other version is signed and needs the `signingInMemoryKey`, `signingInMemoryKeyId` and
`signingInMemoryKeyPassword` Gradle properties, for example as `ORG_GRADLE_PROJECT_*` environment variables; see the
[plugin documentation](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets).

## Print latest versions

```shell
curl -LfsS \
  'https://search.maven.org/solrsearch/select?q=g:com.bkahlert.kommons&rows=20&wt=json' | {
  jq -s \
  '.[0].response.docs[].latestVersion'
}
```
