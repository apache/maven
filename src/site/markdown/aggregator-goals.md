<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
# Aggregator Mojos and Reactor Lifecycle Specification

## Overview

Aggregation was introduced early in Maven 2 ([MNG-250](https://github.com/apache/maven/issues/2896)) to enable plugins to operate across an entire multi-module build reactor rather than on a single isolated module. It is exposed to plugin developers via the `@Mojo(aggregator = true)` annotation (or `@aggregator` in JavaDoc tag format).

This document analyzes the current behavior, outlines historical shortcomings, and establishes a target design specification for refactoring aggregator goals in Maven ([MNG-7991](https://github.com/apache/maven/issues/8718)).

---

## 1. Current Aggregator Behavior

Maven treats aggregator Mojos differently depending on whether they are invoked directly via the Command Line Interface (CLI) or bound to a build lifecycle phase in a POM.

### 1.1 CLI Invocation (`mvn plugin:goal`)
When an aggregating goal is invoked from the command line:
1. **Task Segment Classification**: `DefaultLifecycleTaskSegmentCalculator` inspects the Mojo descriptor:
   ```java
   boolean aggregating = mojoDescriptor.isAggregator() || !mojoDescriptor.isProjectRequired();
   ```
2. **Aggregating Task Segment**: A distinct aggregating `TaskSegment` is created.
3. **Execution on Root Project Only**: Aggregating task segments are restricted to the top-level project (`session.getTopLevelProject()`). Submodules in the reactor are skipped for this goal.
   - In the **singlethreaded** and **multithreaded** builders, this filtering is performed by `BuildListCalculator`.
   - In the **concurrent builder** (`-b concurrent`), it is performed by `BuildPlanExecutor` inside `buildInitialPlan()`:
     ```java
     Map<MavenProject, List<MavenProject>> projects = taskSegment.isAggregating()
         ? Collections.singletonMap(rootProject, allProjects.get(rootProject))
         : allProjects;
     ```
4. **Ordering**: Tasks are executed in CLI argument order — task segments are produced in CLI argument order with no automatic re-sorting. In `mvn clean install site:stage`, the non-aggregating `clean install` segment runs first (across all modules), then the aggregating `site:stage` segment runs once on the root. If the user writes `mvn site:stage install`, the aggregating segment runs first.

### 1.2 Lifecycle-Bound Execution (`<phase>...</phase>`)
When an aggregator Mojo is bound to a phase in a `pom.xml`:
1. **Lifecycle Segment Classification**: Lifecycle phases (e.g. `package`, `verify`) produce standard non-aggregating `TaskSegment`s.
2. **Per-Module Execution**: The goal is injected into the execution plan of **every module** in the reactor that inherits the plugin configuration.
3. **Redundant Executions**: Unless the project author explicitly configures `<inherited>false</inherited>` on the plugin execution in the parent POM, the aggregator Mojo executes once for each module in the reactor.
4. **Thread Locking (multithreaded builder only)**: When using the multithreaded builder (`-T`), `MojoExecutor` acquires an exclusive reactor-wide write lock (`aggregatorLock.writeLock()`) whenever an aggregator executes:
   ```java
   acquiredAggregatorLock = aggregator ? aggregatorLock.writeLock() : aggregatorLock.readLock();
   ```
   This means that executing an aggregator Mojo across multiple submodules repeatedly halts parallel build concurrency across all threads.

   In the **concurrent builder** (`-b concurrent`), project-level locking is disabled entirely — the concurrent `MojoExecutor` overrides `useProjectLock()` to return `false`. The concurrent builder instead relies on the build plan DAG to enforce ordering between steps.

### 1.3 Dependency Resolution
When executing an aggregator Mojo:
- `MojoExecutor.ensureDependenciesAreResolved` checks `mojoDescriptor.isAggregator()`.
- If `true`, Maven resolves dependencies not only for the current project, but also across all projects in `session.getProjects()` matching the scopes requested by the Mojo.

### 1.4 Forked Lifecycles
When an aggregator Mojo declares `@Execute(phase = ...)` or `@Execute(goal = ...)`, the concurrent builder (`BuildPlanExecutor`) handles each case differently:
- **`@Execute(phase = ...)`**: A forked plan is computed for the **current project only** (`Collections.singletonMap(step.project, ...)`), up to the specified phase. The main plan is appended after the forked plan as a DAG dependency.
- **`@Execute(goal = ...)`**: A forked plan is computed for the current project, plus `step.project.getCollectedProjects()` if the mojo is also an aggregator. This means forking a non-aggregator goal targets only the current project; forking an aggregator goal spans the reactor tree rooted at the current project, which can lead to duplicated builds, redundant tests, and nested reactor re-executions.

---

## 2. Identified Shortcomings & Problem Space

Over successive Maven releases, multiple discrepancies and pain points have been identified:

1. **CLI vs. Lifecycle Discrepancy ([MNG-6336](https://github.com/apache/maven/issues/7529))**:
   - Plugin authors annotate a goal with `@Mojo(aggregator = true)` with the expectation that Maven will execute it once per reactor.
   - While CLI invocation honors this expectation (running only on root), lifecycle binding runs it on every module.
   - Forcing users to configure `<inherited>false</inherited>` is error-prone, unintuitive, and breaks down in nested multi-module structures.

2. **Execution Timing: Aggregate at Start vs. Aggregate at End**:
   - In hierarchical builds, the root project is typically evaluated first in reactor order.
   - Consequently, an aggregator bound to `package` or `verify` on the root module executes *before* child modules have compiled or packaged their artifacts.
   - When the purpose of the aggregator is to combine artifacts or reports produced by child modules (e.g. aggregated Javadoc, Jacoco code coverage, distribution zip), executing on the root project at the start fails because child outputs do not yet exist.
   - Projects often resort to artificial "distribution" child modules placed last in reactor order to work around this limitation.

3. **Submodule Forking Issues ([MNG-7672](https://github.com/apache/maven/issues/8780), [MNG-7163](https://github.com/apache/maven/issues/8291), [MNG-2184](https://github.com/apache/maven/issues/4453))**:
   - When an aggregator goal is executed within a submodule, forking triggers a build of the entire reactor from within that submodule, repeating already-completed goals.

4. **Reporting and Site Generation Edge Cases**:
   - The `maven-site-plugin` and `maven-reporting-exec` maintain separate aggregation abstractions for generating multi-module documentation and stage reports, leading to inconsistencies between CLI reporting and lifecycle documentation goals.

---

## 3. Maven 4: `before:`/`after:` Phase System

Maven 4 introduces a first-class solution to several of the shortcomings above through
the `before:` and `after:` phase prefixes and two special reactor-scoped phases: `all` and `each`.

> **Note**: The `before:`/`after:` phase system and the `all`/`each` phases work with **all** builders
> (singlethreaded, multithreaded `-T`, and concurrent `-b concurrent`). They work with any POM model version
> (4.0.0 or 4.1.0) — no model version upgrade is required. The concurrent builder additionally provides
> DAG-based ordering enforcement (no `aggregatorLock`), while the legacy builders execute phases sequentially.

### 3.1 The `all` and `each` Phases

The default Maven 4 lifecycle defines a top-level `all` phase that wraps the entire reactor, with `each`
representing a single subproject's full build:

```
all                    ← reactor-scoped wrapper (children link)
└── each               ← per-subproject scope
    ├── validate / initialize
    ├── build (sources → compile → ready → package)
    ├── verify (unit-test, integration-test)
    ├── install
    └── deploy
```

The `all` phase carries a `children(all)` link, which is translated into ordering constraints:
- The root project's `before:all` fires first; every child project's `before:all` waits for it to complete.
- The root project's `after:all` fires only after every child's `after:all` has completed.

This means a plugin goal bound to `after:all` on the root project is **guaranteed to run once, after all
reactor subprojects have finished their full build**. With the concurrent builder, this is enforced by DAG
edges; with the legacy builders, by sequential phase execution order.

### 3.2 Solving the Aggregation Problems

| Problem (section 2) | Solution |
| :--- | :--- |
| **CLI vs. lifecycle discrepancy** ([MNG-6336](https://github.com/apache/maven/issues/7529)) | Bind to `after:all` on the root project — runs once after the full reactor. The timing guarantee is enforced regardless of whether the goal was invoked via CLI or lifecycle, and works with all builders. Note: `<inherited>false</inherited>` is still required if the execution is declared in a parent POM, since the `before:`/`after:` system controls ordering, not inheritance. |
| **Execution timing** (aggregator runs before children are built) | `after:all` fires after all children complete `all`, which includes `build`/`verify`. Artifact outputs are available. |
| **`before:each` / `after:each`** | Goals bound to these phases run once per subproject, bracketing each module's full build. Useful for per-module setup/teardown. |

### 3.3 Example: Aggregated Javadoc after the Full Reactor

```xml
<!-- works with any builder and any model version -->
<execution>
  <id>aggregate-javadoc</id>
  <goals><goal>aggregate</goal></goals>
  <phase>after:all</phase>
</execution>
```

This executes `maven-javadoc-plugin:aggregate` exactly once on the root project, after every subproject has
completed its `all` phase (which includes compilation and packaging). No distribution submodule workaround
required.

### 3.4 Decomposing `aggregator = true`

The `aggregator` flag is a **compound attribute** that bundles four distinct runtime behaviors. Maven 4 already provides orthogonal replacements for each:

| Behavior | What `aggregator=true` does | Maven 4 replacement | Status |
| :--- | :--- | :--- | :--- |
| **Execution scope** | Run once on the root project, not per-module | Bind to `all` phase (`before:all`, `after:all`) | Available |
| **Dependency resolution scope** | Resolve dependencies for all reactor projects | `@Resolution` annotation (per-field, per-project injection in `DefaultMavenPluginManager`) | Available |
| **Exclusive locking** | Acquire reactor-wide write lock in `-T` builder | DAG edges enforce ordering in the concurrent builder — no lock needed | Available |
| **Fork scope** | Forked lifecycles include all subprojects | Ordering constraints via `@After` (forks deprecated — see section 3.5) | Available |

This decomposition means new Maven 4 plugins should **not** use `@Mojo(aggregator = true)`. Instead:

- Bind the goal to `after:all` (or `before:all`) for reactor-scoped execution.
- Use `@Resolution` fields for dependency injection, which is already orthogonal to aggregator.
- Express ordering constraints via `@After` with `PROJECT`, `DEPENDENCIES`, or `CHILDREN` type values.

For backward compatibility, existing plugin descriptors declaring `aggregator=true` will continue to work. The target is to automatically map the flag to equivalent DAG constraints internally so that legacy plugins benefit without source changes (see §3.6 roadmap and [#13763](https://github.com/apache/maven/issues/13763)).

### 3.5 Deprecating Forked Lifecycles (`@Execute`)

Forked lifecycles (`@Execute(phase = ...)`) let a mojo fork a lifecycle phase in a separate execution context before running. Analysis of all Apache Maven plugins using `@Execute` shows that **none require fork isolation** — every use is purely an ordering guarantee:

| Plugin | `@Execute` target | Actual need |
| :--- | :--- | :--- |
| maven-javadoc-plugin | `@Execute(phase = COMPILE)` | Compiled classes must exist |
| maven-source-plugin | `@Execute(phase = GENERATE_SOURCES)` | Generated sources must exist |
| maven-dependency-plugin | `@Execute(phase = TEST_COMPILE)` | Classes compiled for analysis |
| maven-pmd-plugin | `@Execute(phase = TEST_COMPILE)` | Classes compiled for analysis |
| maven-surefire (report) | `@Execute(lifecycle = "surefire", phase = TEST)` | Test results must exist |

Every case reduces to "phase X must have completed before I run" — a DAG edge, not an isolated execution context. The `-no-fork` mojo variants (`javadoc-no-fork`, `aggregate-no-fork`, `jxr-no-fork`) exist only to opt out of unwanted forking, confirming that the fork is the wrong abstraction.

The Maven 4 replacement is `@After` constraints:

```java
// Instead of @Execute(phase = LifecyclePhase.COMPILE)
@After(phase = "compile", type = After.Type.PROJECT)

// Instead of @Execute(phase = LifecyclePhase.TEST_COMPILE)
@After(phase = "test-compile", type = After.Type.PROJECT)
```

The `computeForkPlan()` machinery, `$plan$` sentinel detection, and `getExecutionProject()` API can be deprecated for v4 mojos. As part of the migration roadmap, Maven 3 plugin descriptors declaring `@Execute` should be translated into equivalent `@After` constraints at descriptor load time.

### 3.6 Migration Roadmap

| Phase | Action |
| :--- | :--- |
| **Maven 4.0** | `before:`/`after:` phases available with all builders; `aggregator` and `@Execute` still supported |
| **Maven 4.x** | Deprecate `aggregator` and `@Execute` with runtime warnings ([#13762](https://github.com/apache/maven/issues/13762)); auto-map `aggregator=true` to DAG constraints in the concurrent builder ([#13763](https://github.com/apache/maven/issues/13763)); provide migration guide ([#13764](https://github.com/apache/maven/issues/13764)) |
| **Maven 5.x** | Remove forked lifecycle machinery; remove `aggregator` flag |

---

## 4. Related Issues

| Key | GitHub Issue | Summary | Relationship |
| :--- | :--- | :--- | :--- |
| MNG-7991 | [#8718](https://github.com/apache/maven/issues/8718) | Refactor "aggregator" goal feature | Umbrella / Architecture |
| MNG-6336 | [#7529](https://github.com/apache/maven/issues/7529) | Aggregator Mojo should be executed only once even when part of the lifecycle | Core defect |
| MNG-250 | [#2896](https://github.com/apache/maven/issues/2896) | Make aggregation feasible | Original origin |
| MNG-2184 | [#4453](https://github.com/apache/maven/issues/4453) | Possible problem with `@aggregator` and forked lifecycles | Forking defect |
| MNG-5665 | [#6657](https://github.com/apache/maven/issues/6657) | Advanced Lifecycle Handling | Lifecycle enhancement |
| MNG-7163 | [#8291](https://github.com/apache/maven/issues/8291) | Aggregating Mojo re-executes goals for child modules that are already executed | Forking duplication |
| MNG-7672 | [#8780](https://github.com/apache/maven/issues/8780) | Aggregate goals executed in a submodule forks the whole reactor | Submodule isolation |
| MNG-4504 | [#5543](https://github.com/apache/maven/issues/5543) | Disallowing all aggregator bindings to any lifecycle is too broad | Lifecycle policy |
| MNG-7900 | [#9015](https://github.com/apache/maven/issues/9015) | Wrapper lifecycle in multi module project should be executed only in root module | Wrapper lifecycle |
| - | [#13529](https://github.com/apache/maven/issues/13529) | Concurrent builder drops later lifecycle phases when merging overlapping task segments (fixed in [#13532](https://github.com/apache/maven/pull/13532)) | Task segment merging |
| - | [#13762](https://github.com/apache/maven/issues/13762) | Deprecate `aggregator=true` and `@Execute` with runtime warnings | Deprecation |
| - | [#13763](https://github.com/apache/maven/issues/13763) | Auto-map `aggregator=true` to DAG constraints in the concurrent builder | Auto-migration |
| - | [#13764](https://github.com/apache/maven/issues/13764) | Plugin author migration guide for `aggregator=true` to `after:all` | Documentation |
