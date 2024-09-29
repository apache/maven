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
package org.apache.maven.cling.invoker.mvn.local;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.Constants;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.cli.mvn.MavenParser;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.cli.internal.extension.io.CoreExtensionsStaxReader;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.props.MavenPropertiesLoader;
import org.apache.maven.cling.invoker.mvn.CommonsCliMavenOptions;
import org.apache.maven.cling.invoker.mvn.DefaultMavenInvokerRequest;
import org.apache.maven.cling.invoker.mvn.LayeredMavenOptions;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.properties.internal.SystemProperties;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.api.Constants.MAVEN_HOME;
import static org.apache.maven.api.Constants.MAVEN_INSTALLATION_CONF;
import static org.apache.maven.cling.invoker.Utils.getCanonicalPath;
import static org.apache.maven.cling.invoker.Utils.or;
import static org.apache.maven.cling.invoker.Utils.prefix;
import static org.apache.maven.cling.invoker.Utils.stripLeadingAndTrailingQuotes;
import static org.apache.maven.cling.invoker.Utils.toMap;

public class LocalParser implements MavenParser {
    @Override
    public MavenInvokerRequest parse(ParserRequest parserRequest) throws ParserException, IOException {
        requireNonNull(parserRequest);

        // the basics
        HashMap<String, String> overrides = new HashMap<>();
        FileSystem fileSystem = requireNonNull(getFileSystem(parserRequest));
        Path cwd = requireNonNull(getCwd(parserRequest, fileSystem, overrides));
        Path installationDirectory = requireNonNull(getInstallationDirectory(parserRequest, fileSystem, overrides));
        Path userHomeDirectory = requireNonNull(getUserHomeDirectory(parserRequest, fileSystem, overrides));

        // top/root
        Path topDirectory = getCanonicalPath(requireNonNull(getTopDirectory(parserRequest, cwd)));
        RootLocator rootLocator =
                ServiceLoader.load(RootLocator.class).iterator().next();
        @Nullable Path rootDirectory = rootLocator.findRoot(topDirectory);
        if (rootDirectory == null) {
            parserRequest.logger().warn(rootLocator.getNoRootMessage());
        } else {
            rootDirectory = getCanonicalPath(rootDirectory);
        }

        // options; args and maven.config
        CLIManager argCLIManager = new CLIManager();
        CommonsCliMavenOptions argOptions = parseCliOptions(argCLIManager, parserRequest.args());
        CommonsCliMavenOptions mavenConfigOptions = null;
        if (rootDirectory != null) {
            Path mavenConfig = rootDirectory.resolve(".mvn/maven.config");
            if (Files.isRegularFile(mavenConfig)) {
                CLIManager mavenConfigCLIManager = new CLIManager();
                mavenConfigOptions = parseConfigOptions(mavenConfigCLIManager, mavenConfig);
            }
        }

        // warn about deprecated options
        warnAboutDeprecatedOptions(parserRequest, "CLI", argOptions);
        if (mavenConfigOptions != null) {
            warnAboutDeprecatedOptions(parserRequest, "maven.config", mavenConfigOptions);
        }

        // options: layer args and mavenConfig (layer is null safe, will just leave nulls out)
        MavenOptions mavenOptions = LayeredMavenOptions.layer(argOptions, mavenConfigOptions);

        // system and user properties
        Map<String, String> systemProperties = populateSystemProperties(overrides);
        Map<String, String> paths = new HashMap<>();
        paths.put("session.topDirectory", topDirectory.toString());
        if (rootDirectory != null) {
            paths.put("session.rootDirectory", rootDirectory.toString());
        }
        Map<String, String> userProperties = populateUserProperties(systemProperties, fileSystem, paths, mavenOptions);

        // options: interpolate
        mavenOptions = mavenOptions.interpolate(Arrays.asList(paths, systemProperties, userProperties));

        // core extensions
        ArrayList<CoreExtension> extensions = new ArrayList<>();
        String installationExtensionsFile = userProperties.get(Constants.MAVEN_INSTALLATION_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(installationExtensionsFile, fileSystem));

        String projectExtensionsFile = userProperties.get(Constants.MAVEN_PROJECT_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(projectExtensionsFile, fileSystem));

        String userExtensionsFile = userProperties.get(Constants.MAVEN_USER_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(userExtensionsFile, fileSystem));

        return new DefaultMavenInvokerRequest(
                cwd,
                installationDirectory,
                userHomeDirectory,
                userProperties,
                systemProperties,
                parserRequest.logger(),
                parserRequest.messageBuilderFactory(),
                topDirectory,
                rootDirectory,
                parserRequest.in(),
                parserRequest.out(),
                parserRequest.err(),
                extensions,
                mavenOptions);
    }

