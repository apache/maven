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

 ---
 Default Artifact Handlers Reference
 ---
 Hervé Boutemy
 ---
 2026-06-25
 ---

Default Artifact Handlers Reference

  Artifact handlers (see {{{../maven-artifact/apidocs/org/apache/maven/artifact/handler/ArtifactHandler.html} API}})
  define for each {{{../maven-model/maven.html#class_dependency}dependency type}} information on the artifact
  (classifier, extension, language) and how to manage it as dependency (add to classpath, include dependencies).

*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
|| type                 || classifier   || extension || language || added to classpath   || includesDependencies ||
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<pom>>>             |               | <= type>   | none      |                       |                       |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<maven-plugin>>>    |               | <<<jar>>>  | java      | <<true>>              |                       |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<jar>>>             |               | <= type>   | java      | <<true>>              |                       |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<java-source>>>     | <<<sources>>> | <<<jar>>>  | java      |                       |                       |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<javadoc>>>         | <<<javadoc>>> | <<<jar>>>  | java      | <<true>>              |                       |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<test-jar>>>        | <<<tests>>>   | <<<jar>>>  | java      | <<true>>              |                       |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<fatjar>>>          |               | <<<jar>>>  | java      | <<true>>              | <<<true>>>            |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<ejb>>>             |               | <<<jar>>>  | java      | <<true>>              |                       |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<ejb-client>>>      | <<<client>>>  | <<<jar>>>  | java      | <<true>>              |                       |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<war>>>             |               | <= type>   | java      |                       | <<<true>>>            |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<ear>>>             |               | <= type>   | java      |                       | <<<true>>>            |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+
| <<<rar>>>             |               | <= type>   | java      |                       | <<<true>>>            |
*-----------------------+---------------+------------+-----------+-----------------------+-----------------------+

  <<<fatjar>>> is new in Maven 3.10.0