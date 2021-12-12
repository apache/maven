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

## Getting Started

To on-board incremental Maven you need to complete several steps:

* Declare caching extension in your project
* Add cache config in `.mvn` (optional) to customize default behavior
* Validate build results and iteratively adjust config to properly reflect project specifics
* Setup remote cache (optional)

### Declaring cache extension

```xml
<extension>
    <groupId>org.apache.maven.extensions</groupId>
    <artifactId>maven-build-cache-extension</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</extension>
```

### Adding cache config

Copy [default config `maven-cache-config.xml`](maven-cache-config.xml)
to [`.mvn/`](https://maven.apache.org/configure.html) directory of your project.  
To get overall understanding of cache machinery it is recommended to review the config and read comments. In typical
scenario you need to adjust:

* Exclusions for unstable, temporary files or environment specific files
* Plugins reconciliation rules – add critical plugins parameters to reconciliation
* Source code files selectors. Though source code locations discovered automatically from project and plugins config,
  there might be edge cases.
* remote cache location (if remote cache is used)

### Adjusting cache config

Having extension run usual command, like `mvn package`. Verify the caching engine is activated:

* Check log output - there should be cache related output or initialization error message.
* Navigate to your local repo directory - there should be a sibling directory `cache` next to the usual
  local `repository`.
* Find `buildinfo.xml` in the cache repository for typical module and review it. Ensure that
  * expected source code files are present in the build info
  * all critical plugins and their critical parameters are covered by config

It is recommended to find the best working trade-off between fairness and cache efficiency. Adding unnecessary rules and
checks could reduce both performance and cache efficiency (hit rate).

### Adding caching CI and remote cache

To leverage remote cache feature there should a shared storage provide. Any technology supported
by [Maven Wagon](https://maven.apache.org/wagon/) will suffice. In simplest form it could be a http web server which
supports get/put operations ([Nginx OSS](http://nginx.org/en/) with fs module or any other equivalent).
See [Remote cache setup](remote-cache.md) for detailed description of cache setup
