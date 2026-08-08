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
 Maven Logging
 -----
 Hervé Boutemy
 -----
 2013-08-02
 -----

Maven Logging

 {{{/maven-logging.html}End-user logging documentation}} is available {{{/maven-logging.html}in Maven site}}.
 This documentation is focused on internal implementation details.

* Logging API

 Maven uses
 {{{https://codehaus-plexus.github.io/plexus-containers/plexus-container-default/apidocs/org/codehaus/plexus/logging/package-summary.html}Plexus
 Container logging API}}, like any other Plexus components, ie
 {{{https://codehaus-plexus.github.io/plexus-containers/plexus-container-default/apidocs/org/codehaus/plexus/logging/LoggerManager.html}LoggerManager}}
 / {{{https://codehaus-plexus.github.io/plexus-containers/plexus-container-default/apidocs/org/codehaus/plexus/logging/Logger.html}Logger}}.

 Starting with Maven 3.1.0:

  *  Maven supports SLF4J API logging API too, ie {{{http://slf4j.org/apidocs/org/slf4j/LoggerFactory.html}LoggerFactory}} /
     {{{http://slf4j.org/apidocs/org/slf4j/Logger.html}Logger}},

  *  instead of implementing Plexus logging API itself with basic output to console, Maven implements it using SLF4J API in
     {{{./apidocs/org/apache/maven/cli/logging/Slf4jLoggerManager.html}Slf4jLoggerManager}}
     / {{{./apidocs/org/apache/maven/cli/logging/Slf4jLogger.html}Slf4jLogger}}.


* Logging Implementation

 Maven 3.1.0 ships bundled with {{{https://www.slf4j.org/api/org/slf4j/simple/SimpleLogger.html}SLF4J simple logger}} and since 3.5.0 {{{../maven-slf4j-provider/}Maven-customized <<<maven-slf4j-provider>>>}},
 but is ready to use other logging implementations: SLF4J is responsible for loading the implementation, referred to as
 {{{http://www.slf4j.org/manual.html#swapping}"SLF4J bindings"}}.

 Logging configuration loading is actually done by logging implementation, without any Maven extensions to support merging
 Maven installation configuration with per-user configuration for example:
 <<<$\{maven.conf}/logging>>> directory was added to core's classpath (see <<<$\{maven.home}/bin/m2.conf>>>). See your implementation
 documentation for details on file names, formats, and so on.

 During Maven initialization, Maven tweaks default root logging level to match CLI verbosity choice. Since such feature isn't available
 in SLF4J API, logging implementation specific extensions need to be added into Maven to support these CLI options: see
 {{{./apidocs/org/apache/maven/cli/logging/Slf4jConfigurationFactory.html}Slf4jConfigurationFactory}} /
 {{{./apidocs/org/apache/maven/cli/logging/Slf4jConfiguration.html}Slf4jConfiguration}}.

~~ TODO document META-INF/maven/slf4j-configuration.properties

* Getting Logger Instance

 Starting with Maven 3.1.0, SLF4J Logger can be used directly. This technique can be used safely in Maven core
 components or in plugins/component not requiring compatibility with previous Maven versions.

+-----+
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass
{
   final Logger logger = LoggerFactory.getLogger( MyClass.class );
}
+-----+

* Logger Name

 Logger name is basically the classical fully qualified class name: it's not visible by default, but can be activated (see {{{/maven-logging.html}user documentation}}).

 Notice that before Maven 3.1.0, with logger created by Maven, some code used to pass logger from class to class because it could not create a new logger:
 discrepencies between logger name and actual class may happen.
