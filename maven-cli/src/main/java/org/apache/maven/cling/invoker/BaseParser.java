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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.apache.maven.api.Constants;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.cli.internal.extension.io.CoreExtensionsStaxReader;
import org.apache.maven.cli.props.MavenPropertiesLoader;
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

public abstract class BaseParser<O extends Options, R extends InvokerRequest<O>> implements Parser<R> {
    @Override
    public R parse(ParserRequest parserRequest) throws ParserException, IOException {
        requireNonNull(parserRequest);

        // the basics
        HashMap<String, String> overrides = new HashMap<>();
        Path cwd = requireNonNull(getCwd(parserRequest, overrides));
        Path installationDirectory = requireNonNull(getInstallationDirectory(parserRequest, overrides));
        Path userHomeDirectory = requireNonNull(getUserHomeDirectory(parserRequest, overrides));

        // top/root
        Path topDirectory = requireNonNull(getTopDirectory(parserRequest, cwd));
        Path rootDirectory = requireNonNull(getRootDirectory(parserRequest, cwd, topDirectory));

        // options
        List<O> parsedOptions = parseCliOptions(rootDirectory, parserRequest.args());

        // warn about deprecated options
        parsedOptions.forEach(o -> o.warnAboutDeprecatedOptions(
                parserRequest, new PrintWriter(parserRequest.out() != null ? parserRequest.out() : System.out, true)));

        // assemble options if needed
        O options = assembleOptions(parsedOptions);

        // system and user properties
        Map<String, String> systemProperties = populateSystemProperties(overrides);
        Map<String, String> paths = new HashMap<>();
        paths.put("session.topDirectory", topDirectory.toString());
        paths.put("session.rootDirectory", rootDirectory.toString());
        Map<String, String> userProperties =
                populateUserProperties(systemProperties, installationDirectory, paths, options);

        // options: interpolate
        Options interpolatedOptions = options.interpolate(Arrays.asList(paths, systemProperties, userProperties));

        // core extensions
        ArrayList<CoreExtension> extensions = new ArrayList<>();
        String installationExtensionsFile = userProperties.get(Constants.MAVEN_INSTALLATION_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(installationExtensionsFile, installationDirectory));

        String projectExtensionsFile = userProperties.get(Constants.MAVEN_PROJECT_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(projectExtensionsFile, cwd));

        String userExtensionsFile = userProperties.get(Constants.MAVEN_USER_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(userExtensionsFile, userHomeDirectory));

        return getInvokerRequest(
                parserRequest,
                cwd,
                installationDirectory,
                userHomeDirectory,
                userProperties,
                systemProperties,
                topDirectory,
                rootDirectory,
                extensions,
                interpolatedOptions);
    }

    @SuppressWarnings("ParameterNumber")
    protected abstract R getInvokerRequest(
            ParserRequest parserRequest,
            Path cwd,
            Path installationDirectory,
            Path userHomeDirectory,
            Map<String, String> userProperties,
            Map<String, String> systemProperties,
            Path topDirectory,
            Path rootDirectory,
            ArrayList<CoreExtension> extensions,
            Options options);

    protected Path getCwd(ParserRequest parserRequest, Map<String, String> overrides) throws ParserException {
        if (parserRequest.cwd() != null) {
            Path result = getCanonicalPath(parserRequest.cwd());
            overrides.put("user.dir", result.toString());
            return result;
        } else {
            return getCanonicalPath(Paths.get(System.getProperty("user.dir")));
        }
    }

    protected Path getInstallationDirectory(ParserRequest parserRequest, Map<String, String> overrides)
            throws ParserException {
        Path result;
        if (parserRequest.mavenHome() != null) {
            result = getCanonicalPath(parserRequest.mavenHome());
            overrides.put(MAVEN_HOME, result.toString());
        } else {
            String mavenHome = System.getProperty(Constants.MAVEN_HOME);
            if (mavenHome == null) {
                throw new ParserException("local mode requires " + Constants.MAVEN_HOME + " Java System Property set");
            }
            result = getCanonicalPath(Paths.get(mavenHome));
        }
        return result;
    }

    protected Path getUserHomeDirectory(ParserRequest parserRequest, Map<String, String> overrides)
            throws ParserException {
        if (parserRequest.userHome() != null) {
            Path result = getCanonicalPath(parserRequest.userHome());
            overrides.put("user.home", result.toString());
            return result;
        } else {
            return getCanonicalPath(Paths.get(System.getProperty("user.home")));
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
        return getCanonicalPath(topDirectory);
    }

    protected Path getRootDirectory(ParserRequest parserRequest, Path cwd, Path topDirectory) throws ParserException {
        RootLocator rootLocator =
                ServiceLoader.load(RootLocator.class).iterator().next();
        Path rootDirectory = rootLocator.findRoot(topDirectory);

        // TODO: multiModuleProjectDirectory vs rootDirectory?
        // fallback if no root? otherwise make sure they are same?
        Path mmpd = System.getProperty("maven.multiModuleProjectDirectory") == null
                ? null
                : getCanonicalPath(cwd.resolve(requireNonNull(
                        System.getProperty("maven.multiModuleProjectDirectory"),
                        "maven.multiModuleProjectDirectory is not set")));
        if (rootDirectory == null) {
            parserRequest.logger().warn(rootLocator.getNoRootMessage());
            rootDirectory = requireNonNull(
                    mmpd, "maven.multiModuleProjectDirectory is not set and rootDirectory was not discovered");
        } else {
            rootDirectory = getCanonicalPath(rootDirectory);
            if (mmpd != null && !Objects.equals(rootDirectory, mmpd)) {
                parserRequest.logger().warn("Project root directory and multiModuleProjectDirectory are not aligned");
            }
        }
        return getCanonicalPath(rootDirectory);
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
            Path installationDirectory,
            Map<String, String> paths,
            Options options)
            throws ParserException, IOException {
        Properties userProperties = new Properties();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        Map<String, String> userSpecifiedProperties = options.userProperties().orElse(new HashMap<>());
        userProperties.putAll(userSpecifiedProperties);

        // ----------------------------------------------------------------------
        // Load config files
        // ----------------------------------------------------------------------
        Function<String, String> callback =
                or(paths::get, prefix("cli.", userSpecifiedProperties::get), systemProperties::get);

        Path mavenConf;
        if (systemProperties.get(MAVEN_INSTALLATION_CONF) != null) {
            mavenConf = installationDirectory.resolve(systemProperties.get(MAVEN_INSTALLATION_CONF));
        } else if (systemProperties.get("maven.conf") != null) {
            mavenConf = installationDirectory.resolve(systemProperties.get("maven.conf"));
        } else if (systemProperties.get(MAVEN_HOME) != null) {
            mavenConf = installationDirectory
                    .resolve(systemProperties.get(MAVEN_HOME))
                    .resolve("conf");
        } else {
            mavenConf = installationDirectory.resolve("");
        }
        Path propertiesFile = mavenConf.resolve("maven.properties");
        MavenPropertiesLoader.loadProperties(userProperties, propertiesFile, callback, false);

        return toMap(userProperties);
    }

    protected abstract List<O> parseCliOptions(Path rootDirectory, List<String> args)
            throws ParserException, IOException;

    protected abstract O assembleOptions(List<O> parsedOptions);

    protected List<CoreExtension> readCoreExtensionsDescriptor(String extensionsFile, Path cwd)
            throws ParserException, IOException {
        try {
            if (extensionsFile != null) {
                Path extensionsPath = cwd.resolve(extensionsFile);
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
