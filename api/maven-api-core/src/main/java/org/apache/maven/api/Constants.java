/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.api;

import org.apache.maven.api.annotations.Config;

/**
 * Configuration constants.
 */
public final class Constants {

    /**
     * Maven home.
     */
    @Config(readOnly = true)
    public static final String MAVEN_HOME = "maven.home";

    /**
     * Maven configuration.
     */
    @Config(defaultValue = "${maven.home}/conf")
    public static final String MAVEN_CONF = "maven.conf";

    /**
     * Maven user home.
     */
    @Config(defaultValue = "${user.home}/.m2")
    public static final String MAVEN_USER_HOME = "maven.user.home";

    /**
     * Maven local repository.
     */
    @Config(defaultValue = "${maven.user.home}/repository")
    public static final String MAVEN_REPO_LOCAL = "maven.repo.local";

    /**
     * Maven project-wide extensions.
     */
    @Config(defaultValue = "${session.rootDirectory}/.mvn/extensions.xml")
    public static final String MAVEN_PROJECT_EXTENSIONS = "maven.project.extensions";

    /**
     * Maven user extensions.
     */
    @Config(defaultValue = "${maven.user.home}/extensions.xml")
    public static final String MAVEN_USER_EXTENSIONS = "maven.user.extensions";

    /**
     * Maven global toolchains.
     */
    @Config(defaultValue = "${maven.conf}/toolchains.xml")
    public static final String MAVEN_GLOBAL_TOOLCHAINS = "maven.global.toolchains";

    /**
     * Maven user toolchains.
     */
    @Config(defaultValue = "${maven.user.home}/toolchains.xml")
    public static final String MAVEN_USER_TOOLCHAINS = "maven.user.toolchains";

    /**
     * Extensions class path.
     */
    @Config
    public static final String MAVEN_EXT_CLASS_PATH = "maven.ext.class.path";

    /**
     * Maven output color mode.
     * Allowed values are <code>auto</code>, <code>always</code>, <code>never</code>.
     */
    @Config(defaultValue = "auto")
    public static final String MAVEN_STYLE_COLOR_PROPERTY = "maven.style.color";

    /**
     * Build timestamp format.
     */
    @Config(source = Config.Source.MODEL, defaultValue = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    public static final String MAVEN_BUILD_TIMESTAMP_FORMAT = "maven.build.timestamp.format";

    /**
     * User controlled relocations.
     * This property is a comma separated list of entries with the syntax <code>GAV&gt;GAV</code>.
     * The first <code>GAV</code> can contain <code>*</code> for any elem (so <code>*:*:*</code> would mean ALL, something
     * you don't want). The second <code>GAV</code> is either fully specified, or also can contain <code>*</code>,
     * then it behaves as "ordinary relocation": the coordinate is preserved from relocated artifact.
     * Finally, if right hand <code>GAV</code> is absent (line looks like <code>GAV&gt;</code>), the left hand matching
     * <code>GAV</code> is banned fully (from resolving).
     * <p>
     * Note: the <code>&gt;</code> means project level, while <code>&gt;&gt;</code> means global (whole session level,
     * so even plugins will get relocated artifacts) relocation.
     * </p>
     * <p>
     * For example,
     * <pre>maven.relocations.entries = org.foo:*:*>, \\<br/>    org.here:*:*>org.there:*:*, \\<br/>    javax.inject:javax.inject:1>>jakarta.inject:jakarta.inject:1.0.5</pre>
     * means: 3 entries, ban <code>org.foo group</code> (exactly, so <code>org.foo.bar</code> is allowed),
     * relocate <code>org.here</code> to <code>org.there</code> and finally globally relocate (see <code>&gt;&gt;</code> above)
     * <code>javax.inject:javax.inject:1</code> to <code>jakarta.inject:jakarta.inject:1.0.5</code>.
     * </p>
     */
    @Config
    public static final String MAVEN_RELOCATIONS_ENTRIES = "maven.relocations.entries";

    /**
     * User property for version filters expression, a semicolon separated list of filters to apply. By default, no version
     * filter is applied (like in Maven 3).
     * <p>
     * Supported filters:
     * <ul>
     *     <li>"h" or "h(num)" - highest version or top list of highest ones filter</li>
     *     <li>"l" or "l(num)" - lowest version or bottom list of lowest ones filter</li>
     *     <li>"s" - contextual snapshot filter</li>
     *     <li>"e(G:A:V)" - predicate filter (leaves out G:A:V from range, if hit, V can be range)</li>
     * </ul>
     * Example filter expression: <code>"h(5);s;e(org.foo:bar:1)</code> will cause: ranges are filtered for "top 5" (instead
     * full range), snapshots are banned if root project is not a snapshot, and if range for <code>org.foo:bar</code> is
     * being processed, version 1 is omitted.
     * </p>
     *
     * @since 4.0.0
     */
    @Config
    public static final String MAVEN_VERSION_FILTERS = "maven.version.filters";

    /**
     * User property for chained LRM: list of "tail" local repository paths (separated by comma), to be used with
     * {@code org.eclipse.aether.util.repository.ChainedLocalRepositoryManager}.
     * Default value: <code>null</code>, no chained LRM is used.
     *
     * @since 3.9.0
     */
    @Config
    public static final String MAVEN_REPO_LOCAL_TAIL = "maven.repo.local.tail";

    /**
     * User property for reverse dependency tree. If enabled, Maven will record ".tracking" directory into local
     * repository with "reverse dependency tree", essentially explaining WHY given artifact is present in local
     * repository.
     * Default: <code>false</code>, will not record anything.
     *
     * @since 3.9.0
     */
    @Config(defaultValue = "false")
    public static final String MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE = "maven.repo.local.record.reverse.tree";

    /**
     * User property for selecting dependency manager behaviour regarding transitive dependencies and dependency
     * management entries in their POMs. Maven 3 targeted full backward compatibility with Maven2, hence it ignored
     * dependency management entries in transitive dependency POMs. Maven 4 enables "transitivity" by default, hence
     * unlike Maven2, obeys dependency management entries deep in dependency graph as well.
     * <p>
     * Default: <code>"true"</code>.
     * </p>
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "true")
    public static final String MAVEN_RESOLVER_DEPENDENCY_MANAGER_TRANSITIVITY =
            "maven.resolver.dependency.manager.transitivity";

    /**
     * Resolver transport to use.
     * Can be <code>default</code>, <code>wagon</code>, <code>apache</code>, <code>jdk</code> or <code>auto</code>.
     */
    @Config(defaultValue = "default")
    public static final String MAVEN_RESOLVER_TRANSPORT = "maven.resolver.transport";

    /**
     * Plugin validation level.
     */
    @Config(defaultValue = "inline")
    public static final String MAVEN_PLUGIN_VALIDATION = "maven.plugin.validation";

    /**
     * Plugin validation exclusions.
     */
    @Config
    public static final String MAVEN_PLUGIN_VALIDATION_EXCLUDES = "maven.plugin.validation.excludes";

    /**
     * ProjectBuilder parallelism.
     */
    @Config(type = "java.lang.Integer", defaultValue = "cores/2 + 1")
    public static final String MAVEN_PROJECT_BUILDER_PARALLELISM = "maven.project.builder.parallelism";

    private Constants() {}
}
