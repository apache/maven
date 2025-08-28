# GH-11074: Recursive Project URL Test

This integration test demonstrates the behavior difference between Maven 3 and Maven 4 when handling recursive variable references in `project.url`.

## Issue Description

When a POM contains `<url>${project.url}</url>`, Maven 4 now correctly detects this as a recursive variable reference and fails with:

```
[ERROR] recursive variable reference: project.url
```

This was not detected in Maven 3, but Maven 4's improved variable resolution correctly identifies and prevents infinite loops during interpolation.

## Test Case

The test POM (`pom.xml`) contains:

```xml
<url>${project.url}</url>
```

This creates a recursive reference where `project.url` tries to resolve to itself.

## Expected Behavior

- **Maven 3**: Build fails with "recursive variable reference: project.url" error
- **Maven 4+**: Build fails with "recursive variable reference: project.url" error

Both versions correctly detect and reject recursive references when building projects directly.

## Real-world Example

This issue was observed with dependencies like `com.slack.api:slack-api-client:jar:1.45.4` which contained similar recursive references in their POMs.
