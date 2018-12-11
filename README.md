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
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven/apache-maven.svg?label=Maven%20Central)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.apache.maven%22%20AND%20a%3A%22apache-maven%22)
[![Jenkins Status](https://img.shields.io/jenkins/s/https/builds.apache.org/job/maven-box/job/maven/job/master.svg?style=flat-square)][build]
[![Jenkins tests](https://img.shields.io/jenkins/t/https/builds.apache.org/job/maven-box/job/maven/job/master.svg?style=flat-square)][test-results]


Apache Maven is a software project management and comprehension tool. Based on
the concept of a project object model (POM), Maven can manage a project's
build, reporting and documentation from a central piece of information.

If you think you have found a bug, please file an issue in the [Maven Issue Tracker](https://issues.apache.org/jira/browse/MNG).

Documentation
-------------

More information can be found on [Apache Maven Homepage][maven-home].
Questions related to the usage of Maven should be posted on
the [Maven User List][users-list].


Where can I get the latest release?
-----------------------------------
You can download release source from our [download page][maven-download].

Contributing
------------

If you are interested in the development of Maven, please consult the 
documentation first and afterwards you are welcome to join the developers 
mailing list to ask question or discuss new ideas / features / bugs etc.

Take a look into the [contribution guidelines](CONTRIBUTING.md).

License
-------
This code is under the [Apache Licence v2][license]

See the `NOTICE` file for required notices and attributions.

Donations
---------
You like Apache Maven? Then [donate back to the ASF](https://www.apache.org/foundation/contributing.html) to support the development.

License
-------
[Apache License, Version 2.0, January 2004][license]

Quick Build
-------
If you want to bootstrap Maven, you'll need:
- Java 1.7+
- Maven 3.0.5 or later
- Run Maven, specifying a location into which the completed Maven distro should be installed:
```
mvn -DdistributionTargetDir="$HOME/app/maven/apache-maven-3.6.x-SNAPSHOT" clean package
```


[home]: https://maven.apache.org/
[license]: https://www.apache.org/licenses/LICENSE-2.0
[build]: https://builds.apache.org/job/maven-box/job/maven/job/master/
[test-results]: https://builds.apache.org/job/maven-box/job/maven/job/master/lastCompletedBuild/testReport/
[build-status]: https://img.shields.io/jenkins/s/https/builds.apache.org/job/maven-box/job/maven/job/master.svg?style=flat-square
[build-tests]: https://img.shields.io/jenkins/t/https/builds.apache.org/job/maven-box/job/maven/job/master.svg?style=flat-square
[maven-home]: https://maven.apache.org/
[maven-download]: https://maven.apache.org/download.cgi
[users-list]: https://maven.apache.org/mailing-lists.html
[dev-ml-list]: https://www.mail-archive.com/dev@maven.apache.org/
[code-style]: http://maven.apache.org/developers/conventions/code.html
[core-it]: https://maven.apache.org/core-its/core-it-suite/
[building-maven]: https://maven.apache.org/guides/development/guide-building-maven.html
[cla]: https://www.apache.org/licenses/#clas

