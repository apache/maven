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
    @Config(readOnly = true, source = Config.Source.SYSTEM_PROPERTIES)
    public static final String MAVEN_HOME = "maven.home";

    /**
     * Maven version.
     *
     * @since 3.0.0
     */
    @Config(readOnly = true, source = Config.Source.SYSTEM_PROPERTIES)
    public static final String MAVEN_VERSION = "maven.version";

    /**
     * Maven major version: contains the major segment of this Maven version.
     *
     * @since 4.0.0
     */
    @Config(readOnly = true, source = Config.Source.SYSTEM_PROPERTIES)
    public static final String MAVEN_VERSION_MAJOR = "maven.version.major";

    /**
     * Maven minor version: contains the minor segment of this Maven version.
     *
     * @since 4.0.0
     */
    @Config(readOnly = true, source = Config.Source.SYSTEM_PROPERTIES)
    public static final String MAVEN_VERSION_MINOR = "maven.version.minor";

    /**
     * Maven patch version: contains the patch segment of this Maven version.
     *
     * @since 4.0.0
     */
    @Config(readOnly = true, source = Config.Source.SYSTEM_PROPERTIES)
    public static final String MAVEN_VERSION_PATCH = "maven.version.patch";

    /**
     * Maven snapshot: contains "true" if this Maven is a snapshot version.
     *
     * @since 4.0.0
     */
    @Config(readOnly = true, source = Config.Source.SYSTEM_PROPERTIES)
    public static final String MAVEN_VERSION_SNAPSHOT = "maven.version.snapshot";

    /**
     * Maven build version: a human-readable string containing this Maven version, buildnumber, and time of its build.
     *
     * @since 3.0.0
     */
    @Config(readOnly = true, source = Config.Source.SYSTEM_PROPERTIES)
    public static final String MAVEN_BUILD_VERSION = "maven.build.version";

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
    @Config(defaultValue = "${maven.user.conf}/toolchains.xml")
    public static final String MAVEN_USER_TOOLCHAINS = "maven.user.toolchains";

    /**
     * Extensions class path.
     */
    @Config
    public static final String MAVEN_EXT_CLASS_PATH = "maven.ext.class.path";

    @Config(defaultValue = "${maven.user.conf}/settings-security4.xml")
    public static final String MAVEN_SETTINGS_SECURITY = "maven.settings.security";

    public static final String MAVEN_SETTINGS_SECURITY_FILE_NAME = "settings-security4.xml";

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
     *     <li>"ns" - unconditional snapshot filter (no snapshots selected from ranges)</li>
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
     * management entries in their POMs. Maven 3 targeted full backward compatibility with Maven 2. Hence, it ignored
     * dependency management entries in transitive dependency POMs. Maven 4 enables "transitivity" by default. Hence
     * unlike Maven 3, it obeys dependency management entries deep in the dependency graph as well.
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
     * User property for controlling consumer POM flattening behavior.
     * When set to <code>true</code> (default), consumer POMs are flattened by removing
     * dependency management and keeping only direct dependencies with transitive scopes.
     * When set to <code>false</code>, consumer POMs preserve dependency management
     * like parent POMs, allowing dependency management to be inherited by consumers.
     *
     * @since 4.1.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_CONSUMER_POM_FLATTEN = "maven.consumer.pom.flatten";

    /**
     * User property for controlling "maven personality". If activated Maven will behave
     * like the previous major version, Maven 3.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_MAVEN3_PERSONALITY = "maven.maven3Personality";

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

    /**
     * User property for controlling whether build POMs are deployed alongside consumer POMs.
     * When set to <code>false</code>, only the consumer POM will be deployed, and the build POM
     * will be excluded from deployment. This is useful to avoid deploying internal build information
     * that is not needed by consumers of the artifact.
     * <br/>
     * Default: <code>"true"</code>.
     *
     * @since 4.1.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "true")
    public static final String MAVEN_DEPLOY_BUILD_POM = "maven.deploy.buildPom";

    /**
     * User property used to store the build timestamp.
     *
     * @since 4.0.0
     */
    @Config(type = "java.time.Instant")
    public static final String MAVEN_START_INSTANT = "maven.startInstant";

    /**
     * Max number of problems for each severity level retained by the model builder.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Integer", defaultValue = "100")
    public static final String MAVEN_BUILDER_MAX_PROBLEMS = "maven.builder.maxProblems";

    /**
     * Configuration property for version range resolution used metadata "nature".
     * It may contain following string values:
     * <ul>
     *     <li>"auto" - decision done based on range being resolver: if any boundary is snapshot, use "release_or_snapshot", otherwise "release"</li>
     *     <li>"release_or_snapshot" - the default</li>
     *     <li>"release" - query only release repositories to discover versions</li>
     *     <li>"snapshot" - query only snapshot repositories to discover versions</li>
     * </ul>
     * Default (when unset) is using request carried nature. Hence, this configuration really makes sense with value
     * {@code "auto"}, while ideally callers needs update and use newly added method on version range request to
     * express preference.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "release_or_snapshot")
    public static final String MAVEN_VERSION_RANGE_RESOLVER_NATURE_OVERRIDE =
            "maven.versionRangeResolver.natureOverride";

    /**
     * Comma-separated list of XML contexts/fields to intern during POM parsing for memory optimization.
     * When not specified, a default set of commonly repeated contexts will be used.
     * Example: "groupId,artifactId,version,scope,type"
     *
     * @since 4.0.0
     */
    @Config
    public static final String MAVEN_MODEL_BUILDER_INTERNS = "maven.modelBuilder.interns";

    /**
     * All system properties used by Maven Logger start with this prefix.
     *
     * @since 4.0.0
     */
    public static final String MAVEN_LOGGER_PREFIX = "maven.logger.";

    /**
     * Default log level for all instances of SimpleLogger. Must be one of ("trace", "debug", "info",
     * "warn", "error" or "off"). If not specified, defaults to "info".
     *
     * @since 4.0.0
     */
    @Config
    public static final String MAVEN_LOGGER_DEFAULT_LOG_LEVEL = MAVEN_LOGGER_PREFIX + "defaultLogLevel";

    /**
     * Set to true if you want the current date and time to be included in output messages. Default is false.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_LOGGER_SHOW_DATE_TIME = MAVEN_LOGGER_PREFIX + "showDateTime";

    /**
     * The date and time format to be used in the output messages. The pattern describing the date and
     * time format is defined by SimpleDateFormat. If the format is not specified or is invalid, the
     * number of milliseconds since start up will be output.
     *
     * @since 4.0.0
     */
    @Config
    public static final String MAVEN_LOGGER_DATE_TIME_FORMAT = MAVEN_LOGGER_PREFIX + "dateTimeFormat";

    /**
     * If you would like to output the current thread id, then set to true. Defaults to false.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_LOGGER_SHOW_THREAD_ID = MAVEN_LOGGER_PREFIX + "showThreadId";

    /**
     * Set to true if you want to output the current thread name. Defaults to true.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "true")
    public static final String MAVEN_LOGGER_SHOW_THREAD_NAME = MAVEN_LOGGER_PREFIX + "showThreadName";

    /**
     * Set to true if you want the Logger instance name to be included in output messages. Defaults to true.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "true")
    public static final String MAVEN_LOGGER_SHOW_LOG_NAME = MAVEN_LOGGER_PREFIX + "showLogName";

    /**
     * Set to true if you want the last component of the name to be included in output messages. Defaults to false.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_LOGGER_SHOW_SHORT_LOG_NAME = MAVEN_LOGGER_PREFIX + "showShortLogName";

    /**
     * The output target which can be the path to a file, or the special values "System.out" and "System.err".
     * Default is "System.err".
     *
     * @since 4.0.0
     */
    @Config
    public static final String MAVEN_LOGGER_LOG_FILE = MAVEN_LOGGER_PREFIX + "logFile";

    /**
     * Should the level string be output in brackets? Defaults to false.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_LOGGER_LEVEL_IN_BRACKETS = MAVEN_LOGGER_PREFIX + "levelInBrackets";

    /**
     * The string value output for the warn level. Defaults to WARN.
     *
     * @since 4.0.0
     */
    @Config(defaultValue = "WARN")
    public static final String MAVEN_LOGGER_WARN_LEVEL = MAVEN_LOGGER_PREFIX + "warnLevelString";

    /**
     * If the output target is set to "System.out" or "System.err" (see preceding entry), by default, logs will
     * be output to the latest value referenced by System.out/err variables. By setting this parameter to true,
     * the output stream will be cached, i.e. assigned once at initialization time and re-used independently of
     * the current value referenced by System.out/err.
     *
     * @since 4.0.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_LOGGER_CACHE_OUTPUT_STREAM = MAVEN_LOGGER_PREFIX + "cacheOutputStream";

    /**
     * maven.logger.log.a.b.c - Logging detail level for a SimpleLogger instance named "a.b.c". Right-side value
     * must be one of "trace", "debug", "info", "warn", "error" or "off". When a logger named "a.b.c" is initialized,
     * its level is assigned from this property. If unspecified, the level of nearest parent logger will be used,
     * and if none is set, then the value specified by {@code maven.logger.defaultLogLevel} will be used.
     *
     * @since 4.0.0
     */
    public static final String MAVEN_LOGGER_LOG_PREFIX = MAVEN_LOGGER_PREFIX + "log.";

    /**
     * User property key for cache configuration.
     *
     * @since 4.1.0
     */
    public static final String MAVEN_CACHE_CONFIG_PROPERTY = "maven.cache.config";

    /**
     * User property to enable cache statistics display at the end of the build.
     * When set to true, detailed cache statistics including hit/miss ratios,
     * request type breakdowns, and retention policy effectiveness will be displayed
     * when the build completes.
     *
     * @since 4.1.0
     */
    @Config(type = "java.lang.Boolean", defaultValue = "false")
    public static final String MAVEN_CACHE_STATS = "maven.cache.stats";

    /**
     * User property to configure separate reference types for cache keys.
     * This enables fine-grained analysis of cache misses caused by key vs value evictions.
     * Supported values are {@code HARD}, {@code SOFT} and {@code WEAK}.
     *
     * @since 4.1.0
     */
    public static final String MAVEN_CACHE_KEY_REFS = "maven.cache.keyValueRefs";

    /**
     * User property to configure separate reference types for cache values.
     * This enables fine-grained analysis of cache misses caused by key vs value evictions.
     * Supported values are {@code HARD}, {@code SOFT} and {@code WEAK}.
     *
     * @since 4.1.0
     */
    public static final String MAVEN_CACHE_VALUE_REFS = "maven.cache.keyValueRefs";

    /**
     * User property key for configuring which object types are pooled by ModelObjectProcessor.
     * Value should be a comma-separated list of simple class names (e.g., "Dependency,Plugin,Build").
     * Default is "Dependency" for backward compatibility.
     *
     * @since 4.1.0
     */
    @Config(defaultValue = "Dependency")
    public static final String MAVEN_MODEL_PROCESSOR_POOLED_TYPES = "maven.model.processor.pooledTypes";

    /**
     * User property key for configuring the default reference type used by ModelObjectProcessor.
     * Valid values are: "SOFT", "HARD", "WEAK", "NONE".
     * Default is "HARD" for optimal performance.
     *
     * @since 4.1.0
     */
    @Config(defaultValue = "HARD")
    public static final String MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE = "maven.model.processor.referenceType";

    /**
     * User property key prefix for configuring per-object-type reference types.
     * Format: maven.model.processor.referenceType.{ClassName} = {ReferenceType}
     * Example: maven.model.processor.referenceType.Dependency = SOFT
     *
     * @since 4.1.0
     */
    public static final String MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE_PREFIX = "maven.model.processor.referenceType.";

    private Constants() {}
}
