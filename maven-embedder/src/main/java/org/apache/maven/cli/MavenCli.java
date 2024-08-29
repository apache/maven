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
package org.apache.maven.cli;

import javax.xml.stream.XMLStreamException;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.inject.AbstractModule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.maven.BuildAbort;
import org.apache.maven.InternalErrorException;
import org.apache.maven.Maven;
import org.apache.maven.api.Constants;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Problem;
import org.apache.maven.building.Source;
import org.apache.maven.cli.configuration.ConfigurationProcessor;
import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;
import org.apache.maven.cli.event.DefaultEventSpyContext;
import org.apache.maven.cli.event.ExecutionEventLogger;
import org.apache.maven.cli.internal.BootstrapCoreExtensionManager;
import org.apache.maven.cli.internal.extension.io.CoreExtensionsStaxReader;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.cli.logging.Slf4jConfigurationFactory;
import org.apache.maven.cli.logging.Slf4jLoggerManager;
import org.apache.maven.cli.logging.Slf4jStdoutLogger;
import org.apache.maven.cli.props.MavenPropertiesLoader;
import org.apache.maven.cli.transfer.ConsoleMavenTransferListener;
import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.cli.transfer.SimplexTransferListener;
import org.apache.maven.cli.transfer.Slf4jMavenTransferListener;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ProfileActivation;
import org.apache.maven.execution.ProjectActivation;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.execution.scope.internal.MojoExecutionScopeModule;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.internal.impl.SisuDiBridgeModule;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.logwrapper.LogLevelRecorder;
import org.apache.maven.logwrapper.MavenSlf4jWrapperFactory;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.properties.internal.SystemProperties;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.session.scope.internal.SessionScopeModule;
import org.apache.maven.toolchain.building.DefaultToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuilder;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.interpolation.BasicInterpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.logging.LoggerManager;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.transfer.TransferListener;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

import static java.util.Comparator.comparing;
import static org.apache.maven.api.Constants.MAVEN_HOME;
import static org.apache.maven.api.Constants.MAVEN_INSTALLATION_CONF;
import static org.apache.maven.cli.CLIManager.BATCH_MODE;
import static org.apache.maven.cli.CLIManager.COLOR;
import static org.apache.maven.cli.CLIManager.FORCE_INTERACTIVE;
import static org.apache.maven.cli.CLIManager.NON_INTERACTIVE;
import static org.apache.maven.cli.ResolveFile.resolveFile;

// TODO push all common bits back to plexus cli and prepare for transition to Guice. We don't need 50 ways to make CLIs

/**
 */
public class MavenCli {

    public static final String MULTIMODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";

    private static final String MVN_MAVEN_CONFIG = ".mvn/maven.config";

    private ClassWorld classWorld;

    private LoggerManager plexusLoggerManager;

    private ILoggerFactory slf4jLoggerFactory;

    private Logger slf4jLogger;

    private EventSpyDispatcher eventSpyDispatcher;

    private ModelProcessor modelProcessor;

    private Maven maven;

    private MavenExecutionRequestPopulator executionRequestPopulator;

    private ToolchainsBuilder toolchainsBuilder;

    private DefaultSecDispatcher dispatcher;

    private Map<String, ConfigurationProcessor> configurationProcessors;

    private CLIManager cliManager;

    private MessageBuilderFactory messageBuilderFactory;

    private FileSystem fileSystem = FileSystems.getDefault();

    private static final Pattern NEXT_LINE = Pattern.compile("\r?\n");

    public MavenCli() {
        this(null);
    }

    // This supports painless invocation by the Verifier during embedded execution of the core ITs
    public MavenCli(ClassWorld classWorld) {
        this.classWorld = classWorld;
        this.messageBuilderFactory = new JLineMessageBuilderFactory();
    }

    public static void main(String[] args) {
        int result = main(args, null);

        System.exit(result);
    }

    public static int main(String[] args, ClassWorld classWorld) {
        MavenCli cli = new MavenCli();

        MessageUtils.systemInstall();
        MessageUtils.registerShutdownHook();
        int result = cli.doMain(new CliRequest(args, classWorld));
        MessageUtils.systemUninstall();

        return result;
    }

    // TODO need to externalize CliRequest
    public static int doMain(String[] args, ClassWorld classWorld) {
        MavenCli cli = new MavenCli();
        return cli.doMain(new CliRequest(args, classWorld));
    }

    /**
     * This supports painless invocation by the Verifier during embedded execution of the core ITs.
     * See <a href="http://maven.apache.org/shared/maven-verifier/xref/org/apache/maven/it/Embedded3xLauncher.html">
     * <code>Embedded3xLauncher</code> in <code>maven-verifier</code></a>
     *
     * @param args CLI args
     * @param workingDirectory working directory
     * @param stdout stdout
     * @param stderr stderr
     * @return return code
     */
    public int doMain(String[] args, String workingDirectory, PrintStream stdout, PrintStream stderr) {
        PrintStream oldout = System.out;
        PrintStream olderr = System.err;

        final Set<String> realms;
        if (classWorld != null) {
            realms = new HashSet<>();
            for (ClassRealm realm : classWorld.getRealms()) {
                realms.add(realm.getId());
            }
        } else {
            realms = Collections.emptySet();
        }

        try {
            if (stdout != null) {
                System.setOut(stdout);
            }
            if (stderr != null) {
                System.setErr(stderr);
            }

            CliRequest cliRequest = new CliRequest(args, classWorld);
            cliRequest.workingDirectory = workingDirectory;

            return doMain(cliRequest);
        } finally {
            if (classWorld != null) {
                for (ClassRealm realm : new ArrayList<>(classWorld.getRealms())) {
                    String realmId = realm.getId();
                    if (!realms.contains(realmId)) {
                        try {
                            classWorld.disposeRealm(realmId);
                        } catch (NoSuchRealmException ignored) {
                            // can't happen
                        }
                    }
                }
            }
            System.setOut(oldout);
            System.setErr(olderr);
        }
    }

    // TODO need to externalize CliRequest
    public int doMain(CliRequest cliRequest) {
        PlexusContainer localContainer = null;
        try {
            initialize(cliRequest);
            cli(cliRequest);
            properties(cliRequest);
            logging(cliRequest);
            informativeCommands(cliRequest);
            version(cliRequest);
            localContainer = container(cliRequest);
            commands(cliRequest);
            configure(cliRequest);
            toolchains(cliRequest);
            populateRequest(cliRequest);
            encryption(cliRequest);
            return execute(cliRequest);
        } catch (ExitException e) {
            return e.exitCode;
        } catch (UnrecognizedOptionException e) {
            // pure user error, suppress stack trace
            return 1;
        } catch (BuildAbort e) {
            CLIReportingUtils.showError(slf4jLogger, "ABORTED", e, cliRequest.showErrors);

            return 2;
        } catch (Exception e) {
            CLIReportingUtils.showError(slf4jLogger, "Error executing Maven.", e, cliRequest.showErrors);

            return 1;
        } finally {
            if (localContainer != null) {
                localContainer.dispose();
            }
        }
    }