    protected FileSystem getFileSystem(ParserRequest parserRequest) throws ParserException, IOException {
        return parserRequest.cwd() != null ? parserRequest.cwd().getFileSystem() : FileSystems.getDefault();
    }

    protected Path getCwd(ParserRequest parserRequest, FileSystem fileSystem, Map<String, String> overrides)
            throws ParserException {
        if (parserRequest.cwd() != null) {
            Path result = getCanonicalPath(parserRequest.cwd());
            overrides.put("user.dir", result.toString());
            return result;
        } else {
            return getCanonicalPath(fileSystem.getPath(System.getProperty("user.dir")));
        }
    }

    protected Path getInstallationDirectory(
            ParserRequest parserRequest, FileSystem fileSystem, Map<String, String> overrides) throws ParserException {
        Path result;
        if (parserRequest.mavenHome() != null) {
            result = getCanonicalPath(parserRequest.mavenHome());
            overrides.put(MAVEN_HOME, result.toString());
        } else {
            String mavenHome = System.getProperty(Constants.MAVEN_HOME);
            if (mavenHome == null) {
                throw new ParserException("local mode requires " + Constants.MAVEN_HOME + " Java System Property set");
            }
            result = getCanonicalPath(fileSystem.getPath(mavenHome));
        }
        // TODO: we still do this but would be cool if this becomes unneeded
        System.setProperty(Constants.MAVEN_HOME, result.toString());
        return result;
    }

    protected Path getUserHomeDirectory(
            ParserRequest parserRequest, FileSystem fileSystem, Map<String, String> overrides) throws ParserException {
        if (parserRequest.userHome() != null) {
            Path result = getCanonicalPath(parserRequest.userHome());
            overrides.put("user.home", result.toString());
            return result;
        } else {
            return getCanonicalPath(fileSystem.getPath(System.getProperty("user.home")));
        }
    }

    protected Path getTopDirectory(ParserRequest parserRequest, Path cwd) throws ParserException {
        // We need to locate the top level project which may be pointed at using
        // the -f/--file option.
        Path topDirectory = cwd;
        boolean isAltFile = false;
        for (String arg : parserRequest.args()) {
            if (isAltFile) {
                // this is the argument following -f/--file
                Path path = topDirectory.resolve(stripLeadingAndTrailingQuotes(arg));
                if (Files.isDirectory(path)) {
                    topDirectory = path;
                } else if (Files.isRegularFile(path)) {
                    topDirectory = path.getParent();
                    if (!Files.isDirectory(topDirectory)) {
                        throw new ParserException("Directory " + topDirectory
                                + " extracted from the -f/--file command-line argument " + arg + " does not exist");
                    }
                } else {
                    throw new ParserException(
                            "POM file " + arg + " specified with the -f/--file command line argument does not exist");
                }
                break;
            } else {
                // Check if this is the -f/--file option
                isAltFile = arg.equals("-f") || arg.equals("--file");
            }
        }
        return topDirectory;
    }

    protected Map<String, String> populateSystemProperties(Map<String, String> overrides) throws ParserException {
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
        systemProperties.setProperty("maven.version", mavenVersion);

        String mavenBuildVersion = CLIReportingUtils.createMavenVersionString(buildProperties);
        systemProperties.setProperty("maven.build.version", mavenBuildVersion);

        Map<String, String> result = toMap(systemProperties);
        if (overrides != null) {
            result.putAll(overrides);
        }
        return result;
    }

