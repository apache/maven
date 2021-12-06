<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Overview

This document describes generic approach to remote cache setup. The process implies good knowledge of both Maven and the
project. Due to Maven model limitation the process is semi-manual, but allows you to achieve sufficient control and
transparency over caching logic.

## Before you start

Before you start, please keep in mind basic principles:

* Cache is key based, the key is produced by HashTree-like technique. The key is produced by hashing every configured
  source code file, every dependency and effective pom (including plugin parameters). Every element's hash contributes
  to the key. In order to produce the same key there engine must consume exactly the same hashes.
* There is no built-in normalization of line endings in this implementation, file hash calculation is raw bytes
  based. The most obvious implication could be illustrated by a simple Git checkout. By default, git will check out
  source code with CRLF line endings on win and LF on Linux. Because of that builds over same commit on a Linux agent
  and local build on Windows workstation will yield different hashes.
* Parameters of plugins are reconciled in runtime. For example to avoid of accidentally reusing builds which never run
  tests ensure that critical surefire parameters are tracked (`skipTests` and similar) in config. The same applies for
  all over plugins.

# Step-By-Step

## Minimize number of moving parts

* Run build with single threaded builder to make sure logs from different modules do not interfere
* Use the same branch which no-one else commits to
* Designate single agent/node for CI builds
* Preferably use the same OS between CI and local machine

## Fork branch for cache setup purposes

Fork stable code branch for cache setup purposes as you will need source code which doesn't change over time of setup.
Also, you likely will need to do code changes as long as you go.

## Setup http server to store artifacts

In order to share build results cache needs a shared storage. The simplest option is to set up a http server which
supports http PUT/GET/HEAD operations will suffice (Nginx, Apache or similar). Add the url to config and
change `remote@enabled` to true:

```xml
<remote enabled="true">
    <url>http://your-buildcache-url</url>
</remote>
```

