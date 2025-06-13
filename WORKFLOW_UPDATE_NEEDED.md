# GitHub Actions Workflow Update Required

## Issue
The GitHub Actions workflow `.github/workflows/maven.yml` needs to be updated to work with the new integration test default behavior.

## Required Change
In the `integration-tests` job, line 245 needs to be updated from:
```yaml
run: mvn install -e -B -V -Prun-its,mimir
```

To:
```yaml
run: mvn install -e -B -V -Prun-its,maven-from-build,mimir
```

## Reason
The integration-tests job downloads the built Maven distribution and uses it to run the build. In this context, we want to test the Maven installation that's running the build (which happens to be the built distribution), so we need to use the `maven-from-build` profile.

Without this change, the default behavior will try to extract the distribution from `apache-maven/target/apache-maven-${project.version}-bin.zip`, which doesn't exist in the CI context where the distribution is downloaded as an artifact.

## Note
This file can be deleted after the workflow is updated.