    void initialize(CliRequest cliRequest) throws ExitException {
        if (cliRequest.workingDirectory == null) {
            cliRequest.workingDirectory = System.getProperty("user.dir");
        }

        if (cliRequest.multiModuleProjectDirectory == null) {
            String basedirProperty = System.getProperty(MULTIMODULE_PROJECT_DIRECTORY);
            if (basedirProperty == null) {
                System.err.format("-D%s system property is not set.", MULTIMODULE_PROJECT_DIRECTORY);
                throw new ExitException(1);
            }
            File basedir = new File(basedirProperty);
            try {
                cliRequest.multiModuleProjectDirectory = basedir.getCanonicalFile();
            } catch (IOException e) {
                cliRequest.multiModuleProjectDirectory = basedir.getAbsoluteFile();
            }
        }

        // We need to locate the top level project which may be pointed at using
        // the -f/--file option.  However, the command line isn't parsed yet, so
        // we need to iterate through the args to find it and act upon it.
        Path topDirectory = fileSystem.getPath(cliRequest.workingDirectory);
        boolean isAltFile = false;
        for (String arg : cliRequest.args) {
            if (isAltFile) {
                // this is the argument following -f/--file
                Path path = topDirectory.resolve(stripLeadingAndTrailingQuotes(arg));
                if (Files.isDirectory(path)) {
                    topDirectory = path;
                } else if (Files.isRegularFile(path)) {
                    topDirectory = path.getParent();
                    if (!Files.isDirectory(topDirectory)) {
                        System.err.println("Directory " + topDirectory
                                + " extracted from the -f/--file command-line argument " + arg + " does not exist");
                        throw new ExitException(1);
                    }
                } else {
                    System.err.println(
                            "POM file " + arg + " specified with the -f/--file command line argument does not exist");
                    throw new ExitException(1);
                }
                break;
            } else {
                // Check if this is the -f/--file option
                isAltFile = arg.equals("-f") || arg.equals("--file");
            }
        }
        topDirectory = getCanonicalPath(topDirectory);
        cliRequest.topDirectory = topDirectory;
        // We're very early in the process, and we don't have the container set up yet,
        // so we rely on the JDK services to eventually look up a custom RootLocator.
        // This is used to compute {@code session.rootDirectory} but all {@code project.rootDirectory}
        // properties will be computed through the RootLocator found in the container.
        RootLocator rootLocator =
                ServiceLoader.load(RootLocator.class).iterator().next();
        cliRequest.rootDirectory = rootLocator.findRoot(topDirectory);

        //
        // Make sure the Maven home directory is an absolute path to save us from confusion with say drive-relative
        // Windows paths.
        //
        String mavenHome = System.getProperty(Constants.MAVEN_HOME);

        if (mavenHome != null) {
            System.setProperty(
                    Constants.MAVEN_HOME,
                    getCanonicalPath(fileSystem.getPath(mavenHome)).toString());
        }
    }

    void cli(CliRequest cliRequest) throws Exception {
        //
        // Parsing errors can happen during the processing of the arguments and we prefer not having to check if
        // the logger is null and construct this so we can use an SLF4J logger everywhere.
        //
        slf4jLogger = new Slf4jStdoutLogger();

        cliManager = new CLIManager();

        CommandLine mavenConfig = null;
        try {
            File configFile = new File(cliRequest.multiModuleProjectDirectory, MVN_MAVEN_CONFIG);

            if (configFile.isFile()) {
                try (Stream<String> lines = Files.lines(configFile.toPath(), Charset.defaultCharset())) {
                    String[] args = lines.filter(arg -> !arg.isEmpty() && !arg.startsWith("#"))
                            .toArray(String[]::new);
                    mavenConfig = cliManager.parse(args);
                    List<?> unrecognized = mavenConfig.getArgList();
                    if (!unrecognized.isEmpty()) {
                        // This file can only contain options, not args (goals or phases)
                        throw new ParseException("Unrecognized maven.config file entries: " + unrecognized);
                    }
                }
            }
        } catch (ParseException e) {
            System.err.println("Unable to parse maven.config file options: " + e.getMessage());
            cliManager.displayHelp(System.out);
            throw e;
        }

        try {
            CommandLine mavenCli = cliManager.parse(cliRequest.args);
            if (mavenConfig == null) {
                cliRequest.commandLine = mavenCli;
            } else {
                cliRequest.commandLine = cliMerge(mavenConfig, mavenCli);
            }
        } catch (ParseException e) {
            System.err.println("Unable to parse command line options: " + e.getMessage());
            cliManager.displayHelp(System.out);
            throw e;
        }

        // check for presence of unsupported command line options
        try {
            if (cliRequest.commandLine.hasOption("llr")) {
                throw new UnrecognizedOptionException("Option '-llr' is not supported starting with Maven 3.9.1");
            }
        } catch (ParseException e) {
            System.err.println("Unsupported options: " + e.getMessage());
            cliManager.displayHelp(System.out);
            throw e;
        }
    }

    private void informativeCommands(CliRequest cliRequest) throws ExitException {
        if (cliRequest.commandLine.hasOption(CLIManager.HELP)) {
            cliManager.displayHelp(System.out);
            throw new ExitException(0);
        }

        if (cliRequest.commandLine.hasOption(CLIManager.VERSION)) {
            if (cliRequest.commandLine.hasOption(CLIManager.QUIET)) {
                System.out.println(CLIReportingUtils.showVersionMinimal());
            } else {
                System.out.println(CLIReportingUtils.showVersion());
            }
            throw new ExitException(0);
        }

        if (cliRequest.rootDirectory == null) {
            slf4jLogger.info(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE);
        }
    }

    private CommandLine cliMerge(CommandLine mavenConfig, CommandLine mavenCli) {
        CommandLine.Builder commandLineBuilder = new CommandLine.Builder();

        // the args are easy, CLI only since maven.config file can only contain options
        for (String arg : mavenCli.getArgs()) {
            commandLineBuilder.addArg(arg);
        }

        /* Although this looks wrong in terms of order Commons CLI stores the value of options in
         * an array and when a value is potentionally overriden it is added to the array. The single
         * arg option value is retrieved and instead of returning values[values.length-1] it returns
         * values[0] which means that the original value instead of the overridden one is returned
         * (first wins). With properties values are truely overriden since at the end a map is used
         * to merge which means last wins.
         *
         * TODO Report this behavioral bug with Commons CLI
         */
        // now add all options, except for user properties with CLI first then maven.config file
        List<Option> setPropertyOptions = new ArrayList<>();
        for (Option opt : mavenCli.getOptions()) {
            if (String.valueOf(CLIManager.SET_USER_PROPERTY).equals(opt.getOpt())) {
                setPropertyOptions.add(opt);
            } else {
                commandLineBuilder.addOption(opt);
            }
        }
        for (Option opt : mavenConfig.getOptions()) {
            commandLineBuilder.addOption(opt);
        }
        // finally add the CLI user properties
        for (Option opt : setPropertyOptions) {
            commandLineBuilder.addOption(opt);
        }
        return commandLineBuilder.build();
    }

