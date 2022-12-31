<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
Apache Maven Plexus Utils
=========================

This module is a repackaging of the `org.codehaus.plexus:plexus-utils` jar, which contains a few modified classes to allow a seamless transition between the Maven 3.x and 4.x APIs.

The Maven 4.x API is based on immutable data objects. The Maven model contains a few classes that contain some open xml data for configuration (`Plugin`, `PluginExecution`, `ReportPlugin` and `ReportSet`). So the v3 API which was using the `org.codehaus.plexus.utils.xml.Xpp3Dom` class now wraps the `org.apache.maven.api.Dom` interface node.  This is completely transparent for existing plugins, but the correct (new) classes have to be used.

Given the new implementation of `org.codehaus.plexus.utils.xml.Xpp3Dom` now relies on `org.apache.maven.api.Dom`, the modifications can't be made inside the `plexus-utils` project, because Maven itself depends on it.

This is drop-in replacement for `plexus-utils` 3.4.2.
