## Summary

Fixes #13390 by preventing properties declared in a dependency POM from activating property-based profiles in that same POM during external model building.

## Problem

Maven 3.10.0 made dependency POM properties available to the external profile activator. As a result, a POM such as:

```xml
<properties>
  <example.enable.extra>true</example.enable.extra>
</properties>
```

could activate its own profile:

```xml
<activation>
  <property>
    <name>example.enable.extra</name>
  </property>
</activation>
```

This caused dependency profiles to contribute unexpected dependencies. It also violated Maven's documented profile activation behavior, where POM-defined properties should not themselves activate profiles.

## Regression Cause

The regression was introduced by the external-model profile activation sandbox from #13114, the 3.10.x backport of #13112, which fixed #13084. That change merged the dependency model's project properties into the system-property lookup used during profile activation. Consequently, a property declared in a dependency POM could be mistaken for a system property and activate a profile in that same POM.

The fix retains the security behavior introduced by #13084: consumer user properties are still suppressed, file-based activation remains filtered, and platform/system-property activation continues to work.

## Changes

- Stop exposing dependency POM-local properties through the external profile activation system-property lookup.
- Preserve activation based on actual system/platform properties such as `java.version` and `os.name`.
- Continue suppressing consumer user properties (`-D`) when activating profiles from dependency POMs.
- Preserve negated-property/default-on profiles such as RESTEasy's `!resteasy.dependencies.eap` profile.
- Apply the fix consistently to both `compat/maven-model-builder` and `impl/maven-impl`.

## Edited and Added Tests

- Edited `compat/maven-model-builder/src/test/java/org/apache/maven/model/building/ExternalModelProfileActivationTest.java`:
  - Changed the external-model assertion so a positive property activation backed by the POM's own `<properties>` remains inactive.
  - Kept coverage for system-property activation, user-property suppression, file activation filtering, JDK activation, and negated-property default-on activation.
- Edited `impl/maven-impl/src/test/java/org/apache/maven/impl/model/DefaultModelBuilderTest.java`:
  - Added an assertion that the dependency POM's declared property does not activate its profile.
- Edited `impl/maven-impl/src/test/resources/poms/factory/resolved-model-with-profiles.xml`:
  - Added a POM-local property and a profile activated by that property to reproduce #13390.
- Edited both external profile activation implementations:
  - `compat/maven-model-builder/.../DefaultModelBuilder.java`
  - `impl/maven-impl/.../DefaultProfileActivationContext.java`
  - Removed the POM-property fallback from the external system-property lookup.

## Validation

- `git diff --check` passes.
- The worktree was reviewed after the changes and contains only the PR description file as an uncommitted documentation addition.
- Full Maven test execution was not possible in the development environment because Maven is not installed (`mvn: command not found`).
- The regression tests are included in the PR but have not been executed locally due to the missing Maven installation.
