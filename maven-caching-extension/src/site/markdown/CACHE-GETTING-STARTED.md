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
    <groupId>org.apache.maven.caching</groupId>
    <artifactId>maven-caching-extension</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</extension>
```

### Adding cache config

Copy [default config `maven-cache-config.xml`](maven-cache-config.xml)
to [`.mvn/`](https://maven.apache.org/configure.html) dir of your project.  
To get overall understanding of cache machinery it is recommended to review the config and read comments. In typical
scenario you need to adjust:

* remote cache location
* source code files selectors
* plugins reconciliation rules - add critical plugins parameters to reconciliation
* add non-standard source code locations (most of locations discovered automatically from project and plugins config,
  but still there might be edge cases)

### Adjusting cache config

Having incremental Maven and the config in place you're all set. To run first cacheable build just
execute: `mvn clean install`

* Ensure that extension is started and the cache config is picked up. Just check log output - you will notice cache
  related output or initialization error message.
* Navigate to your local repo directory - there should be sibling next to your local repo named `cache`
* Find `buildinfo.xml` for typical module and review it. Ensure that
    * expected source code files are present in build info
    * all critical plugins and their critical parameters are covered by config

Notice - in configuration you should find best working trade-off between fairness and cache efficiency. Adding
unnecessary rules and checks could reduce both performance and cache efficiency (hit rate).

### Adding caching CI and remote cache

To leverage remote cache feature you need web server which supports get/put operations
like [Nginx OSS](http://nginx.org/en/) (with fs module) or binary repo in Artifactory. It is recommended to populate
remote cache from CI build. Benefits are:

* such scheme provides predictable and consistent artifacts in remote cache
* usually CI builds project fast enough to populate cache for team members See [Remote cache setup](CACHE-REMOTE.md) for
  detailed description of cache setup
