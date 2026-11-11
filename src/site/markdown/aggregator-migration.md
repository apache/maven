
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Migrating Aggregator Mojos to Maven 4 Lifecycle Ordering

This guide explains how Maven plugin authors can migrate from the legacy
`@Mojo(aggregator = true)` and `@Execute` APIs to Maven 4's explicit
lifecycle-ordering and dependency-resolution APIs.

It covers:

- The behavior of existing aggregator mojos with different Maven builders.
- The mapping from `aggregator = true` to `after:all` or `after:<phase>`.
- How to use `@Resolution` for dependency resolution.
- How to replace lifecycle forking with `@After`.
- How to access reactor projects through the `Session` API.
- Important behavioral differences between Maven builders.
- An incremental migration path for existing plugins.

> **Important:** Existing plugins using `aggregator = true` do not necessarily
> need an immediate source migration. The concurrent-builder improvement
> proposed in [PR #13798](https://github.com/apache/maven/pull/13798) allows
> existing lifecycle-bound aggregator goals to receive the correct
> phase-specific ordering automatically when using a Maven version containing
> that change. The explicit lifecycle-ordering API remains the recommended
> approach for new plugins and for consistent behavior across builders.

## 1. Understanding the Existing Aggregator API

Maven plugins traditionally use `@Mojo(aggregator = true)` to declare goals
that operate at the reactor level rather than independently on each project.

A typical aggregator mojo looks like this:

```java
@Mojo(
    name = "aggregate",
    aggregator = true,
    threadSafe = true
)
public class AggregateMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException {
        // Aggregate information from reactor projects.
    }
}
```

The `aggregator = true` flag combines several responsibilities:

- Reactor-level execution.
- Execution ordering relative to child projects.
- Dependency-resolution behavior.
- Inheritance and execution behavior.
- Special handling by the build planner.

Combining these responsibilities in one boolean makes the execution model
less explicit and creates additional complexity for the build planner.

Maven 4 provides more specific APIs that allow plugin authors to declare
these requirements independently.

However, the behavior of existing plugins must be considered separately
from the recommended migration path for new plugins.

## 2. Understanding the Concurrent-Builder Improvement

[PR #13798](https://github.com/apache/maven/pull/13798) changes how the
concurrent builder schedules existing lifecycle-bound aggregator goals.

### 2.1 How existing aggregator goals behave

Consider an aggregator mojo bound to the `package` phase.

The mojo may need the packaged artifacts produced by all child modules.
Previously, the concurrent builder could schedule the aggregator before
those child projects had completed the declared phase.

PR #13798 addresses this by automatically scheduling aggregator goals as
DAG steps after the children have completed the declared lifecycle phase.

For example, an aggregator bound to `package` waits for the child projects
to complete `package`.

This is equivalent to an `after:package` relationship with children, not
to `after:all`.

### 2.2 Existing plugins do not require immediate migration

With the concurrent-builder improvement:

- Existing plugins using `aggregator = true` receive improved
  phase-specific ordering without source changes.
- An aggregator bound to a lifecycle phase runs after its child projects
  complete that declared phase.
- Inherited copies are suppressed when an ancestor in the active reactor
  already provides the execution.
- Plugin authors do not need to modify existing plugin source code solely
  to obtain this concurrent-builder scheduling behavior.

This behavior applies to the concurrent builder. It does not mean that
the legacy singlethreaded and `-T N` builders have received the same
implementation change.

### 2.3 Why this is not the same as `after:all`

The distinction is important.

An aggregator bound to `package` should run after the children complete
`package`. It should not necessarily wait for the entire reactor lifecycle.

`after:all` has a different meaning: it runs after the entire reactor
build has completed its lifecycle work.

Therefore:

- `after:<phase>` expresses ordering after a particular lifecycle phase.
- `after:all` expresses ordering after the complete reactor lifecycle.
- PR #13798 automatically provides phase-specific ordering for existing
  aggregator goals in the concurrent builder.

The automatic behavior introduced by PR #13798 should not be described
as a migration of `aggregator = true` to `after:all`.

## 3. Builder Compatibility Matrix

The behavior differs depending on the Maven builder.

| Problem | Singlethreaded | `-T N` | `-b concurrent` |
|---|---|---|---|
| Aggregator runs only at root | Yes | Yes | Yes |
| Aggregator runs after children finish phase | Yes (sequential) | Yes (`aggregatorLock`) | Yes, after PR #13798 |
| Lifecycle-bound aggregator runs per module | MNG-6336 remains open | MNG-6336 remains open | Fixed by PR #13798 |

The matrix highlights three different concerns:

1. **Root-level execution:** Aggregator goals operate at the reactor level.
2. **Phase ordering:** The aggregator must wait for child projects to
   complete the lifecycle phase whose outputs it needs.
3. **Per-module execution:** A lifecycle-bound aggregator should not run
   as an independent aggregator execution in every module when an
   ancestor in the active reactor already provides the execution.

The concurrent-builder improvement addresses the relevant scheduling and
inheritance behavior for `-b concurrent`.

The legacy builders behave differently:

- The singlethreaded builder relies on sequential execution.
- The `-T N` builder uses `aggregatorLock` for serialization.
- The lifecycle-bound aggregator-per-module problem tracked by
  [MNG-6336](https://issues.apache.org/jira/browse/MNG-6336) remains open
  for the singlethreaded and `-T N` builders.

Do not assume that the concurrent-builder improvement also changes the
legacy builders.

## 4. Mapping the Legacy API to Maven 4

The following table summarizes the recommended mapping.

| Legacy API or behavior | Maven 4 replacement | Purpose |
|---|---|---|
| `@Mojo(aggregator = true)` | `after:all` or `after:<phase>` | Explicit lifecycle and reactor ordering |
| Aggregator dependency resolution | `@Resolution(pathScope = ..., requestType = ...)` | Field-level dependency resolution |
| `@Execute(phase = "...")` | `@After(phase = "...", type = ...)` | Lifecycle ordering without forking |
| Reactor project access | `Session.getProjects()` | Access projects in the current reactor |
| `<inherited>false</inherited>` | Retain where required for parent-POM execution configuration | Controls POM inheritance |

The replacement APIs separate responsibilities that were previously
combined in `aggregator = true`.

For new plugins, this provides a clearer and more composable execution
model.

For existing plugins, migration should be based on the behavior the
plugin actually requires.

## 5. Express Lifecycle Ordering with `before:` and `after:` Phases

Maven 4 provides lifecycle phase prefixes that allow plugin authors to
declare ordering requirements explicitly.

The two important forms are:

- `before:<phase>` — runs before the specified lifecycle phase.
- `after:<phase>` — runs after the specified lifecycle phase.

These phase bindings work with all builders:

- Singlethreaded.
- Multithreaded (`-T N`).
- Concurrent (`-b concurrent`).

The concurrent builder enforces ordering through DAG edges. The legacy
builders maintain ordering through sequential execution.

### 5.1 Before: Legacy aggregator binding

A legacy aggregator mojo may look like this:

```java
@Mojo(
    name = "aggregate",
    defaultPhase = LifecyclePhase.PACKAGE,
    aggregator = true
)
public class AggregateMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException {
        // Collect packaged artifacts from reactor projects.
    }
}
```

### 5.2 After: Explicit phase ordering

For a new Maven 4 plugin, bind the goal to `after:package` when it needs
the outputs of the package phase.

The lifecycle binding is declared in the POM:

```xml
<execution>
    <id>aggregate</id>
    <phase>after:package</phase>
    <goals>
        <goal>aggregate</goal>
    </goals>
</execution>
```

The corresponding mojo no longer needs the legacy aggregator flag
for this explicit ordering configuration:

```java
@Mojo(name = "aggregate")
public class AggregateMojo {

    // Maven 4 mojo implementation.
}
```

Use `after:package` when the mojo needs packaged artifacts from the
child projects.

Similarly:

- Use `after:compile` when the mojo needs compiled output.
- Use `after:test` when the mojo needs test-phase results.
- Use `after:package` when the mojo needs packaged artifacts.
- Use `after:all` when the mojo must wait for the entire reactor lifecycle.

Choose the phase that matches the actual requirement of the mojo.

### 5.3 Running after the entire reactor with `after:all`

Use `after:all` when a goal must execute after every project in the
reactor has completed its full lifecycle.

For example, a reporting or aggregation mojo may need the final results
from all modules.

A root POM execution can be configured as follows:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.example</groupId>
            <artifactId>example-maven-plugin</artifactId>
            <version>1.0.0</version>
            <executions>
                <execution>
                    <id>aggregate-reactor-results</id>
                    <phase>after:all</phase>
                    <inherited>false</inherited>
                    <goals>
                        <goal>aggregate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

When the execution is declared in a parent POM and must run only there,
`<inherited>false</inherited>` prevents the execution from being inherited
by child POMs.

The `after:all` phase provides the lifecycle ordering. The inheritance
configuration controls where the execution is declared and inherited.
These are separate concerns.

The `after:all` phase of child projects completes before the corresponding
phase of their parent, allowing a root-level aggregation goal to run
after child lifecycle work.

## 6. Replace Aggregator Dependency Resolution with `@Resolution`

The legacy aggregator behavior could imply broad dependency resolution,
even when a mojo needed only a particular dependency scope or request type.

Maven 4 introduces `@Resolution` to declare dependency-resolution
requirements on individual injected fields.

### 6.1 Before: Broad aggregator resolution

With the legacy aggregator approach, dependency resolution could be
associated with the aggregator mojo as a whole.

This made it difficult to express different resolution requirements
for different injected fields.

### 6.2 After: Field-level resolution

Maven 4 allows each field to declare its resolution requirements.

For example:

```java
@Mojo(name = "aggregate")
public class AggregateMojo {

    @Parameter
    @Resolution(pathScope = "main-compile")
    private List<Path> dependencies;

}
```

The `pathScope` identifies the dependency path scope.

The optional `requestType` controls the kind of request:

- `collect`
- `flatten`
- `resolve`

When `requestType` is not specified, Maven infers it from the
`pathScope` and the injected field type.

The appropriate scope and request type should be selected according
to the data the mojo actually requires.

### 6.3 Benefits of granular resolution

Using `@Resolution` provides:

- More precise dependency resolution.
- Control over dependency path scope.
- Control over the resolution request type.
- Different resolution requirements for different fields.
- Less unnecessary dependency resolution.
- A clearer declaration of the data required by the mojo.

This replaces the broad aggregator-level resolution approach with
field-level requirements.

## 7. Replace `@Execute` with `@After`

The legacy `@Execute` annotation allows a mojo to fork another
lifecycle execution.

For example:

### 7.1 Before: Lifecycle forking

```java
@Mojo(
    name = "aggregate",
    aggregator = true
)
@Execute(phase = LifecyclePhase.PACKAGE)
public class AggregateMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException {
        // Aggregate results after the forked lifecycle.
    }
}
```

Lifecycle forking can cause goals to execute again as part of the
forked lifecycle.

This can lead to duplicate execution and unnecessary work, including
the problem tracked by
[MNG-7163](https://issues.apache.org/jira/browse/MNG-7163).

### 7.2 After: Explicit ordering with `@After`

Maven 4 provides `@After` to express lifecycle ordering without
starting another lifecycle execution.

For example:

```java
@Mojo(name = "aggregate")
@After(
    phase = "package",
    type = After.Type.CHILDREN
)
public class AggregateMojo {

    // Maven 4 mojo implementation.
}
```

The `After.Type` values describe the project relationship:

- `PROJECT` — ordering relative to the specified phase in the project.
- `CHILDREN` — ordering relative to the specified phase in child projects.
- `DEPENDENCIES` — ordering relative to the specified phase in dependency
  projects.

Choose the type that matches the projects whose execution must complete
before the mojo runs.

### 7.3 Why `@After` is preferred

Replacing `@Execute` with `@After` provides:

- Explicit lifecycle ordering.
- Ordering constraints that the build planner can enforce.
- No additional lifecycle fork.
- Reduced risk of duplicate goal execution.
- A clearer relationship between the mojo and the lifecycle phase it
  depends on.

The replacement of `@Execute` with `@After` is independently motivated
by lifecycle-forking and duplicate-execution concerns. It is not
dependent on the concurrent-builder aggregator improvement.

## 8. Access Reactor Projects Through the Session API

Legacy aggregator mojos often rely on aggregator-specific behavior to
access information from all projects in the reactor.

Maven 4 provides the `Session` API for accessing the current build
session and its projects.

The reactor project list can be obtained through:

```java
session.getProjects()
```

For example, a Maven 4 mojo can inject the session and iterate over
the projects:

```java
@Mojo(name = "aggregate")
public class AggregateMojo {

    @Inject
    private Session session;

    public void execute() {
        for (Project project : session.getProjects()) {
            // Process each project in the reactor.
        }
    }
}
```

Use the Maven 4 API types and imports corresponding to the Maven API
version targeted by the plugin.

This separates reactor project access from the legacy aggregator flag
and makes the required project collection explicit.

## 9. Important Behavioral Differences

### 9.1 Builder compatibility

The `before:` and `after:` phase system works with all builders.

It is not exclusive to the concurrent builder.

The integration test `MavenITmng8244PhaseAllTest` verifies phase behavior
with legacy and concurrent builders.

The implementation differs:

- The concurrent builder uses DAG edges to enforce ordering.
- The singlethreaded builder executes phases sequentially.
- The `-T N` builder uses its existing aggregator-lock behavior where
  applicable.

### 9.2 Ordering guarantees

With the concurrent builder, lifecycle ordering is represented through
DAG dependencies.

This allows Maven to coordinate project and child-project execution
without relying on the legacy `aggregatorLock` mechanism for the
phase-based ordering model.

With legacy builders, ordering is maintained through sequential
execution.

### 9.3 No lifecycle forking

`@After` specifies an ordering constraint without starting another
lifecycle execution.

This differs from `@Execute`, which requests lifecycle forking.

Avoiding lifecycle forking helps prevent duplicate execution and
addresses the behavior tracked by MNG-7163 / #8291.

### 9.4 Granular dependency resolution

`@Resolution` allows each injected field to declare its dependency
resolution requirements.

This replaces broad aggregator-level resolution with more precise
control over dependency paths and request types.

### 9.5 POM model compatibility

The `before:` and `after:` phase bindings work with POM model versions
4.0.0 and 4.1.0.

The `@After` and `@Resolution` annotations are part of the Maven 4 API.

A POM model version upgrade is not required solely to use these phase
bindings.

## 10. Why `aggregator = true` Should Eventually Be Deprecated

The concurrent-builder improvement makes existing aggregator plugins
work more correctly without source changes in that builder.

However, this does not remove the reasons for moving toward explicit
lifecycle-ordering APIs.

The deprecation rationale is based on API design, not on a claim that
all existing aggregator plugins are broken.

### 10.1 Semantic ambiguity

The `aggregator` boolean combines several different concerns:

- Execution ordering.
- Dependency-resolution scope.
- Inheritance behavior.
- Reactor-level execution semantics.

A single boolean does not clearly communicate which behavior a plugin
actually requires.

The newer APIs separate these concerns and make the intended behavior
explicit.

### 10.2 Maintenance burden

The legacy aggregator flag requires special handling in the build
planner to provide the expected behavior.

Explicit lifecycle phase bindings provide a more composable model.

Instead of relying on a planner special case, plugin authors can
declare the ordering relationship directly.

### 10.3 The legacy-builder gap remains

The concurrent-builder improvement does not solve the lifecycle-bound
aggregator-per-module issue for the singlethreaded and `-T N` builders.

This is tracked by
[MNG-6336](https://issues.apache.org/jira/browse/MNG-6336).

The explicit lifecycle-ordering API provides a clearer way to express
the required behavior across builders.

### 10.4 `@Execute` has an independent replacement path

The move from `@Execute` to `@After` remains independently justified.

The issue is lifecycle forking and the possibility of duplicate goal
execution.

Even when an existing aggregator goal receives improved scheduling
from the concurrent builder, replacing lifecycle forking with explicit
ordering remains a separate migration concern.

### 10.5 When should deprecation happen?

Deprecation should be coordinated with the availability of the
replacement path.

The proposed conditions are:

1. PR #13798 is merged, so existing aggregator goals receive the
   improved concurrent-builder scheduling.
2. At least one major plugin, such as Maven Javadoc Plugin or Maven
   Site Plugin, has migrated to demonstrate that the new API is practical.
3. Plugin descriptor generation from `@After` annotations is available
   without requiring external tooling.

These conditions help ensure that plugin authors have a usable migration
path before the legacy API is formally deprecated.

## 11. Plugin Descriptor Generation

A Maven plugin's annotations must be represented in its plugin
descriptor so Maven can discover and execute its goals.

Historically, Maven plugin authors have used `maven-plugin-tools`
to generate plugin descriptors.

[PR #13658](https://github.com/apache/maven/pull/13658) introduces
`maven-api-plugin-processor`, a JSR 269 annotation processor for
Maven 4 plugin descriptor generation.

The processor generates plugin descriptor metadata at compile time,
including:

- Mojo metadata from `@Mojo`.
- Parameter metadata from `@Parameter`.
- Dependency-resolution metadata from `@Resolution`.
- Ordering metadata from `@After` and repeatable `@Afters`.
- The `<afterLinks>` entries required to represent lifecycle ordering.

The processor is designed to be integrated directly into Apache Maven's
Maven 4 plugin development workflow without requiring external tooling
for this annotation processing.

This is important because declaring `@After` in source code is not
sufficient unless the corresponding ordering metadata is generated
in the plugin descriptor.

Once the processor is available and integrated into the Maven 4 plugin
development workflow, plugin authors will not need to wait for
`maven-plugin-tools` to add `@After` support.

The longer-term direction is to use the built-in processor for Maven 4
plugin descriptor generation, simplifying the plugin development path.

## 12. Incremental Migration Path

Plugin authors do not need to migrate every part of an existing plugin
at the same time.

The migration can be performed incrementally.

### Step 0: Understand existing behavior

First, identify the Maven version and builder used by consumers.

With the concurrent-builder improvement from PR #13798, existing
`aggregator = true` plugins already receive improved phase-specific
ordering in `-b concurrent` builds when using a Maven version that
includes the change.

Therefore, an immediate source migration is not required solely to
obtain that concurrent-builder ordering behavior.

However, this does not resolve the legacy-builder gap tracked by
MNG-6336.

Migration to explicit lifecycle ordering is recommended for new
plugins and for consistent behavior across all builders.

### Step 1: Replace implicit ordering with an explicit phase binding

Identify the lifecycle phase whose outputs the aggregator mojo needs.

Choose the appropriate binding:

- `after:compile` for compiled output.
- `after:test` for test-phase results.
- `after:package` for packaged artifacts.
- `after:all` when the entire reactor lifecycle must complete.

This makes the ordering requirement explicit.

### Step 2: Replace `@Execute` with `@After`

If the mojo uses `@Execute`, identify the lifecycle phase and project
relationship it depends on.

Replace the forked lifecycle with the corresponding `@After`
ordering constraint.

This removes the need to start another lifecycle execution merely
to ensure that another phase has completed.

### Step 3: Migrate dependency resolution to `@Resolution`

Review the dependencies injected into the mojo.

For each field, determine:

- The required dependency path scope.
- The required request type.
- Whether the mojo needs collection, flattening, or resolution.

Declare the appropriate requirements using `@Resolution`.

This avoids resolving more dependencies than the mojo actually needs.

### Step 4: Replace implicit reactor project access

Review how the mojo obtains reactor project information.

Use the Maven `Session` API and `session.getProjects()` to retrieve
the projects required by the aggregation logic.

This makes reactor project access explicit and independent of the
legacy aggregator flag.

### Step 5: Verify plugin descriptor generation

Ensure that the plugin descriptor contains the metadata required
by the Maven 4 API.

In particular, verify that:

- Mojo metadata is generated.
- Parameter metadata is present.
- Resolution metadata is present for fields using `@Resolution`.
- Ordering metadata is present for `@After` annotations.

Use the Maven 4 annotation processor when it is available in the
plugin's build environment.

### Step 6: Test with all supported builders

Test the migrated plugin with:

- The singlethreaded builder.
- The `-T N` builder.
- The concurrent builder using `-b concurrent`.

Do not assume that passing tests with one builder guarantees the
same behavior with the others.

## 13. Testing Checklist

Before releasing a migrated plugin, verify the following.

### Lifecycle ordering

- [ ] The mojo executes after the lifecycle phase it depends on.
- [ ] Child projects complete the required phase before the aggregator runs.
- [ ] `after:all` is used only when the entire reactor lifecycle must finish.
- [ ] `@After` expresses the intended project relationship.
- [ ] The mojo does not rely on unnecessary lifecycle forking.

### Aggregator behavior

- [ ] The mojo processes the intended reactor projects.
- [ ] Root-level execution does not create unwanted inherited copies.
- [ ] The behavior is tested with nested multi-module projects.
- [ ] The behavior is tested with relevant profiles and plugin executions.
- [ ] Existing `aggregator = true` behavior is understood for supported
      Maven versions and builders.

### Dependency resolution

- [ ] Each `@Resolution` field declares the required path scope.
- [ ] The request type matches the intended resolution operation.
- [ ] The plugin does not resolve unnecessary dependencies.
- [ ] Required artifacts are available when the mojo executes.

### Builder compatibility

- [ ] The plugin is tested with the singlethreaded builder.
- [ ] The plugin is tested with the `-T N` builder.
- [ ] The plugin is tested with `-b concurrent`.
- [ ] Expected behavior is documented for each supported builder.
- [ ] The MNG-6336 limitation is considered where relevant.

### Plugin descriptor

- [ ] The generated descriptor contains the expected mojo metadata.
- [ ] `@After` ordering metadata is generated correctly.
- [ ] `@Resolution` metadata is generated correctly.
- [ ] The plugin can be discovered and executed by the target Maven version.

## 14. Related Issues, Pull Requests, and Tests

The following references provide additional implementation and design
context.

### Issues

- [MNG-6336](https://issues.apache.org/jira/browse/MNG-6336) —
  Lifecycle-bound aggregator goals running per module with legacy builders.
- [MNG-7163](https://issues.apache.org/jira/browse/MNG-7163) —
  Aggregating mojos re-executing goals for child modules that have
  already executed them.
- [Maven issue #13764](https://github.com/apache/maven/issues/13764) —
  Plugin author migration guide for `aggregator=true` to `after:all`.

### Pull requests

- [PR #13330](https://github.com/apache/maven/pull/13330) —
  Architecture specification for aggregator goal lifecycle behavior.
- [PR #13798](https://github.com/apache/maven/pull/13798) —
  Fix concurrent builder ordering for lifecycle-bound aggregators.
- [PR #13658](https://github.com/apache/maven/pull/13658) —
  Add `maven-api-plugin-processor` for Maven 4 plugin descriptor generation.

### Integration tests

The following integration test resources are useful when studying
phase-ordering behavior:

- `its/core/src/test/resources/mng-8244-phase-all/`
- `its/core/src/test/resources/mng-8653-*/`

The `MavenITmng8244PhaseAllTest` integration test is particularly
relevant for verifying that `before:` and `after:` phase behavior
works with both legacy and concurrent builders.

## 15. Summary

Maven 4 provides explicit APIs for lifecycle ordering, dependency
resolution, and reactor project access.

The concurrent-builder improvement in PR #13798 changes the migration
picture for existing aggregator plugins: they can receive correct
phase-specific ordering in concurrent builds without an immediate
source migration.

However, this improvement does not make `aggregator = true` the
preferred API for new plugins, nor does it resolve every limitation
of the legacy builders.

The recommended direction is:

- Use `after:<phase>` when a mojo must run after a particular lifecycle
  phase.
- Use `after:all` when a mojo must run after the entire reactor lifecycle.
- Use `@After` instead of `@Execute` to express ordering without
  lifecycle forking.
- Use `@Resolution` for explicit, field-level dependency resolution.
- Use `Session.getProjects()` to access reactor projects.
- Verify plugin descriptor generation and test behavior with all
  supported builders.

The goal is not to force every existing plugin to migrate immediately.
It is to provide plugin authors with a clearer, more explicit, and
more maintainable API for new development and gradual migration.
