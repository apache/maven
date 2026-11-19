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
package org.apache.maven.cling.invoker;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.maven.api.Constants;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.CoreExtensions;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.cisupport.CIInfo;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.cli.extensions.InputLocation;
import org.apache.maven.api.cli.extensions.InputSource;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.settings.Extension;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.cling.internal.extension.io.CoreExtensionsStaxReader;
import org.apache.maven.cling.invoker.cisupport.CIDetectorHelper;
import org.apache.maven.cling.props.MavenPropertiesLoader;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.properties.internal.SystemProperties;
import org.apache.maven.settings.v4.SettingsStaxReader;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.CliUtils.createInterpolator;
import static org.apache.maven.cling.invoker.CliUtils.getCanonicalPath;
import static org.apache.maven.cling.invoker.CliUtils.or;
import static org.apache.maven.cling.invoker.CliUtils.prefix;
import static org.apache.maven.cling.invoker.CliUtils.stripLeadingAndTrailingQuotes;
import static org.apache.maven.cling.invoker.CliUtils.toMap;

public abstract class BaseParser implements Parser {

    @SuppressWarnings("VisibilityModifier")
    public static class LocalContext {
        public final ParserRequest parserRequest;
        public final Map<String, String> systemPropertiesOverrides;

        public LocalContext(ParserRequest parserRequest) {
            this.parserRequest = parserRequest;
            this.systemPropertiesOverrides = new HashMap<>();
        }

        public boolean parsingFailed = false;
        public Path cwd;
        public Path installationDirectory;
        public Path userHomeDirectory;
        public Map<String, String> systemProperties;
        public Map<String, String> userProperties;
        public Path topDirectory;

        @Nullable
        public Path rootDirectory;

        @Nullable
        public List<CoreExtensions> extensions;

        @Nullable
        public CIInfo ciInfo;

        @Nullable
        public Options options;

        // Cached early-parsed settings files — avoids reading the same file twice
        // (once for extensions in readCoreExtensionsDescriptor, once for CLI args in MavenParser).
        @Nullable
        public Path cachedProjectSettingsPath;

        @Nullable
        public Settings cachedProjectSettings;

        public Map<String, String> extraInterpolationSource() {
            Map<String, String> extra = new HashMap<>();
            extra.put("session.topDirectory", topDirectory.toString());
            if (rootDirectory != null) {
                extra.put("session.rootDirectory", rootDirectory.toString());
            }
            return extra;
        }
    }

