# How to release Retable

## Maven central
To Release a new version of retable onto maven central :

1. Create and push your version tag
2. [Publishing job](https://github.com/4sh/retable/actions/workflows/publish_to_maven_central_on_create_release.yml) will be automatically start
3. Wait the end of the job
4. Go to Sonatype central [deployments section](https://central.sonatype.com/publishing/deployments) (you must be login and have access to `io.github.4sh` namespace)
5. Publish it

---
Old steps: 
5. Go to sonatype repo manager in [Staging Repositories section](https://s01.oss.sonatype.org/#stagingRepositories) (you must be login)
6. Close your staging repo and wait all rules passed
7. Release your repo
