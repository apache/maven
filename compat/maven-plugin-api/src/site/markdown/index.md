---
title: Introduction
author:
  - Hervé Boutemy
date: 2012-06-02
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

# Maven 3 Plugin API

The API for Maven 3 plugins - composed of goals implemented by Mojos - development:

- goal code extends [`AbstractMojo` base class](./apidocs/org/apache/maven/plugin/AbstractMojo.html) that implements [`Mojo` interface](./apidocs/org/apache/maven/plugin/Mojo.html),
- [`Log` interface](./apidocs/org/apache/maven/plugin/logging/Log.html) provides easy logging for the goal.

A plugin is described in a [`META-INF/maven/plugin.xml` plugin descriptor](../../api/maven-api-plugin/plugin.html), generally generated from plugin sources using [maven-plugin-plugin](/plugin-tools/maven-plugin-plugin/).

## See Also

- [Mojo API Specification](/developers/mojo-api-specification.html)
- [Plugin Tools](/plugin-tools/) that provide [maven-plugin-plugin](/plugin-tools/maven-plugin-plugin/) to generate the [`META-INF/maven/plugin.xml` plugin descriptor](./plugin.html)
- [Plugin Testing](/plugin-testing/) frameworks