    @Override
    public InvokerRequest parseInvocation(ParserRequest parserRequest) {
        requireNonNull(parserRequest);

        LocalContext context = new LocalContext(parserRequest);

        // the basics
        try {
            context.cwd = getCwd(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            context.cwd = getCanonicalPath(Paths.get("."));
            parserRequest.logger().error("Error determining working directory", e);
        }
        try {
            context.installationDirectory = getInstallationDirectory(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            context.installationDirectory = context.cwd;
            parserRequest.logger().error("Error determining installation directory", e);
        }
        try {
            context.userHomeDirectory = getUserHomeDirectory(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            context.userHomeDirectory = context.cwd;
            parserRequest.logger().error("Error determining user home directory", e);
        }

        // top/root
        try {
            context.topDirectory = getTopDirectory(context);
        } catch (IllegalArgumentException e) {
            // User-facing error (e.g. -f points to a non-existent file): report the message directly,
            // without wrapping it in an internal "Error determining top directory" prefix.
            context.parsingFailed = true;
            context.topDirectory = context.cwd;
            parserRequest.logger().error(e.getMessage());
        } catch (Exception e) {
            context.parsingFailed = true;
            context.topDirectory = context.cwd;
            parserRequest.logger().error("Error determining top directory", e);
        }
        try {
            context.rootDirectory = getRootDirectory(context);
        } catch (IllegalArgumentException e) {
            context.parsingFailed = true;
            context.rootDirectory = context.cwd;
            parserRequest.logger().error(e.getMessage());
        } catch (Exception e) {
            context.parsingFailed = true;
            context.rootDirectory = context.cwd;
            parserRequest.logger().error("Error determining root directory", e);
        }

        // options
        try {
            context.options = parseCliOptions(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            context.options = null;
            parserRequest.logger().error("Error parsing program arguments", e);
        }

        // system and user properties
        try {
            context.systemProperties = populateSystemProperties(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            context.systemProperties = new HashMap<>();
            parserRequest.logger().error("Error populating system properties", e);
        }
        try {
            context.userProperties = populateUserProperties(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            context.userProperties = new HashMap<>();
            parserRequest.logger().error("Error populating user properties", e);
        }

        // options: interpolate
        if (context.options != null) {
            context.options = context.options.interpolate(Interpolator.chain(
                    context.extraInterpolationSource()::get,
                    context.userProperties::get,
                    context.systemProperties::get));
        }

        // below we use effective properties as both system + user are present
        // core extensions
        try {
            context.extensions = readCoreExtensionsDescriptor(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            parserRequest.logger().error("Error reading core extensions descriptor", e);
        }

        // CI detection
        context.ciInfo = detectCI(context);

        // only if not failed so far; otherwise we may have no options to validate
        if (!context.parsingFailed) {
            validate(context);
        }

        return getInvokerRequest(context);
    }

    protected void validate(LocalContext context) {
        Options options = context.options;

        options.failOnSeverity().ifPresent(severity -> {
            String c = severity.toLowerCase(Locale.ENGLISH);
            if (!Arrays.asList("warn", "warning", "error").contains(c)) {
                context.parsingFailed = true;
                context.parserRequest
                        .logger()
                        .error("Invalid fail on severity threshold '" + c
                                + "'. Supported values are 'WARN', 'WARNING' and 'ERROR'.");
            }
        });
        options.altUserSettings()
                .ifPresent(userSettings ->
                        failIfFileNotExists(context, userSettings, "The specified user settings file does not exist"));
        options.altProjectSettings()
                .ifPresent(projectSettings -> failIfFileNotExists(
                        context, projectSettings, "The specified project settings file does not exist"));
        options.altInstallationSettings()
                .ifPresent(installationSettings -> failIfFileNotExists(
                        context, installationSettings, "The specified installation settings file does not exist"));
        options.altUserToolchains()
                .ifPresent(userToolchains -> failIfFileNotExists(
                        context, userToolchains, "The specified user toolchains file does not exist"));
        options.altInstallationToolchains()
                .ifPresent(installationToolchains -> failIfFileNotExists(
                        context, installationToolchains, "The specified installation toolchains file does not exist"));
        options.color().ifPresent(color -> {
            String c = color.toLowerCase(Locale.ENGLISH);
            if (!Arrays.asList("always", "yes", "force", "never", "no", "none", "auto", "tty", "if-tty")
                    .contains(c)) {
                context.parsingFailed = true;
                context.parserRequest
                        .logger()
                        .error("Invalid color configuration value '" + c
                                + "'. Supported values are 'auto', 'always', 'never'.");
            }
        });
    }

    protected void failIfFileNotExists(LocalContext context, String fileName, String message) {
        Path path = context.cwd.resolve(fileName);
        if (!Files.isRegularFile(path)) {
            context.parsingFailed = true;
            context.parserRequest.logger().error(message + ": " + path);
        }
    }

    protected InvokerRequest getInvokerRequest(LocalContext context) {
        return new BaseInvokerRequest(
                context.parserRequest,
                context.parsingFailed,
                context.cwd,
                context.installationDirectory,
                context.userHomeDirectory,
                context.userProperties,
                context.systemProperties,
                context.topDirectory,
                context.rootDirectory,
                context.extensions,
                context.ciInfo,
                context.options);
    }

    protected Path getCwd(LocalContext context) {
        if (context.parserRequest.cwd() != null) {
            Path result = getCanonicalPath(context.parserRequest.cwd());
            context.systemPropertiesOverrides.put("user.dir", result.toString());
            return result;
        } else {
            Path result = getCanonicalPath(Paths.get(System.getProperty("user.dir")));
            mayOverrideDirectorySystemProperty(context, "user.dir", result);
            return result;
        }
    }

    protected Path getInstallationDirectory(LocalContext context) {
        if (context.parserRequest.mavenHome() != null) {
            Path result = getCanonicalPath(context.parserRequest.mavenHome());
            context.systemPropertiesOverrides.put(Constants.MAVEN_HOME, result.toString());
            return result;
        } else {
            String mavenHome = System.getProperty(Constants.MAVEN_HOME);
            if (mavenHome == null) {
                throw new IllegalStateException(
                        "local mode requires " + Constants.MAVEN_HOME + " Java System Property set");
            }
            Path result = getCanonicalPath(Paths.get(mavenHome));
            mayOverrideDirectorySystemProperty(context, Constants.MAVEN_HOME, result);
            return result;
        }
    }

    protected Path getUserHomeDirectory(LocalContext context) {
        if (context.parserRequest.userHome() != null) {
            Path result = getCanonicalPath(context.parserRequest.userHome());
            context.systemPropertiesOverrides.put("user.home", result.toString());
            return result;
        } else {
            // MSYS2 and Git Bash hand over Windows paths in their POSIX style, e.g. /c/Users/alice
            Path result = getCanonicalPath(Paths.get(MsysPaths.toNativePath(System.getProperty("user.home"))));
            mayOverrideDirectorySystemProperty(context, "user.home", result);
            return result;
        }
    }

    /**
     * This method is needed to "align" values used later on for interpolations and path calculations.
     * We enforce "canonical" paths, so IF key and canonical path value disagree, let override it.
     */
    protected void mayOverrideDirectorySystemProperty(LocalContext context, String javaSystemPropertyKey, Path value) {
        String valueString = value.toString();
        if (!Objects.equals(System.getProperty(javaSystemPropertyKey), valueString)) {
            context.systemPropertiesOverrides.put(javaSystemPropertyKey, valueString);
        }
    }

    protected Path getTopDirectory(LocalContext context) {
        // We need to locate the top level project which may be pointed at using
        // the -f/--file option.
        Path topDirectory = requireNonNull(context.cwd);
        boolean isAltFile = false;
        for (String arg : context.parserRequest.args()) {
            if (isAltFile) {
                // this is the argument following -f/--file
                Path path = topDirectory.resolve(stripLeadingAndTrailingQuotes(arg));
                if (Files.isDirectory(path)) {
                    topDirectory = path;
                } else if (Files.isRegularFile(path)) {
                    topDirectory = path.getParent();
                    if (!Files.isDirectory(topDirectory)) {
                        throw new IllegalArgumentException("Directory " + topDirectory
                                + " extracted from the -f/--file command-line argument " + arg + " does not exist");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "POM file " + arg + " specified with the -f/--file command line argument does not exist");
                }
                break;
            } else {
                // Check if this is the -f/--file option
                isAltFile = arg.equals("-f") || arg.equals("--file");
            }
        }
        return getCanonicalPath(topDirectory);
    }

    @Nullable
    protected Path getRootDirectory(LocalContext context) {
        return CliUtils.findRoot(context.topDirectory);
    }

    protected Map<String, String> populateSystemProperties(LocalContext context) {
        Properties systemProperties = new Properties();

        // ----------------------------------------------------------------------
        // Load environment and system properties
        // ----------------------------------------------------------------------

        EnvironmentUtils.addEnvVars(systemProperties);
        SystemProperties.addSystemProperties(systemProperties);
        systemProperties.putAll(context.systemPropertiesOverrides);

        // ----------------------------------------------------------------------
        // Properties containing info about the currently running version of Maven
        // These override any corresponding properties set on the command line
        // ----------------------------------------------------------------------

        Properties buildProperties = CLIReportingUtils.getBuildProperties();

        String mavenVersion = buildProperties.getProperty(CLIReportingUtils.BUILD_VERSION_PROPERTY);
        systemProperties.setProperty(Constants.MAVEN_VERSION, mavenVersion);

        boolean snapshot = mavenVersion.endsWith("SNAPSHOT");
        if (snapshot) {
            mavenVersion = mavenVersion.substring(0, mavenVersion.length() - "SNAPSHOT".length());
            if (mavenVersion.endsWith("-")) {
                mavenVersion = mavenVersion.substring(0, mavenVersion.length() - 1);
            }
        }
        String[] versionElements = mavenVersion.split("\\.");
        if (versionElements.length != 3) {
            throw new IllegalStateException("Maven version is expected to have 3 segments: '" + mavenVersion + "'");
        }
        systemProperties.setProperty(Constants.MAVEN_VERSION_MAJOR, versionElements[0]);
        systemProperties.setProperty(Constants.MAVEN_VERSION_MINOR, versionElements[1]);
        systemProperties.setProperty(Constants.MAVEN_VERSION_PATCH, versionElements[2]);
        systemProperties.setProperty(Constants.MAVEN_VERSION_SNAPSHOT, Boolean.toString(snapshot));

        String mavenBuildVersion = CLIReportingUtils.createMavenVersionString(buildProperties);
        systemProperties.setProperty(Constants.MAVEN_BUILD_VERSION, mavenBuildVersion);

        Path mavenConf;
        if (systemProperties.getProperty(Constants.MAVEN_INSTALLATION_CONF) != null) {
            mavenConf = context.installationDirectory.resolve(
                    systemProperties.getProperty(Constants.MAVEN_INSTALLATION_CONF));
        } else if (systemProperties.getProperty("maven.conf") != null) {
            mavenConf = context.installationDirectory.resolve(systemProperties.getProperty("maven.conf"));
        } else if (systemProperties.getProperty(Constants.MAVEN_HOME) != null) {
            mavenConf = context.installationDirectory
                    .resolve(systemProperties.getProperty(Constants.MAVEN_HOME))
                    .resolve("conf");
        } else {
            mavenConf = context.installationDirectory.resolve("");
        }

        UnaryOperator<String> callback = or(
                context.extraInterpolationSource()::get,
                context.systemPropertiesOverrides::get,
                systemProperties::getProperty);
        Path propertiesFile = mavenConf.resolve("maven-system.properties");
        try {
            MavenPropertiesLoader.loadProperties(systemProperties, propertiesFile, callback, false);
        } catch (IOException e) {
            throw new IllegalStateException("Error loading properties from " + propertiesFile, e);
        }

        Map<String, String> result = toMap(systemProperties);
        result.putAll(context.systemPropertiesOverrides);
        return result;
    }

    protected Map<String, String> populateUserProperties(LocalContext context) {
        Properties userProperties = new Properties();
        Map<String, String> paths = context.extraInterpolationSource();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        Map<String, String> userSpecifiedProperties = context.options != null
                ? new HashMap<>(context.options.userProperties().orElse(new HashMap<>()))
                : new HashMap<>();
        // Resolve only early-available path placeholders (session.topDirectory & co); leave everything
        // else literal so it can be evaluated later with full session/project context by the
        // plugin parameter expression evaluator (gh-12507: wiping unresolved ${...} to the empty
        // string silently broke e.g. -DaltDeploymentRepository=id::file://${project.build.directory}/x).
        createInterpolator().interpolate(userSpecifiedProperties, paths::get, false);

        // ----------------------------------------------------------------------
        // Load config files
        // ----------------------------------------------------------------------
        UnaryOperator<String> callback =
                or(paths::get, prefix("cli.", userSpecifiedProperties::get), context.systemProperties::get);

        Path mavenConf;
        if (context.systemProperties.get(Constants.MAVEN_INSTALLATION_CONF) != null) {
            mavenConf = context.installationDirectory.resolve(
                    context.systemProperties.get(Constants.MAVEN_INSTALLATION_CONF));
        } else if (context.systemProperties.get("maven.conf") != null) {
            mavenConf = context.installationDirectory.resolve(context.systemProperties.get("maven.conf"));
        } else if (context.systemProperties.get(Constants.MAVEN_HOME) != null) {
            mavenConf = context.installationDirectory
                    .resolve(context.systemProperties.get(Constants.MAVEN_HOME))
                    .resolve("conf");
        } else {
            mavenConf = context.installationDirectory.resolve("");
        }
        Path propertiesFile = mavenConf.resolve("maven-user.properties");
        try {
            MavenPropertiesLoader.loadProperties(userProperties, propertiesFile, callback, false);
        } catch (IOException e) {
            throw new IllegalStateException("Error loading properties from " + propertiesFile, e);
        }

        // CLI specified properties are most dominant
        userProperties.putAll(userSpecifiedProperties);

        return toMap(userProperties);
    }

    protected abstract Options parseCliOptions(LocalContext context);

    /**
     * Important: This method must return list of {@link CoreExtensions} in precedence order.
     */
    protected List<CoreExtensions> readCoreExtensionsDescriptor(LocalContext context) {
        ArrayList<CoreExtensions> result = new ArrayList<>();
        Path file;
        List<CoreExtension> loaded;

        Map<String, String> eff = new HashMap<>(context.systemProperties);
        eff.putAll(context.userProperties);

        // project: check settings.xml <extensions> first; fall back to extensions.xml
        Path projectSettingsFile = resolveProjectSettingsFile(context, eff);
        Settings projectSettings = readSettingsEarlyCached(projectSettingsFile, context);
        if (projectSettings != null
                && projectSettings.getExtensions() != null
                && !projectSettings.getExtensions().isEmpty()) {
            // settings.xml is authoritative for project extensions
            List<CoreExtension> fromSettings = projectSettings.getExtensions().stream()
                    .map(e -> toCoreExtension(e, projectSettingsFile))
                    .collect(Collectors.toList());
            result.add(new CoreExtensions(projectSettingsFile, fromSettings));
            // Warn if legacy extensions.xml also exists alongside settings.xml <extensions>
            Path legacyExtensionsFile = context.cwd.resolve(eff.get(Constants.MAVEN_PROJECT_EXTENSIONS));
            if (Files.isRegularFile(legacyExtensionsFile)) {
                context.parserRequest
                        .logger()
                        .warn("Both " + projectSettingsFile + " <extensions> and "
                                + legacyExtensionsFile + " exist. The settings.xml <extensions> take precedence;"
                                + " consider migrating " + legacyExtensionsFile + " entries into "
                                + projectSettingsFile + " and removing the legacy file.");
            }
        } else {
            // legacy fallback: .mvn/extensions.xml
            file = context.cwd.resolve(eff.get(Constants.MAVEN_PROJECT_EXTENSIONS));
            loaded = readCoreExtensionsDescriptorFromFile(file, false);
            if (!loaded.isEmpty()) {
                result.add(new CoreExtensions(file, loaded));
            }
        }

        // user: check settings.xml <extensions> first, then extensions.xml
        Path userSettingsFile = resolveUserSettingsFile(context, eff);
        Settings userSettings = readSettingsEarly(userSettingsFile, context);
        if (userSettings != null
                && userSettings.getExtensions() != null
                && !userSettings.getExtensions().isEmpty()) {
            List<CoreExtension> fromSettings = userSettings.getExtensions().stream()
                    .map(e -> toCoreExtension(e, userSettingsFile))
                    .collect(Collectors.toList());
            result.add(new CoreExtensions(userSettingsFile, fromSettings));
        } else {
            file = context.userHomeDirectory.resolve(eff.get(Constants.MAVEN_USER_EXTENSIONS));
            loaded = readCoreExtensionsDescriptorFromFile(file, true);
            if (!loaded.isEmpty()) {
                result.add(new CoreExtensions(file, loaded));
            }
        }

        // installation: check settings.xml <extensions> first, then extensions.xml
        Path installationSettingsFile = resolveInstallationSettingsFile(context, eff);
        Settings installationSettings = readSettingsEarly(installationSettingsFile, context);
        if (installationSettings != null
                && installationSettings.getExtensions() != null
                && !installationSettings.getExtensions().isEmpty()) {
            List<CoreExtension> fromSettings = installationSettings.getExtensions().stream()
                    .map(e -> toCoreExtension(e, installationSettingsFile))
                    .collect(Collectors.toList());
            result.add(new CoreExtensions(installationSettingsFile, fromSettings));
        } else {
            file = context.installationDirectory.resolve(eff.get(Constants.MAVEN_INSTALLATION_EXTENSIONS));
            loaded = readCoreExtensionsDescriptorFromFile(file, true);
            if (!loaded.isEmpty()) {
                result.add(new CoreExtensions(file, loaded));
            }
        }

        return result.isEmpty() ? null : List.copyOf(result);
    }

    /**
     * Resolves the project settings file path without DI, for the early pre-DI pass.
     */
    protected Path resolveProjectSettingsFile(LocalContext context, Map<String, String> eff) {
        // honour --project-settings override if already parsed
        if (context.options instanceof MavenOptions mavenOpts) {
            if (mavenOpts.altProjectSettings().isPresent()) {
                return context.cwd.resolve(mavenOpts.altProjectSettings().get());
            }
        }
        String projectSettingsStr = eff.get(Constants.MAVEN_PROJECT_SETTINGS);
        if (projectSettingsStr != null && context.rootDirectory != null) {
            Path resolved = context.rootDirectory.resolve(projectSettingsStr);
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }
        }
        // default: .mvn/settings.xml relative to rootDirectory
        if (context.rootDirectory != null) {
            Path defaultPath = context.rootDirectory.resolve(".mvn/settings.xml");
            if (Files.isRegularFile(defaultPath)) {
                return defaultPath;
            }
        }
        return null;
    }

    /**
     * Resolves the user settings file path without DI, for the early pre-DI pass.
     */
    protected Path resolveUserSettingsFile(LocalContext context, Map<String, String> eff) {
        if (context.options instanceof MavenOptions mavenOpts) {
            if (mavenOpts.altUserSettings().isPresent()) {
                return context.cwd.resolve(mavenOpts.altUserSettings().get());
            }
        }
        String userSettingsStr = eff.get(Constants.MAVEN_USER_SETTINGS);
        if (userSettingsStr != null) {
            Path resolved = context.userHomeDirectory.resolve(userSettingsStr);
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }
        }
        return null;
    }

    /**
     * Resolves the installation settings file path without DI, for the early pre-DI pass.
     */
    protected Path resolveInstallationSettingsFile(LocalContext context, Map<String, String> eff) {
        if (context.options instanceof MavenOptions mavenOpts) {
            if (mavenOpts.altInstallationSettings().isPresent()) {
                return context.cwd.resolve(mavenOpts.altInstallationSettings().get());
            }
        }
        String installationSettingsStr = eff.get(Constants.MAVEN_INSTALLATION_SETTINGS);
        if (installationSettingsStr != null) {
            Path resolved = context.installationDirectory.resolve(installationSettingsStr);
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }
        }
        return null;
    }

    /**
     * Lightweight early parse of a settings.xml file for pre-DI data (extensions, args/options).
     * Returns {@code null} if the file is absent or cannot be parsed.
     *
     * <p><b>Known limitation:</b> property interpolation is intentionally skipped ({@code false, null}
     * arguments to the reader) because the full session is not yet set up. Extension coordinates
     * containing {@code ${...}} property references (e.g. {@code <version>${my.ext.version}</version>})
     * will be used literally and are therefore not supported in early-parsed blocks.
     */
    @Nullable
    protected Settings readSettingsEarly(@Nullable Path settingsFile, LocalContext context) {
        if (settingsFile == null || !Files.isRegularFile(settingsFile)) {
            return null;
        }
        try (InputStream is = Files.newInputStream(settingsFile)) {
            return new SettingsStaxReader().read(is, false, null);
        } catch (Exception e) {
            context.parserRequest
                    .logger()
                    .warn("Could not read settings file for early parsing: " + settingsFile + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Caching wrapper around {@link #readSettingsEarly}: on the first call for a given path the result
     * is stored in {@code context.cachedProjectSettings}; subsequent calls for the same path return the
     * cached instance without re-reading the file. This avoids the double-parse that would otherwise
     * occur when both {@code readCoreExtensionsDescriptor} and {@code MavenParser.parseCliOptions}
     * independently resolve and read the project settings file on startup.
     *
     * <p>Only the <em>project</em> settings file is cached here (user/installation settings are only
     * read in one place and do not need caching). Pass {@code null} to skip caching and fall through to
     * the regular parse.
     */
    @Nullable
    protected Settings readSettingsEarlyCached(@Nullable Path settingsFile, LocalContext context) {
        if (settingsFile == null) {
            return null;
        }
        if (settingsFile.equals(context.cachedProjectSettingsPath)) {
            return context.cachedProjectSettings;
        }
        Settings result = readSettingsEarly(settingsFile, context);
        context.cachedProjectSettingsPath = settingsFile;
        context.cachedProjectSettings = result;
        return result;
    }

    /**
     * Converts a settings {@link Extension} to a {@link CoreExtension}, expanding the compact {@code id} shorthand.
     */
    protected CoreExtension toCoreExtension(Extension ext, Path sourceFile) {
        String groupId = ext.getGroupId();
        String artifactId = ext.getArtifactId();
        String version = ext.getVersion();
        String id = ext.getId();
        if (id != null && !id.isEmpty()) {
            if (groupId != null || artifactId != null || version != null) {
                throw new IllegalArgumentException("Extension id '" + id
                        + "' must not be combined with groupId/artifactId/version in " + sourceFile);
            }
            String[] parts = id.split(":", -1);
            if (parts.length != 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid extension id '" + id + "': expected groupId:artifactId:version in " + sourceFile);
            }
            groupId = parts[0];
            artifactId = parts[1];
            version = parts[2];
        }
        if (groupId == null
                || groupId.isEmpty()
                || artifactId == null
                || artifactId.isEmpty()
                || version == null
                || version.isEmpty()) {
            throw new IllegalArgumentException(
                    "Extension must specify either id or all of groupId, artifactId, and version in " + sourceFile);
        }
        org.apache.maven.api.cli.extensions.CoreExtension.Builder builder =
                org.apache.maven.api.cli.extensions.CoreExtension.newBuilder(true);
        builder.groupId(groupId);
        builder.artifactId(artifactId);
        builder.version(version);
        builder.location(
                "",
                org.apache.maven.api.cli.extensions.InputLocation.of(
                        -1, -1, org.apache.maven.api.cli.extensions.InputSource.of(sourceFile.toString())));
        if (ext.getClassLoadingStrategy() != null) {
            builder.classLoadingStrategy(ext.getClassLoadingStrategy());
        }
        if (ext.getConfiguration() != null) {
            builder.configuration(ext.getConfiguration());
        }
        return builder.build();
    }

    protected List<CoreExtension> readCoreExtensionsDescriptorFromFile(Path extensionsFile, boolean allowMetaVersions) {
        try {
            if (extensionsFile != null && Files.exists(extensionsFile)) {
                try (InputStream is = Files.newInputStream(extensionsFile)) {
                    return validateCoreExtensionsDescriptorFromFile(
                            extensionsFile,
                            List.copyOf(new CoreExtensionsStaxReader()
                                    .read(is, true, InputSource.of(extensionsFile.toString()))
                                    .getExtensions()),
                            allowMetaVersions);
                }
            }
            return List.of();
        } catch (XMLStreamException | IOException e) {
            throw new IllegalArgumentException("Failed to parse extensions file: " + extensionsFile, e);
        }
    }

    protected List<CoreExtension> validateCoreExtensionsDescriptorFromFile(
            Path extensionFile, List<CoreExtension> coreExtensions, boolean allowMetaVersions) {
        Map<String, List<InputLocation>> gasLocations = new HashMap<>();
        Map<String, List<InputLocation>> metaVersionLocations = new HashMap<>();
        for (CoreExtension coreExtension : coreExtensions) {
            String ga = coreExtension.getGroupId() + ":" + coreExtension.getArtifactId();
            InputLocation location = coreExtension.getLocation("");
            gasLocations.computeIfAbsent(ga, k -> new ArrayList<>()).add(location);
            // TODO: metaversions could be extensible enum with these two values out of the box
            if ("LATEST".equals(coreExtension.getVersion()) || "RELEASE".equals(coreExtension.getVersion())) {
                metaVersionLocations.computeIfAbsent(ga, k -> new ArrayList<>()).add(location);
            }
        }
        if (gasLocations.values().stream().anyMatch(l -> l.size() > 1)) {
            throw new IllegalStateException("Extension conflicts in file " + extensionFile + ": "
                    + gasLocations.entrySet().stream()
                            .map(e -> e.getKey() + " defined on lines "
                                    + e.getValue().stream()
                                            .map(l -> String.valueOf(l.getLineNumber()))
                                            .collect(Collectors.joining(", ")))
                            .collect(Collectors.joining("; ")));
        }
        if (!allowMetaVersions && !metaVersionLocations.isEmpty()) {
            throw new IllegalStateException("Extension with illegal version in file " + extensionFile + ": "
                    + metaVersionLocations.entrySet().stream()
                            .map(e -> e.getKey() + " defined on lines "
                                    + e.getValue().stream()
                                            .map(l -> String.valueOf(l.getLineNumber()))
                                            .collect(Collectors.joining(", ")))
                            .collect(Collectors.joining("; ")));
        }
        return coreExtensions;
    }

    @Nullable
    protected CIInfo detectCI(LocalContext context) {
        List<CIInfo> detected = CIDetectorHelper.detectCI();
        if (detected.isEmpty()) {
            return null;
        } else if (detected.size() > 1) {
            // warn
            context.parserRequest
                    .logger()
                    .warn("Multiple CI systems detected: "
                            + detected.stream().map(CIInfo::name).collect(Collectors.joining(", ")));
        }
        return detected.get(0);
    }
}
