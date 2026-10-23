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
few enhancements. Unlike standard properties files, these files are 
UTF-8 encoded.
 
### File Inclusion

Additional files can be loaded using the special `${includes}` key. 
The associated value is a comma-separated list of additional files to 
include. Each item may be enclosed in quotes to gracefully include spaces. 
Items are trimmed before being loaded.  If the first character of an item 
is a question mark, the load will silently fail if the file does not exist.
```
${includes} = ?"${maven.user.conf}/maven-system.properties", \
              ?"${maven.project.conf}/maven-system.properties"
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

The main system properties source `${maven.home}/conf/maven-system.properties` defines a few basic properties,
but more importantly, loads the _user wide_ system properties from `~/.m2/maven-system.properties`
and the _project_ specific system properties from `${session.rootDirectory}/.mvn/maven-system.properties`.

The main user properties source `${maven.home}/conf/maven-user.properties` defines a few inclusions only, to
load the _user wide_ user properties from `~/.m2/maven-user.properties` and the _project_ specific user properties 
from `${session.rootDirectory}/.mvn/maven-user.properties`.

For the complete list of available Maven configuration properties and options, see [Maven Configuration Options](configuration.html).

## Settings

Settings are configured in the `~/.m2/settings.xml` file (user level),
`${session.rootDirectory}/.mvn/settings.xml` (project level), and
`${maven.installation.conf}/settings.xml` (Maven installation level). See [Settings Reference](./api/maven-api-settings/settings.html) for details.

## Extensions

Extensions allow you to add additional functionality to Maven. They are
configured in the `~/.m2/extensions.xml` file (user level),
`${session.rootDirectory}/.mvn/extensions.xml` (project level), and `${maven.installation.conf}/extensions.xml` (Maven installation level).

## Maven Command Line Arguments

Maven is a command line tool, and, as such, accepts various command
line arguments.  The `MAVEN_ARGS` environment variable is used to
provide additional arguments to Maven. These arguments will be
prepended to the actual list of command line arguments.

For example, options and goals could be defined with the value
`-B -V checkstyle:checkstyle`.

## Dependency injection diagnostics

Set the `maven.di.debug` Java system property before startup to print Maven DI
bindings, including those discovered during container creation. For example, to
run the `validate` phase with diagnostics:

```sh
MAVEN_OPTS="-Dmaven.di.debug=true" mvn validate
```

Use `true` (case insensitive) or an empty value to show all bindings.

An unset property or any other value, including `false`, disables native dumps.
Sisu diagnostics use the separate `sisu.debug` property. Set both to inspect
both containers:

```sh
MAVEN_OPTS="-Dmaven.di.debug=true -Dsisu.debug=true" mvn validate
```

Each block starts with `Maven DI native registry` and identifies the injector
and registry revision. Full snapshots list keys in sorted order, including
aliases and keys without candidates. Candidates within each key appear in
descending priority order. Deltas use the same order for new or changed
candidates, followed by removals. Output goes to standard output; Maven may
prefix lines with `[stdout]`.

Candidate details list priority, scope, source, declared dependencies,
constructor and factory method parameters, declared exceptions and initializers
on separate lines. Initializers identify injected fields and methods. Optional
dependencies start with `?`; this does not mean they are missing.

Class loader references such as `L1` distinguish types from different realms.
They remain stable across a full snapshot and its deltas; each block ends with a
legend of their full identities. `bootstrap` identifies the bootstrap class
loader.

Snapshots print after discovery and when lookup or injection observes changed
registrations, including context bindings added before a Maven 4 Mojo runs. The
first block is a full snapshot. Later blocks show changed keys, individual
candidates and scopes: `+` for additions, `-` for removals and `~` for changes.
Delta headers give the previous and current registry revisions. Repeated lookups
of an unchanged registry produce no output. Discovery or registration failures
produce a block marked `partial`.

Snapshots print in capture order. Parallel callers wait until their snapshot is
processed. Checkpoints from callbacks on the writer thread queue until the
callback returns. Changing the stream resets the history. Output failures and
checkpoints that observe diagnostics disabled also reset it. The next output
starts with a full snapshot.

Snapshots do not create components, call providers or initializers, or obtain
scope instances. Bound instances, suppliers and annotation qualifiers appear by
type and identity, without exposing values or calling their `toString()`
methods. Only the native registry is included; components supplied on demand by
Sisu or Guice may be absent.
