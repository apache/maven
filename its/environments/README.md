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

 # Integration Test Environments

This directory contains definitions for different test environments.

## Linux based test environments

The linux based test environments use [Docker](https://www.docker.com/) and will have a `Dockerfile`.

The test procedure will typically be something like this:

```
$ ID=$(docker build -q .) && docker run --rm -t -i $ID bash
$ cd $HOME
$ git clone https://gitbox.apache.org/repos/asf/maven.git
$ ( cd maven && mvn clean verify )
$ git clone https://gitbox.apache.org/repos/asf/maven-integration-testing.git
$ ( cd maven-integration-testing && mvn clean install -Prun-its -Dmaven.repo.local=$HOME/work/repo -DmavenDistro=$HOME/maven/apache-maven/target/apache-maven-...-bin.zip )
```

## Other operating systems

The non-linux based test environments use [Vagrant](https://www.vagrantup.com/) and will have a `Vagrantfile`.

The Vagrant based test environments all require [Virtualbox](https://www.virtualbox.org/wiki/Downloads) as the vagrant provider.

NOTE: Where there are additional downloads required to populate the base box image, there will be a `Makefile` in the environment directory. In these cases you will need to run `make` before `vagrant up` will work.

For most unixes the test procedure will be something like:

```
$ vagrant ssh
$ git clone https://gitbox.apache.org/repos/asf/maven.git
$ ( cd maven && mvn clean verify )
$ git clone https://gitbox.apache.org/repos/asf/maven-integration-testing.git
$ ( cd maven-integration-testing && mvn clean install -Prun-its -Dmaven.repo.local=$HOME/work/repo -DmavenDistro=$HOME/maven/apache-maven/target/apache-maven-...-bin.zip )
```
