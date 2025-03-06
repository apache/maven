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
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.cli.extensions.InputLocation;
import org.apache.maven.api.cli.extensions.InputSource;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.cling.internal.extension.io.CoreExtensionsStaxReader;
import org.apache.maven.cling.props.MavenPropertiesLoader;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.properties.internal.SystemProperties;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.Utils.getCanonicalPath;
import static org.apache.maven.cling.invoker.Utils.or;
import static org.apache.maven.cling.invoker.Utils.prefix;
import static org.apache.maven.cling.invoker.Utils.stripLeadingAndTrailingQuotes;
import static org.apache.maven.cling.invoker.Utils.toMap;

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

        public Options options;

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
        } catch (Exception e) {
            context.parsingFailed = true;
            context.topDirectory = context.cwd;
            parserRequest.logger().error("Error determining top directory", e);
        }
        try {
            context.rootDirectory = getRootDirectory(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            context.rootDirectory = context.cwd;
            parserRequest.logger().error("Error determining root directory", e);
        }

        // options
        List<Options> parsedOptions;
        try {
            parsedOptions = parseCliOptions(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            parsedOptions = List.of(emptyOptions());
            parserRequest.logger().error("Error parsing program arguments", e);
        }

        // assemble options if needed
        try {
            context.options = assembleOptions(parsedOptions);
        } catch (Exception e) {
            context.parsingFailed = true;
            context.options = emptyOptions();
            parserRequest.logger().error("Error assembling program arguments", e);
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
        context.options = context.options.interpolate(Interpolator.chain(
                context.extraInterpolationSource()::get, context.userProperties::get, context.systemProperties::get));

        // core extensions
        try {
            context.extensions = readCoreExtensionsDescriptor(context);
        } catch (Exception e) {
            context.parsingFailed = true;
            parserRequest.logger().error("Error reading core extensions descriptor", e);
        }

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

    protected abstract Options emptyOptions();

    protected abstract InvokerRequest getInvokerRequest(LocalContext context);

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
            Path result = getCanonicalPath(Paths.get(System.getProperty("user.home")));
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
        return Utils.findRoot(context.topDirectory);
    }

    protected Map<String, String> populateSystemProperties(LocalContext context) {
        Properties systemProperties = new Properties();

        // ----------------------------------------------------------------------
        // Load environment and system properties
        // ----------------------------------------------------------------------

        EnvironmentUtils.addEnvVars(systemProperties);
        SystemProperties.addSystemProperties(systemProperties);

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

        Map<String, String> result = toMap(systemProperties);
        result.putAll(context.systemPropertiesOverrides);
        return result;
    }

    protected Map<String, String> populateUserProperties(LocalContext context) {
        Properties userProperties = new Properties();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        Map<String, String> userSpecifiedProperties =
                context.options.userProperties().orElse(new HashMap<>());

        // ----------------------------------------------------------------------
        // Load config files
        // ----------------------------------------------------------------------
        Map<String, String> paths = context.extraInterpolationSource();
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
        Path propertiesFile = mavenConf.resolve("maven.properties");
        try {
            MavenPropertiesLoader.loadProperties(userProperties, propertiesFile, callback, false);
        } catch (IOException e) {
            throw new IllegalStateException("Error loading properties from " + propertiesFile, e);
        }

        // CLI specified properties are most dominant
        userProperties.putAll(userSpecifiedProperties);

        return toMap(userProperties);
    }

    protected abstract List<Options> parseCliOptions(LocalContext context);

    protected abstract Options assembleOptions(List<Options> parsedOptions);

    protected List<CoreExtensions> readCoreExtensionsDescriptor(LocalContext context) {
        ArrayList<CoreExtensions> result = new ArrayList<>();
        List<CoreExtension> loaded;

        loaded = readCoreExtensionsDescriptorFromFile(context.installationDirectory.resolve(
                context.userProperties.get(Constants.MAVEN_INSTALLATION_EXTENSIONS)));
        if (!loaded.isEmpty()) {
            result.add(new CoreExtensions(CoreExtensions.Source.INSTALLATION, loaded));
        }

        loaded = readCoreExtensionsDescriptorFromFile(
                context.userHomeDirectory.resolve(context.userProperties.get(Constants.MAVEN_USER_EXTENSIONS)));
        if (!loaded.isEmpty()) {
            result.add(new CoreExtensions(CoreExtensions.Source.USER, loaded));
        }

        loaded = readCoreExtensionsDescriptorFromFile(
                context.cwd.resolve(context.userProperties.get(Constants.MAVEN_PROJECT_EXTENSIONS)));
        if (!loaded.isEmpty()) {
            result.add(new CoreExtensions(CoreExtensions.Source.PROJECT, loaded));
        }
        return result.isEmpty() ? null : result;
    }

    protected List<CoreExtension> readCoreExtensionsDescriptorFromFile(Path extensionsFile) {
        try {
            if (extensionsFile != null && Files.exists(extensionsFile)) {
                try (InputStream is = Files.newInputStream(extensionsFile)) {
                    return validateCoreExtensionsDescriptorFromFile(
                            extensionsFile,
                            new CoreExtensionsStaxReader()
                                    .read(is, true, new InputSource(extensionsFile.toString()))
                                    .getExtensions());
                }
            }
            return List.of();
        } catch (XMLStreamException | IOException e) {
            throw new IllegalArgumentException("Failed to parse extensions file: " + extensionsFile, e);
        }
    }

    protected List<CoreExtension> validateCoreExtensionsDescriptorFromFile(
            Path extensionFile, List<CoreExtension> coreExtensions) {
        Map<String, List<InputLocation>> gasLocations = new HashMap<>();
        for (CoreExtension coreExtension : coreExtensions) {
            String ga = coreExtension.getGroupId() + ":" + coreExtension.getArtifactId();
            InputLocation location = coreExtension.getLocation("");
            gasLocations.computeIfAbsent(ga, k -> new ArrayList<>()).add(location);
        }
        if (gasLocations.values().stream().noneMatch(l -> l.size() > 1)) {
            return coreExtensions;
        }
        throw new IllegalStateException("Extension conflicts in file " + extensionFile + ": "
                + gasLocations.entrySet().stream()
                        .map(e -> e.getKey() + " defined on lines "
                                + e.getValue().stream()
                                        .map(l -> String.valueOf(l.getLineNumber()))
                                        .collect(Collectors.joining(", ")))
                        .collect(Collectors.joining("; ")));
    }
}
