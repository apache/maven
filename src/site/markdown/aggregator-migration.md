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

> **Warning:** The Maven 4 APIs discussed in this guide, including
> `@Mojo`, `@After`, `@Resolution`, and `@Execute`, are marked
> `@Experimental`. Their contracts may change or the APIs may be
> removed without notice. Plugin authors should take this into
> account before adopting them in production plugins.

## 1. Understand the existing behavior

A legacy Mojo may declare itself as an aggregator:

```java
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "report", aggregator = true)
public class ReportMojo {
    // ...
}
```

Aggregator status affects how a goal operates in a multi-project build.
It does not, by itself, express a precise ordering dependency on another
lifecycle phase.

Some legacy Mojos also use `@Execute` to request execution of another
goal or lifecycle phase. This introduces lifecycle-forking behavior that
must be considered separately when migrating.

Before changing an annotation, identify whether the Mojo needs to:

- Run once for a multi-project build rather than once per project.
- Run after a particular lifecycle phase has completed.
- Access information or dependencies from other projects.
- Fork another lifecycle or goal before executing its own work.

These requirements are distinct and may require different solutions.

### Direct migration to the Maven 4 `@Mojo` annotation

Maven 4 provides its own `@Mojo` annotation in
`org.apache.maven.api.plugin.annotations`. It retains the
`aggregator` attribute.

If a plugin needs to preserve its existing aggregator behavior while
adopting the Maven 4 annotation, it can retain `aggregator = true`:

```java
import org.apache.maven.api.plugin.annotations.Mojo;

@Mojo(name = "report", aggregator = true)
public class ReportMojo {
    // ...
}
```

This is the direct annotation migration path. The Maven 4 `aggregator`
attribute identifies a Mojo that uses the Maven project and its
subprojects.

However, retaining `aggregator = true` does not express a specific
lifecycle-ordering relationship. If the plugin requires execution
after a particular phase or after the entire reactor, consider the
lifecycle-ordering mechanisms described below.

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

The `@After` annotation is repeatable. A Mojo can declare multiple
ordering constraints when it must run after more than one phase:

```java
@After(phase = "compile")
@After(phase = "test-compile")
public class ReportMojo {
    // ...
}
```

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

## 3. Run a Mojo after the entire reactor with `after:all`

Maven 4 introduces additional lifecycle phases, including `all`,
`each`, `before:all`, `after:all`, `before:each`, and `after:each`.

The `after:all` phase runs at the end of a project's build. In a
multi-project build, the `after:all` phases of child projects run
before the `after:all` phase of their parent project.

This allows a goal bound to `after:all` in the root project to execute
after the child projects have completed their builds.

For example, bind a goal to `after:all` in the root POM:

```xml
<plugin>
  <groupId>org.example</groupId>
  <artifactId>example-maven-plugin</artifactId>
  <version>1.0.0</version>
  <inherited>false</inherited>
  <executions>
    <execution>
      <id>final-report</id>
      <phase>after:all</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Replace the plugin coordinates and goal name with the actual values
for your plugin.

The `<inherited>false</inherited>` setting prevents the plugin
configuration from being inherited by child projects. When the
execution is declared only in the root POM, this allows the goal to
run at the root project's `after:all` phase after the child projects
have completed.

Do not assume that binding a goal to `after:all` automatically
preserves every behavior of a legacy aggregator Mojo. Check how the
goal is scheduled, how often it executes, and what project and session
information it receives.

Also distinguish `after:all` from `after:<phase>`. Use `after:all`
when the goal needs to run after the complete project hierarchy has
finished its build. Use a phase-specific ordering constraint when
the goal only needs to wait for a particular phase.

## 4. Review dependency resolution

Maven provides the experimental `@Resolution` annotation for declaring
dependency resolution requirements on a Mojo field. It has been available
since Maven 4.0.0.

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

Place this field inside the Mojo class and verify that its type and
resolution requirements match the needs of the plugin.

The `pathScope` specifies the dependency path scope. The `requestType`
can be `collect`, `flatten`, or `resolve`; when omitted, Maven infers
the request type from the field type and other annotation attributes.

Do not add `@Resolution` merely because a Mojo previously used
`aggregator = true`. Declare dependency resolution requirements only
when the Mojo needs them.

## 5. Understand lifecycle forking

A legacy `@Execute` declaration can request a separate lifecycle or goal
execution before the Mojo runs. An ordering constraint is not equivalent
to such a fork.

Before removing `@Execute`, determine:

- Which phase or goal is forked.
- Whether the fork is required to generate inputs for the Mojo.
- Whether those inputs will already exist at the new execution point.
- Whether the new ordering changes execution count or build behavior.

The `@After` annotation expresses an ordering relationship; it does
not itself execute a forked lifecycle or goal.

If the plugin depends on lifecycle-forking semantics, do not remove the
fork until equivalent behavior has been demonstrated by tests.

## 6. Migrate incrementally

A safe migration process is:

1. Identify the reason the Mojo uses `aggregator = true` or `@Execute`.
2. Separate reactor-wide execution requirements from lifecycle ordering.
3. Determine whether retaining the Maven 4 `@Mojo` annotation's
   `aggregator` attribute is sufficient.
4. Select an appropriate lifecycle phase or `@After` constraint only
   where it represents the required ordering.
5. Use `after:all` when the goal must execute after the complete
   reactor build, and configure the execution in the root POM
   appropriately.
6. Declare dependency resolution requirements explicitly where needed.
7. Preserve lifecycle forking if the Mojo still depends on it.
8. Test a single-project build and a multi-project reactor.
9. Test both legacy and concurrent builders when the target Maven
   version supports them.
10. Compare execution counts, logs, generated files, and dependency
    results with the previous implementation.

## 7. Compatibility considerations

The annotations and lifecycle features available depend on the Maven API
and runtime version. Check the minimum Maven version supported by your
plugin before adopting newer APIs.

The Maven 4 annotations discussed in this guide are experimental.
Their contracts may change or the APIs may be removed without notice.

If the plugin must support multiple Maven versions, consider maintaining
a compatible implementation or documenting the required version. Avoid
assuming that a new API annotation is available in every runtime that
can execute the legacy plugin.

## 8. Testing checklist

Before releasing a migrated plugin, verify that:

- The goal executes at the intended point in the lifecycle.
- The goal runs the expected number of times in a reactor.
- A goal bound to `after:all` in the root POM executes after its child
  projects have completed.
- Project and session information remain correct.
- Required dependencies have been collected or resolved.
- Lifecycle forking still behaves as intended.
- Ordering remains correct with concurrent builds.
- Existing integration tests continue to pass.

A migration is complete only when the new implementation preserves the
plugin's required behavior, not simply when the old annotation has
been removed.
