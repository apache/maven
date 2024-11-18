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
     *
     * @since 3.0.0
     */
    @Config(readOnly = true)
    public static final String MAVEN_HOME = "maven.home";

    /**
     * Maven installation configuration directory.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.home}/conf")
    public static final String MAVEN_INSTALLATION_CONF = "maven.installation.conf";

    /**
     * Maven user configuration directory.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${user.home}/.m2")
    public static final String MAVEN_USER_CONF = "maven.user.conf";

    /**
     * Maven project configuration directory.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${session.rootDirectory}/.mvn")
    public static final String MAVEN_PROJECT_CONF = "maven.project.conf";

    /**
     * Maven local repository.
     *
     * @since 3.0.0
     */
    @Config(defaultValue = "${maven.user.conf}/repository")
    public static final String MAVEN_REPO_LOCAL = "maven.repo.local";

    /**
     * Maven central repository URL.
     * The property will have the value of the <code>MAVEN_REPO_CENTRAL</code>
     * environment variable if it is defined.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "https://repo.maven.apache.org/maven2")
    public static final String MAVEN_REPO_CENTRAL = "maven.repo.central";

    /**
     * Maven installation settings.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.installation.conf}/settings.xml")
    public static final String MAVEN_INSTALLATION_SETTINGS = "maven.installation.settings";

    /**
     * Maven user settings.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.user.conf}/settings.xml")
    public static final String MAVEN_USER_SETTINGS = "maven.user.settings";

    /**
     * Maven project settings.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.project.conf}/settings.xml")
    public static final String MAVEN_PROJECT_SETTINGS = "maven.project.settings";

    /**
     * Maven installation extensions.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.installation.conf}/extensions.xml")
    public static final String MAVEN_INSTALLATION_EXTENSIONS = "maven.installation.extensions";

    /**
     * Maven user extensions.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.user.conf}/extensions.xml")
    public static final String MAVEN_USER_EXTENSIONS = "maven.user.extensions";

    /**
     * Maven project extensions.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.project.conf}/extensions.xml")
    public static final String MAVEN_PROJECT_EXTENSIONS = "maven.project.extensions";

    /**
     * Maven installation toolchains.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.installation.conf}/toolchains.xml")
    public static final String MAVEN_INSTALLATION_TOOLCHAINS = "maven.installation.toolchains";

    /**
     * Maven user toolchains.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "${maven.user.home}/toolchains.xml")
    public static final String MAVEN_USER_TOOLCHAINS = "maven.user.toolchains";

    /**
     * Extensions class path.
     */
    @Config
    public static final String MAVEN_EXT_CLASS_PATH = "maven.ext.class.path";

    public static final String MAVEN_STYLE_PREFIX = "maven.style.";

    // Style Names
    public static final String MAVEN_STYLE_TRANSFER_NAME = "transfer";
    public static final String MAVEN_STYLE_TRACE_NAME = "trace";
    public static final String MAVEN_STYLE_DEBUG_NAME = "debug";
    public static final String MAVEN_STYLE_INFO_NAME = "info";
    public static final String MAVEN_STYLE_WARNING_NAME = "warning";
    public static final String MAVEN_STYLE_ERROR_NAME = "error";
    public static final String MAVEN_STYLE_SUCCESS_NAME = "success";
    public static final String MAVEN_STYLE_FAILURE_NAME = "failure";
    public static final String MAVEN_STYLE_STRONG_NAME = "strong";
    public static final String MAVEN_STYLE_MOJO_NAME = "mojo";
    public static final String MAVEN_STYLE_PROJECT_NAME = "project";

    // Default Values
    public static final String MAVEN_STYLE_TRANSFER_DEFAULT = "f:bright-black";
    public static final String MAVEN_STYLE_TRACE_DEFAULT = "bold,f:magenta";
    public static final String MAVEN_STYLE_DEBUG_DEFAULT = "bold,f:cyan";
    public static final String MAVEN_STYLE_INFO_DEFAULT = "bold,f:blue";
    public static final String MAVEN_STYLE_WARNING_DEFAULT = "bold,f:yellow";
    public static final String MAVEN_STYLE_ERROR_DEFAULT = "bold,f:red";
    public static final String MAVEN_STYLE_SUCCESS_DEFAULT = "bold,f:green";
    public static final String MAVEN_STYLE_FAILURE_DEFAULT = "bold,f:red";
    public static final String MAVEN_STYLE_STRONG_DEFAULT = "bold";
    public static final String MAVEN_STYLE_MOJO_DEFAULT = "f:green";
    public static final String MAVEN_STYLE_PROJECT_DEFAULT = "f:cyan";

    /**
     * Maven output color mode.
     * Allowed values are <code>auto</code>, <code>always</code>, <code>never</code>.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "auto")
    public static final String MAVEN_STYLE_COLOR_PROPERTY = MAVEN_STYLE_PREFIX + "color";

    /**
     * Color style for transfer messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_TRANSFER_DEFAULT)
    public static final String MAVEN_STYLE_TRANSFER = MAVEN_STYLE_PREFIX + MAVEN_STYLE_TRANSFER_NAME;

    /**
     * Color style for trace messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_TRACE_DEFAULT)
    public static final String MAVEN_STYLE_TRACE = MAVEN_STYLE_PREFIX + MAVEN_STYLE_TRACE_NAME;

    /**
     * Color style for debug messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_DEBUG_DEFAULT)
    public static final String MAVEN_STYLE_DEBUG = MAVEN_STYLE_PREFIX + MAVEN_STYLE_DEBUG_NAME;

    /**
     * Color style for info messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_INFO_DEFAULT)
    public static final String MAVEN_STYLE_INFO = MAVEN_STYLE_PREFIX + MAVEN_STYLE_INFO_NAME;

    /**
     * Color style for warning messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_WARNING_DEFAULT)
    public static final String MAVEN_STYLE_WARNING = MAVEN_STYLE_PREFIX + MAVEN_STYLE_WARNING_NAME;

    /**
     * Color style for error messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_ERROR_DEFAULT)
    public static final String MAVEN_STYLE_ERROR = MAVEN_STYLE_PREFIX + MAVEN_STYLE_ERROR_NAME;

    /**
     * Color style for success messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_SUCCESS_DEFAULT)
    public static final String MAVEN_STYLE_SUCCESS = MAVEN_STYLE_PREFIX + MAVEN_STYLE_SUCCESS_NAME;

    /**
     * Color style for failure messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_FAILURE_DEFAULT)
    public static final String MAVEN_STYLE_FAILURE = MAVEN_STYLE_PREFIX + MAVEN_STYLE_FAILURE_NAME;

    /**
     * Color style for strong messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_STRONG_DEFAULT)
    public static final String MAVEN_STYLE_STRONG = MAVEN_STYLE_PREFIX + MAVEN_STYLE_STRONG_NAME;

    /**
     * Color style for mojo messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_MOJO_DEFAULT)
    public static final String MAVEN_STYLE_MOJO = MAVEN_STYLE_PREFIX + MAVEN_STYLE_MOJO_NAME;

    /**
     * Color style for project messages.
     * @since 4.0.0
     */
    @Config(defaultValue = MAVEN_STYLE_PROJECT_DEFAULT)
    public static final String MAVEN_STYLE_PROJECT = MAVEN_STYLE_PREFIX + MAVEN_STYLE_PROJECT_NAME;

    /**
     * Build timestamp format.
     *
     * @since 3.0.0
     */
    @Config(source = Config.Source.MODEL, defaultValue = "yyyy-MM-dd'T'HH:mm:ssXXX")
    public static final String MAVEN_BUILD_TIMESTAMP_FORMAT = "maven.build.timestamp.format";

    /**
     * User controlled relocations.
     * This property is a comma separated list of entries with the syntax <code>GAV&gt;GAV</code>.
     * The first <code>GAV</code> can contain <code>*</code> for any elem (so <code>*:*:*</code> would mean ALL, something
     * you don't want). The second <code>GAV</code> is either fully specified, or also can contain <code>*</code>,
     * then it behaves as "ordinary relocation": the coordinate is preserved from relocated artifact.
     * Finally, if right hand <code>GAV</code> is absent (line looks like <code>GAV&gt;</code>), the left hand matching
     * <code>GAV</code> is banned fully (from resolving).
     * <br/>
     * Note: the <code>&gt;</code> means project level, while <code>&gt;&gt;</code> means global (whole session level,
     * so even plugins will get relocated artifacts) relocation.
     * <br/>
     * For example,
     * <pre>maven.relocations.entries = org.foo:*:*>, \\<br/>    org.here:*:*>org.there:*:*, \\<br/>    javax.inject:javax.inject:1>>jakarta.inject:jakarta.inject:1.0.5</pre>
     * means: 3 entries, ban <code>org.foo group</code> (exactly, so <code>org.foo.bar</code> is allowed),
     * relocate <code>org.here</code> to <code>org.there</code> and finally globally relocate (see <code>&gt;&gt;</code> above)
     * <code>javax.inject:javax.inject:1</code> to <code>jakarta.inject:jakarta.inject:1.0.5</code>.
     *
     * @since 4.0.0
     */
    @Config
    public static final String MAVEN_RELOCATIONS_ENTRIES = "maven.relocations.entries";

    /**
     * User property for version filter expression used in session, applied to resolving ranges: a semicolon separated
     * list of filters to apply. By default, no version filter is applied (like in Maven 3).
     * <br/>
     * Supported filters:
     * <ul>
     *     <li>"h" or "h(num)" - highest version or top list of highest ones filter</li>
     *     <li>"l" or "l(num)" - lowest version or bottom list of lowest ones filter</li>
     *     <li>"s" - contextual snapshot filter</li>
     *     <li>"e(G:A:V)" - predicate filter (leaves out G:A:V from range, if hit, V can be range)</li>
     * </ul>
     * Example filter expression: <code>"h(5);s;e(org.foo:bar:1)</code> will cause: ranges are filtered for "top 5" (instead
     * full range), snapshots are banned if root project is not a snapshot, and if range for <code>org.foo:bar</code> is
     * being processed, version 1 is omitted. Value in this property builds
     * <code>org.eclipse.aether.collection.VersionFilter</code> instance.
     *
     * @since 4.0.0
     */
    @Config
    public static final String MAVEN_VERSION_FILTER = "maven.session.versionFilter";

    /**
     * User property for chained LRM: the new "head" local repository to use, and "push" the existing into tail.
     * Similar to <code>maven.repo.local.tail</code>, this property may contain comma separated list of paths to be
     * used as local repositories (combine with chained local repository), but while latter is "appending" this
     * one is "prepending".
     *
     * @since 4.0.0
     */
    @Config
    public static final String MAVEN_REPO_LOCAL_HEAD = "maven.repo.local.head";

    /**
     * User property for chained LRM: list of "tail" local repository paths (separated by comma), to be used with
     * <code>org.eclipse.aether.util.repository.ChainedLocalRepositoryManager</code>.
     * Default value: <code>null</code>, no chained LRM is used.
     *
     * @since 3.9.0
     */
    @Config
    public static final String MAVEN_REPO_LOCAL_TAIL = "maven.repo.local.tail";

    /**
     * User property for chained LRM: whether to ignore "availability check" in tail or not. Usually you do want
     * to ignore it. This property is mapped onto corresponding Resolver 2.x property, is like a synonym for it.
     * Default value: <code>true</code>.
     *
     * @since 3.9.0
     * @see <a href="https://maven.apache.org/resolver/configuration.html">Resolver Configuration: aether.chainedLocalRepository.ignoreTailAvailability</a>
     */
    @Config
    public static final String MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY = "maven.repo.local.tail.ignoreAvailability";

    /**
     * User property for reverse dependency tree. If enabled, Maven will record ".tracking" directory into local
     * repository with "reverse dependency tree", essentially explaining WHY given artifact is present in local
     * repository.
     * Default: <code>false</code>, will not record anything.
     *
     * @since 3.9.0
     */
    @Config(defaultValue = "false")
    public static final String MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE = "maven.repo.local.recordReverseTree";

    /**
     * User property for selecting dependency manager behaviour regarding transitive dependencies and dependency
     * management entries in their POMs. Maven 3 targeted full backward compatibility with Maven2, hence it ignored
     * dependency management entries in transitive dependency POMs. Maven 4 enables "transitivity" by default, hence
     * unlike Maven2, obeys dependency management entries deep in dependency graph as well.
     * <br/>
     * Default: <code>"true"</code>.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "true")
    public static final String MAVEN_RESOLVER_DEPENDENCY_MANAGER_TRANSITIVITY =
            "maven.resolver.dependencyManagerTransitivity";

    /**
     * Resolver transport to use.
     * Can be <code>default</code>, <code>wagon</code>, <code>apache</code>, <code>jdk</code> or <code>auto</code>.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "default")
    public static final String MAVEN_RESOLVER_TRANSPORT = "maven.resolver.transport";

    /**
     * Plugin validation level.
     *
     * @since 3.9.2
     */
    @Config(defaultValue = "inline")
    public static final String MAVEN_PLUGIN_VALIDATION = "maven.plugin.validation";

    /**
     * Plugin validation exclusions.
     *
     * @since 3.9.6
     */
    @Config
    public static final String MAVEN_PLUGIN_VALIDATION_EXCLUDES = "maven.plugin.validation.excludes";

    /**
     * ProjectBuilder parallelism.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Integer", defaultValue = "cores/2 + 1")
    public static final String MAVEN_MODEL_BUILDER_PARALLELISM = "maven.modelBuilder.parallelism";

    /**
     * User property for enabling/disabling the consumer POM feature.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "true")
    public static final String MAVEN_CONSUMER_POM = "maven.consumer.pom";

    /**
     * User property for disabling version resolver cache.
     *
     * @since 3.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_VERSION_RESOLVER_NO_CACHE = "maven.versionResolver.noCache";

    /**
     * User property for overriding calculated "build number" for snapshot deploys. Caution: this property should
     * be RARELY used (if used at all). It may help in special cases like "aligning" a reactor build subprojects
     * build numbers to perform a "snapshot lock down". Value given here must be <code>maxRemoteBuildNumber + 1</code>
     * or greater, otherwise build will fail. How the number to be obtained is left to user (ie by inspecting
     * snapshot repository metadata or alike).
     *
     * Note: this feature is present in Maven 3.9.7 but with different key: <code>maven.buildNumber</code>. In Maven 4
     * as part of cleanup effort this key was renamed to properly reflect its purpose.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Integer")
    public static final String MAVEN_DEPLOY_SNAPSHOT_BUILD_NUMBER = "maven.deploy.snapshot.buildNumber";

    private Constants() {}
}
