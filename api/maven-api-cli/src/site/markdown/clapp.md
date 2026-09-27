<!---
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

# Maven CLAPP (Command Line Application) Guide

Starting with Maven 4.1.0, Apache Maven provides native support for **CLAPP** (Command Line Application / CLI tool) extensions.

CLAPP enables developers to build and distribute standalone CLI utilities that run directly via the standard Maven launcher (`mvn --clapp <toolname>`) while maintaining **per-tool isolated classpaths**.

---

## Why CLAPP?

Traditionally, adding a new CLI command to Maven required placing all of its third-party runtime dependencies into the shared `${maven.home}/lib/` directory. This created several issues:
- **Dependency bloat**: All dependencies became part of the core Maven classpath.
- **Version conflicts**: Conflicts between library versions required by different CLI tools and the Maven core.
- **Lack of isolation**: Classes could accidentally leak or collide across tools.

CLAPP solves this by isolating each tool into its own sub-directory inside `${maven.home}/lib/clapp/<toolname>/`.

### Distribution Directory vs. Local Repository (`.m2/repository`)

- **`${maven.home}/lib/clapp/<toolname>/`**: Designed for tools bundled with or installed directly into the Maven distribution (such as system administration utilities or pre-packaged corporate CLI tools), ensuring their private JARs do not leak into the core `plexus.core` realm.
- **`~/.m2/repository/`**: Remains Maven's standard dependency cache. Tools running under CLAPP have full access to Maven core repository services (such as Resolver and RepositorySystem) to dynamically resolve and load dependencies from the local `.m2` repository or remote repositories as needed.

---

## Architecture & Class Loading

When a user executes:

```bash
mvn --clapp <toolname> [args...]
```

1. The `mvn` launcher script detects `--clapp <toolname>`.
2. It sets the system property `maven.clapp.name=<toolname>` and reads `${maven.home}/lib/clapp/<toolname>/clapp.properties` to obtain the entry-point class.
3. The launcher delegates to `org.apache.maven.cling.MavenClappCling`.
4. `MavenClappCling` constructs a child `URLClassLoader` containing all `*.jar` files located in `${maven.home}/lib/clapp/<toolname>/`. This child classloader delegates to the parent core Maven classloader (the Plexus `plexus.core` realm).
5. The entry-point class's `run(String[] args, ClassWorld world)` method (or `main(...)`) is invoked via reflection.
6. The child `URLClassLoader` is automatically closed upon completion via try-with-resources.

This model provides two key guarantees:
1. **Access to Maven APIs**: The tool can freely access and use Maven core APIs provided by the parent classloader.
2. **Dependency Isolation**: The tool's private dependencies are NOT visible to the core Maven realm or to any other CLAPP tools.

---

## Packaging a Tool

### 1. Directory Structure

Place your tool files in a subfolder under `${maven.home}/lib/clapp/`:

```
${maven.home}/
  lib/
    clapp/
      <toolname>/
        clapp.properties        <- Required configuration
        my-tool-1.0.jar         <- Tool binary
        dependency-a-2.1.jar    <- Tool private dependency
        dependency-b-3.0.jar    <- Tool private dependency
```

Tool names must consist only of alphanumeric characters, hyphens, and underscores (`^[a-zA-Z0-9_-]+$`).

### 2. Descriptor (`clapp.properties`)

Create a `clapp.properties` file in your tool's directory:

```properties
# Fully-qualified class name of your tool's entry point
mainClass=org.example.mytool.MyToolCling
```

### 3. Entry-Point Implementation

The recommended entry-point method is `public static int run(String[] args, ClassWorld world)`:

```java
package org.example.mytool;

import org.codehaus.plexus.classworlds.ClassWorld;

public class MyToolCling {

    /**
     * Recommended entry-point method for CLI tools.
     * Note: public static int main(String[] args, ClassWorld world) and standard
     * public static void main(String[] args) are also supported for compatibility.
     */
    public static int run(String[] args, ClassWorld world) {
        System.out.println("Executing MyTool with arguments: " + String.join(" ", args));

        // Use core Maven services or your private dependencies here

        return 0; // Return exit code (0 for success, non-zero for failure)
    }
}
```

---

## Error Handling

- **Invalid tool name or path traversal**: If the tool name contains path separators or characters outside `^[a-zA-Z0-9_-]+$`, an `IllegalArgumentException` is thrown.
- **Missing main class or entry point**: If the class or method cannot be found or accessed, `MavenClappCling.ClappException` is thrown.
- **I/O issues**: If the lib directory cannot be scanned, `java.io.IOException` is thrown.

---

## Running Your Tool

Users can execute your CLAPP tool using the standard Maven wrapper or installed `mvn`:

```bash
# Argument form:
mvn --clapp mytool --format=json

# Equals form:
mvn --clapp=mytool --format=json
```
