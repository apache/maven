title: Maven Plugin Mixin
author: Maven Team
date: ${project.version}

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

# Maven Plugin Mixin

`maven-plugin-mixin` is a mixin for Maven 4 plugin projects. It bundles a JSR 269
annotation processor that generates Maven 4 plugin descriptors
(`META-INF/maven/plugin.xml`) and DI factory classes at compile time — with
**no dependency on `maven-plugin-plugin`** or `maven-plugin-tools`.

## Overview

When you annotate a class with `@org.apache.maven.api.plugin.annotations.Mojo`,
the processor fires during `javac` and produces:

1. **`META-INF/maven/plugin.xml`** — the plugin descriptor in v2.0.0 format,
   describing every mojo's goal name, default phase, parameters, dependency
   resolution requirements, `@After` ordering constraints, and more.

2. **One factory class per mojo** — e.g. `TouchMojo$MojoFactory`, annotated with
   `@Named("groupId:artifactId:version:goal")`. The DI index processor
   (from `maven-api-di`) picks these up automatically and registers them in
   `META-INF/maven/org.apache.maven.api.di.Inject`.

## Usage

Add the mixin to your plugin's POM (requires model version 4.2.0):

```xml
<project xmlns="http://maven.apache.org/POM/4.2.0" root="true" ...>
  <modelVersion>4.2.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>my-maven-plugin</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.apache.maven</groupId>
      <artifactId>maven-api-core</artifactId>
      <version>${maven.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.maven</groupId>
      <artifactId>maven-api-di</artifactId>
      <version>${maven.version}</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>

  <mixins>
    <mixin>
      <groupId>org.apache.maven</groupId>
      <artifactId>maven-plugin-mixin</artifactId>
      <version>${maven.version}</version>
    </mixin>
  </mixins>
</project>
```

The mixin automatically configures the compiler plugin with the annotation
processor and passes the resolved project coordinates (`groupId`, `artifactId`,
`version`) as `-A` options. No manual compiler configuration required.

## Why factories?

`DefaultMavenPluginManager.loadV4Mojo()` looks up mojos by
`Key.of(implementationClass, roleHint)` where `roleHint = "g:a:v:goal"`.
The `@Named` qualifier on the factory class matches this key so the DI container
can locate and instantiate the mojo.

## Supported annotations

| Annotation | Extracted information |
|------------|----------------------|
| `@Mojo` | goal name, default phase, projectRequired, aggregator, onlineRequired, configurator, dependencyCollection, dependencyResolutionPathScopes |
| `@Execute` | executePhase, executeGoal, executeLifecycle |
| `@After` / `@Afters` | afterLink phase, type (PROJECT/DEPENDENCIES/CHILDREN), scope |
| `@Parameter` (field/method) | name, alias, type, required, editable (inverted from `readonly`), expression (`${property}`), defaultValue, Javadoc description |
| `@Resolution` (field) | field name, pathScope, requestType |
| Javadoc | mojo description, `@since`, `@deprecated`; inline tags like `{@code}` and `{@link}` are stripped |

## Type hierarchy

`@Parameter` and `@Resolution` annotations are collected from the full class
hierarchy — including fields declared in superclasses from dependency JARs.
Subclass fields shadow parent fields with the same effective name.

Note: `Elements.getDocComment()` only works for source elements. Parameters
inherited from binary (compiled) parent classes will have empty descriptions,
which is the same limitation as `maven-plugin-tools` when source JARs are
unavailable.

## Design principles

- **Zero external dependencies** — only the JDK `java.compiler` and `java.xml`
  modules are required.
- **Self-processing disabled** — `<maven.compiler.proc>none</maven.compiler.proc>`
  prevents the processor from attempting to process itself.
- **Non-claiming** — the processor returns `false` from `process()`, so other
  processors and IDEs can still observe `@Mojo` annotations.
- **String-based annotation lookup** — annotation type names are compared as
  strings; no compile-time dependency on `maven-api-core` or `maven-api-di`.
- **Factories generated in normal rounds** — factory source files are emitted
  during the round in which the mojo is first seen (not in `processingOver()`),
  ensuring that `DiIndexProcessor` can process `@Named` in a subsequent round.
