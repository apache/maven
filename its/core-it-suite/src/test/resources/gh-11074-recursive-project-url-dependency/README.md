# GH-11074: Recursive Project URL in Dependency POM Test

This integration test demonstrates how Maven 4 handles recursive variable references in dependency POMs.

## Issue Description

When a dependency's POM contains `<url>${project.url}</url>`, Maven 4 correctly detects this as a recursive variable reference and produces a warning while still allowing the build to continue.

## Test Setup

- `pom.xml`: Consumer project that depends on `bad-dependency`
- `repo/org/apache/maven/its/mngXXXX/bad-dependency/1.0/bad-dependency-1.0.pom`: Dependency POM with recursive `project.url`
- `settings-template.xml`: Settings to use the local test repository

## Expected Behavior

- **Maven 3**: Build succeeds, recursive reference may not be detected
- **Maven 4+**: Build succeeds but shows warning:
  ```
  [WARNING] The POM for org.apache.maven.its.gh11074:bad-dependency:jar:1.0 is invalid, transitive dependencies (if any) will not be available: 1 problem was for org.apache.maven.its.gh11074:bad-dependency:jar:1.0
      - [ERROR] recursive variable reference: project.url
  ```

## Real-world Impact

This matches the behavior described in the original issue where dependencies like `com.slack.api:slack-api-client:jar:1.45.4` would show similar warnings in Maven 4 but not cause build failures.