    protected Map<String, String> populateUserProperties(
            Map<String, String> systemProperties,
            FileSystem fileSystem,
            Map<String, String> paths,
            MavenOptions mavenOptions)
            throws ParserException, IOException {
        Properties userProperties = new Properties();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        Map<String, String> userSpecifiedProperties =
                mavenOptions.userProperties().orElse(new HashMap<>());
        userProperties.putAll(userSpecifiedProperties);

        // ----------------------------------------------------------------------
        // Load config files
        // ----------------------------------------------------------------------
        Function<String, String> callback =
                or(paths::get, prefix("cli.", userSpecifiedProperties::get), systemProperties::get);

        Path mavenConf;
        if (systemProperties.get(MAVEN_INSTALLATION_CONF) != null) {
            mavenConf = fileSystem.getPath(systemProperties.get(MAVEN_INSTALLATION_CONF));
        } else if (systemProperties.get("maven.conf") != null) {
            mavenConf = fileSystem.getPath(systemProperties.get("maven.conf"));
        } else if (systemProperties.get(MAVEN_HOME) != null) {
            mavenConf = fileSystem.getPath(systemProperties.get(MAVEN_HOME), "conf");
        } else {
            mavenConf = fileSystem.getPath("");
        }
        Path propertiesFile = mavenConf.resolve("maven.properties");
        MavenPropertiesLoader.loadProperties(userProperties, propertiesFile, callback, false);

        // ----------------------------------------------------------------------
        // I'm leaving the setting of system properties here as not to break
        // the SystemPropertyProfileActivator. This won't harm embedding. jvz.
        // ----------------------------------------------------------------------
        Set<String> sys = SystemProperties.getSystemProperties().stringPropertyNames();
        userProperties.stringPropertyNames().stream()
                .filter(k -> !sys.contains(k))
                .forEach(k -> System.setProperty(k, userProperties.getProperty(k)));

        return toMap(userProperties);
    }

    protected CommonsCliMavenOptions parseCliOptions(CLIManager cliManager, String[] args) throws ParserException {
        try {
            return new CommonsCliMavenOptions(cliManager, cliManager.parse(args));
        } catch (ParseException e) {
            throw new ParserException("Failed to parse command line options: " + e.getMessage(), e);
        }
    }

    protected CommonsCliMavenOptions parseConfigOptions(CLIManager cliManager, Path configFile)
            throws ParserException, IOException {
        try (Stream<String> lines = Files.lines(configFile, Charset.defaultCharset())) {
            String[] args =
                    lines.filter(arg -> !arg.isEmpty() && !arg.startsWith("#")).toArray(String[]::new);
            CommandLine mavenConfig = cliManager.parse(args);
            List<String> goals = mavenConfig.getArgList();
            if (!goals.isEmpty()) {
                // This file can only contain options, not args (goals or phases)
                throw new ParserException("Unrecognized maven.config file entries: " + goals);
            }
            return new CommonsCliMavenOptions(cliManager, mavenConfig);
        } catch (ParseException e) {
            throw new ParserException("Failed to parse maven.config file: " + e.getMessage(), e);
        }
    }

    protected void warnAboutDeprecatedOptions(
            ParserRequest parserRequest, String location, CommonsCliMavenOptions options) {
        if (options.getUsedDeprecatedOptions().isEmpty()) {
            return;
        }
        parserRequest.logger().warn("Detected deprecated option use in {}", location);
        for (Option option : options.getUsedDeprecatedOptions()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The option -").append(option.getOpt());
            if (option.getLongOpt() != null) {
                sb.append(",--").append(option.getLongOpt());
            }
            sb.append(" is deprecated ");
            if (option.getDeprecated().isForRemoval()) {
                sb.append("and will be removed in a future version");
            }
            if (option.getDeprecated().getSince() != null) {
                sb.append("since Maven ").append(option.getDeprecated().getSince());
            }
            parserRequest.logger().warn(sb.toString());
        }
    }

    protected List<CoreExtension> readCoreExtensionsDescriptor(String extensionsFile, FileSystem fileSystem)
            throws ParserException, IOException {
        try {
            if (extensionsFile != null) {
                Path extensionsPath = fileSystem.getPath(extensionsFile);
                if (Files.exists(extensionsPath)) {
                    try (InputStream is = Files.newInputStream(extensionsPath)) {
                        return new CoreExtensionsStaxReader().read(is, true).getExtensions();
                    }
                }
            }
            return List.of();
        } catch (XMLStreamException e) {
            throw new ParserException("Failed to parse extensions file: " + extensionsFile, e);
        }
    }
}
