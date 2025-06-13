# GitHub Actions Workflow Update Required

## Issue
The GitHub Actions workflow `.github/workflows/maven.yml` needs to be updated to work with the new integration test default behavior.

## Problem
The new default behavior expects to find the Maven distribution at:
`apache-maven/target/apache-maven-${project.version}-bin.zip`

But in the CI workflow, the distribution is downloaded as an artifact to `maven-dist/` directory.

## Required Change
In the `integration-tests` job, after the "Download Maven distribution" step (around line 216), add a new step to copy the distribution to the expected location:

```yaml
      - name: Copy distribution for integration tests
        shell: bash
        run: |
          mkdir -p apache-maven/target
          cp maven-dist/apache-maven-*-bin.zip apache-maven/target/
```

This should be added before the "Extract Maven distribution" step.

## Alternative Solution
Instead of copying the file, you could use the `maven-distro` profile:
```yaml
run: mvn install -e -B -V -Prun-its,mimir -DmavenDistro=$PWD/maven-dist/apache-maven-*-bin.zip
```

## Reason
The new default behavior extracts and tests the built distribution from the expected location. By copying the downloaded distribution to that location, the default behavior works as intended and we test the actual built distribution.

## Note
This file can be deleted after the workflow is updated.
