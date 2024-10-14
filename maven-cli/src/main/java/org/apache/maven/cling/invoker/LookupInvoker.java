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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.apache.maven.api.Constants;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.cli.logging.Slf4jConfigurationFactory;
import org.apache.maven.cli.transfer.ConsoleMavenTransferListener;
import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.cli.transfer.SimplexTransferListener;
import org.apache.maven.cli.transfer.Slf4jMavenTransferListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.logwrapper.LogLevelRecorder;
import org.apache.maven.logwrapper.MavenSlf4jWrapperFactory;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.eclipse.aether.transfer.TransferListener;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.Utils.toFile;
import static org.apache.maven.cling.invoker.Utils.toMavenExecutionRequestLoggingLevel;
import static org.apache.maven.cling.invoker.Utils.toProperties;

/**
 * Plexus invoker implementation, that boots up Plexus DI container. This class expects fully setup ClassWorld via constructor.
 *
 * @param <O> the options type
 * @param <R> the request type
 * @param <C> the context type
 */
public abstract class LookupInvoker<
                O extends Options, R extends InvokerRequest<O>, C extends LookupInvoker.LookupInvokerContext<O, R, C>>
        implements Invoker<R> {

    /**
     * Exception for intentional exit: No message or anything will be displayed, just the
     * carried exit code will be returned from {@link #invoke(InvokerRequest)} method.
     */
    public static final class ExitException extends InvokerException {
        private final int exitCode;

        public ExitException(int exitCode) {
            super("EXIT");
            this.exitCode = exitCode;
        }
    }

    @SuppressWarnings("VisibilityModifier")
    public static class LookupInvokerContext<
                    O extends Options, R extends InvokerRequest<O>, C extends LookupInvokerContext<O, R, C>>
            implements AutoCloseable {
        public final LookupInvoker<O, R, C> invoker;
        public final ProtoLookup protoLookup;
        public final R invokerRequest;
        public final Function<String, Path> cwdResolver;
        public final Function<String, Path> installationResolver;
        public final Function<String, Path> userResolver;
        public final InputStream stdIn;
        public final PrintWriter stdOut;
        public final PrintWriter stdErr;

        protected LookupInvokerContext(LookupInvoker<O, R, C> invoker, R invokerRequest) {
            this.invoker = invoker;
            this.protoLookup = invoker.protoLookup;
            this.invokerRequest = requireNonNull(invokerRequest);
            this.cwdResolver = s -> invokerRequest.cwd().resolve(s).normalize().toAbsolutePath();
            this.installationResolver = s -> invokerRequest
                    .installationDirectory()
                    .resolve(s)
                    .normalize()
                    .toAbsolutePath();
            this.userResolver = s ->
                    invokerRequest.userHomeDirectory().resolve(s).normalize().toAbsolutePath();
            this.stdIn = invokerRequest.in().orElse(System.in);
            this.stdOut = new PrintWriter(invokerRequest.out().orElse(System.out), true);
            this.stdErr = new PrintWriter(invokerRequest.err().orElse(System.err), true);
            this.logger = invokerRequest.parserRequest().logger();
        }

        public Logger logger;
        public ILoggerFactory loggerFactory;
        public Slf4jConfiguration slf4jConfiguration;
        public Slf4jConfiguration.Level loggerLevel;
        public ClassLoader currentThreadContextClassLoader;
        public ContainerCapsule containerCapsule;
        public Lookup lookup;
        public SettingsBuilder settingsBuilder;

        public boolean interactive;
        public Path localRepositoryPath;
        public Path installationSettingsPath;
        public Path projectSettingsPath;
        public Path userSettingsPath;
        public Settings effectiveSettings;

        @Override
        public void close() throws InvokerException {
            if (containerCapsule != null) {
                containerCapsule.close();
            }
        }
    }

    protected final ProtoLookup protoLookup;

    public LookupInvoker(ProtoLookup protoLookup) {
        this.protoLookup = requireNonNull(protoLookup);
    }

    @Override
    public int invoke(R invokerRequest) throws InvokerException {
        requireNonNull(invokerRequest);

        Properties oldProps = (Properties) System.getProperties().clone();
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        try (C context = createContext(invokerRequest)) {
            try {
                if (context.currentThreadContextClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(context.currentThreadContextClassLoader);
                }
                return doInvoke(context);
            } catch (ExitException e) {
                return e.exitCode;
            } catch (Exception e) {
                throw handleException(context, e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
            System.setProperties(oldProps);
        }
    }

    protected int doInvoke(C context) throws Exception {
        pushProperties(context);
        validate(context);
        prepare(context);
        configureLogging(context);
        activateLogging(context);
        helpOrVersionAndMayExit(context);
        preCommands(context);
        container(context);
        lookup(context);
        init(context);
        postCommands(context);
        settings(context);
        return execute(context);
    }

    protected InvokerException handleException(LookupInvokerContext<O, R, C> context, Exception e)
            throws InvokerException {
        boolean showStackTrace = context.invokerRequest.options().showErrors().orElse(false);
        if (showStackTrace) {
            context.logger.error(
                    "Error executing " + context.invokerRequest.parserRequest().commandName() + ".", e);
        } else {
            context.logger.error(
                    "Error executing " + context.invokerRequest.parserRequest().commandName() + ".");
            context.logger.error(e.getMessage());
            for (Throwable cause = e.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
                context.logger.error("Caused by: " + cause.getMessage());
            }
        }
        return new InvokerException(e.getMessage(), e);
    }

    protected abstract C createContext(R invokerRequest) throws InvokerException;

    protected void pushProperties(C context) throws Exception {
        R invokerRequest = context.invokerRequest;
        HashSet<String> sys = new HashSet<>(invokerRequest.systemProperties().keySet());
        invokerRequest.userProperties().entrySet().stream()
                .filter(k -> !sys.contains(k.getKey()))
                .forEach(k -> System.setProperty(k.getKey(), k.getValue()));
        System.setProperty(
                Constants.MAVEN_HOME, invokerRequest.installationDirectory().toString());
    }

    protected void validate(C context) throws Exception {}

    protected void prepare(C context) throws Exception {}

    protected void configureLogging(C context) throws Exception {
        R invokerRequest = context.invokerRequest;
        // LOG COLOR
        Options mavenOptions = invokerRequest.options();
        Map<String, String> userProperties = invokerRequest.userProperties();
        String styleColor = mavenOptions
                .color()
                .orElse(userProperties.getOrDefault(
                        Constants.MAVEN_STYLE_COLOR_PROPERTY, userProperties.getOrDefault("style.color", "auto")));
        if ("always".equals(styleColor) || "yes".equals(styleColor) || "force".equals(styleColor)) {
            MessageUtils.setColorEnabled(true);
        } else if ("never".equals(styleColor) || "no".equals(styleColor) || "none".equals(styleColor)) {
            MessageUtils.setColorEnabled(false);
        } else if (!"auto".equals(styleColor) && !"tty".equals(styleColor) && !"if-tty".equals(styleColor)) {
            throw new IllegalArgumentException(
                    "Invalid color configuration value '" + styleColor + "'. Supported are 'auto', 'always', 'never'.");
        } else {
            boolean isBatchMode = !mavenOptions.forceInteractive().orElse(false)
                    && mavenOptions.nonInteractive().orElse(false);
            if (isBatchMode || mavenOptions.logFile().isPresent()) {
                MessageUtils.setColorEnabled(false);
            }
        }

        context.loggerFactory = LoggerFactory.getILoggerFactory();
        context.slf4jConfiguration = Slf4jConfigurationFactory.getConfiguration(context.loggerFactory);

        context.loggerLevel = Slf4jConfiguration.Level.INFO;
        if (mavenOptions.verbose().orElse(false)) {
            context.loggerLevel = Slf4jConfiguration.Level.DEBUG;
        } else if (mavenOptions.quiet().orElse(false)) {
            context.loggerLevel = Slf4jConfiguration.Level.ERROR;
        }
        context.slf4jConfiguration.setRootLoggerLevel(context.loggerLevel);
        // else fall back to default log level specified in conf
        // see https://issues.apache.org/jira/browse/MNG-2570

        // LOG STREAMS
        if (mavenOptions.logFile().isPresent()) {
            Path logFile = context.cwdResolver.apply(mavenOptions.logFile().get());
            // redirect stdout and stderr to file
            try {
                PrintStream ps = new PrintStream(Files.newOutputStream(logFile));
                System.setOut(ps);
                System.setErr(ps);
            } catch (IOException e) {
                throw new InvokerException("Cannot set up log " + e.getMessage(), e);
            }
        }
    }

    protected void activateLogging(C context) throws Exception {
        R invokerRequest = context.invokerRequest;
        Options mavenOptions = invokerRequest.options();

        context.slf4jConfiguration.activate();
        org.slf4j.Logger l = context.loggerFactory.getLogger(this.getClass().getName());
        context.logger = (level, message, error) -> l.atLevel(org.slf4j.event.Level.valueOf(level.name()))
                .setCause(error)
                .log(message);

        if (mavenOptions.failOnSeverity().isPresent()) {
            String logLevelThreshold = mavenOptions.failOnSeverity().get();

            if (context.loggerFactory instanceof MavenSlf4jWrapperFactory) {
                LogLevelRecorder logLevelRecorder = new LogLevelRecorder(logLevelThreshold);
                ((MavenSlf4jWrapperFactory) context.loggerFactory).setLogLevelRecorder(logLevelRecorder);
                context.logger.info("Enabled to break the build on log level " + logLevelThreshold + ".");
            } else {
                context.logger.warn("Expected LoggerFactory to be of type '" + MavenSlf4jWrapperFactory.class.getName()
                        + "', but found '"
                        + context.loggerFactory.getClass().getName() + "' instead. "
                        + "The --fail-on-severity flag will not take effect.");
            }
        }
    }

    protected void helpOrVersionAndMayExit(C context) throws Exception {
        R invokerRequest = context.invokerRequest;
        if (invokerRequest.options().help().isPresent()) {
            invokerRequest.options().displayHelp(context.invokerRequest.parserRequest(), context.stdOut);
            throw new ExitException(0);
        }
        if (invokerRequest.options().showVersionAndExit().isPresent()) {
            if (invokerRequest.options().quiet().orElse(false)) {
                context.stdOut.println(CLIReportingUtils.showVersionMinimal());
            } else {
                context.stdOut.println(CLIReportingUtils.showVersion());
            }
            throw new ExitException(0);
        }
    }

    protected void preCommands(C context) throws Exception {
        Options mavenOptions = context.invokerRequest.options();
        if (mavenOptions.verbose().orElse(false) || mavenOptions.showVersion().orElse(false)) {
            context.stdOut.println(CLIReportingUtils.showVersion());
        }
    }

    protected void container(C context) throws Exception {
        context.containerCapsule = createContainerCapsuleFactory().createContainerCapsule(context);
        context.lookup = context.containerCapsule.getLookup();
        context.settingsBuilder = context.lookup.lookup(SettingsBuilder.class);

        // refresh logger in case container got customized by spy
        org.slf4j.Logger l = context.loggerFactory.getLogger(this.getClass().getName());
        context.logger = (level, message, error) -> l.atLevel(org.slf4j.event.Level.valueOf(level.name()))
                .setCause(error)
                .log(message);
    }

    protected ContainerCapsuleFactory<O, R, C> createContainerCapsuleFactory() {
        return new PlexusContainerCapsuleFactory<>();
    }

    protected void lookup(C context) throws Exception {}

    protected void init(C context) throws Exception {}

    protected void postCommands(C context) throws Exception {
        R invokerRequest = context.invokerRequest;
        Logger logger = context.logger;
        if (invokerRequest.options().showErrors().orElse(false)) {
            logger.info("Error stacktraces are turned on.");
        }
        if (context.invokerRequest.options().verbose().orElse(false)) {
            logger.debug("Message scheme: " + (MessageUtils.isColorEnabled() ? "color" : "plain"));
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
                logger.debug(buff.toString());
            }
        }
    }

    protected void settings(C context) throws Exception {
        settings(context, context.settingsBuilder);
    }

    protected void settings(C context, SettingsBuilder settingsBuilder) throws Exception {
        Options mavenOptions = context.invokerRequest.options();
        Path userSettingsFile = null;

        if (mavenOptions.altUserSettings().isPresent()) {
            userSettingsFile =
                    context.cwdResolver.apply(mavenOptions.altUserSettings().get());

            if (!Files.isRegularFile(userSettingsFile)) {
                throw new FileNotFoundException("The specified user settings file does not exist: " + userSettingsFile);
            }
        } else {
            String userSettingsFileStr = context.invokerRequest.userProperties().get(Constants.MAVEN_USER_SETTINGS);
            if (userSettingsFileStr != null) {
                userSettingsFile = context.userResolver.apply(userSettingsFileStr);
            }
        }

        Path projectSettingsFile = null;

        if (mavenOptions.altProjectSettings().isPresent()) {
            projectSettingsFile =
                    context.cwdResolver.apply(mavenOptions.altProjectSettings().get());

            if (!Files.isRegularFile(projectSettingsFile)) {
                throw new FileNotFoundException(
                        "The specified project settings file does not exist: " + projectSettingsFile);
            }
        } else {
            String projectSettingsFileStr =
                    context.invokerRequest.userProperties().get(Constants.MAVEN_PROJECT_SETTINGS);
            if (projectSettingsFileStr != null) {
                projectSettingsFile = context.cwdResolver.apply(projectSettingsFileStr);
            }
        }

        Path installationSettingsFile = null;

        if (mavenOptions.altInstallationSettings().isPresent()) {
            installationSettingsFile = context.cwdResolver.apply(
                    mavenOptions.altInstallationSettings().get());

            if (!Files.isRegularFile(installationSettingsFile)) {
                throw new FileNotFoundException(
                        "The specified installation settings file does not exist: " + installationSettingsFile);
            }
        } else {
            String installationSettingsFileStr =
                    context.invokerRequest.userProperties().get(Constants.MAVEN_INSTALLATION_SETTINGS);
            if (installationSettingsFileStr != null) {
                installationSettingsFile = context.installationResolver.apply(installationSettingsFileStr);
            }
        }

        context.installationSettingsPath = installationSettingsFile;
        context.projectSettingsPath = projectSettingsFile;
        context.userSettingsPath = userSettingsFile;

        SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setGlobalSettingsFile(toFile(installationSettingsFile));
        settingsRequest.setProjectSettingsFile(toFile(projectSettingsFile));
        settingsRequest.setUserSettingsFile(toFile(userSettingsFile));
        settingsRequest.setSystemProperties(toProperties(context.invokerRequest.systemProperties()));
        Properties props = toProperties(context.invokerRequest.userProperties());
        props.put(
                "session.rootDirectory", context.invokerRequest.rootDirectory().toString());
        props.put("session.topDirectory", context.invokerRequest.topDirectory().toString());

        settingsRequest.setUserProperties(props);
        customizeSettingsRequest(context, settingsRequest);

        context.logger.debug("Reading installation settings from '"
                + (settingsRequest.getGlobalSettingsSource() != null
                        ? settingsRequest.getGlobalSettingsSource().getLocation()
                        : settingsRequest.getGlobalSettingsFile())
                + "'");
        context.logger.debug("Reading project settings from '"
                + (settingsRequest.getProjectSettingsSource() != null
                        ? settingsRequest.getProjectSettingsSource().getLocation()
                        : settingsRequest.getProjectSettingsFile())
                + "'");
        context.logger.debug("Reading user settings from '"
                + (settingsRequest.getUserSettingsSource() != null
                        ? settingsRequest.getUserSettingsSource().getLocation()
                        : settingsRequest.getUserSettingsFile())
                + "'");

        SettingsBuildingResult settingsResult = settingsBuilder.build(settingsRequest);
        customizeSettingsResult(context, settingsResult);

        context.effectiveSettings = settingsResult.getEffectiveSettings();
        context.interactive = mayDisableInteractiveMode(context, context.effectiveSettings.isInteractiveMode());
        context.localRepositoryPath = localRepositoryPath(context);

        if (!settingsResult.getProblems().isEmpty()) {
            context.logger.warn("");
            context.logger.warn("Some problems were encountered while building the effective settings");

            for (SettingsProblem problem : settingsResult.getProblems()) {
                context.logger.warn(problem.getMessage() + " @ " + problem.getLocation());
            }
            context.logger.warn("");
        }
    }

    protected void customizeSettingsRequest(C context, SettingsBuildingRequest settingsBuildingRequest)
            throws Exception {}

    protected void customizeSettingsResult(C context, SettingsBuildingResult settingsBuildingResult) throws Exception {}

    protected boolean mayDisableInteractiveMode(C context, boolean proposedInteractive) {
        if (!context.invokerRequest.options().forceInteractive().orElse(false)) {
            if (context.invokerRequest.options().nonInteractive().orElse(false)) {
                return false;
            } else {
                boolean runningOnCI = isRunningOnCI(context);
                if (runningOnCI) {
                    context.logger.info(
                            "Making this build non-interactive, because the environment variable CI equals \"true\"."
                                    + " Disable this detection by removing that variable or adding --force-interactive.");
                    return false;
                }
            }
        }
        return proposedInteractive;
    }

    protected Path localRepositoryPath(C context) {
        // user override
        String userDefinedLocalRepo = context.invokerRequest.userProperties().get(Constants.MAVEN_REPO_LOCAL);
        if (userDefinedLocalRepo == null) {
            userDefinedLocalRepo = context.invokerRequest.systemProperties().get(Constants.MAVEN_REPO_LOCAL);
            if (userDefinedLocalRepo != null) {
                context.logger.warn("The property '" + Constants.MAVEN_REPO_LOCAL
                        + "' has been set using a JVM system property which is deprecated. "
                        + "The property can be passed as a Maven argument or in the Maven project configuration file,"
                        + "usually located at ${session.rootDirectory}/.mvn/maven.properties.");
            }
        }
        if (userDefinedLocalRepo != null) {
            return context.cwdResolver.apply(userDefinedLocalRepo);
        }
        // settings
        userDefinedLocalRepo = context.effectiveSettings.getLocalRepository();
        if (userDefinedLocalRepo != null) {
            return context.userResolver.apply(userDefinedLocalRepo);
        }
        // defaults
        return context.userResolver
                .apply(context.invokerRequest.userProperties().get(Constants.MAVEN_USER_CONF))
                .resolve("repository");
    }

    protected void populateRequest(C context, MavenExecutionRequest request) throws Exception {
        populateRequestFromSettings(request, context.effectiveSettings);

        Options options = context.invokerRequest.options();
        request.setLoggingLevel(toMavenExecutionRequestLoggingLevel(context.loggerLevel));
        request.setLocalRepositoryPath(context.localRepositoryPath.toFile());
        request.setLocalRepository(createLocalArtifactRepository(context.localRepositoryPath));

        request.setInteractiveMode(context.interactive);
        request.setShowErrors(options.showErrors().orElse(false));
        request.setBaseDirectory(context.invokerRequest.topDirectory().toFile());
        request.setSystemProperties(toProperties(context.invokerRequest.systemProperties()));
        request.setUserProperties(toProperties(context.invokerRequest.userProperties()));
        request.setMultiModuleProjectDirectory(
                context.invokerRequest.rootDirectory().toFile());

        request.setRootDirectory(context.invokerRequest.rootDirectory());
        request.setTopDirectory(context.invokerRequest.topDirectory());

        request.addPluginGroup("org.apache.maven.plugins");
        request.addPluginGroup("org.codehaus.mojo");
    }

    /**
     * TODO: get rid of this!!!
     */
    @Deprecated
    private ArtifactRepository createLocalArtifactRepository(Path baseDirectory) {
        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        ArtifactRepositoryPolicy blah = new ArtifactRepositoryPolicy(
                true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE);
        return new MavenArtifactRepository(
                "local", "file://" + baseDirectory.toUri().getRawPath(), layout, blah, blah);
    }

    protected void populateRequestFromSettings(MavenExecutionRequest request, Settings settings) throws Exception {
        if (settings == null) {
            return;
        }
        request.setOffline(settings.isOffline());
        request.setInteractiveMode(settings.isInteractiveMode());
        request.setPluginGroups(settings.getPluginGroups());
        request.setLocalRepositoryPath(settings.getLocalRepository());
        for (Server server : settings.getServers()) {
            request.addServer(server);
        }

        //  <proxies>
        //    <proxy>
        //      <active>true</active>
        //      <protocol>http</protocol>
        //      <host>proxy.somewhere.com</host>
        //      <port>8080</port>
        //      <username>proxyuser</username>
        //      <password>somepassword</password>
        //      <nonProxyHosts>www.google.com|*.somewhere.com</nonProxyHosts>
        //    </proxy>
        //  </proxies>

        for (Proxy proxy : settings.getProxies()) {
            if (!proxy.isActive()) {
                continue;
            }
            request.addProxy(proxy);
        }

        // <mirrors>
        //   <mirror>
        //     <id>nexus</id>
        //     <mirrorOf>*</mirrorOf>
        //     <url>http://repository.sonatype.org/content/groups/public</url>
        //   </mirror>
        // </mirrors>

        for (Mirror mirror : settings.getMirrors()) {
            request.addMirror(mirror);
        }

        for (Repository remoteRepository : settings.getRepositories()) {
            try {
                request.addRemoteRepository(MavenRepositorySystem.buildArtifactRepository(remoteRepository));
            } catch (InvalidRepositoryException e) {
                // do nothing for now
            }
        }

        for (Repository pluginRepository : settings.getPluginRepositories()) {
            try {
                request.addPluginArtifactRepository(MavenRepositorySystem.buildArtifactRepository(pluginRepository));
            } catch (InvalidRepositoryException e) {
                // do nothing for now
            }
        }

        request.setActiveProfiles(settings.getActiveProfiles());
        for (Profile rawProfile : settings.getProfiles()) {
            request.addProfile(SettingsUtils.convertFromSettingsProfile(rawProfile));

            if (settings.getActiveProfiles().contains(rawProfile.getId())) {
                List<Repository> remoteRepositories = rawProfile.getRepositories();
                for (Repository remoteRepository : remoteRepositories) {
                    try {
                        request.addRemoteRepository(MavenRepositorySystem.buildArtifactRepository(remoteRepository));
                    } catch (InvalidRepositoryException e) {
                        // do nothing for now
                    }
                }

                List<Repository> pluginRepositories = rawProfile.getPluginRepositories();
                for (Repository pluginRepository : pluginRepositories) {
                    try {
                        request.addPluginArtifactRepository(
                                MavenRepositorySystem.buildArtifactRepository(pluginRepository));
                    } catch (InvalidRepositoryException e) {
                        // do nothing for now
                    }
                }
            }
        }
    }

    protected int calculateDegreeOfConcurrency(String threadConfiguration) {
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

    protected boolean isRunningOnCI(C context) {
        String ciEnv = context.invokerRequest.systemProperties().get("env.CI");
        return ciEnv != null && !"false".equals(ciEnv);
    }

    protected TransferListener determineTransferListener(C context, boolean noTransferProgress) {
        boolean quiet = context.invokerRequest.options().quiet().orElse(false);
        boolean logFile = context.invokerRequest.options().logFile().isPresent();
        boolean runningOnCI = isRunningOnCI(context);
        boolean quietCI = runningOnCI
                && !context.invokerRequest.options().forceInteractive().orElse(false);

        if (quiet || noTransferProgress || quietCI) {
            return new QuietMavenTransferListener();
        } else if (context.interactive && !logFile) {
            return new SimplexTransferListener(new ConsoleMavenTransferListener(
                    context.invokerRequest.messageBuilderFactory(),
                    context.stdOut,
                    context.invokerRequest.options().verbose().orElse(false)));
        } else {
            return new Slf4jMavenTransferListener();
        }
    }

    protected abstract int execute(C context) throws Exception;
}
