# Releasing

## Publishing to Maven Local

In order to publish a release with its `version` ending with `-SNAPSHOT` to
your local Maven repository:

```shell
./gradlew snapshot publishToMavenLocal
```

## Publishing to Maven Central

In order to publish a final release:

- Create the new release using the [GitHub release workflow][github-release-workflow]
- Publish the release at [Maven Central Nexus][maven-central-nexus]
    - Details are included in the workflow output
    - In case of problems, contact [Sonatype][sonatype-jira]

## Print latest versions

```shell
curl -LfsS \
  'https://search.maven.org/solrsearch/select?q=g:com.bkahlert.kommons&rows=20&wt=json' | {
  jq -s \
  '.[0].response.docs[].latestVersion'
}
```

[github-release-workflow]: .github/workflows/release.yml

[maven-central-nexus]: https://oss.sonatype.org/#stagingRepositories

[sonatype-jira]: https://issues.sonatype.org/secure/Dashboard.jspa
