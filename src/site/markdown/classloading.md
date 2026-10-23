<!---
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

# Maven Classloading

This document describes the classloading mechanism used by Maven 4.x.
Maven uses [Plexus Classworlds][classworlds] to build a hierarchy of isolated
classloaders (called **realms**) so that plugins, extensions and the
Maven core can each see only the classes they are supposed to see.

The following diagram gives an overview of the realm hierarchy during a
typical Maven build. Solid arrows indicate the parent classloader
relationship. Dashed blue arrows indicate package imports. Dashed green
arrows indicate export feeds.

![Maven 4.x Classloader Hierarchy](images/classloading.svg)

Each realm is described in detail below.

## ClassWorlds Bootstrap ClassLoader

This classloader is created and managed by the calling environment: the
JVM launcher, an integration testing harness, or an IDE embedder.

During a normal command-line invocation this is the JVM System
ClassLoader and contains:

- `${maven.home}/boot/plexus-classworlds-*.jar` — the Plexus
  Classworlds library that bootstraps all other realms
- Any `-javaagent` classes specified on the JVM command line

## Maven Core Realm (`plexus.core`)

Contains all Maven runtime classes: `MavenProject`, `AbstractMojo`,
Plexus/Sisu DI, SLF4J, Guice, the Aether resolver, and more.

The contents are configured by `${maven.home}/bin/m2.conf` and
typically include:

- `${maven.home}/lib/ext/*.jar`
- `${maven.home}/lib/*.jar`

This realm is the **container realm** in the Plexus/Sisu container
unless core extensions are present, in which case the `maven.ext` realm
takes over as the container realm (see below).

## Core Extension Realms (`coreExtension>G:A:V`)

Core extensions are loaded from `.mvn/extensions.xml` during Maven
startup, **before the POM is parsed**. Each core extension gets its own
isolated classloader. Because they load before POM parsing, core
extensions can influence the build itself (e.g. polyglot Maven).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <extension>
    <groupId>...</groupId>
    <artifactId>...</artifactId>
    <version>...</version>
    <!-- optional: parent-first (default), self-first, plugin -->
    <classLoadingStrategy>...</classLoadingStrategy>
  </extension>
</extensions>
```

Each extension can use `META-INF/maven/extension.xml` to declare
exported packages and artifacts. These exports are aggregated into
`CoreExports` and become visible to all plugins and build extensions
through the Maven API realm.

The `<classLoadingStrategy>` element controls the realm's parent
wiring:

| Strategy | Behavior |
|---|---|
| `parent-first` (default) | Imports everything from the core realm. Classes from the core are preferred over the extension's own. |
| `plugin` | Imports only `CoreExports` packages (the Maven API surface). Same isolation as build extensions. |
| `self-first` | Uses the core realm as parent but checks its own classes first. Allows the extension to override core classes. |

Core extensions are loaded as part of Maven runtime startup and
disposed of as part of Maven runtime shutdown.

### Exports

If a core extension includes `META-INF/maven/extension.xml`, it can
declare which packages and artifacts it exports:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extension>
  <exportedPackages>
    <exportedPackage>org.example.myextension</exportedPackage>
  </exportedPackages>
  <exportedArtifacts>
    <exportedArtifact>org.example:myextension</exportedArtifact>
  </exportedArtifacts>
</extension>
```

Exported packages become part of the Maven API surface — they are
visible to all plugins and extensions. Exported artifacts are filtered
from consumer classpaths to avoid class duplication.

If no `extension.xml` is present, no packages or artifacts are exported,
but the extension can still contribute components to Maven Core
extension points (e.g. lifecycle mappings, event listeners) via Sisu
component scanning.

## Maven Extensions Realm (`maven.ext`)

This realm is created only when core extensions are configured or when
`-Dmaven.ext.class.path` is specified. It aggregates:

- Exported packages from all core extension realms
- JARs from `-Dmaven.ext.class.path` (added directly to the realm)

If created, this realm becomes the container realm in the Plexus/Sisu
container. If no core extensions or ext classpath entries exist,
`plexus.core` remains the container realm.

