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

### Overview

Cache configuration provides you additional control over incremental Maven behavior. Follow it step by step to
understand how it works and figure out your optimal config

### Minimal config

Absolutely minimal config

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<cache xmlns="http://maven.apache.org/CACHE-CONFIG/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://maven.apache.org/CACHE-CONFIG/1.0.0 http://maven.apache.org/xsd/cache-config-1.0.0.xsd">

    <configuration>
        <enabled>true</enabled>
        <hashAlgorithm>XX</hashAlgorithm>
    </configuration>

    <input>
        <global>
            <glob>{*.java,*.xml,*.properties}</glob>
        </global>
    </input>
</cache>
```

### Enabling remote cache

Just add `<remote>` section under `<configuration>`

```xml
    <configuration>
        <enabled>true</enabled>
        <hashAlgorithm>XX</hashAlgorithm>
        <remote>
            <url>https://yourserver:port</url>
        </remote>
    </configuration>
```

### Adding more file types to input

Add all the project specific source code files in `<glob>`. Scala in this case:

```xml
    <input>
        <global>
            <glob>{*.java,*.xml,*.properties,*.scala}</glob>
        </global>
    </input>
```

### Adding source directory for bespoke project layouts

In most of the cases incremental Maven will recognize directories automatically by build introspection. If not, you can
add additional directories with `<include>`. Also you can filter out undesirable dirs and files by using exclude tag

```xml
    <input>
        <global>
            <glob>{*.java,*.xml,*.properties,*.scala}</glob>
            <includes>
                <include>importantdir/</include>
            </includes>
            <excludes>
                <exclude>tempfile.out</exclude>
            </excludes>
        </global>
    </input>
```

### Plugin property is env specific (breaks checksum and caching)

Consider to exclude env specific properties:

```xml
    <input>
        <global>
            ...
        </global>
        <plugins>
            <plugin artifactId="maven-surefire-plugin">
                <effectivePom>
                    <excludeProperties>
                        <excludeProperty>argLine</excludeProperty>
                    </excludeProperties>
                </effectivePom>
            </plugin>
        </plugins>
    </input>
```

Implications - builds with different `argLine` will have identical checksum. Validate that is semantically valid.

### Plugin property points to directory where only subset of files is relevant

If plugin configuration property points to `somedir` it will be scanned with default glob. You can tweak it with custom
processing rule

```xml
    <input>
        <global>
            ...
        </global>
        <plugins>
            <plugin artifactId="protoc-maven-plugin">
                <dirScan mode="auto">
                    <!--<protoBaseDirectory>${basedir}/..</protoBaseDirectory>-->
                    <tagScanConfigs>
                        <tagScanConfig tagName="protoBaseDirectory" recursive="false" glob="{*.proto}"/>
                    </tagScanConfigs>
                </dirScan>
            </plugin>
        </plugins>
    </input>
```

### Local repository is not updated because `install` is cached

Add `executionControl/runAlways` section

```xml
    <executionControl>
        <runAlways>
            <plugins>
                <plugin artifactId="maven-failsafe-plugin"/>
            </plugins>
            <executions>
                <execution artifactId="maven-dependency-plugin">
                    <execIds>
                        <execId>unpack-autoupdate</execId>
                    </execIds>
                </execution>
            </executions>
            <goalsLists>
                <goalsList artifactId="maven-install-plugin">
                    <goals>
                        <goal>install</goal>
                    </goals>
                </goalsList>
            </goalsLists>
        </runAlways>
    </executionControl>
``` 

### I occasionally cached build with `-DskipTests=true` and tests do not run now

If you add command line flags to your build, they do not participate in effective pom - Maven defers final value
resolution to plugin runtime. To invalidate build if filed value is different in runtime, add reconciliation section
to `executionControl`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<cache xmlns="http://maven.apache.org/CACHE-CONFIG/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://maven.apache.org/CACHE-CONFIG/1.0.0 http://maven.apache.org/xsd/cache-config-1.0.0.xsd">
    <configuration>
        ...
    </configuration>
    <executionControl>
        <runAlways>
            ...
        </runAlways>
        <reconcile>
            <plugins>
                <plugin artifactId="maven-surefire-plugin" goal="test">
                    <reconciles>
                        <reconcile propertyName="skip" skipValue="true"/>
                        <reconcile propertyName="skipExec" skipValue="true"/>
                        <reconcile propertyName="skipTests" skipValue="true"/>
                        <reconcile propertyName="testFailureIgnore" skipValue="true"/>
                    </reconciles>
                </plugin>
            </plugins>
        </reconcile>
    </executionControl>
</cache>
```

Please notice `skipValue` attribute. It denotes value which forces skipped execution.
Read `propertyName="skipTests" skipValue="true"` as if property skipTests has value true, plugin will skip execution If
you declare such value incremental Maven will reuse appropriate full-build though technically they are different, but
because full-build is better it is safe to reuse

### How to renormalize line endings in working copy after committing .gitattributes (git 2.16+)

Ensure you've committed (and ideally pushed everything) - no changes in working copy. After that:

```shell
# Rewrite objects and update index
git add --renormalize .
# Commit changes
git commit -m "Normalizing line endings"
# Remove working copy paths from git cache
git rm --cached -r .
# Refresh with new line endings
git reset --hard
```

### I want to cache interim build and override it later with final version

Solution: set `-Dremote.cache.save.final=true` to nodes which produce final builds. Such builds will not be overridden
and eventually will replace all interim builds
