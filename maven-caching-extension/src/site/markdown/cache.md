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

Build cache is an extension targeted to simplify and make more efficient work with large repositories in Maven. That is
achieved by a combination of features:

* Incremental builds over the changed project graph part only
* Subtree support in multimodule projects (caches discovered from the larger project)
* Version normalization to support project version agnostic caches
* Project state restoration (partial) to avoid expensive tasks (code generation and similar)

Large projects usually pose scalability challenges and work with such projects require build tool which scales. Cache
extension addresses that with incremental build execution and ability to efficiently work on sub-parts of a larger
project without building and installing dependencies from the larger project. Though, features implemented in maven
should give noticeable benefits in medium and small sized projects as well.

### Cache concepts

Idea of Incremental Maven is to calculate key from module inputs, store outputs in cache and restore them later
transparently to the standard Maven core. In order to calculate the key cache engine analyzes source code, build flow,
plugins and their parameters. This allows to deterministically associate each project state with unique key and restore
up-to-date (not changed) projects from cache and rebuild out-of-date(changed) ones. Restoring artifacts associated with
a particular project state improves build times by avoiding re-building unnecessary modules. Cache does not make any
interventions to actual build execution process and fully delegates build work to Maven core. This ensures that
artifacts produced in presence of cache are equivalent to result produced by a standard Maven build.   
To achieve accurate key calculation incremental Maven combines automatic introspection
of [project object model](https://maven.apache.org/pom.html#What_is_the_POM) and allows fine-grained tuning by means of
configuration file and xml attributes. Source code content fingerprinting is digests based which is more reliable over
widely used file timestamps in tools like Make or Apache Ant. Deterministic build state allows reliably cache outputs
even of the build in progress and share them between teams using remote cache. Deterministic inputs calculation allows
distributed and parallel builds running in heterogeneous environments (like cloud of build agents)
could efficiently reuse cached build artifacts. Therefore, incremental Maven is particularly well-suited for large Maven
projects that have significant number of small modules. Remote cache in conjunction with relocatable inputs
identification effectively enables "change once - build once" approach across all environments.

### Maven insights

The challenge of implementing build cache in Maven is that domain model is overly generic and doesn't have dedicated api
for build inputs. Because of that, even 2 identically looking builds from the same source code could normally produce 2
different results. The question here is tolerance level - can you accept particular discrepancies or not. For most of
teams artifacts produced in the same build environment from the same source code will be considered equivalent and
technical differences between them (like different timestamps in jar manifests) could be ignored. Now consider scenario
when artifact is first produced with compiler X and cached. Later, without touching source code, compiler changes to Y
and build yields significantly different outcomes of compilation. Should the produced artifacts be considered as
equivalent? Both Yes and No answers are possible and could be even desirable in different scenarios. When productivity
and performance are the primary concerns it could be desirable to tolerate insignificant discrepancies and maximise
reuse of cached builds. As long as correctness is in focus there could be demand to comply with the exact release
process. In the same way as with classic Maven, correctness is ensured by proper build configuration and controllable
build environments. In the same way as with classic Maven the previous build is just an approximation of today build
with some tolerance (implementation, configuration and environment driven).

### Implementation insights

At very simple form, the incremental Maven is essentially a hash function which takes Maven project and produces cache
key for a project. Then the key is used to store and restore build results. Because of different factors there could be
collisions and instabilities in the produced key. Collision could happen if the same key produced from the semantically
different build states and will result in unintended reuse. Instability means that same input yields different key in
different runs resulting in cache misses. The ultimate target is to find tradeoff between correctness and performance by
means of configuration. In current implementation this is achieved by configuring cache processing rules in xml file.

In order to achieve better correctness need to:

* Verify that every relevant file is selected as input to engine
* Add critical plugin parameters to reconciliation (because they could be overridden from command line)

In order to achieve better reuse need to:

* ensure that non-critical files (test logs, readme and similar) are filtered out from build inputs.
* non-critical plugin parameters (like number of threads in build)  are filtered out from build inputs
* Source code is relocatable and build parameters are relocatable (not environment specific)

Essentially cache setup is a process of inspecting build, taking these decision and reflect them in the cache
configuration.

Please notice though idea of perfectly matching builds might be tempting, but it is not practical with regard to
caching. Perfect correctness could lead to prevailing hit misses and render caching useless when applied to real
projects. In practice, configuring sufficient(good enough) correctness might yield the best outcomes. Incremental Maven
provides flexible and transparent control over caching policy and allows achieving desired outcomes - maximize usability
or maximize equivalence(correctness) between pre-cached candidates and requested builds.

## Usage

Cache extension is an opt-in feature. It is delivered as is and though the tool went through careful verification it's
still build owner's responsibility to verify build outcomes.

### Recommended Scenarios

Given all the information above, the Incremental Maven is recommended to use in scenarios when productivity and
performance are in priority. Typical cases are:

* Continuous integration. In conjunction with remote cache incremental Maven could drastically reduce build times,
  validate pull requests faster and reduce load on CI nodes
* Speedup developer builds. By reusing cached builds developers could verify changes much faster and be more productive.
  No more `-DskipTests` and similar.
* Assemble artifacts faster. In some development models it might be critical to have as fast build/deploy cycle as
  possible. Caching helps to cut down time drastically in such scenarios because it doesn't require to build cached
  dependencies.

For cases there correctness must be ensured (eg prod builds), it is recommended to disable cache and do clean builds.
Such scheme allows to validate cache correctness by reconciling outcomes of cached builds against the reference builds.

## See also

* [Getting started](getting-started.md) - getting starting with cache and usage manual
* [Usage](usage.md) - shared cache setup procedure
* [Remote cache setup](remote-cache.md) - shared cache setup procedure
* [How-To](how-to.md) - cookbook for typical scenarios
* [Performance](performance.md) - performance tuning
* [Cache Parameters](parameters.md) - description of supported parameters
* [Sample config file](maven-cache-config.xml)


* Maximilian Novikov - Project lead. Idea, design, coordination and verification.
* Alexander Ashitkin - Co-design and implementation of the majority of functionality
* Alexander Novoselov - Hashing module implementation