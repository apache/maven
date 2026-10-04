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

Aggregation was introduced early in Maven 2 ([MNG-250](https://issues.apache.org/jira/browse/MNG-250)) to enable plugins to operate across an entire multi-module build reactor rather than on a single isolated module. It is exposed to plugin developers via the `@Mojo(aggregator = true)` annotation (or `@aggregator` in JavaDoc tag format).

This document analyzes the current behavior, outlines historical shortcomings, and establishes a target design specification for refactoring aggregator goals in Maven ([MNG-7991](https://issues.apache.org/jira/browse/MNG-7991)).

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
3. **Execution on Root Project Only**: `BuildListCalculator` (and `BuildPlanExecutor` in the concurrent path) restricts aggregating task segments to the top-level project (`session.getTopLevelProject()`). Submodules in the reactor are skipped for this goal.
4. **Ordering**: If the CLI invocation specifies both a lifecycle phase and an aggregator goal (e.g. `mvn clean install site:stage`), the normal lifecycle runs across all modules first, and the aggregator goal executes once at the end on the root module.

### 1.2 Lifecycle-Bound Execution (`<phase>...</phase>`)
When an aggregator Mojo is bound to a phase in a `pom.xml`:
1. **Lifecycle Segment Classification**: Lifecycle phases (e.g. `package`, `verify`) produce standard non-aggregating `TaskSegment`s.
2. **Per-Module Execution**: The goal is injected into the execution plan of **every module** in the reactor that inherits the plugin configuration.
3. **Redundant Executions**: Unless the project author explicitly configures `<inherited>false</inherited>` on the plugin execution in the parent POM, the aggregator Mojo executes once for each module in the reactor.
4. **Thread Locking**: In parallel builds (`-T`), `MojoExecutor` acquires an exclusive reactor-wide write lock (`aggregatorLock.writeLock()`) whenever an aggregator executes:
   ```java
   acquiredAggregatorLock = aggregator ? aggregatorLock.writeLock() : aggregatorLock.readLock();
   ```
   Executing an aggregator Mojo across multiple submodules repeatedly halts parallel build concurrency across all threads.

### 1.3 Dependency Resolution
When executing an aggregator Mojo:
- `MojoExecutor.ensureDependenciesAreResolved` checks `mojoDescriptor.isAggregator()`.
- If `true`, Maven resolves dependencies not only for the current project, but also across all projects in `session.getProjects()` matching the scopes requested by the Mojo.

### 1.4 Forked Lifecycles
When an aggregator Mojo declares `@Execute(phase = ...)` or `@Execute(goal = ...)`:
- `BuildPlanExecutor` creates a forked plan encompassing the current project and all collected sub-projects (`step.project.getCollectedProjects()`).
- This forks execution across the entire reactor tree, leading to duplicated builds, redundant tests, and nested reactor re-executions.

---

## 2. Identified Shortcomings & Problem Space

Over successive Maven releases, multiple discrepancies and pain points have been identified:

1. **CLI vs. Lifecycle Discrepancy ([MNG-6336](https://issues.apache.org/jira/browse/MNG-6336))**:
   - Plugin authors annotate a goal with `@Mojo(aggregator = true)` with the expectation that Maven will execute it once per reactor.
   - While CLI invocation honors this expectation (running only on root), lifecycle binding runs it on every module.
   - Forcing users to configure `<inherited>false</inherited>` is error-prone, unintuitive, and breaks down in nested multi-module structures.

2. **Execution Timing: Aggregate at Start vs. Aggregate at End**:
   - In hierarchical builds, the root project is typically evaluated first in reactor order.
   - Consequently, an aggregator bound to `package` or `verify` on the root module executes *before* child modules have compiled or packaged their artifacts.
   - When the purpose of the aggregator is to combine artifacts or reports produced by child modules (e.g. aggregated Javadoc, Jacoco code coverage, distribution zip), executing on the root project at the start fails because child outputs do not yet exist.
   - Projects often resort to artificial "distribution" child modules placed last in reactor order to work around this limitation.

3. **Submodule Forking Issues ([MNG-7672](https://issues.apache.org/jira/browse/MNG-7672), [MNG-7163](https://issues.apache.org/jira/browse/MNG-7163), [MNG-2184](https://issues.apache.org/jira/browse/MNG-2184))**:
   - When an aggregator goal is executed within a submodule, forking triggers a build of the entire reactor from within that submodule, repeating already-completed goals.

4. **Reporting and Site Generation Edge Cases**:
   - The `maven-site-plugin` and `maven-reporting-exec` maintain separate aggregation abstractions for generating multi-module documentation and stage reports, leading to inconsistencies between CLI reporting and lifecycle documentation goals.

---

## 3. Target Architecture & Design Specification (Maven 4+)

To address [MNG-7991](https://issues.apache.org/jira/browse/MNG-7991) and related issues, the reactor aggregation mechanism should be decoupled and modernized:

### 3.1 Explicit Execution Semantics

Rather than overloading a single boolean flag `aggregator = true`, execution scope should distinguish between:

| Scope | Description | Execution Timing |
| :--- | :--- | :--- |
| **`PROJECT`** (Default) | Standard Mojo execution per project module. | Normal reactor topological order. |
| **`REACTOR_ROOT`** | Executes once on the root project of the reactor. | Top of reactor build. |
| **`REACTOR_AT_END`** | Executes once across collected projects *after* all reactor modules complete the phase. | End of reactor build (similar to deploy-at-end). |

### 3.2 Lifecycle Engine Unification
- **Automatic Root-Only Lifecycle Binding ([MNG-6336](https://issues.apache.org/jira/browse/MNG-6336))**: When a goal marked as an aggregator is bound to a lifecycle phase, Maven's lifecycle planner should default to executing it once on the root project unless explicitly declared otherwise.
- **Phase Post-Aggregator Hooks ([MNG-5665](https://issues.apache.org/jira/browse/MNG-5665))**: Support lifecycle phases that trigger aggregation tasks after reactor submodules have completed upstream tasks (e.g. `post-verify-reactor`).

### 3.3 Forking Deduplication
- Forked lifecycles initiated by an aggregator must verify previously satisfied phases in the reactor session cache to avoid repeating work already accomplished in the current build.

---

## 4. Related Issues

| Key | Summary | Relationship |
| :--- | :--- | :--- |
| [MNG-7991](https://issues.apache.org/jira/browse/MNG-7991) | Refactor "aggregator" goal feature | Umbrella / Architecture |
| [MNG-6336](https://issues.apache.org/jira/browse/MNG-6336) | Aggregator Mojo should be executed only once even when part of the lifecycle | Core defect |
| [MNG-250](https://issues.apache.org/jira/browse/MNG-250) | Make aggregation feasible | Original origin |
| [MNG-2184](https://issues.apache.org/jira/browse/MNG-2184) | Possible problem with `@aggregator` and forked lifecycles | Forking defect |
| [MNG-5665](https://issues.apache.org/jira/browse/MNG-5665) | Advanced Lifecycle Handling | Lifecycle enhancement |
| [MNG-7163](https://issues.apache.org/jira/browse/MNG-7163) | Aggregating Mojo re-executes goals for child modules that are already executed | Forking duplication |
| [MNG-7672](https://issues.apache.org/jira/browse/MNG-7672) | Aggregate goals executed in a submodule forks the whole reactor | Submodule isolation |
| [MNG-4504](https://issues.apache.org/jira/browse/MNG-4504) | Disallowing all aggregator bindings to any lifecycle is too broad | Lifecycle policy |
| [MNG-7900](https://issues.apache.org/jira/browse/MNG-7900) | Wrapper lifecycle in multi module project should be executed only in root module | Wrapper lifecycle |
