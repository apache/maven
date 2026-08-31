#!/bin/sh
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

mvn install:install-file -Dfile=master-1.pom -DpomFile=master-1.pom
mvn install:install-file -Dfile=master-x.pom -DpomFile=master-x.pom
mvn install:install-file -Dfile=ejb-1.jar -DpomFile=ejb-1.pom
mvn install:install-file -Dfile=ejb-1-client.jar -Dclassifier=client -DpomFile=ejb-1.pom
mvn install:install-file -Dfile=delegate-1.jar -DpomFile=delegate-1.pom
