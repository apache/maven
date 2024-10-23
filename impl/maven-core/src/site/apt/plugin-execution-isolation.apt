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
 Maven plugins
 -----
 The Maven Team
 -----

Maven plugin execution isolation

 Maven2 takes advantage of Plexus' ability to execute a component using a
 ClassWorlds ClassRealm that is populated with the JAR containing the
 component in question and all of its dependencies. Using ClassWorlds
 notation for realms we have the following:

+-----+
      [plexus.core]
            ^
            |
   [plexus.core.maven]
     ^            ^
     |            |
[plugin0]      [plugin1]
+-----+

 The <<<plexus.core>>> realm contains the resources required to run any
 plexus application; The <<<plexus.core.maven>>> realm contains all of the
 resources required to run Maven. Each subsequent plugin realm contains the
 JAR plugin as well as its dependencies. The realms noted above are setup
 in a hierarchical structure where the resources in the parent realms are
 available but the <<realm is searched first before a search is made in
 the parent realm>>.

 Plugins are guaranteed to be provided the resources found in
 <<<plexus.core>>> and <<<plexus.core.maven>>> realms at run-time if required.
 Plugins can state compile-time dependencies on any of the resources found in
 the core realms listed above and these dependencies will be included in the
 plugin descriptor that is generated but when running within Maven these
 resources will be filtered out. In other words these resources will not
 be added the realm created for the plugins execution as they are provided
 in the parent realms.

