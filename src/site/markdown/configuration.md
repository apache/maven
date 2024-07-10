
# Configuration Options
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





| No | Key | Type | Description | Default Value | Since | Source |
| --- | --- | --- | --- | --- | --- | --- |
| 1. | `maven.build.timestamp.format` | `String` | Build timestamp format. |  `yyyy-MM-dd'T'HH:mm:ssXXX`  | 3.0.0 | Model properties |
| 2. | `maven.ext.class.path` | `String` | Extensions class path. |  -  |  | User properties |
| 3. | `maven.home` | `String` | Maven home. |  -  | 3.0.0 | User properties |
| 4. | `maven.install.conf` | `String` | Maven install configuration directory. |  `${maven.home}/conf`  | 4.0.0 | User properties |
| 5. | `maven.install.extensions` | `String` | Maven install extensions. |  `${maven.install.conf}/extensions.xml`  | 4.0.0 | User properties |
| 6. | `maven.install.settings` | `String` | Maven install settings. |  `${maven.install.conf}/settings.xml`  | 4.0.0 | User properties |
| 7. | `maven.install.toolchains` | `String` | Maven install toolchains. |  `${maven.install.conf}/toolchains.xml`  | 4.0.0 | User properties |
| 8. | `maven.plugin.validation` | `String` | Plugin validation level. |  `inline`  | 3.9.2 | User properties |
| 9. | `maven.plugin.validation.excludes` | `String` | Plugin validation exclusions. |  -  | 3.9.6 | User properties |
| 10. | `maven.project.conf` | `String` | Maven project configuration directory. |  `${session.rootDirectory}/.mvn`  | 4.0.0 | User properties |
| 11. | `maven.project.extensions` | `String` | Maven project extensions. |  `${maven.project.conf}/extensions.xml`  | 4.0.0 | User properties |
| 12. | `maven.project.settings` | `String` | Maven project settings. |  `${maven.project.conf}/settings.xml`  | 4.0.0 | User properties |
| 13. | `maven.projectBuilder.parallelism` | `Integer` | ProjectBuilder parallelism. |  `cores/2 + 1`  | 4.0.0 | User properties |
| 14. | `maven.relocations.entries` | `String` | User controlled relocations. This property is a comma separated list of entries with the syntax <code>GAV&gt;GAV</code>. The first <code>GAV</code> can contain <code>\*</code> for any elem (so <code>\*:\*:\*</code> would mean ALL, something you don't want). The second <code>GAV</code> is either fully specified, or also can contain <code>\*</code>, then it behaves as "ordinary relocation": the coordinate is preserved from relocated artifact. Finally, if right hand <code>GAV</code> is absent (line looks like <code>GAV&gt;</code>), the left hand matching <code>GAV</code> is banned fully (from resolving). <p> Note: the <code>&gt;</code> means project level, while <code>&gt;&gt;</code> means global (whole session level, so even plugins will get relocated artifacts) relocation. </p> <p> For example, <pre>maven.relocations.entries = org.foo:\*:\*>, \\<br/>    org.here:\*:\*>org.there:\*:\*, \\<br/>    javax.inject:javax.inject:1>>jakarta.inject:jakarta.inject:1.0.5</pre> means: 3 entries, ban <code>org.foo group</code> (exactly, so <code>org.foo.bar</code> is allowed), relocate <code>org.here</code> to <code>org.there</code> and finally globally relocate (see <code>&gt;&gt;</code> above) <code>javax.inject:javax.inject:1</code> to <code>jakarta.inject:jakarta.inject:1.0.5</code>. </p> |  -  | 4.0.0 | User properties |
| 15. | `maven.repo.local` | `String` | Maven local repository. |  `${maven.user.conf}/repository`  | 3.0.0 | User properties |
| 16. | `maven.repo.local.recordReverseTree` | `String` | User property for reverse dependency tree. If enabled, Maven will record ".tracking" directory into local repository with "reverse dependency tree", essentially explaining WHY given artifact is present in local repository. Default: <code>false</code>, will not record anything. |  `false`  | 3.9.0 | User properties |
| 17. | `maven.repo.local.tail` | `String` | User property for chained LRM: list of "tail" local repository paths (separated by comma), to be used with {@code org.eclipse.aether.util.repository.ChainedLocalRepositoryManager} . Default value: <code>null</code>, no chained LRM is used. |  -  | 3.9.0 | User properties |
| 18. | `maven.resolver.dependencyManagerTransitivity` | `String` | User property for selecting dependency manager behaviour regarding transitive dependencies and dependency management entries in their POMs. Maven 3 targeted full backward compatibility with Maven2, hence it ignored dependency management entries in transitive dependency POMs. Maven 4 enables "transitivity" by default, hence unlike Maven2, obeys dependency management entries deep in dependency graph as well. <p> Default: <code>"true"</code>. </p> |  `true`  | 4.0.0 | User properties |
| 19. | `maven.resolver.transport` | `String` | Resolver transport to use. Can be <code>default</code>, <code>wagon</code>, <code>apache</code>, <code>jdk</code> or <code>auto</code>. |  `default`  | 4.0.0 | User properties |
| 20. | `maven.style.color` | `String` | Maven output color mode. Allowed values are <code>auto</code>, <code>always</code>, <code>never</code>. |  `auto`  | 4.0.0 | User properties |
| 21. | `maven.user.conf` | `String` | Maven user configuration directory. |  `${user.home}/.m2`  | 4.0.0 | User properties |
| 22. | `maven.user.extensions` | `String` | Maven user extensions. |  `${maven.user.conf}/extensions.xml`  | 4.0.0 | User properties |
| 23. | `maven.user.settings` | `String` | Maven user settings. |  `${maven.user.conf}/settings.xml`  | 4.0.0 | User properties |
| 24. | `maven.user.toolchains` | `String` | Maven user toolchains. |  `${maven.user.home}/toolchains.xml`  | 4.0.0 | User properties |
| 25. | `maven.versionFilters` | `String` | User property for version filters expression, a semicolon separated list of filters to apply. By default, no version filter is applied (like in Maven 3). <p> Supported filters: <ul> <li>"h" or "h(num)" - highest version or top list of highest ones filter</li> <li>"l" or "l(num)" - lowest version or bottom list of lowest ones filter</li> <li>"s" - contextual snapshot filter</li> <li>"e(G:A:V)" - predicate filter (leaves out G:A:V from range, if hit, V can be range)</li> </ul> Example filter expression: <code>"h(5);s;e(org.foo:bar:1)</code> will cause: ranges are filtered for "top 5" (instead full range), snapshots are banned if root project is not a snapshot, and if range for <code>org.foo:bar</code> is being processed, version 1 is omitted. </p> |  -  | 4.0.0 | User properties |

