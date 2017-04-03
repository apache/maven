<!---
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
Integration Test Environments
=============================

This directory contains [Vagrant](https://www.vagrantup.com/) definitions for the more exotic test environments.

The test environments all require [Virtualbox](https://www.virtualbox.org/wiki/Downloads) as the vargrant provider.

NOTE: Where there are additional downloads required to populate the base box image, there will be a `Makefile` in the environment directory.
In these cases you will need to run `make build` before `vagrant up` will work.

For most unixes the test procedure will be something like:

    $ vagrant ssh
    $ git clone https://git-wip-us.apache.org/repos/asf/maven.git
    $ cd maven
    $ mvn clean verify -D
    $ cd ..
    $ git clone https://git-wip-us.apache.org/repos/asf/maven-integration-testing.git
    $ cd maven-integration-testing
    $ mvn clean install -Prun-its -Dmaven.repo.local=$HOME/work/repo -DmavenDistro=$HOME/maven/apache-maven/target/apache-maven-...-bin.zip
