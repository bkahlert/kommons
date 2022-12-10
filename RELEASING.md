# Releasing

[Maven Central Nexus](https://oss.sonatype.org/#stagingRepositories)  
[Sonatype Jira](https://issues.sonatype.org/secure/Dashboard.jspa)

New versions are released using the [release workflow](.github/workflows/release.yml).


## Print latest versions

```shell
curl -LfsS \
  'https://search.maven.org/solrsearch/select?q=g:com.bkahlert.kommons&rows=20&wt=json' | {
  jq -s \
  '.[0].response.docs[].latestVersion'
}
```
