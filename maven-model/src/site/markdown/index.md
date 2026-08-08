~~ Licensed to the Apache Software Foundation (ASF) under one
~~ or more contributor license agreements.  See the NOTICE file
~~ distributed with this work for additional information
~~ regarding copyright ownership.  The ASF licenses this file
~~ to you under the Apache License, Version 2.0 (the
~~ "License"); you may not use this file except in compliance
~~ with the License.  You may obtain a copy of the License at
~~
~~ http://www.apache.org/licenses/LICENSE-2.0
~~
~~ Unless required by applicable law or agreed to in writing,
~~ software distributed under the License is distributed on an
~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~~ KIND, either express or implied.  See the License for the
~~ specific language governing permissions and limitations
~~ under the License.

 -----
 Introduction
 -----
 Jason van Zyl
 Vincent Siveton
 Hervé Boutemy
 -----
 2011-06-12
 -----

Maven Model

 This is strictly the model for Maven POM (Project Object Model), so really just plain objects. All the effective model
 building logic from multiple POMs and building context is done in {{{../maven-model-builder/}Maven Model Builder}}.

 The following are generated from this model:

   * {{{./apidocs/index.html}Java sources}} with Reader and Writers for the Xpp3 XML parser

   * A {{{./maven.html}Descriptor Reference}}

   * An XSD {{{https://maven.apache.org/xsd/maven-v3_0_0.xsd}for Maven 1.1}} and {{{https://maven.apache.org/xsd/maven-4.0.0.xsd}for Maven 2.0}}.
