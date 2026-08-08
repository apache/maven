---
title: Default Artifact Handlers Reference
author:
  - Hervé Boutemy
date: 2026-06-25
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

# Default Artifact Handlers Reference

Artifact handlers (see [ API](../maven-artifact/apidocs/org/apache/maven/artifact/handler/ArtifactHandler.html)) define for each [dependency type](../maven-model/maven.html#class_dependency) information on the artifact (classifier, extension, language) and how to manage it as dependency (add to classpath, include dependencies).

|type|classifier|extension|language|added to classpath|includesDependencies|
|:---|:---|:---|:---|:---|:---|
|`pom`| |_= type_|none| | |
|`maven-plugin`| |`jar`|java|**true**| |
|`jar`| |_= type_|java|**true**| |
|`java-source`|`sources`|`jar`|java| | |
|`javadoc`|`javadoc`|`jar`|java|**true**| |
|`test-jar`|`tests`|`jar`|java|**true**| |
|`fatjar`| |`jar`|java|**true**|`true`|
|`ejb`| |`jar`|java|**true**| |
|`ejb-client`|`client`|`jar`|java|**true**| |
|`war`| |_= type_|java| |`true`|
|`ear`| |_= type_|java| |`true`|
|`rar`| |_= type_|java| |`true`|

`fatjar` is new in Maven 3.10.0

