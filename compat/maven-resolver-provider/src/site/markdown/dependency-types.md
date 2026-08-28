---
title: Default Dependency Types
author: 
  - Hervé Boutemy
date: 2024-04-02
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

# Default Dependency Types Reference

Defined in `DefaultTypeProvider` ([javadoc](./apidocs/org/apache/maven/repository/internal/type/DefaultTypeProvider.html), [source](./xref/org/apache/maven/repository/internal/type/DefaultTypeProvider.html)):

|type|classifier|extension|language|path types|includesDependencies|
|:---|:---|:---|:---|:---|:---|
|**Maven**| | | | | |
|`pom`| |_= type_|none| | |
|`bom` *| |`pom`|none| | |
|`maven-plugin`| |`jar`|java|classes| |
|**Java**| | | | | |
|`jar`| |_= type_|java|classes, modules| |
|`java-source`|`sources`|`jar`|java| | |
|`javadoc`|`javadoc`|`jar`|java|classes| |
|`test-jar`|`tests`|`jar`|java|classes, patch module| |
|`test-java-source` *|`test-sources`|`jar`|java| | |
|`modular-jar` *| |`jar`|java|modules| |
|`classpath-jar` *| |`jar`|java|classes| |
|`fatjar` *| |`jar`|java|classes|`true`|
|`processor` *| |`jar`|java|processor classes, processor modules| |
|`classpath-processor` *| |`jar`|java|processor classes| |
|`modular-processor` *| |`jar`|java|processor modules| |
|**Java/Jakarta EE**| | | | | |
|`ejb`| |`jar`|java|classes| |
|`ejb-client`|`client`|`jar`|java|classes| |
|`war`| |_= type_|java| |`true`|
|`ear`| |_= type_|java| |`true`|
|`rar`| |_= type_|java| |`true`|
|`par` *| |_= type_|java| |`true`|

- = new in Maven 4