If proxy or authentication is required to access remote cache, add server record to settings.xml as described
in [Servers](https://maven.apache.org/settings.html#Servers). The server should be referenced from cache config:

```
TBD
```

Beside the http server, remote cache could be configured using any storage which is supported
by [Maven Wagon](https://maven.apache.org/wagon/). That includes a wide set of options, including SSH, FTP and many
others. See Wagon documentation for a full list of options and other details.

## Build selection

Build stored in cache ideally should be a build assembled in the most correct, comprehensive and complete way. Pull
requests builds are good candidates to populate cache usually because this is there quality safeguards are applied
normally.

## CI Build setup to seed shared cache

Allow writes in remote cache add jvm property to designated CI builds.

```
-Dremote.cache.save.enabled=true
```

Run the build, review log and ensure that artifacts are uploaded to remote cache. Now, rerun build and ensure that it
completes almost instantly because it is fully cached.

## Remote cache relocation to local builds

As practice shows, developers often don't realize that builds they run in local and CI environments are different. So
straightforward attempt to reuse remote cache in local build usually results in cache misses because of difference in
plugins, parameters, profiles, environment, etc. In order to reuse results you might need to change poms, cache config,
CI jobs and the project itself. This part is usually most challenging and time-consuming. Follow steps below to
iteratively achieve working configuration.

* Enable fail fast mode to fail build on the first discrepancy between
* Provide reference to the CI build as a baseline for comparison between your local and remote builds. Go to the
  reference CI build log and one of the final lines of the build should be a line about saving `cache-report.xml`

```
[INFO] [CACHE] Saved to remote cache https://your-cache-url/<...>/915296a3-4596-4eb5-bf37-f6e13ebe087e/cache-report.xml
```

Copy the link to a `cache-report.xml` and provide it to your local build as a baseline for comparison.

* Run local build. Command line should look similar to this:

```bash
mvn verify -Dremote.cache.failFast=true -Dremote.cache.baselineUrl=https://your-cache-url/<...>/915296a3-4596-4eb5-bf37-f6e13ebe087e/cache-report.xml
```

Once discrepancy between remote and local builds detected cache will fail with diagnostic info in
project's `target/incremental-maven` directory:

```
* buildinfo-baseline-3c64673e23259e6f.xml - build specification from baseline build
* buildinfo-db43936e0666ce7.xml - build specification of local build
* buildsdiff.xml - comparison report with list of discrepancies 
```

Review `buildsdiff.xml` file and eliminate detected discrepancies. You can also diff build-info files directly to get
low level insights. See techniques to configure cache in [How-To](CACHE-HOWTO.md) and troubleshooting of typical issues
in the section below.

# Common issues

## Issue 1: Local checkout is with different line endings

Solution: normalise line endings. Current implementation doesn't have built-in line endings normalization, it has to be
done externally. In git it is recommended to use `.gitattributes` file to establish consistent line endings across all
envs for file types specific to this project

## Issue 2: Effective poms mismatch because of plugins injection by profiles

Different profiles between remote and local builds likely result in different text of effective poms. As effective pom
contributes hash value to the key that could lead to cache misses. Solution: instead of adding/removing specific plugins
by profiles, set default value of the plugin's `skip` or `disabled` flag in a profile properties instead. Instead of:

```xml
<profiles>
  <profile>
    <id>run-plugin-in-ci-only</id>
    <build>
      <plugins>
        <plugin>
          <artifactId>surefire-report-maven-plugin</artifactId>
          <configuration>
            <!-- my configuration -->
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

Use:

```xml
<properties>
    <!-- default value -->
    <skip.plugin.property>true</skip.plugin.property>
</properties>
<build>
  <plugins>
      <plugin>
          <artifactId>maven-surefire-plugin</artifactId>
          <configuration>
              <!-- plugin behavior is controlled by property -->
              <skip>${skip.plugin.property}</skip>
          </configuration>
      </plugin>
  </plugins>
</build>
<profiles>
  <profile>
      <id>run-plugin-in-ci-only</id>
      <properties>
          <!-- override to run plugin in reference ci build -->
          <skip.plugin.property>false</skip.plugin.property>
      </properties>
  </profile>
</profiles>
```

Hint: effective poms could be found in `buildinfo` files under `/build/projectsInputInfo/item[@type='pom']`
xpath (`item type="pom"`).

## Issue 3: Effective pom mismatch because of environment specific properties

Potential reason: Sometimes it is not possible to avoid discrepancies in different environments - for example if plugin
takes command line as parameter, it will be likely different on Win and linux. Such commands will appear in effective
pom as a different literal values and will result in a different effective pom hash and cache key mismatch. Solution:
filter out such properties from effective pom:

```xml
<input>
    <global>
        ...
    </global>
    <plugin artifactId="maven-surefire-plugin">
        <effectivePom>
            <excludeProperty>argLine</excludeProperty>
        </effectivePom>
    </plugin>
</input>
```

## Issue 4: Unexpected or transient files in cache key calculation

Potential reasons: plugins or tests emit temporary files (logs and similar) in non-standard locations. Solution: adjust
global exclusions list to filter out the unexpected files:

```xml
<global>
    <exclude>tempfile.out</exclude>
</global>
```

see sample config for exact syntax

## Issue 5: Difference in tracked plugin properties

Tracked property in config means it is critical for determining is build up to date or not. Discrepancies could happen
for any plugin for a number of reasons. Example: local build is using java target 1.6, remote: 1.8. `buildsdiff.xml`
will produce something like

```xml
<mismatch item="target"
          current="1.8"
          baseline="1.6"
          reason="Plugin: default-compile:compile:compile:maven-compiler-plugin:org.apache.maven.plugins:3.8.1 has mismatch in tracked property and cannot be reused"
          resolution="Align properties between remote and local build or remove property from tracked list if mismatch could be tolerated. In some cases it is possible to add skip value to ignore lax mismatch"/>
```

Solution is at your discretion. If the property is tracked, out-of-date status is fair and expected. If you want to
relax consistency rules in favor of compatibility, remove property from the reconciliations list
