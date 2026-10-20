title: Usage
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

# Usage

## Minimal Maven 4 plugin

A complete, functional Maven 4 plugin with one mojo and one `@Parameter`:

```java
package com.example;

import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * Prints a greeting.
 *
 * @since 1.0.0
 */
@Mojo(name = "greet", defaultPhase = "compile")
public class GreetMojo implements Mojo {

    /**
     * The name to greet.
     *
     * @since 1.0.0
     */
    @Parameter(property = "greet.name", defaultValue = "World")
    private String name;

    @Override
    public void execute() {
        System.out.println("Hello, " + name + "!");
    }
}
```

POM configuration:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-api-plugin-processor</artifactId>
            <version>${maven.version}</version>
          </path>
          <path>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-api-di</artifactId>
            <version>${maven.version}</version>
          </path>
        </annotationProcessorPaths>
        <compilerArgs>
          <arg>-Amaven.plugin.groupId=${project.groupId}</arg>
          <arg>-Amaven.plugin.artifactId=${project.artifactId}</arg>
          <arg>-Amaven.plugin.version=${project.version}</arg>
        </compilerArgs>
      </configuration>
    </plugin>
  </plugins>
</build>
```

At compile time, the processor generates:

- `target/classes/META-INF/maven/plugin.xml` — the plugin descriptor
- `target/generated-sources/annotations/com/example/GreetMojo$MojoFactory.java`
  — the DI factory, compiled and registered in the DI index automatically

## Using `@After` ordering constraints

```java
@Mojo(name = "touch", defaultPhase = "compile")
@After(phase = "resources")
public class TouchMojo implements Mojo { ... }
```

Generates an `<afterLink>` element in the descriptor, which the Maven 4
concurrent builder uses to enforce ordering within the lifecycle DAG.

## Dependency resolution

```java
@Mojo(name = "list-deps", defaultPhase = "compile")
public class ListDepsMojo implements Mojo {

    @Resolution(pathScope = "main", requestType = "resolve")
    private DependencyResolverResult deps;
    ...
}
```

The processor emits a `<resolution>` element and infers
`<dependencyResolution>main</dependencyResolution>` from the `@Resolution`
fields (unless overridden by `@Mojo.dependencyResolutionPathScopes()`).

## Packaging note

Because `maven-api-plugin-processor` generates the plugin descriptor at compile
time, the plugin does **not** use `<packaging>maven-plugin</packaging>`. Use
standard `<packaging>jar</packaging>` (or omit packaging, which defaults to
`jar`). Maven 4 loads the plugin from the descriptor in the JAR, not from the
packaging lifecycle.
