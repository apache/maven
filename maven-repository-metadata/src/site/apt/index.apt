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
 2009-04-26
 -----

Maven Repository Metadata Model

 This is strictly the model for Maven Repository Metadata, so really just plain objects.

 The metadata file name is:

 * <<<maven-metadata.xml>>> in a remote repository,

 * <<<maven-metadata-\<repo-id>.xml>>> in a local repository, for metadata from a repository with <<<repo-id>>> identifier.

 []

 Depending on what the directory represents ("groupId", "groupId/artifactId" or "groupId/artifactId/version"),
 the Maven Repository Metadata file contains 3 different sets of metadata:

 [[1]] in a "groupId" directory: a "groupId" directory may contain Maven plugins artifacts, which are described in metadata's <<<plugins>>> element,

 [[2]] in a "groupId/artifactId" directory: metadata describes <<<groupId>>>, <<<artifactId>>> and <<<versioning>>> element that
       gives data about available versions (<<<latest>>>, <<<release>>>, <<<versions>>> list and <<<lastUpdated>>>),

 [[3]] in a "groupId/artifactId/version" snapshot artifact directory: metadata describes <<<groupId>>>, <<<artifactId>>>, <<<version>>> (base version, i.e. ending in <<<-SNAPSHOT>>>) and
       <<<versioning>>> element that gives data about snapshot (<<<snapshot>>>, <<<lastUpdated>>> and <<<snapshotVersions>>> list). Notice that a
       release artifact directory is not expected to provide metadata.

 []

 The following are generated from this model:

   * {{{./apidocs/index.html}Java sources}} with Reader and Writers for the Xpp3 XML parser, to read and write <<<maven-metadata(-*).xml>>> files,

   * a {{{./repository-metadata.html}Descriptor Reference}}.
 
 
 For more information see this page: {{{https://maven.apache.org/repositories/metadata.html}Maven Metadata}}.
 
