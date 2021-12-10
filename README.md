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
Apache Maven Build Cache Extension
==================================

This project provides a Build Cache Extension feature which calculates out-of-date modules in the build dependencies graph and improves build times by avoiding re-building unnecessary modules.
Read [cache guide](src/site/markdown/cache.md) for more details.

Building
--------
The code currently relies on un-released modifications in the core Maven project.  Two submodules are included in this git repository to allow building the needed distributions and perform integration tests using those.

In order to build those distributions, you first need to launch once the `build-maven.sh` script or the following command in the `maven/maven3` and `maven/maven4` directories:
```
mvn install -DskipTests -P versionlessMavenDist
```
This will build the custom distributions of maven.

License
-------
This code is under the [Apache License, Version 2.0, January 2004][license].

See the [`NOTICE`](./NOTICE) file for required notices and attributions.

[license]: https://www.apache.org/licenses/LICENSE-2.0
