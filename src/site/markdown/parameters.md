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

## Build Cache Parameters

This documents contains various configuration parameters supported by cache engine

### Command line flags

| Parameter   | Description | Usage Scenario |
| ----------- | ----------- | ----------- |
| `-Dremote.cache.configPath=path to file`              | Location of cache configuration file                          | Cache config is not in default location |
| `-Dremote.cache.enabled=(true/false)`                 | Remote cache and associated features disabled/enabled         | To remove noise from logs then remote cache is not available |
| `-Dremote.cache.save.enabled=(true/false)`            | Remote cache save allowed or not                              | To designate nodes which allowed to push in remote shared cache |
| `-Dremote.cache.save.final=(true/false)`              | Prohibit to override remote cache                             | To ensure that reference build is not overridden by interim build |
| `-Dremote.cache.failFast=(true/false)`                | Fail on the first module which cannot be restored from cache  | Remote cache setup/tuning/troubleshooting |
| `-Dremote.cache.baselineUrl=<http url>`               | Location of baseline build for comparison                     | Remote cache setup/tuning/troubleshooting |
| `-Dremote.cache.lazyRestore=(true/false)`             | Restore artifacts from remote cache lazily                    | Performance optimization |
| `-Dremote.cache.restoreGeneratedSources=(true/false)` | Do not restore generated sources and directly attached files  | Performance optimization |

### Project level properties

Project level parameters allow overriding global parameters on project level Must be specified as project properties:

```xml
<pom>
    ...
    <properties>
        <remote.cache.input.glob>{*.css}</remote.cache.input.glob>
    </properties>
</pom>
```

| Parameter                     | Description |
| ----------------------------- | ----------- |
| `remote.cache.input.glob`     | Project specific glob to select sources. Overrides global glob. |
| `remote.cache.input`          | Property prefix to mark paths which must be additionally scanned for source code. Value of property starting with this prefix will be treated as path relatively to current project/module |
| `remote.cache.exclude`        | Property prefix to mark paths which must be excluded from source code search. Value of property starting with this prefix will be treated as path to current project/module  |
| `remote.cache.processPlugins` | Introspect plugins to find inputs or not. Default is true. |
