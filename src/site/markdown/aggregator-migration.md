<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Migrating Aggregator Mojos to Lifecycle Ordering

This guide helps Maven plugin authors evaluate whether a Mojo using
`aggregator = true` or lifecycle forking should migrate to lifecycle
ordering with `@After` and phases such as `after:all`.

**Important:** `aggregator = true`, lifecycle forking, and lifecycle ordering
serve different purposes. They are not interchangeable. Choose the migration
based on the behavior the plugin requires.

## 1. Understand the existing behavior

A legacy Mojo may declare itself as an aggregator:

```java
@Mojo(name = "report", aggregator = true)
public class ReportMojo {
    // ...
}
```

Aggregator status affects how a goal operates in a multi-project build.
It does not, by itself, express a precise ordering dependency on another
lifecycle phase.

Some legacy Mojos also use `@Execute` to request execution of another goal
or lifecycle phase. This introduces lifecycle-forking behavior that must
be considered separately when migrating.

Before changing an annotation, identify whether the Mojo needs to:

- Run once for a multi-project build rather than once per project.
- Run after a particular lifecycle phase has completed.
- Access information or dependencies from other projects.
- Fork another lifecycle or goal before executing its own work.

These requirements are distinct and may require different solutions.

## 2. Express lifecycle ordering with `@After`

The Maven API provides the `@After` annotation to express that a Mojo
must run after a specified lifecycle phase.

For example:

```java
import org.apache.maven.api.plugin.annotations.After;
import org.apache.maven.api.plugin.annotations.Mojo;

@Mojo(name = "report")
@After(phase = "resources")
public class ReportMojo {
    // ...
}
```

This declares an ordering constraint relative to the `resources` phase.
It does not, on its own, make the Mojo an aggregator or guarantee that it
runs only once for the entire reactor.

The annotation also supports an ordering type:

```java
@After(phase = "package", type = After.Type.PROJECT)
```

The available types are:

- `PROJECT`: expresses an ordering relationship for a project.
- `DEPENDENCIES`: expresses an ordering relationship involving dependencies.
- `CHILDREN`: expresses an ordering relationship involving child projects.

For dependency ordering, `scope` can further describe the dependency scope:

```java
@After(
    phase = "ready",
    type = After.Type.DEPENDENCIES,
    scope = "compile"
)
```

Use the actual phases and relationship required by your plugin. Verify the
supported phase names and ordering semantics against the Maven version
your plugin targets.

## 3. Consider `after:all`

Maven lifecycle phases such as `after:all` provide a way to express work
associated with the end of the overall build lifecycle.

A plugin goal may be bound to such a phase in a lifecycle configuration.
For example, a POM can bind a goal to `after:all`:

```xml
<execution>
  <id>final-report</id>
  <phase>after:all</phase>
  <goals>
    <goal>report</goal>
  </goals>
</execution>
```

Replace `report` with the actual goal name and configure the plugin
coordinates and execution as required.

Do not assume that moving a goal to `after:all` automatically preserves
all behavior of a legacy aggregator Mojo. Check how the goal is scheduled,
how often it executes, and what project and session information it receives.

## 4. Review dependency resolution

Maven provides the experimental `@Resolution` annotation for declaring dependency resolution requirements on a Mojo field. It has been available since Maven 4.0.0.

For example, a Mojo that needs resolved dependency paths can declare:

```java
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.plugin.annotations.Resolution;

@Resolution(
    pathScope = "compile",
    requestType = "resolve"
)
private List<Path> dependencyPaths;
```

Place this field inside the Mojo class and verify that its type and resolution requirements match the needs of the plugin.

The `pathScope` specifies the dependency path scope. The `requestType` can be `collect`, `flatten`, or `resolve`; when omitted, Maven infers the request type from the field type and other annotation attributes.

Do not add `@Resolution` merely because a Mojo previously used `aggregator = true`. Declare dependency resolution requirements only when the Mojo needs them.

## 5. Understand lifecycle forking

A legacy `@Execute` declaration can request a separate lifecycle or goal
execution before the Mojo runs. An ordering constraint is not equivalent
to such a fork.

Before removing `@Execute`, determine:

- Which phase or goal is forked.
- Whether the fork is required to generate inputs for the Mojo.
- Whether those inputs will already exist at the new execution point.
- Whether the new ordering changes execution count or build behavior.

If the plugin depends on lifecycle-forking semantics, do not remove the
fork until equivalent behavior has been demonstrated by tests.

## 6. Migrate incrementally

A safe migration process is:

1. Identify the reason the Mojo uses `aggregator = true` or `@Execute`.
2. Separate reactor-wide execution requirements from lifecycle ordering.
3. Select an appropriate lifecycle phase or `@After` constraint only where
   it represents the required ordering.
4. Declare dependency resolution requirements explicitly where needed.
5. Preserve lifecycle forking if the Mojo still depends on it.
6. Test a single-project build and a multi-project reactor.
7. Test both legacy and concurrent builders when the target Maven version
   supports them.
8. Compare execution counts, logs, generated files, and dependency results
   with the previous implementation.

## 7. Compatibility considerations

The annotations and lifecycle features available depend on the Maven API
and runtime version. Check the minimum Maven version supported by your
plugin before adopting newer APIs.

If the plugin must support multiple Maven versions, consider maintaining
a compatible implementation or documenting the required version. Avoid
assuming that a new API annotation is available in every runtime that
can execute the legacy plugin.

## 8. Testing checklist

Before releasing a migrated plugin, verify that:

- The goal executes at the intended point in the lifecycle.
- The goal runs the expected number of times in a reactor.
- Project and session information remain correct.
- Required dependencies have been collected or resolved.
- Lifecycle forking still behaves as intended.
- Ordering remains correct with concurrent builds.
- Existing integration tests continue to pass.

A migration is complete only when the new implementation preserves the
plugin's required behavior, not simply when the old annotation has been
removed.