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
Apache Maven
============

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)][license]
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/apache/maven/maven/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/maven/README.md)

- [master](https://github.com/apache/maven) = 4.1.x
[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci-maven.apache.org%2Fjob%2FMaven%2Fjob%2Fmaven-box%2Fjob%2Fmaven%2Fjob%2Fmaster%2F)
  ][build]
[![Java CI](https://github.com/apache/maven/actions/workflows/maven.yml/badge.svg?branch=master)][gh-build]

- [4.0.x](https://github.com/apache/maven/tree/maven-4.0.x): 
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven/apache-maven.svg?label=Maven%20Central&versionPrefix=4.0)](https://central.sonatype.com/artifact/org.apache.maven/apache-maven)
[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci-maven.apache.org%2Fjob%2FMaven%2Fjob%2Fmaven-box%2Fjob%2Fmaven%2Fjob%2Fmaven-4.0.x%2F)][build-4.0]
[![Java CI](https://github.com/apache/maven/actions/workflows/maven.yml/badge.svg?branch=maven-4.0.x)][gh-build-4.0]

- [3.9.x](https://github.com/apache/maven/tree/maven-3.9.x): 
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven/apache-maven.svg?label=Maven%20Central&versionPrefix=3.)](https://central.sonatype.com/artifact/org.apache.maven/apache-maven)
[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci-maven.apache.org%2Fjob%2FMaven%2Fjob%2Fmaven-box%2Fjob%2Fmaven%2Fjob%2Fmaven-3.9.x%2F)][build-3.9]
[![Java CI](https://github.com/apache/maven/actions/workflows/maven.yml/badge.svg?branch=maven-3.9.x)][gh-build-3.9]

Apache Maven is a software project management and comprehension tool. Based on
the concept of a project object model (POM), Maven can manage a project's
build, reporting and documentation from a central piece of information.

If you think you have found a bug, please file an issue in the [Maven Issue Tracker](https://github.com/apache/maven/issues).

Documentation
-------------

More information can be found on [Apache Maven Homepage][maven-home].
Questions related to the usage of Maven should be posted on
the [Maven User List][users-list].


Where can I get the latest release?
-----------------------------------
You can download the release source from our [download page][maven-download].

Contributing
------------

If you are interested in the development of Maven, please consult the
documentation first and afterward you are welcome to join the developers
mailing list to ask questions or discuss new ideas/features/bugs etc.

Take a look into the [contribution guidelines](CONTRIBUTING.md).

License
-------
This code is under the [Apache License, Version 2.0, January 2004][license].

See the [`NOTICE`](./NOTICE) file for required notices and attributions.

Donations
---------
Do you like Apache Maven? Then [donate back to the ASF](https://www.apache.org/foundation/contributing.html) to support the development.

Quick Build
-------
If you want to bootstrap Maven, you'll need:
- Java 17+
- Maven 3.6.3 or later
- Run Maven, specifying a location into which the completed Maven distro should be installed:
    ```
    mvn -DdistributionTargetDir="$HOME/app/maven/apache-maven-4.1.x-SNAPSHOT" clean package
    ```


[home]: https://maven.apache.org/
[license]: https://www.apache.org/licenses/LICENSE-2.0
[build]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/master/
[build-4.0]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/maven-4.0.x/
[build-3.9]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/maven-3.9.x/
[gh-build]: https://github.com/apache/maven/actions/workflows/maven.yml?query=branch%3Amaster
[gh-build-4.0]: https://github.com/apache/maven/actions/workflows/maven.yml?query=branch%3Amaven-4.0.x
[gh-build-3.9]: https://github.com/apache/maven/actions/workflows/maven.yml?query=branch%3Amaven-3.9.x
[maven-home]: https://maven.apache.org/
[maven-download]: https://maven.apache.org/download.cgi
[users-list]: https://maven.apache.org/mailing-lists.html
[dev-ml-list]: https://www.mail-archive.com/dev@maven.apache.org/
[code-style]: http://maven.apache.org/developers/conventions/code.html
[core-it]: https://maven.apache.org/core-its/core-it-suite/
[building-maven]: https://maven.apache.org/guides/development/guide-building-maven.html
[cla]: https://www.apache.org/licenses/#clas

