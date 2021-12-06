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

## Normal usage

Once extension is activated, cache will kick-in automatically on every lifecycle build of phase `package` or higher.

## Subtree builds

Build could be invoked on any module in project and will try to discover cache by introspecting dependencies. In order
to identify which dependencies are part of cacheable project the cache engine needs to know:

* full project root location which must be passed with `-Dmaven.multiModuleProjectDirectory`
* Specify profiles which activate full graph in config:

```
TBD
```

## Disable cache

Disable in config:
```xml
<cache>
  <configuration>
    <enabled>true</enabled>
  </configuration>
</cache>
```
On command line:
```
-Dremote.cache.enabled=false
```

## IDE support

Cache extension is generally compatible with ides with one limitation:

* The cache doesn't restore full project state. Compiled classes and other output directories will not be restored from
  cache if `clean` was invoked. In order to work efficiently, IDE should be configured to not use maven
  output (`target`) directories for compilation. In that case compilation caches will be maintained by IDE leveraging
  both fast builds and fast compilation

