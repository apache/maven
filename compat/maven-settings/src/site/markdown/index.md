---
title: Introduction
author: 
  - Vincent Siveton
date: 2006-11-04
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

# Maven Settings Model

This is the model for Maven settings in `org.apache.maven.settings` package, delegating content to [Maven 4 API immutable settings](../../api/maven-api-settings/index.html). All the effective model building logic from multiple settings files is done in [Maven Settings Builder](../maven-settings-builder/).

The following are generated from this model:

- [Java sources](./apidocs/index.html) with Reader and Writers for the Xpp3 XML parser, `ToAPiV3()` and `ToApiV4()` transformers, and `v4` package for Merger and v4 Reader and Writers for the Xpp3 XML parser,
- A [Descriptor Reference](../../api/maven-api-settings/settings.html)
- An [XSD](https://maven.apache.org/xsd/settings-2.0.0.xsd)

## See Also User Documentation

- [Settings Reference](https://maven.apache.org/settings.html),
- [Mirror Settings](https://maven.apache.org/guides/mini/guide-mirror-settings.html),
- [Security and Deployment Settings](https://maven.apache.org/guides/mini/guide-deployment-security-settings.html),
- [Password Encryption](https://maven.apache.org/guides/mini/guide-encryption-4.html),
- [Configuring a proxy](https://maven.apache.org/guides/mini/guide-proxies.html).
