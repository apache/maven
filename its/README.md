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
Maven Core Integration Tests
========

<https://maven.apache.org/core-its/>

If you want to run the integration tests against a custom build of Maven use the following command:

```
mvn clean install -Prun-its -Dmaven.repo.local=`pwd`/repo -DmavenDistro=/path/to/apache-maven-dist.zip
```

or if behind a proxy

```
mvn clean install -Prun-its -Dmaven.repo.local=`pwd`/repo -DmavenDistro=/path/to/apache-maven-dist.zip -Dproxy.active=true -Dproxy.type=http -Dproxy.host=... -Dproxy.port=... -Dproxy.user=... -Dproxy.pass=...
```

Using the script 

Build Maven core with the profile `-PversionlessMavenDist`

Now Run the script: `sh ./run-its.sh` 

Maven Developers List: dev@maven.apache.org
