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

## Performance Tuning

Various setup options which affect cache performance.

### General notes

Tuning of cache performance could reduce both resources consumption and build execution time but that is not guaranteed.
In many scenarios build time of invalidated (changed) projects could be dominating in overall build time. Performance
wins achieved by a faster cache engine might not correlate with final build times in straightforward way. As usual with
performance, effect of performance optimizations should be carefully measured in relevant scenarios.

### Hash algorithm selection

By default, cache uses SHA-256 algorithm which is sufficiently fast and provides negligible probability of hash
collisions. In projects with large codebase, performance of hash algorithms becomes more important and in such
scenarios [XX](https://cyan4973.github.io/xxHash/) or XXMM (memory mapped files) hashing algorithms provide better
performance.

```xml
<hashAlgorithm>XX</hashAlgorithm>
```

or
```xml

<hashAlgorithm>XXMM</hashAlgorithm>
```

### Filter out unnecessary/huge artifacts

Price of uploading and downloading from cache of huge artifacts could be significant. In many scenarios assembling WAR,
EAR or ZIP archive could be done more efficiently locally from cached JARs than storing bundles. In order to filter out
artifacts add configuration section:

```xml
<cache>
    <output>
        <exclude>
            <pattern>.*\.zip</pattern>
        </exclude>
    </output>
</cache>
```

### Use lazy restore

By default, cache tries to restore all artifacts for a project preemptively. Lazy restore could give a significant time
and resources wins for remote cache by avoiding requesting and downloading unnecessary artifacts from cache. Use command
line flag:

```
-Dremote.cache.lazyRestore=true";
```

Note: In case of cache corruption lazy cache cannot fallback to normal execution, it will fail instead. To heal the
corrupted cache need to force rewrite of the cache or remove corrupted cache entries manually

### Disable project files restoration

By default, cache support partial restore of source code state from cached generated sources (and potentially more,
depending on configuration). This could be helpful in local environment, but likely unnecessary and adds overhead in
continuous integration. To disable add command line flag

```
-Dremote.cache.restoreGeneratedSources=false";
```

### Disable post-processing of archives(JARs, WARs, etc) META-INF

Post-processing is disabled by default, but for some projects cache could be configured to auto-correct metadata (most
notably [MANIFEST.MF `Implementation-Version`](https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes))
. This could be rather expensive as it requires copying and repacking archive entries. If metadata state is not relevant
for the build (continuous integration, `verify` scenarios and similar) consider disabling it:

```xml
<cache>
    <configuration>
        ...
        <projectVersioning adadjustMetaInf="false"/>
        ...
    </configuration>
    ...
</cache>
```
