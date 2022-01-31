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

[![ASF Jira](https://img.shields.io/endpoint?url=https%3A%2F%2Fmaven.apache.org%2Fbadges%2Fasf_jira-MNG.json)][jira]
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)][license]
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven/apache-maven.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.apache.maven/apache-maven)
[![Jenkins Status](https://img.shields.io/jenkins/s/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/master.svg?)][build]
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/master.svg?)][test-results]


Apache Maven is a software project management and comprehension tool. Based on
the concept of a project object model (POM), Maven can manage a project's
build, reporting and documentation from a central piece of information.

If you think you have found a bug, please file an issue in the [Maven Issue Tracker][jira].

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
- Java 8+
- Maven 3.0.5 or later
- Run Maven, specifying a location into which the completed Maven distro should be installed:
    ```
    mvn -DdistributionTargetDir="$HOME/app/maven/apache-maven-4.0.x-SNAPSHOT" clean package
    ```


[home]: https://maven.apache.org/
[jira]: https://issues.apache.org/jira/projects/MNG/
[license]: https://www.apache.org/licenses/LICENSE-2.0
[build]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/master/
[test-results]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/master/lastCompletedBuild/testReport/
[build-status]: https://img.shields.io/jenkins/s/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/master.svg?
[build-tests]: https://img.shields.io/jenkins/t/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven/job/master.svg?
[maven-home]: https://maven.apache.org/
[maven-download]: https://maven.apache.org/download.cgi
[users-list]: https://maven.apache.org/mailing-lists.html
[dev-ml-list]: https://www.mail-archive.com/dev@maven.apache.org/
[code-style]: http://maven.apache.org/developers/conventions/code.html
[core-it]: https://maven.apache.org/core-its/core-it-suite/
[building-maven]: https://maven.apache.org/guides/development/guide-building-maven.html
[cla]: https://www.apache.org/licenses/#clas

