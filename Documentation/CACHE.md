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

## Overview

Idea of Incremental Maven is to specify module inputs and outputs and make them known to standard Maven core. This
allows accurate analysis and determination of out-of-date build artifacts in the build dependencies graph. Making the
dependency graph analysis deterministic leads to improvements in build times by avoiding re-building unnecessary
modules.  
Cache does not make any low-level interventions to build process and delegates actual build work to Maven core. This
guarantees that build results are identical to results produced by standard Maven and are fully reproducible.   
To achieve accurate input and outputs calculation incremental Maven combines automatic introspection
of [project object model](https://maven.apache.org/pom.html#What_is_the_POM) in conjunction with configuration-driven
rules for fine-grained content and execution control. For content analysis it digests based approach which is more
reliable over widely used file timestamps in tools like Make or Apache Ant. Deterministic build state allows reliably
cache even intermediate outputs of build and share them between teams using remote cache. Deterministic inputs
calculation allows distributed and parallel builds running in heterogeneous environments (like cloud of build agents)
could efficiently reuse cached build artifacts. Therefore incremental Maven is particularly well-suited for large Maven
projects that have significant number of small modules. Remote cache in conjunction with precise input identification
effectively enables "change once - build once" approach.

### Maven insights

The challenge of implementing build cache in Maven is that domain model is overly generic and doesn't support well
reproducible builds. You might have never thought of that, but it is a reality that 2 different Maven builds from the
same source code normally produce 2 different results. The question here is tolerance level - can you accept particular
discrepancies or not. For most of teams artifacts produced in the same build environment from the same source code will
be considered equivalent and technical differences between them (like different timestamps in jar manifests) could be
ignored. Now consider scenario when artifact is first produced with compiler X and cached but later without touching a
update compiler changes to Y and yields significantly different outcomes of compilation. Ask yourself a question \- am I
consider artifacts of such builds equivalent? Both Yes and No outcomes are pretty possible and could be even desirable
in different scenarios. When productivity and performance are the primary concerns it could be desirable to tolerate
insignificant discrepancies and maximise reuse of cached builds. As long as correctness in focus there could be demand
to comply with the exact release process. In the same way as with classic Maven, decision stays with you - what is
acceptable difference between builds. In the same way as with classic Maven the previous build is just an approximation
of today build with some tolerance (implementation, configuration and environment driven).

### Implementation insights

At very simple form, the incremental Maven is essentially a hash function which takes Maven project and produces hash
code (checksum). Then hash value is used to fetch and restore build result.  
As with any hash, there could be collisions and instabilities. Collision could happen if the same hash produced from the
different build states and will result in unintended reuse. Instability means that same input yields different hash sums
in different runs - resulting in cache miss. The ultimate target is to achieve desired balance between collisions (
sufficient correctness) and stability (sufficient reuse). In current implementation this is achieved by configuring
project specific processing rules in static configuration file. To avoid unintentional collisions and achieve better
correctness need to ensure that every critical file and plugin parameter accounted in build inputs. In order to achieve
better reuse need to ensure that non-critical files (test logs, readme and similar) and non-critical plugin parameters (
like number of threads in build) are filtered out from build inputs. Essentially cache configuration is a process of
inspecting build, taking these decision and reflect them in the cache configuration.

Please notice though idea of perfectly matching builds might be tempting, but it is not practical with regard to
caching. Perfect correctness means that not a single build could be reused and renders whole idea of builds caching
useless. Whatever build tool you use, there will be always a tradeoff which might be acceptable or not in particular
situation. Incremental Maven provides flexible and transparent control over caching policy and allows achieving desired
outcomes - maximize reusability or maximize equivalence between pre-cached candidates and requested builds.

## Usage

Cornerstone principle of using this tool is that it is delivered as is. Though the tool went through thorough
verification it's still consumer's responsibility to verify final product quality.

### Recommended Scenarios

Given all the information above, the Incremental Maven is recommended to use in scenarios when productivity and
performance are in priority. Typical cases are:

* Speedup CI. In conjunction with remote cache incremental Maven could drastically reduce build times, validate pull
  requests faster and reduce load on CI nodes
* Speedup developer builds. By reusing cached builds developers could verify changes much faster and be more productive.
  No more `-DskipTests` and similar.
* Assemble artifacts faster. In some development models it might be critical to have as fast build/deploy cycle as
  possible. Caching helps to cut down time drastically in such scenarios because it doesn't require to build cached
  dependencies.

For cases there correctness must be ensured (eg prod builds), it is recommended to disable cache and do clean builds.
This also allows you to validate cache correctness and reconcile cache outcomes on CI process.

## Getting Started

To on-board incremental Maven you need to complete several steps:

* Get incremental Maven distribution
* Add cache config in `.mvn`
* Validate build results and iteratively adjust config to project specifics
* Migrate CI to incremental Maven with remote cache (to get full benefit) - optional

### Get incremental Maven distribution

The recommended way is to add [Takari Maven Wrapper](https://github.com/takari/maven-wrapper) to your project. In that
case `maven-wrapper.properties` should reference the latest incremental Maven distribution:

```properties
distributionUrl=https://your-server/maven-incremental.zip
wrapperUrl=https://your-server/maven-wrapper-0.5.5.jar
```

Benefits of using Maven wrapper are following:

* simple distribution across workstations and CI envs
* Maven stays compatible to your branch
* further upgrades are simplified significantly  
  If you refuse wrapper - then download, unzip and install it just as usual Maven. Further it will be assumed you use
  Maven wrapper (`mvnw`)

### Adding cache config

Copy [default config `maven-cache-config.xml`](maven-cache-config.xml)
to [`.mvn/`](https://maven.apache.org/configure.html) dir of your project.  
To get overall understanding of cache machinery it is recommended to review the config and read comments. In typical
scenario you need to adjust:

* remote cache location
* source code files glob
* plugins reconciliation rules - add critical plugin parameters to reconciliation
* add non-standard source code locations (most of locations discovered automatically from project and plugins config,
  but still there might be edge cases)

See also:

* [Remote cache setup](CACHE-REMOTE.md) - instruction how to setup shared cache
* [Cache How-To](CACHE-HOWTO.md) - cookbook for frequently encountered questions
* [Cache Parameters](CACHE-PARAMETERS.md) - description of supported parameters
* Attached [sample `maven-cache-config.xml` config file](maven-cache-config.xml) and elements annotations in xsd schema. (Ctrl+Q in idea should
  show annotations in popup)

### Adjusting cache config

Having incremental Maven and the config in place you're all set. To run first cacheable build just
execute: `mvnw clean install`

* Ensure that the config is picked up and incremental Maven is picked up. Just check log output - you will notice cache
  related output or initialization error message.
* Navigate to your local repo directory - there should be sibling next to your local repo named `cache`
* Find `buildinfo.xml` for typical module and review it. Ensure that
    * expected source code files are present in build info
    * all critical plugins and their critical parameters are covered by config

Notice - in configuration you should find best working trade-off between fairness and cache efficiency. Adding
unnecessary rules and checks could reduce both performance and cache efficiency (hit rate).

### Adding caching CI and remote cache

To leverage remote cache feature you need web server which supports get/put operations
like [Nginx OSS](http://nginx.org/en/) (with fs module) or binary repo in Artifactory. It is recommended to populate
remote cache from CI build. Benefits are:

* such scheme provides predictable and consistent artifacts in remote cache
* usually CI builds project fast enough to populate cache for team members See [Remote cache setup](CACHE-REMOTE.md) for
  detailed description of cache setup

## Credits
CacheConfigImpl
* Maximilian Novikov - Project lead. Idea, design, coordination and verification.
* Alexander Ashitkin - Co-design and implementation of the majority of functionality
* Alexander Novoselov - Hashing module implementation