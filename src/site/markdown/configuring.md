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
# Configuring Apache Maven

## System properties and JVM

The `MAVEN_OPTS` environment variable contains parameters used to 
start up the JVM running Maven and can be used to supply additional 
options to it. For example, JVM memory settings could be defined 
with the value `-Xms256m -Xmx512m`.

JVM options can also be configured in the 
`${session.rootDirectory}/.mvn/jvm.config` file, which means you can 
define the options for your build on a per-project basis. This file 
will become part of your project and will be checked in along with 
your project.

So for example if you put the following JVM options into the 
`.mvn/jvm.config` file:
```
-Xmx2048m -Xms1024m -XX:MaxPermSize=512m -Djava.awt.headless=true
```

The content of this file will be appended to the `MAVEN_OPTS` 
environment variable.

Note that the above mechanism is handled by the Maven launch 
scripts `mvn` and `mvnDebug` (`mvn.cmd` and `mvnDebug.cmd` on 
Windows platforms).

## User Properties

Once the JVM is started, Maven uses _user properties_ to configure
various parts of the system. The main entry point is the 
`${maven.home}/conf/maven.properties` properties file. This file,
along with any other files loaded from it (see below) are called
_Maven properties files_ and are Java properties files with a 
few enhancements. Contrary to standard properties file, those files 
are UTF-8 encoded.
 
### File Inclusion

Additional files can be loaded using the special `${includes}` key. 
The associated value is a comma-separated list of additional files to 
include. Each item may be enclosed in quotes to gracefully include spaces. 
Items are trimmed before being loaded.  If the first character of an item 
is a question mark, the load will silently fail if the file does not exist.
```
${includes} = ?"${maven.user.conf}/maven.properties", \
              ?"${maven.project.conf}/maven.properties"
```

### Property Substitution

Properties undergo the _property substitution_ process, so that any 
value containing a `${xxx}` placeholder will be substituted with the value 
of the `xxx` property. In addition to properties defined in the files
being loaded, the following properties are defined:
* `session.topDirectory`
* `session.rootDirectory`
* `maven.version`
* `maven.build.version`
* `env.XYZ` to refer to the `XYZ` environment variable
* `cli.OPT` to refer to the `OPT` command line option
* system properties

The main `${maven.home}/conf/maven.properties` defines a few basic properties,
but more importantly, loads the _user_ properties from `~/.m2/maven.properties`
and the _project_ specific properties from `${session.rootDirectory}/.mvn/maven.properties`.

## Settings

## Extensions

## Maven Command Line Arguments

Maven is a command line tool, and, as such, accepts various command
line arguments.  The `MAVEN_ARGS` environment value is used to
provide additional arguments to Maven.  Those arguments will be
prepended to the actual list of command line arguments.

For example, options and goals could be defined with the value
`-B -V checkstyle:checkstyle`.

