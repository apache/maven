 -----
 Introduction
 -----
 Hervé Boutemy
 -----
 2011-09-04
 -----

 ~~ Licensed to the Apache Software Foundation (ASF) under one
 ~~ or more contributor license agreements.  See the NOTICE file
 ~~ distributed with this work for additional information
 ~~ regarding copyright ownership.  The ASF licenses this file
 ~~ to you under the Apache License, Version 2.0 (the
 ~~ "License"); you may not use this file except in compliance
 ~~ with the License.  You may obtain a copy of the License at
 ~~
 ~~   http://www.apache.org/licenses/LICENSE-2.0
 ~~
 ~~ Unless required by applicable law or agreed to in writing,
 ~~ software distributed under the License is distributed on an
 ~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~~ KIND, either express or implied.  See the License for the
 ~~ specific language governing permissions and limitations
 ~~ under the License.

 ~~ NOTE: For help with the syntax of this file, see:
 ~~ http://maven.apache.org/doxia/references/apt-format.html


Maven Core ITs

 Maven Core Integration Tests provide tooling to test every aspect of Maven functionalities with any Maven version.

 This project is split in 2 modules:

 * {{{./core-it-support/}Maven Core IT Support}}: Maven Integration Tests support tools, to completely decouple ITs from production plugins,

 * {{{./core-it-suite/}Maven Core ITs suite}}: The effective Maven Integration Tests suite, providing the interesting {{{./core-it-suite/surefire-report.html}tests results}}.


* Running the Core ITs

  By default, the project just packages the tests in an artifact. To actually run them, activate the <<<run-its>>> profile:
  
+----
mvn clean test -Prun-its
+----
  
  This will subject the Maven version running the build to the integration tests.
  
  If you would like to test a different Maven distribution, you can use the <<<mavenHome>>> system property to specify the
  path of the Maven distribution to test:
  
+----
mvn clean test -Prun-its -DmavenHome=<maven-under-test>
+----
  
  Alternatively, you can just specify the version of a previously installed/deployed Maven distribution which will be
  downloaded, unpacked and tested:
  
+----
mvn clean test -Prun-its -DmavenVersion=2.2.1
+----
  
  To run the ITs using embedded Maven 3.x, additionally activate the <<<embedded>>> profile.
  
  ITs that don't require to fork Maven can also be run from the IDE using the Maven projects from the workspace if the
  Maven dependencies are added to the test class path.
  
  If you're behind a proxy, use the system properties <<<proxy.host>>>, <<<proxy.port>>>, <<<proxy.user>>>, <<<proxy.pass>>>
  and <<<proxy.nonProxyHosts>>> to specify the required proxy setup for the ITs. Alternatively, set the system property
  <<<maven.it.central>>> to a URL of a local repository manager (anonymous authentication only) that proxies the required
  artifacts.
 