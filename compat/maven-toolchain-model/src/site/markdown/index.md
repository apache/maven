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

# Maven Toolchain Model

This is the model for Maven toolchain in `org.apache.maven.toolchain` package, delegating content to [Maven 4 API immutable toolchain](../../api/maven-api-toolchain/index.html). All the effective model building logic from multiple toolchains files is done in [Maven Toolchain Builder](../maven-toolchain-builder/).

The following are generated from this model:

- [Java sources](./apidocs/index.html) with Reader and Writers for the Xpp3 XML parser, `ToAPiV3()` and `ToApiV4()` transformers, and `v4` package for Merger and v4 Reader and Writers for the Xpp3 XML parser,
- A [Descriptor Reference](../../api/maven-api-toolchain/toolchains.html)
- An [XSD](https://maven.apache.org/xsd/toolchains-1.2.0.xsd)