    /**
     * configure logging
     */
    void logging(CliRequest cliRequest) {
        // LOG LEVEL
        CommandLine commandLine = cliRequest.commandLine;
        cliRequest.verbose = commandLine.hasOption(CLIManager.VERBOSE) || commandLine.hasOption(CLIManager.DEBUG);
        cliRequest.quiet = !cliRequest.verbose && commandLine.hasOption(CLIManager.QUIET);
        cliRequest.showErrors = cliRequest.verbose || commandLine.hasOption(CLIManager.ERRORS);

        // LOG COLOR
        String styleColor = cliRequest.getUserProperties().getProperty("style.color", "auto");
        styleColor = cliRequest.getUserProperties().getProperty(Constants.MAVEN_STYLE_COLOR_PROPERTY, styleColor);
        styleColor = commandLine.getOptionValue(COLOR, styleColor);
        if ("always".equals(styleColor) || "yes".equals(styleColor) || "force".equals(styleColor)) {
            MessageUtils.setColorEnabled(true);
        } else if ("never".equals(styleColor) || "no".equals(styleColor) || "none".equals(styleColor)) {
            MessageUtils.setColorEnabled(false);
        } else if (!"auto".equals(styleColor) && !"tty".equals(styleColor) && !"if-tty".equals(styleColor)) {
            throw new IllegalArgumentException(
                    "Invalid color configuration value '" + styleColor + "'. Supported are 'auto', 'always', 'never'.");
        } else {
            boolean isBatchMode = !commandLine.hasOption(FORCE_INTERACTIVE)
                    && (commandLine.hasOption(BATCH_MODE) || commandLine.hasOption(NON_INTERACTIVE));
            if (isBatchMode || commandLine.hasOption(CLIManager.LOG_FILE)) {
                MessageUtils.setColorEnabled(false);
            }
        }

        slf4jLoggerFactory = LoggerFactory.getILoggerFactory();
        Slf4jConfiguration slf4jConfiguration = Slf4jConfigurationFactory.getConfiguration(slf4jLoggerFactory);

        if (cliRequest.verbose) {
            cliRequest.request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_DEBUG);
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.DEBUG);
        } else if (cliRequest.quiet) {
            cliRequest.request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_ERROR);
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.ERROR);
        }
        // else fall back to default log level specified in conf
        // see https://issues.apache.org/jira/browse/MNG-2570

        // LOG STREAMS
        if (commandLine.hasOption(CLIManager.LOG_FILE)) {
            File logFile = new File(commandLine.getOptionValue(CLIManager.LOG_FILE));
            logFile = resolveFile(logFile, cliRequest.workingDirectory);

            // redirect stdout and stderr to file
            try {
                PrintStream ps = new PrintStream(new FileOutputStream(logFile));
                System.setOut(ps);
                System.setErr(ps);
            } catch (FileNotFoundException e) {
                //
                // Ignore
                //
            }
        }

        slf4jConfiguration.activate();

        plexusLoggerManager = new Slf4jLoggerManager();
        slf4jLogger = slf4jLoggerFactory.getLogger(this.getClass().getName());

        if (commandLine.hasOption(CLIManager.FAIL_ON_SEVERITY)) {
            String logLevelThreshold = commandLine.getOptionValue(CLIManager.FAIL_ON_SEVERITY);

            if (slf4jLoggerFactory instanceof MavenSlf4jWrapperFactory) {
                LogLevelRecorder logLevelRecorder = new LogLevelRecorder(logLevelThreshold);
                ((MavenSlf4jWrapperFactory) slf4jLoggerFactory).setLogLevelRecorder(logLevelRecorder);
                slf4jLogger.info("Enabled to break the build on log level {}.", logLevelThreshold);
            } else {
                slf4jLogger.warn(
                        "Expected LoggerFactory to be of type '{}', but found '{}' instead. "
                                + "The --fail-on-severity flag will not take effect.",
                        MavenSlf4jWrapperFactory.class.getName(),
                        slf4jLoggerFactory.getClass().getName());
            }
        }

        if (commandLine.hasOption(CLIManager.DEBUG)) {
            slf4jLogger.warn("The option '--debug' is deprecated and may be repurposed as Java debug"
                    + " in a future version. Use -X/--verbose instead.");
        }
    }

    private void version(CliRequest cliRequest) {
        if (cliRequest.verbose || cliRequest.commandLine.hasOption(CLIManager.SHOW_VERSION)) {
            System.out.println(CLIReportingUtils.showVersion());
        }
    }

    private void commands(CliRequest cliRequest) {
        if (cliRequest.showErrors) {
            slf4jLogger.info("Error stacktraces are turned on.");
        }

        if (MavenExecutionRequest.CHECKSUM_POLICY_WARN.equals(cliRequest.request.getGlobalChecksumPolicy())) {
            slf4jLogger.info("Disabling strict checksum verification on all artifact downloads.");
        } else if (MavenExecutionRequest.CHECKSUM_POLICY_FAIL.equals(cliRequest.request.getGlobalChecksumPolicy())) {
            slf4jLogger.info("Enabling strict checksum verification on all artifact downloads.");
        }

        if (slf4jLogger.isDebugEnabled()) {
            slf4jLogger.debug("Message scheme: {}", (MessageUtils.isColorEnabled() ? "color" : "plain"));
            if (MessageUtils.isColorEnabled()) {
                MessageBuilder buff = MessageUtils.builder();
                buff.a("Message styles: ");
                buff.trace("trace").a(' ');
                buff.debug("debug").a(' ');
                buff.info("info").a(' ');
                buff.warning("warning").a(' ');
                buff.error("error").a(' ');
                buff.success("success").a(' ');
                buff.failure("failure").a(' ');
                buff.strong("strong").a(' ');
                buff.mojo("mojo").a(' ');
                buff.project("project");
                slf4jLogger.debug(buff.toString());
            }
        }
    }

    // Needed to make this method package visible to make writing a unit test possible
    // Maybe it's better to move some of those methods to separate class (SoC).
    void properties(CliRequest cliRequest) throws Exception {
        Properties paths = new Properties();
        if (cliRequest.topDirectory != null) {
            paths.put("session.topDirectory", cliRequest.topDirectory.toString());
        }
        if (cliRequest.rootDirectory != null) {
            paths.put("session.rootDirectory", cliRequest.rootDirectory.toString());
        }

        populateProperties(cliRequest.commandLine, paths, cliRequest.systemProperties, cliRequest.userProperties);

        // now that we have properties, interpolate all arguments
        BasicInterpolator interpolator =
                createInterpolator(paths, cliRequest.systemProperties, cliRequest.userProperties);
        CommandLine.Builder commandLineBuilder = new CommandLine.Builder();
        for (Option option : cliRequest.commandLine.getOptions()) {
            if (!String.valueOf(CLIManager.SET_USER_PROPERTY).equals(option.getOpt())) {
                List<String> values = option.getValuesList();
                for (ListIterator<String> it = values.listIterator(); it.hasNext(); ) {
                    it.set(interpolator.interpolate(it.next()));
                }
            }
            commandLineBuilder.addOption(option);
        }
        for (String arg : cliRequest.commandLine.getArgList()) {
            commandLineBuilder.addArg(interpolator.interpolate(arg));
        }
        cliRequest.commandLine = commandLineBuilder.build();
    }

    PlexusContainer container(CliRequest cliRequest) throws Exception {
        if (cliRequest.classWorld == null) {
            cliRequest.classWorld =
                    new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
        }

        ClassRealm coreRealm = cliRequest.classWorld.getClassRealm("plexus.core");
        if (coreRealm == null) {
            coreRealm = cliRequest.classWorld.getRealms().iterator().next();
        }

        List<File> extClassPath = parseExtClasspath(cliRequest);

        CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom(coreRealm);
        List<CoreExtensionEntry> extensions =
                loadCoreExtensions(cliRequest, coreRealm, coreEntry.getExportedArtifacts());

        ClassRealm containerRealm = setupContainerRealm(cliRequest.classWorld, coreRealm, extClassPath, extensions);

        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(cliRequest.classWorld)
                .setRealm(containerRealm)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setJSR250Lifecycle(true)
                .setName("maven");

        Set<String> exportedArtifacts = new HashSet<>(coreEntry.getExportedArtifacts());
        Set<String> exportedPackages = new HashSet<>(coreEntry.getExportedPackages());
        for (CoreExtensionEntry extension : extensions) {
            exportedArtifacts.addAll(extension.getExportedArtifacts());
            exportedPackages.addAll(extension.getExportedPackages());
        }

        final CoreExports exports = new CoreExports(containerRealm, exportedArtifacts, exportedPackages);

        Thread.currentThread().setContextClassLoader(containerRealm);

        DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ILoggerFactory.class).toInstance(slf4jLoggerFactory);
                bind(CoreExports.class).toInstance(exports);
                bind(MessageBuilderFactory.class).toInstance(messageBuilderFactory);
            }
        });

        // NOTE: To avoid inconsistencies, we'll use the TCCL exclusively for lookups
        container.setLookupRealm(null);
        Thread.currentThread().setContextClassLoader(container.getContainerRealm());

        container.setLoggerManager(plexusLoggerManager);

        AbstractValueSource extensionSource = new AbstractValueSource(false) {
            @Override
            public Object getValue(String expression) {
                Object value = cliRequest.userProperties.getProperty(expression);
                if (value == null) {
                    value = cliRequest.systemProperties.getProperty(expression);
                }
                return value;
            }
        };
        for (CoreExtensionEntry extension : extensions) {
            container.discoverComponents(
                    extension.getClassRealm(),
                    new SessionScopeModule(container.lookup(SessionScope.class)),
                    new MojoExecutionScopeModule(container.lookup(MojoExecutionScope.class)),
                    new ExtensionConfigurationModule(extension, extensionSource));
            container.lookup(SisuDiBridgeModule.class).loadFromClassLoader(extension.getClassRealm());
        }

        customizeContainer(container);

        container.getLoggerManager().setThresholds(cliRequest.request.getLoggingLevel());

        eventSpyDispatcher = container.lookup(EventSpyDispatcher.class);

        DefaultEventSpyContext eventSpyContext = new DefaultEventSpyContext();
        Map<String, Object> data = eventSpyContext.getData();
        data.put("plexus", container);
        data.put("workingDirectory", cliRequest.workingDirectory);
        data.put("systemProperties", cliRequest.systemProperties);
        data.put("userProperties", cliRequest.userProperties);
        data.put("versionProperties", CLIReportingUtils.getBuildProperties());
        eventSpyDispatcher.init(eventSpyContext);

        // refresh logger in case container got customized by spy
        slf4jLogger = slf4jLoggerFactory.getLogger(this.getClass().getName());

        maven = container.lookup(Maven.class);

        executionRequestPopulator = container.lookup(MavenExecutionRequestPopulator.class);

        modelProcessor = createModelProcessor(container);

        configurationProcessors = container.lookupMap(ConfigurationProcessor.class);

        toolchainsBuilder = container.lookup(ToolchainsBuilder.class);

        dispatcher = (DefaultSecDispatcher) container.lookup(SecDispatcher.class, "maven");

        return container;
    }

    private List<CoreExtensionEntry> loadCoreExtensions(
            CliRequest cliRequest, ClassRealm containerRealm, Set<String> providedArtifacts) throws Exception {
        if (cliRequest.multiModuleProjectDirectory == null) {
            return Collections.emptyList();
        }

        List<CoreExtension> extensions = new ArrayList<>();

        String installationExtensionsFile =
                cliRequest.getUserProperties().getProperty(Constants.MAVEN_INSTALLATION_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(installationExtensionsFile));

        String projectExtensionsFile = cliRequest.getUserProperties().getProperty(Constants.MAVEN_PROJECT_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(projectExtensionsFile));

        String userExtensionsFile = cliRequest.getUserProperties().getProperty(Constants.MAVEN_USER_EXTENSIONS);
        extensions.addAll(readCoreExtensionsDescriptor(userExtensionsFile));

        if (extensions.isEmpty()) {
            return Collections.emptyList();
        }

        ContainerConfiguration cc = new DefaultContainerConfiguration() //
                .setClassWorld(cliRequest.classWorld) //
                .setRealm(containerRealm) //
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX) //
                .setAutoWiring(true) //
                .setJSR250Lifecycle(true) //
                .setName("maven");

        DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ILoggerFactory.class).toInstance(slf4jLoggerFactory);
            }
        });

        try {
            container.setLookupRealm(null);

            container.setLoggerManager(plexusLoggerManager);

            container.getLoggerManager().setThresholds(cliRequest.request.getLoggingLevel());

            Thread.currentThread().setContextClassLoader(container.getContainerRealm());

            executionRequestPopulator = container.lookup(MavenExecutionRequestPopulator.class);

            configurationProcessors = container.lookupMap(ConfigurationProcessor.class);

            configure(cliRequest);

            MavenExecutionRequest request = DefaultMavenExecutionRequest.copy(cliRequest.request);

            populateRequest(cliRequest, request);

            request = executionRequestPopulator.populateDefaults(request);

            BootstrapCoreExtensionManager resolver = container.lookup(BootstrapCoreExtensionManager.class);

            return Collections.unmodifiableList(resolver.loadCoreExtensions(request, providedArtifacts, extensions));

        } finally {
            executionRequestPopulator = null;
            container.dispose();
        }
    }

    private List<CoreExtension> readCoreExtensionsDescriptor(String extensionsFile)
            throws IOException, XMLStreamException {
        if (extensionsFile != null) {
            Path extensionsPath = Path.of(extensionsFile);
            if (Files.exists(extensionsPath)) {
                try (InputStream is = Files.newInputStream(extensionsPath)) {
                    return new CoreExtensionsStaxReader().read(is, true).getExtensions();
                }
            }
        }
        return List.of();
    }

    private ClassRealm setupContainerRealm(
            ClassWorld classWorld, ClassRealm coreRealm, List<File> extClassPath, List<CoreExtensionEntry> extensions)
            throws Exception {
        if (!extClassPath.isEmpty() || !extensions.isEmpty()) {
            ClassRealm extRealm = classWorld.newRealm("maven.ext", null);

            extRealm.setParentRealm(coreRealm);

            slf4jLogger.debug("Populating class realm '{}'", extRealm.getId());

            for (File file : extClassPath) {
                slf4jLogger.debug("  included '{}'", file);

                extRealm.addURL(file.toURI().toURL());
            }

            for (CoreExtensionEntry entry : reverse(extensions)) {
                Set<String> exportedPackages = entry.getExportedPackages();
                ClassRealm realm = entry.getClassRealm();
                for (String exportedPackage : exportedPackages) {
                    extRealm.importFrom(realm, exportedPackage);
                }
                if (exportedPackages.isEmpty()) {
                    // sisu uses realm imports to establish component visibility
                    extRealm.importFrom(realm, realm.getId());
                }
            }

            return extRealm;
        }

        return coreRealm;
    }

    private static <T> List<T> reverse(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    private List<File> parseExtClasspath(CliRequest cliRequest) {
        String extClassPath = cliRequest.userProperties.getProperty(Constants.MAVEN_EXT_CLASS_PATH);
        if (extClassPath == null) {
            extClassPath = cliRequest.systemProperties.getProperty(Constants.MAVEN_EXT_CLASS_PATH);
            if (extClassPath != null) {
                slf4jLogger.warn(
                        "The property '{}' has been set using a JVM system property which is deprecated. "
                                + "The property can be passed as a Maven argument or in the Maven project configuration file,"
                                + "usually located at ${session.rootDirectory}/.mvn/maven.properties.",
                        Constants.MAVEN_EXT_CLASS_PATH);
            }
        }

        List<File> jars = new ArrayList<>();

        if (extClassPath != null && !extClassPath.isEmpty()) {
            for (String jar : extClassPath.split(File.pathSeparator)) {
                File file = resolveFile(new File(jar), cliRequest.workingDirectory);

                slf4jLogger.debug("  included '{}'", file);

                jars.add(file);
            }
        }

        return jars;
    }

    //
    // This should probably be a separate tool and not be baked into Maven.
    //
    private void encryption(CliRequest cliRequest) throws Exception {
        if (cliRequest.commandLine.hasOption(CLIManager.ENCRYPT_MASTER_PASSWORD)) {
            String passwd = cliRequest.commandLine.getOptionValue(CLIManager.ENCRYPT_MASTER_PASSWORD);

            if (passwd == null) {
                Console cons = System.console();
                char[] password = (cons == null) ? null : cons.readPassword("Master password: ");
                if (password != null) {
                    // Cipher uses Strings
                    passwd = String.copyValueOf(password);

                    // Sun/Oracle advises to empty the char array
                    java.util.Arrays.fill(password, ' ');
                }
            }

            DefaultPlexusCipher cipher = new DefaultPlexusCipher();

            System.out.println(cipher.encryptAndDecorate(passwd, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION));

            throw new ExitException(0);
        } else if (cliRequest.commandLine.hasOption(CLIManager.ENCRYPT_PASSWORD)) {
            String passwd = cliRequest.commandLine.getOptionValue(CLIManager.ENCRYPT_PASSWORD);

            if (passwd == null) {
                Console cons = System.console();
                char[] password = (cons == null) ? null : cons.readPassword("Password: ");
                if (password != null) {
                    // Cipher uses Strings
                    passwd = String.copyValueOf(password);

                    // Sun/Oracle advises to empty the char array
                    java.util.Arrays.fill(password, ' ');
                }
            }

            String configurationFile = dispatcher.getConfigurationFile();

            if (configurationFile.startsWith("~")) {
                configurationFile = System.getProperty("user.home") + configurationFile.substring(1);
            }

            String file = System.getProperty(DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, configurationFile);

            String master = null;

            SettingsSecurity sec = SecUtil.read(file, true);
            if (sec != null) {
                master = sec.getMaster();
            }

            if (master == null) {
                throw new IllegalStateException("Master password is not set in the setting security file: " + file);
            }

            DefaultPlexusCipher cipher = new DefaultPlexusCipher();
            String masterPasswd = cipher.decryptDecorated(master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
            System.out.println(cipher.encryptAndDecorate(passwd, masterPasswd));

            throw new ExitException(0);
        }
    }

    private int execute(CliRequest cliRequest) throws MavenExecutionRequestPopulationException {
        MavenExecutionRequest request = executionRequestPopulator.populateDefaults(cliRequest.request);

        if (cliRequest.request.getRepositoryCache() == null) {
            cliRequest.request.setRepositoryCache(new DefaultRepositoryCache());
        }

        eventSpyDispatcher.onEvent(request);

        MavenExecutionResult result = maven.execute(request);

        eventSpyDispatcher.onEvent(result);

        eventSpyDispatcher.close();

        if (result.hasExceptions()) {
            ExceptionHandler handler = new DefaultExceptionHandler();

            Map<String, String> references = new LinkedHashMap<>();

            List<MavenProject> failedProjects = new ArrayList<>();

            for (Throwable exception : result.getExceptions()) {
                ExceptionSummary summary = handler.handleException(exception);

                logSummary(summary, references, "", cliRequest.showErrors);

                if (exception instanceof LifecycleExecutionException) {
                    failedProjects.add(((LifecycleExecutionException) exception).getProject());
                }
            }

            slf4jLogger.error("");

            if (!cliRequest.showErrors) {
                slf4jLogger.error(
                        "To see the full stack trace of the errors, re-run Maven with the '{}' switch",
                        MessageUtils.builder().strong("-e"));
            }
            if (!slf4jLogger.isDebugEnabled()) {
                slf4jLogger.error(
                        "Re-run Maven using the '{}' switch to enable verbose output",
                        MessageUtils.builder().strong("-X"));
            }

            if (!references.isEmpty()) {
                slf4jLogger.error("");
                slf4jLogger.error("For more information about the errors and possible solutions"
                        + ", please read the following articles:");

                for (Map.Entry<String, String> entry : references.entrySet()) {
                    slf4jLogger.error("{} {}", MessageUtils.builder().strong(entry.getValue()), entry.getKey());
                }
            }

            if (result.canResume()) {
                logBuildResumeHint("mvn [args] -r");
            } else if (!failedProjects.isEmpty()) {
                List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();

                // Sort the failedProjects list in the topologically sorted order.
                failedProjects.sort(comparing(sortedProjects::indexOf));

                MavenProject firstFailedProject = failedProjects.get(0);
                if (!firstFailedProject.equals(sortedProjects.get(0))) {
                    String resumeFromSelector = getResumeFromSelector(sortedProjects, firstFailedProject);
                    logBuildResumeHint("mvn [args] -rf " + resumeFromSelector);
                }
            }

            if (MavenExecutionRequest.REACTOR_FAIL_NEVER.equals(cliRequest.request.getReactorFailureBehavior())) {
                slf4jLogger.info("Build failures were ignored.");

                return 0;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

    private void logBuildResumeHint(String resumeBuildHint) {
        slf4jLogger.error("");
        slf4jLogger.error("After correcting the problems, you can resume the build with the command");
        slf4jLogger.error(MessageUtils.builder().a("  ").strong(resumeBuildHint).toString());
    }

    /**
     * A helper method to determine the value to resume the build with {@code -rf} taking into account the edge case
     *   where multiple modules in the reactor have the same artifactId.
     * <p>
     * {@code -rf :artifactId} will pick up the first module which matches, but when multiple modules in the reactor
     *   have the same artifactId, effective failed module might be later in build reactor.
     * This means that developer will either have to type groupId or wait for build execution of all modules which
     *   were fine, but they are still before one which reported errors.
     * <p>Then the returned value is {@code groupId:artifactId} when there is a name clash and
     * {@code :artifactId} if there is no conflict.
     * This method is made package-private for testing purposes.
     *
     * @param mavenProjects Maven projects which are part of build execution.
     * @param firstFailedProject The first project which has failed.
     * @return Value for -rf flag to resume build exactly from place where it failed ({@code :artifactId} in general
     * and {@code groupId:artifactId} when there is a name clash).
     */
    String getResumeFromSelector(List<MavenProject> mavenProjects, MavenProject firstFailedProject) {
        boolean hasOverlappingArtifactId = mavenProjects.stream()
                        .filter(project -> firstFailedProject.getArtifactId().equals(project.getArtifactId()))
                        .count()
                > 1;

        if (hasOverlappingArtifactId) {
            return firstFailedProject.getGroupId() + ":" + firstFailedProject.getArtifactId();
        }

        return ":" + firstFailedProject.getArtifactId();
    }

    private void logSummary(
            ExceptionSummary summary, Map<String, String> references, String indent, boolean showErrors) {
        String referenceKey = "";

        if (summary.getReference() != null && !summary.getReference().isEmpty()) {
            referenceKey =
                    references.computeIfAbsent(summary.getReference(), k -> "[Help " + (references.size() + 1) + "]");
        }

        String msg = summary.getMessage();

        if (referenceKey != null && !referenceKey.isEmpty()) {
            if (msg.indexOf('\n') < 0) {
                msg += " -> " + MessageUtils.builder().strong(referenceKey);
            } else {
                msg += "\n-> " + MessageUtils.builder().strong(referenceKey);
            }
        }

        String[] lines = NEXT_LINE.split(msg);
        String currentColor = "";

        for (int i = 0; i < lines.length; i++) {
            // add eventual current color inherited from previous line
            String line = currentColor + lines[i];

            // look for last ANSI escape sequence to check if nextColor
            Matcher matcher = LAST_ANSI_SEQUENCE.matcher(line);
            String nextColor = "";
            if (matcher.find()) {
                nextColor = matcher.group(1);
                if (ANSI_RESET.equals(nextColor)) {
                    // last ANSI escape code is reset: no next color
                    nextColor = "";
                }
            }

            // effective line, with indent and reset if end is colored
            line = indent + line + ("".equals(nextColor) ? "" : ANSI_RESET);

            if ((i == lines.length - 1) && (showErrors || (summary.getException() instanceof InternalErrorException))) {
                slf4jLogger.error(line, summary.getException());
            } else {
                slf4jLogger.error(line);
            }

            currentColor = nextColor;
        }

        indent += "  ";

        for (ExceptionSummary child : summary.getChildren()) {
            logSummary(child, references, indent, showErrors);
        }
    }

    private static final Pattern LAST_ANSI_SEQUENCE = Pattern.compile("(\u001B\\[[;\\d]*[ -/]*[@-~])[^\u001B]*$");

    private static final String ANSI_RESET = "\u001B\u005Bm";

    private void configure(CliRequest cliRequest) throws Exception {
        //
        // This is not ideal but there are events specifically for configuration from the CLI which I don't
        // believe are really valid but there are ITs which assert the right events are published so this
        // needs to be supported so the EventSpyDispatcher needs to be put in the CliRequest so that
        // it can be accessed by configuration processors.
        //
        cliRequest.request.setEventSpyDispatcher(eventSpyDispatcher);

        //
        // We expect at most 2 implementations to be available. The SettingsXmlConfigurationProcessor implementation
        // is always available in the core and likely always will be, but we may have another ConfigurationProcessor
        // present supplied by the user. The rule is that we only allow the execution of one ConfigurationProcessor.
        // If there is more than one then we execute the one supplied by the user, otherwise we execute the
        // default SettingsXmlConfigurationProcessor.
        //
        int userSuppliedConfigurationProcessorCount = configurationProcessors.size() - 1;

        if (userSuppliedConfigurationProcessorCount == 0) {
            //
            // Our settings.xml source is historically how we have configured Maven from the CLI so we are going to
            // have to honour its existence forever. So let's run it.
            //
            configurationProcessors.get(SettingsXmlConfigurationProcessor.HINT).process(cliRequest);
        } else if (userSuppliedConfigurationProcessorCount == 1) {
            //
            // Run the user supplied ConfigurationProcessor
            //
            for (Entry<String, ConfigurationProcessor> entry : configurationProcessors.entrySet()) {
                String hint = entry.getKey();
                if (!hint.equals(SettingsXmlConfigurationProcessor.HINT)) {
                    ConfigurationProcessor configurationProcessor = entry.getValue();
                    configurationProcessor.process(cliRequest);
                }
            }
        } else if (userSuppliedConfigurationProcessorCount > 1) {
            //
            // There are too many ConfigurationProcessors so we don't know which one to run so report the error.
            //
            StringBuilder sb = new StringBuilder(String.format(
                    "%nThere can only be one user supplied ConfigurationProcessor, there are %s:%n%n",
                    userSuppliedConfigurationProcessorCount));
            for (Entry<String, ConfigurationProcessor> entry : configurationProcessors.entrySet()) {
                String hint = entry.getKey();
                if (!hint.equals(SettingsXmlConfigurationProcessor.HINT)) {
                    ConfigurationProcessor configurationProcessor = entry.getValue();
                    sb.append(String.format(
                            "%s%n", configurationProcessor.getClass().getName()));
                }
            }
            throw new Exception(sb.toString());
        }
    }

    void toolchains(CliRequest cliRequest) throws Exception {
        File userToolchainsFile = null;

        if (cliRequest.commandLine.hasOption(CLIManager.ALTERNATE_USER_TOOLCHAINS)) {
            userToolchainsFile = new File(cliRequest.commandLine.getOptionValue(CLIManager.ALTERNATE_USER_TOOLCHAINS));
            userToolchainsFile = resolveFile(userToolchainsFile, cliRequest.workingDirectory);

            if (!userToolchainsFile.isFile()) {
                throw new FileNotFoundException(
                        "The specified user toolchains file does not exist: " + userToolchainsFile);
            }
        } else {
            String userToolchainsFileStr = cliRequest.getUserProperties().getProperty(Constants.MAVEN_USER_TOOLCHAINS);
            if (userToolchainsFileStr != null) {
                userToolchainsFile = new File(userToolchainsFileStr);
            }
        }

        File installationToolchainsFile = null;

        if (cliRequest.commandLine.hasOption(CLIManager.ALTERNATE_INSTALLATION_TOOLCHAINS)) {
            installationToolchainsFile =
                    new File(cliRequest.commandLine.getOptionValue(CLIManager.ALTERNATE_INSTALLATION_TOOLCHAINS));
            installationToolchainsFile = resolveFile(installationToolchainsFile, cliRequest.workingDirectory);

            if (!installationToolchainsFile.isFile()) {
                throw new FileNotFoundException(
                        "The specified installation toolchains file does not exist: " + installationToolchainsFile);
            }
        } else if (cliRequest.commandLine.hasOption(CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS)) {
            installationToolchainsFile =
                    new File(cliRequest.commandLine.getOptionValue(CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS));
            installationToolchainsFile = resolveFile(installationToolchainsFile, cliRequest.workingDirectory);

            if (!installationToolchainsFile.isFile()) {
                throw new FileNotFoundException(
                        "The specified installation toolchains file does not exist: " + installationToolchainsFile);
            }
        } else {
            String installationToolchainsFileStr =
                    cliRequest.getUserProperties().getProperty(Constants.MAVEN_INSTALLATION_TOOLCHAINS);
            if (installationToolchainsFileStr != null) {
                installationToolchainsFile = new File(installationToolchainsFileStr);
                installationToolchainsFile = resolveFile(installationToolchainsFile, cliRequest.workingDirectory);
            }
        }

        cliRequest.request.setInstallationToolchainsFile(installationToolchainsFile);
        cliRequest.request.setUserToolchainsFile(userToolchainsFile);

        DefaultToolchainsBuildingRequest toolchainsRequest = new DefaultToolchainsBuildingRequest();
        if (installationToolchainsFile != null && installationToolchainsFile.isFile()) {
            toolchainsRequest.setGlobalToolchainsSource(new FileSource(installationToolchainsFile));
        }
        if (userToolchainsFile != null && userToolchainsFile.isFile()) {
            toolchainsRequest.setUserToolchainsSource(new FileSource(userToolchainsFile));
        }

        eventSpyDispatcher.onEvent(toolchainsRequest);

        slf4jLogger.debug(
                "Reading installation toolchains from '{}'",
                getLocation(toolchainsRequest.getGlobalToolchainsSource(), installationToolchainsFile));
        slf4jLogger.debug(
                "Reading user toolchains from '{}'",
                getLocation(toolchainsRequest.getUserToolchainsSource(), userToolchainsFile));

        ToolchainsBuildingResult toolchainsResult = toolchainsBuilder.build(toolchainsRequest);

        eventSpyDispatcher.onEvent(toolchainsResult);

        executionRequestPopulator.populateFromToolchains(cliRequest.request, toolchainsResult.getEffectiveToolchains());

        if (!toolchainsResult.getProblems().isEmpty() && slf4jLogger.isWarnEnabled()) {
            slf4jLogger.warn("");
            slf4jLogger.warn("Some problems were encountered while building the effective toolchains");

            for (Problem problem : toolchainsResult.getProblems()) {
                slf4jLogger.warn("{} @ {}", problem.getMessage(), problem.getLocation());
            }

            slf4jLogger.warn("");
        }
    }

    private Object getLocation(Source source, File defaultLocation) {
        if (source != null) {
            return source.getLocation();
        }
        return defaultLocation;
    }

    protected MavenExecutionRequest populateRequest(CliRequest cliRequest) {
        return populateRequest(cliRequest, cliRequest.request);
    }

    private MavenExecutionRequest populateRequest(CliRequest cliRequest, MavenExecutionRequest request) {
        slf4jLoggerFactory = LoggerFactory.getILoggerFactory();
        CommandLine commandLine = cliRequest.commandLine;
        String workingDirectory = cliRequest.workingDirectory;
        boolean quiet = cliRequest.quiet;
        boolean verbose = cliRequest.verbose;
        request.setShowErrors(cliRequest.showErrors); // default: false
        File baseDirectory = new File(workingDirectory, "").getAbsoluteFile();

        disableInteractiveModeIfNeeded(cliRequest, request);
        enableOnPresentOption(commandLine, CLIManager.SUPPRESS_SNAPSHOT_UPDATES, request::setNoSnapshotUpdates);
        request.setGoals(commandLine.getArgList());
        request.setReactorFailureBehavior(determineReactorFailureBehaviour(commandLine));
        disableOnPresentOption(commandLine, CLIManager.NON_RECURSIVE, request::setRecursive);
        enableOnPresentOption(commandLine, CLIManager.OFFLINE, request::setOffline);
        enableOnPresentOption(commandLine, CLIManager.UPDATE_SNAPSHOTS, request::setUpdateSnapshots);
        request.setGlobalChecksumPolicy(determineGlobalCheckPolicy(commandLine));
        request.setBaseDirectory(baseDirectory);
        request.setSystemProperties(cliRequest.systemProperties);
        request.setUserProperties(cliRequest.userProperties);
        request.setMultiModuleProjectDirectory(cliRequest.multiModuleProjectDirectory);
        request.setRootDirectory(cliRequest.rootDirectory);
        request.setTopDirectory(cliRequest.topDirectory);
        request.setPom(determinePom(commandLine, workingDirectory, baseDirectory));
        request.setTransferListener(determineTransferListener(quiet, verbose, commandLine, request));
        request.setExecutionListener(determineExecutionListener());

        if ((request.getPom() != null) && (request.getPom().getParentFile() != null)) {
            request.setBaseDirectory(request.getPom().getParentFile());
        }

        request.setResumeFrom(commandLine.getOptionValue(CLIManager.RESUME_FROM));
        enableOnPresentOption(commandLine, CLIManager.RESUME, request::setResume);
        request.setMakeBehavior(determineMakeBehavior(commandLine));
        boolean cacheNotFound = !commandLine.hasOption(CLIManager.CACHE_ARTIFACT_NOT_FOUND)
                || Boolean.parseBoolean(commandLine.getOptionValue(CLIManager.CACHE_ARTIFACT_NOT_FOUND));
        request.setCacheNotFound(cacheNotFound);
        request.setCacheTransferError(false);
        boolean strictArtifactDescriptorPolicy = commandLine.hasOption(CLIManager.STRICT_ARTIFACT_DESCRIPTOR_POLICY)
                && Boolean.parseBoolean(commandLine.getOptionValue(CLIManager.STRICT_ARTIFACT_DESCRIPTOR_POLICY));
        if (strictArtifactDescriptorPolicy) {
            request.setIgnoreMissingArtifactDescriptor(false);
            request.setIgnoreInvalidArtifactDescriptor(false);
        } else {
            request.setIgnoreMissingArtifactDescriptor(true);
            request.setIgnoreInvalidArtifactDescriptor(true);
        }
        enableOnPresentOption(
                commandLine, CLIManager.IGNORE_TRANSITIVE_REPOSITORIES, request::setIgnoreTransitiveRepositories);

        performProjectActivation(commandLine, request.getProjectActivation());
        performProfileActivation(commandLine, request.getProfileActivation());

        final String localRepositoryPath = determineLocalRepositoryPath(request);
        if (localRepositoryPath != null) {
            request.setLocalRepositoryPath(localRepositoryPath);
        }

        //
        // Builder, concurrency and parallelism
        //
        // We preserve the existing methods for builder selection which is to look for various inputs in the threading
        // configuration. We don't have an easy way to allow a pluggable builder to provide its own configuration
        // parameters but this is sufficient for now. Ultimately we want components like Builders to provide a way to
        // extend the command line to accept its own configuration parameters.
        //
        final String threadConfiguration = commandLine.getOptionValue(CLIManager.THREADS);

        if (threadConfiguration != null) {
            int degreeOfConcurrency = calculateDegreeOfConcurrency(threadConfiguration);
            if (degreeOfConcurrency > 1) {
                request.setBuilderId("multithreaded");
                request.setDegreeOfConcurrency(degreeOfConcurrency);
            }
        }

        //
        // Allow the builder to be overridden by the user if requested. The builders are now pluggable.
        //
        request.setBuilderId(commandLine.getOptionValue(CLIManager.BUILDER, request.getBuilderId()));

        return request;
    }

    private void disableInteractiveModeIfNeeded(final CliRequest cliRequest, final MavenExecutionRequest request) {
        CommandLine commandLine = cliRequest.getCommandLine();
        if (commandLine.hasOption(FORCE_INTERACTIVE)) {
            return;
        }

        if (commandLine.hasOption(BATCH_MODE) || commandLine.hasOption(NON_INTERACTIVE)) {
            request.setInteractiveMode(false);
        } else {
            boolean runningOnCI = isRunningOnCI(cliRequest.getSystemProperties());
            if (runningOnCI) {
                slf4jLogger.info(
                        "Making this build non-interactive, because the environment variable CI equals \"true\"."
                                + " Disable this detection by removing that variable or adding --force-interactive.");
                request.setInteractiveMode(false);
            }
        }
    }

    private static boolean isRunningOnCI(Properties systemProperties) {
        String ciEnv = systemProperties.getProperty("env.CI");
        return ciEnv != null && !"false".equals(ciEnv);
    }

    private String determineLocalRepositoryPath(final MavenExecutionRequest request) {
        String userDefinedLocalRepo = request.getUserProperties().getProperty(Constants.MAVEN_REPO_LOCAL);
        if (userDefinedLocalRepo == null) {
            userDefinedLocalRepo = request.getSystemProperties().getProperty(Constants.MAVEN_REPO_LOCAL);
            if (userDefinedLocalRepo != null) {
                slf4jLogger.warn(
                        "The property '{}' has been set using a JVM system property which is deprecated. "
                                + "The property can be passed as a Maven argument or in the Maven project configuration file,"
                                + "usually located at ${session.rootDirectory}/.mvn/maven.properties.",
                        Constants.MAVEN_REPO_LOCAL);
            }
        }
        return userDefinedLocalRepo;
    }

    private File determinePom(final CommandLine commandLine, final String workingDirectory, final File baseDirectory) {
        String alternatePomFile = null;
        if (commandLine.hasOption(CLIManager.ALTERNATE_POM_FILE)) {
            alternatePomFile = commandLine.getOptionValue(CLIManager.ALTERNATE_POM_FILE);
        }

        File current = baseDirectory;
        if (alternatePomFile != null) {
            current = resolveFile(new File(alternatePomFile), workingDirectory);
        }

        if (modelProcessor != null) {
            return modelProcessor.locateExistingPom(current);
        } else {
            return current.isFile() ? current : null;
        }
    }

    // Visible for testing
    static void performProjectActivation(final CommandLine commandLine, final ProjectActivation projectActivation) {
        if (commandLine.hasOption(CLIManager.PROJECT_LIST)) {
            final String[] optionValues = commandLine.getOptionValues(CLIManager.PROJECT_LIST);

            if (optionValues == null || optionValues.length == 0) {
                return;
            }

            for (final String optionValue : optionValues) {
                for (String token : optionValue.split(",")) {
                    String selector = token.trim();
                    boolean active = true;
                    if (!selector.isEmpty()) {
                        if (selector.charAt(0) == '-' || selector.charAt(0) == '!') {
                            active = false;
                            selector = selector.substring(1);
                        } else if (token.charAt(0) == '+') {
                            selector = selector.substring(1);
                        }
                    }
                    boolean optional = false;
                    if (!selector.isEmpty() && selector.charAt(0) == '?') {
                        optional = true;
                        selector = selector.substring(1);
                    }
                    projectActivation.addProjectActivation(selector, active, optional);
                }
            }
        }
    }

    // Visible for testing
    static void performProfileActivation(final CommandLine commandLine, final ProfileActivation profileActivation) {
        if (commandLine.hasOption(CLIManager.ACTIVATE_PROFILES)) {
            final String[] optionValues = commandLine.getOptionValues(CLIManager.ACTIVATE_PROFILES);

            if (optionValues == null || optionValues.length == 0) {
                return;
            }

            for (final String optionValue : optionValues) {
                for (String token : optionValue.split(",")) {
                    String profileId = token.trim();
                    boolean active = true;
                    if (!profileId.isEmpty()) {
                        if (profileId.charAt(0) == '-' || profileId.charAt(0) == '!') {
                            active = false;
                            profileId = profileId.substring(1);
                        } else if (token.charAt(0) == '+') {
                            profileId = profileId.substring(1);
                        }
                    }
                    boolean optional = false;
                    if (!profileId.isEmpty() && profileId.charAt(0) == '?') {
                        optional = true;
                        profileId = profileId.substring(1);
                    }
                    profileActivation.addProfileActivation(profileId, active, optional);
                }
            }
        }
    }

    private ExecutionListener determineExecutionListener() {
        ExecutionListener executionListener = new ExecutionEventLogger(messageBuilderFactory);
        if (eventSpyDispatcher != null) {
            return eventSpyDispatcher.chainListener(executionListener);
        } else {
            return executionListener;
        }
    }

    private String determineReactorFailureBehaviour(final CommandLine commandLine) {
        if (commandLine.hasOption(CLIManager.FAIL_FAST)) {
            return MavenExecutionRequest.REACTOR_FAIL_FAST;
        } else if (commandLine.hasOption(CLIManager.FAIL_AT_END)) {
            return MavenExecutionRequest.REACTOR_FAIL_AT_END;
        } else if (commandLine.hasOption(CLIManager.FAIL_NEVER)) {
            return MavenExecutionRequest.REACTOR_FAIL_NEVER;
        } else {
            // this is the default behavior.
            return MavenExecutionRequest.REACTOR_FAIL_FAST;
        }
    }

    private TransferListener determineTransferListener(
            final boolean quiet,
            final boolean verbose,
            final CommandLine commandLine,
            final MavenExecutionRequest request) {
        boolean runningOnCI = isRunningOnCI(request.getSystemProperties());
        boolean quietCI = runningOnCI && !commandLine.hasOption(FORCE_INTERACTIVE);

        if (quiet || commandLine.hasOption(CLIManager.NO_TRANSFER_PROGRESS) || quietCI) {
            return new QuietMavenTransferListener();
        } else if (request.isInteractiveMode() && !commandLine.hasOption(CLIManager.LOG_FILE)) {
            //
            // If we're logging to a file then we don't want the console transfer listener as it will spew
            // download progress all over the place
            //
            return getConsoleTransferListener(verbose);
        } else {
            // default: batch mode which goes along with interactive
            return getBatchTransferListener();
        }
    }

    private String determineMakeBehavior(final CommandLine cl) {
        if (cl.hasOption(CLIManager.ALSO_MAKE) && !cl.hasOption(CLIManager.ALSO_MAKE_DEPENDENTS)) {
            return MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;
        } else if (!cl.hasOption(CLIManager.ALSO_MAKE) && cl.hasOption(CLIManager.ALSO_MAKE_DEPENDENTS)) {
            return MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM;
        } else if (cl.hasOption(CLIManager.ALSO_MAKE) && cl.hasOption(CLIManager.ALSO_MAKE_DEPENDENTS)) {
            return MavenExecutionRequest.REACTOR_MAKE_BOTH;
        } else {
            return null;
        }
    }

    private String determineGlobalCheckPolicy(final CommandLine commandLine) {
        if (commandLine.hasOption(CLIManager.CHECKSUM_FAILURE_POLICY)) {
            return MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
        } else if (commandLine.hasOption(CLIManager.CHECKSUM_WARNING_POLICY)) {
            return MavenExecutionRequest.CHECKSUM_POLICY_WARN;
        } else {
            return null;
        }
    }

    private void disableOnPresentOption(
            final CommandLine commandLine, final String option, final Consumer<Boolean> setting) {
        if (commandLine.hasOption(option)) {
            setting.accept(false);
        }
    }

    private void disableOnPresentOption(
            final CommandLine commandLine, final char option, final Consumer<Boolean> setting) {
        disableOnPresentOption(commandLine, String.valueOf(option), setting);
    }

    private void enableOnPresentOption(
            final CommandLine commandLine, final String option, final Consumer<Boolean> setting) {
        if (commandLine.hasOption(option)) {
            setting.accept(true);
        }
    }

    private void enableOnPresentOption(
            final CommandLine commandLine, final char option, final Consumer<Boolean> setting) {
        enableOnPresentOption(commandLine, String.valueOf(option), setting);
    }

    private void enableOnAbsentOption(
            final CommandLine commandLine, final char option, final Consumer<Boolean> setting) {
        if (!commandLine.hasOption(option)) {
            setting.accept(true);
        }
    }

    int calculateDegreeOfConcurrency(String threadConfiguration) {
        try {
            if (threadConfiguration.endsWith("C")) {
                String str = threadConfiguration.substring(0, threadConfiguration.length() - 1);
                float coreMultiplier = Float.parseFloat(str);

                if (coreMultiplier <= 0.0f) {
                    throw new IllegalArgumentException("Invalid threads core multiplier value: '" + threadConfiguration
                            + "'. Value must be positive.");
                }

                int procs = Runtime.getRuntime().availableProcessors();
                int threads = (int) (coreMultiplier * procs);
                return threads == 0 ? 1 : threads;
            } else {
                int threads = Integer.parseInt(threadConfiguration);
                if (threads <= 0) {
                    throw new IllegalArgumentException(
                            "Invalid threads value: '" + threadConfiguration + "'. Value must be positive.");
                }
                return threads;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid threads value: '" + threadConfiguration
                    + "'. Supported are int and float values ending with C.");
        }
    }

    // ----------------------------------------------------------------------
    // Properties handling
    // ----------------------------------------------------------------------

    void populateProperties(
            CommandLine commandLine, Properties paths, Properties systemProperties, Properties userProperties)
            throws Exception {

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

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        Properties userSpecifiedProperties =
                commandLine.getOptionProperties(String.valueOf(CLIManager.SET_USER_PROPERTY));
        userProperties.putAll(userSpecifiedProperties);

        // ----------------------------------------------------------------------
        // Load config files
        // ----------------------------------------------------------------------
        Function<String, String> callback =
                or(paths::getProperty, prefix("cli.", commandLine::getOptionValue), systemProperties::getProperty);

        Path mavenConf;
        if (systemProperties.getProperty(MAVEN_INSTALLATION_CONF) != null) {
            mavenConf = fileSystem.getPath(systemProperties.getProperty(MAVEN_INSTALLATION_CONF));
        } else if (systemProperties.getProperty("maven.conf") != null) {
            mavenConf = fileSystem.getPath(systemProperties.getProperty("maven.conf"));
        } else if (systemProperties.getProperty(MAVEN_HOME) != null) {
            mavenConf = fileSystem.getPath(systemProperties.getProperty(MAVEN_HOME), "conf");
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
    }

    private static Function<String, String> prefix(String prefix, Function<String, String> cb) {
        return s -> {
            String v = null;
            if (s.startsWith(prefix)) {
                v = cb.apply(s.substring(prefix.length()));
            }
            return v;
        };
    }

    private static Function<String, String> or(Function<String, String>... callbacks) {
        return s -> {
            for (Function<String, String> cb : callbacks) {
                String r = cb.apply(s);
                if (r != null) {
                    return r;
                }
            }
            return null;
        };
    }

    private static BasicInterpolator createInterpolator(Properties... properties) {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource(new AbstractValueSource(false) {
            @Override
            public Object getValue(String expression) {
                for (Properties props : properties) {
                    Object val = props.getProperty(expression);
                    if (val != null) {
                        return val;
                    }
                }
                return null;
            }
        });
        return interpolator;
    }

    private static String stripLeadingAndTrailingQuotes(String str) {
        final int length = str.length();
        if (length > 1
                && str.startsWith("\"")
                && str.endsWith("\"")
                && str.substring(1, length - 1).indexOf('"') == -1) {
            str = str.substring(1, length - 1);
        }

        return str;
    }

    private static Path getCanonicalPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return getCanonicalPath(path.getParent()).resolve(path.getFileName());
        }
    }

    static class ExitException extends Exception {
        int exitCode;

        ExitException(int exitCode) {
            this.exitCode = exitCode;
        }
    }

    //
    // Customizations available via the CLI
    //

    protected TransferListener getConsoleTransferListener(boolean printResourceNames) {
        return new SimplexTransferListener(
                new ConsoleMavenTransferListener(messageBuilderFactory, System.out, printResourceNames));
    }

    protected TransferListener getBatchTransferListener() {
        return new Slf4jMavenTransferListener();
    }

    protected void customizeContainer(PlexusContainer container) {}

    protected ModelProcessor createModelProcessor(PlexusContainer container) throws ComponentLookupException {
        return container.lookup(ModelProcessor.class);
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
}
