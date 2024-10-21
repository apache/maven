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
import java.util.Properties;
import java.util.function.Function;

import org.apache.maven.api.Constants;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.cli.internal.extension.io.CoreExtensionsStaxReader;
import org.apache.maven.cli.props.MavenPropertiesLoader;
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
    public R parse(ParserRequest parserRequest) throws ParserException, IOException {
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
        List<O> parsedOptions = parseCliOptions(context);

        // warn about deprecated options
        PrintWriter printWriter = new PrintWriter(parserRequest.out() != null ? parserRequest.out() : System.out, true);
        parsedOptions.forEach(o -> o.warnAboutDeprecatedOptions(parserRequest, printWriter::println));

        // assemble options if needed
        context.options = assembleOptions(parsedOptions);

        // system and user properties
        context.systemProperties = populateSystemProperties(context);
        context.userProperties = populateUserProperties(context);

        // options: interpolate
        context.options = context.options.interpolate(
                Arrays.asList(context.extraInterpolationSource(), context.userProperties, context.systemProperties));

        // core extensions
        context.extensions = readCoreExtensionsDescriptor(context);

        return getInvokerRequest(context);
    }

    protected abstract R getInvokerRequest(LocalContext context);

    protected Path getCwd(LocalContext context) throws ParserException {
        if (context.parserRequest.cwd() != null) {
            Path result = getCanonicalPath(context.parserRequest.cwd());
            context.systemPropertiesOverrides.put("user.dir", result.toString());
            return result;
        } else {
            return getCanonicalPath(Paths.get(System.getProperty("user.dir")));
        }
    }

    protected Path getInstallationDirectory(LocalContext context) throws ParserException {
        Path result;
        if (context.parserRequest.mavenHome() != null) {
            result = getCanonicalPath(context.parserRequest.mavenHome());
            context.systemPropertiesOverrides.put(MAVEN_HOME, result.toString());
        } else {
            String mavenHome = System.getProperty(Constants.MAVEN_HOME);
            if (mavenHome == null) {
                throw new ParserException("local mode requires " + Constants.MAVEN_HOME + " Java System Property set");
            }
            result = getCanonicalPath(Paths.get(mavenHome));
        }
        return result;
    }

    protected Path getUserHomeDirectory(LocalContext context) throws ParserException {
        if (context.parserRequest.userHome() != null) {
            Path result = getCanonicalPath(context.parserRequest.userHome());
            context.systemPropertiesOverrides.put("user.home", result.toString());
            return result;
        } else {
            return getCanonicalPath(Paths.get(System.getProperty("user.home")));
        }
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
        return Utils.findRoot(context.topDirectory);
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
        Function<String, String> callback =
                or(paths::get, prefix("cli.", userSpecifiedProperties::get), context.systemProperties::get);

        Path mavenConf;
        if (context.systemProperties.get(MAVEN_INSTALLATION_CONF) != null) {
            mavenConf = context.installationDirectory.resolve(context.systemProperties.get(MAVEN_INSTALLATION_CONF));
        } else if (context.systemProperties.get("maven.conf") != null) {
            mavenConf = context.installationDirectory.resolve(context.systemProperties.get("maven.conf"));
        } else if (context.systemProperties.get(MAVEN_HOME) != null) {
            mavenConf = context.installationDirectory
                    .resolve(context.systemProperties.get(MAVEN_HOME))
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

    protected abstract List<O> parseCliOptions(LocalContext context) throws ParserException, IOException;

    protected abstract O assembleOptions(List<O> parsedOptions);

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
}
