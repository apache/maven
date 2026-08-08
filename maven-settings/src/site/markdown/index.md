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

This is strictly the model for Maven settings. All the effective settings building logic from multiple `settings.xml` files is done in [Maven Settings Builder](../maven-settings-builder/).

The following are generated from this model:

- [Java sources](./apidocs/index.html) with Reader and Writers for the Xpp3 XML parser
- A [Descriptor Reference](./settings.html)
- An [XSD](https://maven.apache.org/xsd/settings-1.2.0.xsd)

## See Also User Documentation

- [ Settings Reference](https://maven.apache.org/settings.html),
- [ Mirror Settings](https://maven.apache.org/guides/mini/guide-mirror-settings.html),
- [ Security and Deployment Settings](https://maven.apache.org/guides/mini/guide-deployment-security-settings.html),
- [ Password Encryption](https://maven.apache.org/guides/mini/guide-encryption.html),
- [ Configuring a proxy](https://maven.apache.org/guides/mini/guide-proxies.html).
