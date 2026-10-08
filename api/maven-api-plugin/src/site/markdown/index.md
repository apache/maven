---
title: Introduction
author: 
  - Guillaume Nodet
date: 2023-11-15
---

<!--
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

# Maven 4 API - Plugin Descriptor Model

This is the immutable model for Maven 4 Plugin Descriptor classes in `org.apache.maven.api.plugin.descriptor` package
and associated lifecycle bindings in `org.apache.maven.api.plugin.descriptor.lifecycle`.

Data about a plugin is stored in [`META-INF/maven/plugin.xml` plugin descriptor](./plugin.html),
generally generated from plugin sources using [maven-plugin-plugin](/plugin-tools/maven-plugin-plugin/)
and [Maven 4 API Core plugin annotations](../maven-api-core/) or [Maven 3 Plugin Tools' annotations](/plugin-tools/maven-plugin-annotations/index.html).

The following are generated from this model:

- [Java sources](./apidocs/index.html) with `Builder` inner classes for immutable instances creation.
- [`META-INF/maven/plugin.xml` plugin descriptor](./plugin.html)
- [`META-INF/maven/lifecycle.xml` plugin descriptor](./lifecycle.html)

## See Also

- [Maven 4 API Core](../maven-api-core/)
- [Maven 3 plugin API and descriptor](../../compat/maven-plugin-api/)
