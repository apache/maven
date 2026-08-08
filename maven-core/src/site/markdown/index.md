---
title: Introduction
author:
  - Hervé Boutemy
date: 2013-07-27
---

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

# Maven Core

Maven Core classes managing the whole build process.

## Reference Documentation

- [lifecycles](./lifecycles.html) and [plugin bindings to `default` lifecycle](./default-bindings.html),
- [default artifact handlers](./artifact-handlers.html), to manage [dependency types](../maven-model/maven.html#class_dependency),
- [extension descriptor](./extension.html) and [core extensions](./core-extensions.html),
- [classloader hierarchy](/guides/mini/guide-maven-classloading.html) done by `ClassRealmManager` component ([javadoc](./apidocs/org/apache/maven/classrealm/ClassRealmManager.html)), with its `DefaultClassRealmManager` implementation ([source](./xref/org/apache/maven/classrealm/DefaultClassRealmManager.html)), using [Plexus Classworlds](https://codehaus-plexus.github.io/plexus-classworlds/),

## Useful entry points

- `Maven` component ([javadoc](./apidocs/org/apache/maven/Maven.html)), with its `DefaultMaven` implementation ([source](./xref/org/apache/maven/DefaultMaven.html)), to drive a full `MavenSession` execution ([javadoc](./apidocs/org/apache/maven/execution/MavenSession.html)
- `ProjectBuilder` component ([javadoc](./apidocs/org/apache/maven/project/ProjectBuilder.html)), with its `DefaultProjectBuilder` implementation ([source](./xref/org/apache/maven/project/DefaultProjectBuilder.html)), to prepare [`MavenProject` descriptor](./apidocs/org/apache/maven/project/MavenProject.html) from POM files,
- `LifecycleExecutor` component ([javadoc](./apidocs/org/apache/maven/lifecycle/LifecycleExecutor.html)), with its `DefaultLifecycleExecutor` implementation([source](/xref/org/apache/maven/lifecycle/DefaultLifecycleExecutor.html)), to plan or execute tasks.  
    on plugin goals execution order:
    - **in a given phase, goals order is not expected to be guaranteed nor finely tuned**: it is just a consequence of the order obtained during [effective model building](../maven-model-builder/), which combines profile activation+injection and inheritance assembly from parents,
    - known limitations are notably that:

        1\. plugin goal execution in a child is usually simply appended (at end): you can't try to insert in the middle of pre-existing inherited executions,

        2\. append happens at plugin level first, then goal level, independently from phases. This means for example that adding pluginA:goal2 to pre-existing (pluginA:goal1, pluginB:goal) will lead to (pluginA:goal1, pluginA:goal2, pluginB:goal)

    - see effective POM as shown by [`help:effective-pom`](/plugins/maven-help-plugin/effective-pom-mojo.html) to see the effective plugins then goals order.
- `MavenPluginManager` component ([javadoc](./apidocs/org/apache/maven/plugin/MavenPluginManager.html)), with its `DefaultMavenPluginManager` implementation ([source](./xref/org/apache/maven/plugin/internal/DefaultMavenPluginManager.html)),
- [PluginParameterExpressionEvaluator](./apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html), used to evaluate plugin parameters values during Mojo configuration,
- `ExceptionHandler` component ([javadoc](./apidocs/org/apache/maven/exception/ExceptionHandler.html)), with its `DefaultExceptionHandler` implementation ([source](./xref/org/apache/maven/exception/DefaultExceptionHandler.html)), use to transform exception into useful end-user messages.

## Toolchains

- [Toolchains descriptor reference](./toolchains.html),
- public API for toolchains-aware plugins: `ToolchainManager` component ([javadoc](./apidocs/org/apache/maven/toolchain/ToolchainManager.html)) with its `DefaultToolchainManager` implementation ([source](./xref/org/apache/maven/toolchain/DefaultToolchainManager.html)), to get selected `Toolchain` ([javadoc](./apidocs/org/apache/maven/toolchain/Toolchain.html)) instance,
- internal `ToolchainManagerPrivate` component ([javadoc](./apidocs/org/apache/maven/toolchain/ToolchainManagerPrivate.html)) with its `DefaultToolchainManagerPrivate` implementation ([source](./xref/org/apache/maven/toolchain/DefaultToolchainManagerPrivate.html)), to manage toolchain selection,
- internal JDK toolchain implementation: `JavaToolchain` interface ([javadoc](./apidocs/org/apache/maven/toolchain/java/JavaToolchain.html)), with its `JavaToolchainImpl` implementation ([source](./xref/org/apache/maven/toolchain/java/JavaToolchainImpl.html)) and `JavaToolchainFactory` factory ([source](./xref/org/apache/maven/toolchain/java/JavaToolchainFactory.html)).
