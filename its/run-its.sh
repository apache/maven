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

if [ -z "$MAVENCODEBASE" ] ; then
 echo Please export MAVENCODEBASE
else
 mvn verify -P versionlessMavenDist -f "$MAVENCODEBASE"
 mvn clean test -Prun-its,embdedded -Dmaven.repo.local=`pwd`/repo  -DmavenDistro="$MAVENCODEBASE/apache-maven/target/apache-maven-bin.zip" -DwrapperDistroDir="$MAVENCODEBASE/apache-maven/target" -DmavenWrapper="$MAVENCODEBASE/maven-wrapper/target/maven-wrapper.jar"
fi


# If behind a proxy try this

# mvn clean install -Prun-its,embedded -Dmaven.repo.local=`pwd`/repo -Dproxy.host=<host> -Dproxy.port=<port> -Dproxy.user= -Dproxy.pass= -Dproxy.nonProxyHosts=<hosts>
