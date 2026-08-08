---
title: Default Artifact Handlers Reference
author:
  - Hervé Boutemy
date: 2013-08-02
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

Some artifact handlers are configured by default in `META-INF/plexus/artifact-handlers.xml`:

|type|classifier|extension|packaging|language|added to classpath|includesDependencies|
|:---|:---|:---|:---|:---|:---|:---|
|`pom`| |_= type_|_= type_|none| | |
|`jar`| |_= type_|_= type_|java|`true`| |
|`test-jar`|`tests`|`jar`|`jar`|java|`true`| |
|`maven-plugin`| |`jar`|_= type_|java|`true`| |
|`ejb`| |`jar`|_= type_|java|`true`| |
|`ejb-client`|`client`|`jar`|`ejb`|java|`true`| |
|`war`| |_= type_|_= type_|java| |`true`|
|`ear`| |_= type_|_= type_|java| |`true`|
|`rar`| |_= type_|_= type_|java| |`true`|
|`java-source`|`sources`|`jar`|_= type_|java| | |
|`javadoc`|`javadoc`|`jar`|_= type_|java|`true`| |

