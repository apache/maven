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
 Hervé Boutemy
 -----
 2014-11-30
 -----

Maven Artifact

 Maven Artifact classes, providing <<<Artifact>>> interface ({{{./apidocs/org/apache/maven/artifact/Artifact.html}javadoc}}),
 with its <<<DefaultArtifact>>> implementation ({{{./xref/org/apache/maven/artifact/DefaultArtifact.html}source}}).

 The jar file is executable and provides a little tool to display how Maven parses and compares versions:

+----+
$ java -jar maven-artifact-*.jar 3.2.4-alpha-1 3.2.4-SNAPSHOT 3.2.4.0
Display parameters as parsed by Maven (in canonical form) and comparison result:
1. 3.2.4-alpha-1 == 3.2.4.alpha.1
   3.2.4-alpha-1 < 3.2.4-SNAPSHOT
2. 3.2.4-SNAPSHOT == 3.2.4.snapshot
   3.2.4-SNAPSHOT < 3.2.4.0
3. 3.2.4.0 == 3.2.4
+----+

* Useful entry points

 * artifact version comparison {{{./apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html}javadoc}},

 * {{{./apidocs/org/apache/maven/artifact/versioning/VersionRange.html}version range}}.
