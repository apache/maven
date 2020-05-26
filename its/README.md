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
export MAVENCODEBASE=<path-to-maven-codebase>
```

You can choose to build the maven project from here with:
```
mvn verify -P local-it -f "$MAVENCODEBASE"
```

Now run (don't forget to update the versions!)
```
mvn clean install -Prun-its,embdedded -Dmaven.repo.local=`pwd`/repo  -DmavenDistro="$MAVENCODEBASE\apache-maven\target\apache-maven-<VERSION>-bin.zip" -DwrapperDistroDir="$MAVENCODEBASE\apache-maven\target" -DmavenWrapper="$MAVENCODEBASE\maven-wrapper\target\maven-wrapper-<VERSION>.jar"
```

or if behind a proxy

```
mvn clean install -Prun-its -Dmaven.repo.local=`pwd`/repo -DmavenDistro=/path/to/apache-maven-dist.zip -Dproxy.active=true -Dproxy.type=http -Dproxy.host=... -Dproxy.port=... -Dproxy.user=... -Dproxy.pass=...
```

Maven Developers List: dev@maven.apache.org
