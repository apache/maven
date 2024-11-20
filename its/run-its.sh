#!/bin/sh

#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# How I run the ITs from a clean slate. Though I do this with a primed Nexus instance. JvZ.
# build maven core using -PversionlessMavenDist

# In rare occasions, some ITs may depend on latest maven snapshots.
# In such cases you need to:
#  - build maven using `mvn install -PversionlessMavenDist -Dmaven.repo.local=[my-repo-local]`
#  - run ITs using `mvn clean install -Prun-its,embedded -Dmaven.repo.local=[my-repo-local] -DmavenDistro=[maven-source-tree]/apache-maven/target/maven-bin.zip`

mvn clean install -Prun-its,embedded -Dmaven.repo.local=`pwd`/repo

# If behind a proxy try this

# mvn clean install -Prun-its,embedded -Dmaven.repo.local=`pwd`/repo -Dproxy.host=<host> -Dproxy.port=<port> -Dproxy.user= -Dproxy.pass= -Dproxy.nonProxyHosts=<hosts>