> **Known limitation:** JARs loaded via `-Dmaven.ext.class.path` are
> added directly to the `maven.ext` realm, but their
> `META-INF/maven/extension.xml` descriptors are never read. Declared
> exports from these JARs are silently ignored. See
> [MNG-8112](https://issues.apache.org/jira/browse/MNG-8112).

## Maven API Realm (`maven.api`)

This realm aggregates all exported packages from `CoreExports` — the
union of Maven core exports and core extension exports. It does not
contain any classes directly; it only imports from the container realm.

This is the classloader that plugins and build extensions see as the
"Maven API." It provides a clean, well-defined surface regardless of
how Maven was launched.

This realm has **no parent classloader** (`null`). It only contains
what is explicitly imported from `CoreExports`. This ensures the
Maven API classpath is consistent and does not include application or
`-javaagent` classes.

## Maven 4 API Realm (`maven.api.v4`)

A more restricted view of the core, separate from `maven.api`, containing
only:

- `org.apache.maven.api` — the Maven 4 API surface
- `org.slf4j` — logging

Like `maven.api`, this realm has **no parent classloader** and imports
directly from the container realm — it is not a child of `maven.api`.

Maven 4 plugins (those using the new Maven 4 API) use this realm
instead of `maven.api`. This ensures Maven 4 plugins only depend on
the stable API, not on internal implementation classes.

## Build Extension Realms (`extension>G:A:V`)

Created for each `<build><extensions><extension>` declaration in the POM
and for plugins with `<extensions>true</extensions>`. Both forms go
through the same classloading path — the POM elements are converted into
synthetic `Plugin` objects internally.

Each build extension is loaded in a fully isolated classloader:

- **Parent:** JVM System ClassLoader (provides access to `-javaagent`
  classes)
- **Imports:** all packages from `maven.api` (wildcard import)

Build extensions can declare exports via
`META-INF/maven/extension.xml`. These exports are imported into the
project realm, making them visible to all plugins in that project.

Maven guarantees that each distinct build extension (identified by
plugin GAV and resolved dependencies) is loaded once and shared across
all projects that use it. The `ExtensionRealmCache` manages this.

Build extensions are **inherited from parent POMs**. The model merger
combines parent and child extension lists, keyed by groupId:artifactId.

## Project Realms (`project>G:A:V`)

Project realms aggregate Maven API packages and packages exported by
project build extensions.

- **Parent:** `maven.api` realm
- **Imports:** exported packages from each build extension realm

Maven guarantees there is one project classloader per unique set of
build extensions. Projects with identical extension sets share the same
classloader.

Legacy Maven 2.x build extensions (single-artifact extensions without
`META-INF/maven/extension.xml`) are loaded directly into the project
classloader rather than getting their own extension realm.

## Plugin Realms (`plugin>G:A:V`)

Each plugin gets its own isolated classloader:

- **Parent:** JVM System ClassLoader (provides access to `-javaagent`
  classes)
- **Imports:** depends on whether the project has build extensions:
  - **Without build extensions:** imports from `maven.api` directly
  - **With build extensions:** imports from the project realm, which
    gives access to both Maven API packages and build extension exports

Maven creates one classloader per unique plugin + dependencies +
build-extensions combination. The `PluginRealmCache` manages this.

**Provided artifact filtering:** artifacts that are already in
`CoreExports` are filtered from the plugin's classpath to avoid
duplicate or conflicting classes. Maven 4 plugins (using `maven.api.v4`)
get a tighter filter — only `org.apache.maven:maven-api-*` artifacts
are filtered.

## Exported Artifacts and Packages

Maven Core, core extensions, and build extensions use
`META-INF/maven/extension.xml` to declare their API surface:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extension>
  <exportedPackages>
    <exportedPackage>org.example.myextension</exportedPackage>
  </exportedPackages>
  <exportedArtifacts>
    <exportedArtifact>org.example:myextension</exportedArtifact>
  </exportedArtifacts>
</extension>
```

**`exportedPackages`** are classname prefixes. Any class whose fully
qualified name starts with one of these prefixes is importable by
consumers. These prefixes are wired as foreign imports into the Maven
API realm and from there into plugin and extension realms.

**`exportedArtifacts`** are artifact coordinates in `groupId:artifactId`
format. When Maven builds a plugin or extension classpath, artifacts
matching these coordinates are excluded — they are already provided by
the exporting realm, and including them would cause class duplication.

## Component Visibility (Sisu Realm Scoping)

Maven uses [Eclipse Sisu][sisu] for dependency injection. Sisu discovers
components (JSR-330 `@Named` beans, Plexus `@Component` classes) by
scanning each realm's classpath. Component **visibility** is scoped by
the realm hierarchy: a component is only visible to consumers whose
realm imports from the component's realm.

This scoping is critical for build extensions that register
project-specific components. For example, a build extension that
registers a custom packaging type must only be visible to projects
that declare that extension — not to other projects in the same reactor.

### How Visibility Works

Sisu's `FilteredBeans` mechanism filters component lookups based on
the Thread Context ClassLoader (TCCL). When Maven processes a project,
the TCCL is set to that project's realm. Lookups are filtered through
the realm's import chain:

| Component source | Visible to |
|---|---|
| Maven Core (container realm) | Everything |
| Core extensions | Everything (exported into `CoreExports`) |
| Build extensions | Only projects whose realm imports from the extension realm |
| Plugins | Only within the plugin's own execution |

### How It Is Wired

When a core extension realm is created, `PlexusContainerCapsuleFactory`
wires visibility in two directions:

1. The extension realm imports from the container realm (`maven.ext`)
   so that Sisu's `FilteredBeans` does not hide container-sourced
   beans when the TCCL is an extension realm.
2. If the extension exports no packages, its realm ID is imported as a
   synthetic marker so that Sisu can still discover the extension's
   components.

For plugins, `DefaultMavenPluginManager.discoverPluginComponents()`
sets the TCCL to the plugin's realm before discovering components. Sisu
then scans only the plugin's realm and its imports.

### Maven 4 Plugin Visibility (maven-di)

Maven 4 plugins — those using `org.apache.maven.api.di.Inject`
annotations — follow a different component visibility model than
Maven 3 plugins. The differences stem from how mojos are instantiated.

**Maven 3 mojos** are instantiated by Sisu via
`container.lookup(Mojo.class, roleHint)`. The lookup is filtered by the
plugin realm's import chain, so the mojo can see components from Maven
Core, core extensions, and build extensions (if the project realm
imports them).

**Maven 4 mojos** are instantiated by a plain `maven-di` injector
(`Injector.create()`) scoped to the plugin's classloader. This injector
has no Sisu access and no realm awareness. Core services (`Session`,
`Project`, `MojoExecution`, `Log`, and all API services) are manually
bound into the injector. The mojo can only see:

- Components discovered from its own plugin realm classloader
  (its own classes and dependencies)
- The manually bound core services

It **cannot** see:

- Build extension components
- Sisu beans from any realm
- Components from other plugins

| | Maven 3 plugin | Maven 4 plugin |
|---|---|---|
| Instantiation | Sisu lookup (realm-filtered) | Plain maven-di injector |
| Core services | Injected by Sisu from container realm | Manually bound |
| Build extension components | Visible if project realm imports the extension | Not visible |
| Other plugin components | Not visible (separate realms) | Not visible (separate injectors) |
| Sisu beans | Yes, filtered by realm imports | No |

This means Maven 4 plugins have a stricter isolation model by default.
A Maven 4 plugin that needs to interact with build extension components
must do so through the Maven API services, not through direct injection.

> **Note:** During the component **discovery** phase,
> `DefaultMavenPluginManager` also creates a `SisuDiBridgeModule` that
> bridges maven-di bindings into Sisu's registry. This bridge is used
> for component *registration* (making the plugin's components visible
> to the container), not for mojo *instantiation*. The mojo itself is
> created by a separate, plain maven-di injector.

### Limitations

Sisu's visibility filtering has known limitations:

- **Filtering only works on explicit lookups**, not on injected
  collections. For example, `Map<String, LifecycleMapping>` injection
  does not filter by realm — all lifecycle mappings from all realms are
  injected. A comment in `DefaultPackagingRegistry` documents this:
  *"SISU visibility filtering can only happen when an explicit lookup
  is performed."*
- **Debugging is difficult.** When a component is not visible due to
  realm scoping, the lookup silently returns null or an empty
  collection. There is no error message indicating that the component
  exists but is hidden by realm boundaries.
- **Project scope is imprecise.** Build extension components bound in
  a project classloader have no clean "project scope" definition in
  Sisu terms. Multiple projects sharing the same extension set share a
  realm, which means extension components are visible to all projects
  in that group, not individually scoped.

[classworlds]: https://codehaus-plexus.github.io/plexus-classworlds/
[sisu]: https://eclipse.dev/sisu/
