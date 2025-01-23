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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.maven.api.Constants;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.cling.internal.extension.io.CoreExtensionsStaxReader;
import org.apache.maven.cling.props.MavenPropertiesLoader;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.properties.internal.SystemProperties;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.InvokerUtils.getCanonicalPath;
import static org.apache.maven.cling.invoker.InvokerUtils.or;
import static org.apache.maven.cling.invoker.InvokerUtils.prefix;
import static org.apache.maven.cling.invoker.InvokerUtils.toMap;

public abstract class BaseParser implements Parser {

    @Nullable
    private static Path findRoot(Path topDirectory) {
        // TODO is this OK? Tracing through the code it looks like topDirectory is nullable
        requireNonNull(topDirectory, "topDirectory");
        Path rootDirectory =
                ServiceLoader.load(RootLocator.class).iterator().next().findRoot(topDirectory);
        if (rootDirectory != null) {
            return getCanonicalPath(rootDirectory);
        }
        return null;
    }

    @SuppressWarnings("VisibilityModifier")
    public static class LocalContext {
        public final ParserRequest parserRequest;
        public final Map<String, String> systemPropertiesOverrides;

        public LocalContext(ParserRequest parserRequest) {
            this.parserRequest = parserRequest;
            this.systemPropertiesOverrides = new HashMap<>();
        }

        public Path cwd;
        public Path installationDirectory;
        public Path userHomeDirectory;
        public Map<String, String> systemProperties;
        public Map<String, String> userProperties;
        public Path topDirectory;

        @Nullable
        public Path rootDirectory;

        public List<CoreExtension> extensions;
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
    public InvokerRequest parseInvocation(ParserRequest parserRequest) throws ParserException, IOException {
        requireNonNull(parserRequest);

        LocalContext context = new LocalContext(parserRequest);

        // the basics
        context.cwd = requireNonNull(getCwd(context));
        context.installationDirectory = requireNonNull(getInstallationDirectory(context));
        context.userHomeDirectory = requireNonNull(getUserHomeDirectory(context));

        // top/root
        context.topDirectory = requireNonNull(getTopDirectory(context));
        context.rootDirectory = getRootDirectory(context);

        // options
        List<Options> parsedOptions = parseCliOptions(context);

        // warn about deprecated options
        PrintWriter printWriter = new PrintWriter(parserRequest.out() != null ? parserRequest.out() : System.out, true);
        parsedOptions.forEach(o -> o.warnAboutDeprecatedOptions(parserRequest, printWriter::println));

        // assemble options if needed
        context.options = assembleOptions(parsedOptions);

        // system and user properties
        context.systemProperties = populateSystemProperties(context);
        context.userProperties = populateUserProperties(context);

        // options: interpolate
        context.options = context.options.interpolate(Interpolator.chain(
                context.extraInterpolationSource()::get, context.userProperties::get, context.systemProperties::get));

        // core extensions
        context.extensions = readCoreExtensionsDescriptor(context);

        return getInvokerRequest(context);
    }

    protected abstract InvokerRequest getInvokerRequest(LocalContext context);

    protected Path getCwd(LocalContext context) throws ParserException {
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

    protected Path getInstallationDirectory(LocalContext context) throws ParserException {
        if (context.parserRequest.mavenHome() != null) {
            Path result = getCanonicalPath(context.parserRequest.mavenHome());
            context.systemPropertiesOverrides.put(Constants.MAVEN_HOME, result.toString());
            return result;
        } else {
            String mavenHome = System.getProperty(Constants.MAVEN_HOME);
            if (mavenHome == null) {
                throw new ParserException("local mode requires " + Constants.MAVEN_HOME + " Java System Property set");
            }
            Path result = getCanonicalPath(Paths.get(mavenHome));
            mayOverrideDirectorySystemProperty(context, Constants.MAVEN_HOME, result);
            return result;
        }
    }

    protected Path getUserHomeDirectory(LocalContext context) throws ParserException {
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

    @Nonnull
    private static String stripLeadingAndTrailingQuotes(String str) {
        requireNonNull(str, "str");
        final int length = str.length();
        if (length > 1
                && str.startsWith("\"")
                && str.endsWith("\"")
                && str.substring(1, length - 1).indexOf('"') == -1) {
            str = str.substring(1, length - 1);
        }
        return str;
    }

    protected Path getTopDirectory(LocalContext context) throws ParserException {
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

    @Nullable
    protected Path getRootDirectory(LocalContext context) throws ParserException {
        return findRoot(context.topDirectory);
    }

    protected Map<String, String> populateSystemProperties(LocalContext context) throws ParserException {
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
        result.putAll(context.systemPropertiesOverrides);
        return result;
    }

    protected Map<String, String> populateUserProperties(LocalContext context) throws ParserException, IOException {
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
        MavenPropertiesLoader.loadProperties(userProperties, propertiesFile, callback, false);

        // CLI specified properties are most dominant
        userProperties.putAll(userSpecifiedProperties);

        return toMap(userProperties);
    }

    protected abstract List<Options> parseCliOptions(LocalContext context) throws ParserException, IOException;

    protected abstract Options assembleOptions(List<Options> parsedOptions);

    protected List<CoreExtension> readCoreExtensionsDescriptor(LocalContext context)
            throws ParserException, IOException {
        ArrayList<CoreExtension> extensions = new ArrayList<>();
        String installationExtensionsFile = context.userProperties.get(Constants.MAVEN_INSTALLATION_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptorFromFile(
                context.installationDirectory.resolve(installationExtensionsFile)));

        String projectExtensionsFile = context.userProperties.get(Constants.MAVEN_PROJECT_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptorFromFile(context.cwd.resolve(projectExtensionsFile)));

        String userExtensionsFile = context.userProperties.get(Constants.MAVEN_USER_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptorFromFile(context.userHomeDirectory.resolve(userExtensionsFile)));

        return extensions;
    }

    protected List<CoreExtension> readCoreExtensionsDescriptorFromFile(Path extensionsFile)
            throws ParserException, IOException {
        try {
            if (extensionsFile != null && Files.exists(extensionsFile)) {
                try (InputStream is = Files.newInputStream(extensionsFile)) {
                    return new CoreExtensionsStaxReader().read(is, true).getExtensions();
                }
            }
            return List.of();
        } catch (XMLStreamException e) {
            throw new ParserException("Failed to parse extensions file: " + extensionsFile, e);
        }
    }

    protected List<String> getJvmArguments(Path rootDirectory) throws ParserException {
        if (rootDirectory != null) {
            Path jvmConfig = rootDirectory.resolve(".mvn/jvm.config");
            if (Files.exists(jvmConfig)) {
                try {
                    return Files.readAllLines(jvmConfig).stream()
                            .filter(l -> !l.isBlank() && !l.startsWith("#"))
                            .flatMap(l -> Arrays.stream(l.split(" ")))
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    throw new ParserException("Failed to read JVM configuration file: " + jvmConfig, e);
                }
            }
        }
        return null;
    }
}
