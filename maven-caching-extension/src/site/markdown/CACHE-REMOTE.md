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

This document describes generic approach to cache setup. The process require expertise in Maven equivalent to expertise
required to author and Maven your project build, it implies good knowledge of both Maven and the project. Due to Maven
model limitation the process is manual, but allows you to achieve sufficient control and transparency over caching
logic.

# Step-By-Step

## Fork branch for cache setup purposes

It is recommended to fork branch for cache setup purposes as you might need to do changes to project build as long as
you go.

## Setup http server to store artifacts

In order to share build results you need shared storage. Basically any http server which supports http PUT/GET/HEAD
operations will work. In our case it is a Nginx OSS with file system module. Add the url to config and
change `remote@enabled` to true:

```xml

<remote enabled="true">
    <url>http://your-buildcache-url</url>
</remote>
```

Known limitations: auth is not supported yet

## Build selection

In order to share build results, you need a golden source of builds. Build stored in cache ideally should be a build
assembled in the most correct, comprehensive and complete way. In such a case you can make sure that whoever reuses such
build doesn't compromise quality of own build. Often per pull requests builds are the best candidates to populate cache
because they seed cache fast and provide sufficient quality safeguards.

## CI Build setup to seed shared cache

In order to share build results, you need to store builds in the shared storage. Default scheme is to configure
designated CI nodes only to push in cache and prohibit elsewhere. In order to allow writes in remote cache add jvm
property to designated CI builds.

```
-Dremote.cache.save.enabled=true
```

Run your branch, review log and ensure that artifacts are uploaded to remote cache. Now, rerun build and ensure that it
completes almost instantly because it is fully cached. Hint: consider limiting first attempts to single agent/node to
simplify troubleshooting.

## Normalize local builds to reuse CI build cache

As practice shows, developers often don't realize that builds they run in local and CI environments are different. So
straightforward attempt to reuse remote cache in local build usually results in cache misses because of difference in
plugins, parameters, profiles, environment, etc. In order to reuse results you might need to change poms, cache config,
CI jobs and the project itself. This part is usually most challenging and time-consuming. Follow steps below to
iteratively achieve working configuration.

### Before you start

Before you start, please keep in mind basic principles:

* Cache is checksum based, it is a complex hash function essentially. In order to to produce the same hash the source
  code, effective poms and dependencies should match.
* There is no built-in normalization of line endings in this implementation, file checksum calculation is raw bytes
  based. The most obvious implication could be illustrated by a simple Git checkout. By default git will check out
  source code with CRLF line endings on win and LF on Linux. Because of that builds over same commit on a Linux agent
  and local build on Windows workstation will yield different checksums.
* Parameters of plugins are manually tracked ony. For example to avoid of accidentally reusing builds which never run
  tests ensure that critical surfire parameters are tracked (`skipTests` and similar) in config. The same applies for
  all over plugins.

### Configure local build in debug mode

To minimize distractions and simplify understanding of discrepancies following is recommended:

* Run build with single threaded builder to make sure logs from different modules do not interfere
* Enable cache fail fast mode to focus on the blocking failure
* Provide reference to the CI build as a baseline for comparison between your local and remote builds. Go to the
  reference CI build and one of the final lines of the build should be

```
[INFO] [CACHE][artifactId] Saved to remote cache https://your-cache-url/<...>/915296a3-4596-4eb5-bf37-f6e13ebe087e/cache-report.xml.
```

followed by a link to a `cache-report.xml` file. The `cache-report.xml` contains aggregated information about the
produced cache and could be used as a baseline for comparison.

* Run local build. Command line should look similar to this:

```bash
mvnw verify -Dremote.cache.failFast=true -Dremote.cache.baselineUrl=https://url-from-ci-build-to-cache-report.xml
```

Once discrepancy between remote and local builds detected cache will fail with diagnostic info
in `target/incremental-maven` directory:

```
* buildinfo-baseline-3c64673e23259e6f.xml - build specification from baseline build
* buildinfo-db43936e0666ce7.xml - build specification of local build
* buildsdiff.xml - comparison report with list of discrepancies 
```

Review `buildsdiff.xml` file and eliminate detected discrepancies.You can also diff build-info files directly to get low
level insights. See techniques to configure cache in [How-To](CACHE-HOWTO.md) and troubleshooting of typical issues in
the section below.

# Common issues

## Issue 1: Local checkout is with different line endings

Solution: normalise line endings. Current implementation doesn't have built-in line endings normalization, it has to be
done externally. In git it is recommended to use `.gitattributes` file to establish consistent line endings across all
envs for file types specific to this project

## Issue 2: Effective poms mismatch because of plugins filtering by profiles

Different profiles between remote and local builds results in different text of effective poms and break checksums.
Solution: instead of adding/removing specific plugins from build altogether with profiles use profile specific `skip`
or `disabled` flags instead. Instead of:

  ```
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

Potential reason: Sometimes it is not possible to avoid discrepancies in different environments - for example if you
need to invoke command line command, it will be likely different on win and linux. Such commands will appear in
effective pom as a different literal values and will result in checksum mismatch Solution: filter out such properties
from cache effective pom:

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

## Issue 4: Unexpected or transient files in checksum calculation

Potential reasons: plugins or tests emit temporary files (logs and similar) in non-standard locations Solution: adjust
global exclusions list to filter out unexpected files:

```
<global>
    <exclude>tempfile.out</exclude>
</global>
```

see sample config for exact syntax

## Issue 5: Difference in tracked plugin properties

Tracked property in config means it is critical for determining is build up to date or not. Discrepancies could happen
for any plugin for a number of reasons. Example: local build is using java target 1.6, remote: 1.8. `buildsdiff.xml`
will produce something like

```
<mismatch item="target"
          current="1.8"
          baseline="1.6"
          reason="Plugin: default-compile:compile:compile:maven-compiler-plugin:org.apache.maven.plugins:3.8.1 has mismatch in tracked property and cannot be reused"
          resolution="Align properties between remote and local build or remove property from tracked list if mismatch could be tolerated. In some cases it is possible to add skip value to ignore lax mismatch"/>
```

Solution is at your discretion. If the property is tracked, out-of-date status is fair and expected. If you want to
relax consistency rules in favor of compatibility, remove property from tracked list

## Issue 5: Version changes invalidate effective pom checksum

Current implementation doesn't support version changes between cache entries. It will result in cache invalidation for
each new version.  
To mitigate the issue please consider migrating off traditional Maven release approach - try to use single version id in
project (eg `<version>MY-PROJECT-LOCAL</version>`). Such approach simplifies git branching workflow significantly.

Deployment of artifacts with specific version from builds with cache is not supported yet.  

