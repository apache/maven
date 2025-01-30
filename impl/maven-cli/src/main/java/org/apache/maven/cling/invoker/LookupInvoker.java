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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.apache.maven.api.Constants;
import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.settings.Mirror;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.spi.PropertyContributor;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.cling.invoker.spi.PropertyContributorsHolder;
import org.apache.maven.cling.logging.Slf4jConfiguration;
import org.apache.maven.cling.logging.Slf4jConfigurationFactory;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.impl.SettingsUtilsV4;
import org.apache.maven.jline.FastTerminal;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.logging.BuildEventListener;
import org.apache.maven.logging.LoggingOutputStream;
import org.apache.maven.logging.ProjectBuildLogAppender;
import org.apache.maven.logging.SimpleBuildEventListener;
import org.apache.maven.logging.api.LogLevelRecorder;
import org.apache.maven.slf4j.MavenSimpleLogger;
import org.codehaus.plexus.PlexusContainer;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.AbstractPosixTerminal;
import org.jline.terminal.spi.TerminalExt;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.Utils.toMavenExecutionRequestLoggingLevel;
import static org.apache.maven.cling.invoker.Utils.toProperties;

/**
 * Lookup invoker implementation, that boots up DI container.
 *
 * @param <C> The context type.
 */
public abstract class LookupInvoker<C extends LookupContext> implements Invoker {
    protected final Lookup protoLookup;

    @Nullable
    protected final Consumer<LookupContext> contextConsumer;

    public LookupInvoker(Lookup protoLookup, @Nullable Consumer<LookupContext> contextConsumer) {
        this.protoLookup = requireNonNull(protoLookup);
        this.contextConsumer = contextConsumer;
    }

    @Override
    public int invoke(InvokerRequest invokerRequest) throws InvokerException {
        requireNonNull(invokerRequest);

        Properties oldProps = new Properties();
        oldProps.putAll(System.getProperties());
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        try (C context = createContext(invokerRequest)) {
            if (contextConsumer != null) {
                contextConsumer.accept(context);
            }
            try {
                if (context.containerCapsule != null
                        && context.containerCapsule.currentThreadClassLoader().isPresent()) {
                    Thread.currentThread()
                            .setContextClassLoader(context.containerCapsule
                                    .currentThreadClassLoader()
                                    .get());
                }
                return doInvoke(context);
            } catch (InvokerException e) {
                throw e;
            } catch (Exception e) {
                throw handleException(context, e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
            System.setProperties(oldProps);
        }
    }

    protected int doInvoke(C context) throws Exception {
        validate(context);
        pushCoreProperties(context);
        pushUserProperties(context);
        configureLogging(context);
        createTerminal(context);
        activateLogging(context);
        helpOrVersionAndMayExit(context);
        preCommands(context);
        container(context);
        postContainer(context);
        pushUserProperties(context); // after PropertyContributor SPI
        lookup(context);
        init(context);
        postCommands(context);
        settings(context);
        return execute(context);
    }

    protected InvokerException handleException(C context, Exception e) throws InvokerException {
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

    protected abstract C createContext(InvokerRequest invokerRequest) throws InvokerException;

    protected void validate(C context) throws InvokerException {
        // in case of parser error: report errors and bail out; invokerRequest contents may be incomplete
        if (!context.invokerRequest.parserErrors().isEmpty()) {
            context.logger.error(
                    "There were " + context.invokerRequest.parserErrors().size() + " parsing errors:");
            for (ParserException parserException : context.invokerRequest.parserErrors()) {
                context.logger.error(parserException.getMessage());
            }
            throw new InvokerException.ExitException(1);
        }

        // warn about deprecated options
        context.invokerRequest
                .options()
                .warnAboutDeprecatedOptions(context.invokerRequest.parserRequest(), s -> context.logger.warn(s));
    }

    protected void pushCoreProperties(C context) throws Exception {
        System.setProperty(
                Constants.MAVEN_HOME,
                context.invokerRequest.installationDirectory().toString());
    }

    /**
     * Note: this method is called twice from {@link #doInvoke(LookupContext)} and modifies context. First invocation
     * when {@link LookupContext#pushedUserProperties} is null will push user properties IF key does not already
     * exist among Java System Properties, and collects all they key it pushes. Second invocation happens AFTER
     * {@link PropertyContributor} SPI invocation, and "refreshes" already pushed user properties by re-writing them
     * as SPI may have modified them.
     */
    protected void pushUserProperties(C context) throws Exception {
        ProtoSession protoSession = context.protoSession;
        HashSet<String> sys = new HashSet<>(protoSession.getSystemProperties().keySet());
        if (context.pushedUserProperties == null) {
            context.pushedUserProperties = new HashSet<>();
            protoSession.getUserProperties().entrySet().stream()
                    .filter(k -> !sys.contains(k.getKey()))
                    .peek(k -> context.pushedUserProperties.add(k.getKey()))
                    .forEach(k -> System.setProperty(k.getKey(), k.getValue()));
        } else {
            protoSession.getUserProperties().entrySet().stream()
                    .filter(k -> context.pushedUserProperties.contains(k.getKey()) || !sys.contains(k.getKey()))
                    .forEach(k -> System.setProperty(k.getKey(), k.getValue()));
        }
    }

    protected void configureLogging(C context) throws Exception {
        // LOG COLOR
        Options mavenOptions = context.invokerRequest.options();
        Map<String, String> userProperties = context.protoSession.getUserProperties();
        String styleColor = mavenOptions
                .color()
                .orElse(userProperties.getOrDefault(
                        Constants.MAVEN_STYLE_COLOR_PROPERTY, userProperties.getOrDefault("style.color", "auto")));
        if ("always".equals(styleColor) || "yes".equals(styleColor) || "force".equals(styleColor)) {
            context.coloredOutput = true;
        } else if ("never".equals(styleColor) || "no".equals(styleColor) || "none".equals(styleColor)) {
            context.coloredOutput = false;
        } else if (!"auto".equals(styleColor) && !"tty".equals(styleColor) && !"if-tty".equals(styleColor)) {
            throw new IllegalArgumentException(
                    "Invalid color configuration value '" + styleColor + "'. Supported are 'auto', 'always', 'never'.");
        } else {
            boolean isBatchMode = !mavenOptions.forceInteractive().orElse(false)
                    && mavenOptions.nonInteractive().orElse(false);
            if (isBatchMode || mavenOptions.logFile().isPresent()) {
                context.coloredOutput = false;
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

        // Create the build log appender; also sets MavenSimpleLogger sink
        ProjectBuildLogAppender projectBuildLogAppender =
                new ProjectBuildLogAppender(determineBuildEventListener(context));
        context.closeables.add(projectBuildLogAppender);
    }

    protected BuildEventListener determineBuildEventListener(C context) {
        if (context.buildEventListener == null) {
            context.buildEventListener = doDetermineBuildEventListener(context);
        }
        return context.buildEventListener;
    }

    protected BuildEventListener doDetermineBuildEventListener(C context) {
        Consumer<String> writer = determineWriter(context);
        return new SimpleBuildEventListener(writer);
    }

    protected void createTerminal(C context) {
        if (context.terminal == null) {
            MessageUtils.systemInstall(
                    builder -> {
                        builder.streams(
                                context.invokerRequest.in().orElse(null),
                                context.invokerRequest.out().orElse(null));
                        builder.systemOutput(TerminalBuilder.SystemOutput.ForcedSysOut);
                        // The exec builder suffers from https://github.com/jline/jline3/issues/1098
                        // We could re-enable it when fixed to provide support for non-standard architectures,
                        // for which JLine does not provide any native library.
                        builder.exec(false);
                        if (context.coloredOutput != null) {
                            builder.color(context.coloredOutput);
                        }
                    },
                    terminal -> doConfigureWithTerminal(context, terminal));

            context.terminal = MessageUtils.getTerminal();
            // JLine is quite slow to start due to the native library unpacking and loading
            // so boot it asynchronously
            context.closeables.add(MessageUtils::systemUninstall);
            MessageUtils.registerShutdownHook(); // safety belt
            if (context.coloredOutput != null) {
                MessageUtils.setColorEnabled(context.coloredOutput);
            }
        } else {
            if (context.coloredOutput != null) {
                MessageUtils.setColorEnabled(context.coloredOutput);
            }
        }
    }

    protected void doConfigureWithTerminal(C context, Terminal terminal) {
        context.terminal = terminal;
        Options options = context.invokerRequest.options();
        if (options.rawStreams().isEmpty() || !options.rawStreams().get()) {
            MavenSimpleLogger stdout = (MavenSimpleLogger) context.loggerFactory.getLogger("stdout");
            MavenSimpleLogger stderr = (MavenSimpleLogger) context.loggerFactory.getLogger("stderr");
            stdout.setLogLevel(LocationAwareLogger.INFO_INT);
            stderr.setLogLevel(LocationAwareLogger.INFO_INT);
            System.setOut(new LoggingOutputStream(s -> stdout.info("[stdout] " + s)).printStream());
            System.setErr(new LoggingOutputStream(s -> stderr.warn("[stderr] " + s)).printStream());
            // no need to set them back, this is already handled by MessageUtils.systemUninstall() above
        }
    }

    protected Consumer<String> determineWriter(C context) {
        if (context.writer == null) {
            context.writer = doDetermineWriter(context);
        }
        return context.writer;
    }

    protected Consumer<String> doDetermineWriter(C context) {
        Options options = context.invokerRequest.options();
        if (options.logFile().isPresent()) {
            Path logFile = context.cwdResolver.apply(options.logFile().get());
            try {
                PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(logFile), true);
                context.closeables.add(printWriter);
                return printWriter::println;
            } catch (IOException e) {
                throw new MavenException("Unable to redirect logging to " + logFile, e);
            }
        } else {
            // Given the terminal creation has been offloaded to a different thread,
            // do not pass directly the terminal writer
            return msg -> {
                PrintWriter pw = context.terminal.writer();
                pw.println(msg);
                pw.flush();
            };
        }
    }

    protected void activateLogging(C context) throws Exception {
        InvokerRequest invokerRequest = context.invokerRequest;
        Options mavenOptions = invokerRequest.options();

        context.slf4jConfiguration.activate();
        org.slf4j.Logger l = context.loggerFactory.getLogger(this.getClass().getName());
        context.logger = (level, message, error) -> l.atLevel(org.slf4j.event.Level.valueOf(level.name()))
                .setCause(error)
                .log(message);

        if (mavenOptions.failOnSeverity().isPresent()) {
            String logLevelThreshold = mavenOptions.failOnSeverity().get();
            if (context.loggerFactory instanceof LogLevelRecorder recorder) {
                LogLevelRecorder.Level level =
                        switch (logLevelThreshold.toLowerCase(Locale.ENGLISH)) {
                            case "warn", "warning" -> LogLevelRecorder.Level.WARN;
                            case "error" -> LogLevelRecorder.Level.ERROR;
                            default -> throw new IllegalArgumentException(
                                    logLevelThreshold
                                            + " is not a valid log severity threshold. Valid severities are WARN/WARNING and ERROR.");
                        };
                recorder.setMaxLevelAllowed(level);
                context.logger.info("Enabled to break the build on log level " + logLevelThreshold + ".");
            } else {
                context.logger.warn("Expected LoggerFactory to be of type '" + LogLevelRecorder.class.getName()
                        + "', but found '"
                        + context.loggerFactory.getClass().getName() + "' instead. "
                        + "The --fail-on-severity flag will not take effect.");
            }
        }
    }

    protected void helpOrVersionAndMayExit(C context) throws Exception {
        InvokerRequest invokerRequest = context.invokerRequest;
        if (invokerRequest.options().help().isPresent()) {
            Consumer<String> writer = determineWriter(context);
            invokerRequest.options().displayHelp(context.invokerRequest.parserRequest(), writer);
            throw new InvokerException.ExitException(0);
        }
        if (invokerRequest.options().showVersionAndExit().isPresent()) {
            showVersion(context);
            throw new InvokerException.ExitException(0);
        }
    }

    protected void showVersion(C context) {
        Consumer<String> writer = determineWriter(context);
        InvokerRequest invokerRequest = context.invokerRequest;
        if (invokerRequest.options().quiet().orElse(false)) {
            writer.accept(CLIReportingUtils.showVersionMinimal());
        } else if (invokerRequest.options().verbose().orElse(false)) {
            writer.accept(CLIReportingUtils.showVersion(
                    ProcessHandle.current().info().commandLine().orElse(null), describe(context.terminal)));

        } else {
            writer.accept(CLIReportingUtils.showVersion());
        }
    }

    protected String describe(Terminal terminal) {
        if (terminal == null) {
            return null;
        }
        if (terminal instanceof FastTerminal ft) {
            terminal = ft.getTerminal();
        }
        List<String> subs = new ArrayList<>();
        subs.add("type=" + terminal.getType());
        if (terminal instanceof TerminalExt te) {
            subs.add("provider=" + te.getProvider().name());
        }
        if (terminal instanceof AbstractPosixTerminal pt) {
            subs.add("pty=" + pt.getPty().getClass().getName());
        }
        return terminal.getClass().getSimpleName() + " (" + String.join(", ", subs) + ")";
    }

    protected void preCommands(C context) throws Exception {
        Options mavenOptions = context.invokerRequest.options();
        boolean verbose = mavenOptions.verbose().orElse(false);
        boolean version = mavenOptions.showVersion().orElse(false);
        if (verbose || version) {
            showVersion(context);
        }
    }

    protected void container(C context) throws Exception {
        if (context.lookup == null) {
            context.containerCapsule = createContainerCapsuleFactory().createContainerCapsule(this, context);
            context.closeables.add(context::closeContainer);
            context.lookup = context.containerCapsule.getLookup();
        } else {
            context.containerCapsule.updateLogging(context);
        }
    }

    protected ContainerCapsuleFactory<C> createContainerCapsuleFactory() {
        return new PlexusContainerCapsuleFactory<>();
    }

    protected void postContainer(C context) throws Exception {
        ProtoSession protoSession = context.protoSession;
        for (PropertyContributor propertyContributor : context.lookup
                .lookup(PropertyContributorsHolder.class)
                .getPropertyContributors()
                .values()) {
            protoSession = protoSession.toBuilder()
                    .withUserProperties(propertyContributor.contribute(protoSession))
                    .build();
        }
        context.protoSession = protoSession;
    }

    protected void lookup(C context) throws Exception {
        if (context.eventSpyDispatcher == null) {
            context.eventSpyDispatcher = context.lookup.lookup(EventSpyDispatcher.class);
        }
    }

    protected void init(C context) throws Exception {
        InvokerRequest invokerRequest = context.invokerRequest;
        Map<String, Object> data = new HashMap<>();
        data.put("plexus", context.lookup.lookup(PlexusContainer.class));
        data.put("workingDirectory", invokerRequest.cwd().toString());
        data.put("systemProperties", toProperties(context.protoSession.getSystemProperties()));
        data.put("userProperties", toProperties(context.protoSession.getUserProperties()));
        data.put("versionProperties", CLIReportingUtils.getBuildProperties());
        context.eventSpyDispatcher.init(() -> data);
    }

    protected void postCommands(C context) throws Exception {
        InvokerRequest invokerRequest = context.invokerRequest;
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
        if (context.effectiveSettings == null) {
            settings(context, true, context.lookup.lookup(SettingsBuilder.class));
        }
    }

    /**
     * This method is invoked twice during "normal" LookupInvoker level startup: once when (if present) extensions
     * are loaded up during Plexus DI creation, and once afterward as "normal" boot procedure.
     * <p>
     * If there are Maven3 passwords presents in settings, this results in doubled warnings emitted. So Plexus DI
     * creation call keeps "emitSettingsWarnings" false. If there are fatal issues, it will anyway "die" at that
     * spot before warnings would be emitted.
     * <p>
     * The method returns a "cleaner" runnable, as during extension loading the context needs to be "cleaned", restored
     * to previous state (as it was before extension loading).
     */
    protected Runnable settings(C context, boolean emitSettingsWarnings, SettingsBuilder settingsBuilder)
            throws Exception {
        Options mavenOptions = context.invokerRequest.options();

        Path userSettingsFile = null;
        if (mavenOptions.altUserSettings().isPresent()) {
            userSettingsFile =
                    context.cwdResolver.apply(mavenOptions.altUserSettings().get());

            if (!Files.isRegularFile(userSettingsFile)) {
                throw new FileNotFoundException("The specified user settings file does not exist: " + userSettingsFile);
            }
        } else {
            String userSettingsFileStr =
                    context.protoSession.getUserProperties().get(Constants.MAVEN_USER_SETTINGS);
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
                    context.protoSession.getUserProperties().get(Constants.MAVEN_PROJECT_SETTINGS);
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
                    context.protoSession.getUserProperties().get(Constants.MAVEN_INSTALLATION_SETTINGS);
            if (installationSettingsFileStr != null) {
                installationSettingsFile = context.installationResolver.apply(installationSettingsFileStr);
            }
        }

        context.installationSettingsPath = installationSettingsFile;
        context.projectSettingsPath = projectSettingsFile;
        context.userSettingsPath = userSettingsFile;

        UnaryOperator<String> interpolationSource = Interpolator.chain(
                context.protoSession.getUserProperties()::get, context.protoSession.getSystemProperties()::get);
        SettingsBuilderRequest settingsRequest = SettingsBuilderRequest.builder()
                .session(context.protoSession)
                .installationSettingsSource(
                        installationSettingsFile != null && Files.exists(installationSettingsFile)
                                ? Sources.fromPath(installationSettingsFile)
                                : null)
                .projectSettingsSource(
                        projectSettingsFile != null && Files.exists(projectSettingsFile)
                                ? Sources.fromPath(projectSettingsFile)
                                : null)
                .userSettingsSource(
                        userSettingsFile != null && Files.exists(userSettingsFile)
                                ? Sources.fromPath(userSettingsFile)
                                : null)
                .interpolationSource(interpolationSource)
                .build();

        customizeSettingsRequest(context, settingsRequest);
        if (context.eventSpyDispatcher != null) {
            context.eventSpyDispatcher.onEvent(settingsRequest);
        }

        context.logger.debug("Reading installation settings from '" + installationSettingsFile + "'");
        context.logger.debug("Reading project settings from '" + projectSettingsFile + "'");
        context.logger.debug("Reading user settings from '" + userSettingsFile + "'");

        SettingsBuilderResult settingsResult = settingsBuilder.build(settingsRequest);
        customizeSettingsResult(context, settingsResult);
        if (context.eventSpyDispatcher != null) {
            context.eventSpyDispatcher.onEvent(settingsResult);
        }

        context.effectiveSettings = settingsResult.getEffectiveSettings();
        context.interactive = mayDisableInteractiveMode(context, context.effectiveSettings.isInteractiveMode());
        context.localRepositoryPath = localRepositoryPath(context);

        if (emitSettingsWarnings && settingsResult.getProblems().hasWarningProblems()) {
            int totalProblems = settingsResult.getProblems().totalProblemsReported();
            context.logger.info("");
            context.logger.info(String.format(
                    "%s %s encountered while building the effective settings (use -e to see details)",
                    totalProblems, (totalProblems == 1) ? "problem was" : "problems were"));

            if (context.invokerRequest.options().showErrors().orElse(false)) {
                for (BuilderProblem problem :
                        settingsResult.getProblems().problems().toList()) {
                    context.logger.warn(problem.getMessage() + " @ " + problem.getLocation());
                }
            }
            context.logger.info("");
        }
        return () -> {
            context.installationSettingsPath = null;
            context.projectSettingsPath = null;
            context.userSettingsPath = null;
            context.effectiveSettings = null;
            context.interactive = true;
            context.localRepositoryPath = null;
        };
    }

    protected void customizeSettingsRequest(C context, SettingsBuilderRequest settingsBuilderRequest)
            throws Exception {}

    protected void customizeSettingsResult(C context, SettingsBuilderResult settingsBuilderResult) throws Exception {}

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
        String userDefinedLocalRepo = context.protoSession.getUserProperties().get(Constants.MAVEN_REPO_LOCAL);
        if (userDefinedLocalRepo == null) {
            userDefinedLocalRepo = context.protoSession.getUserProperties().get(Constants.MAVEN_REPO_LOCAL);
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
        if (userDefinedLocalRepo != null && !userDefinedLocalRepo.isEmpty()) {
            return context.userResolver.apply(userDefinedLocalRepo);
        }
        // defaults
        return context.userResolver
                .apply(context.protoSession.getUserProperties().get(Constants.MAVEN_USER_CONF))
                .resolve("repository");
    }

    protected void populateRequest(C context, Lookup lookup, MavenExecutionRequest request) throws Exception {
        populateRequestFromSettings(request, context.effectiveSettings);

        Options options = context.invokerRequest.options();
        request.setLoggingLevel(toMavenExecutionRequestLoggingLevel(context.loggerLevel));
        request.setLocalRepositoryPath(context.localRepositoryPath.toFile());
        request.setLocalRepository(createLocalArtifactRepository(context.localRepositoryPath));

        request.setInteractiveMode(context.interactive);
        request.setShowErrors(options.showErrors().orElse(false));
        request.setBaseDirectory(context.invokerRequest.topDirectory().toFile());
        request.setSystemProperties(toProperties(context.protoSession.getSystemProperties()));
        request.setUserProperties(toProperties(context.protoSession.getUserProperties()));

        request.setInstallationSettingsFile(
                context.installationSettingsPath != null ? context.installationSettingsPath.toFile() : null);
        request.setProjectSettingsFile(
                context.projectSettingsPath != null ? context.projectSettingsPath.toFile() : null);
        request.setUserSettingsFile(context.userSettingsPath != null ? context.userSettingsPath.toFile() : null);

        request.setTopDirectory(context.invokerRequest.topDirectory());
        if (context.invokerRequest.rootDirectory().isPresent()) {
            request.setMultiModuleProjectDirectory(
                    context.invokerRequest.rootDirectory().get().toFile());
            request.setRootDirectory(context.invokerRequest.rootDirectory().get());
        }

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
            request.addServer(new org.apache.maven.settings.Server(server));
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
            request.addProxy(new org.apache.maven.settings.Proxy(proxy));
        }

        // <mirrors>
        //   <mirror>
        //     <id>nexus</id>
        //     <mirrorOf>*</mirrorOf>
        //     <url>http://repository.sonatype.org/content/groups/public</url>
        //   </mirror>
        // </mirrors>

        for (Mirror mirror : settings.getMirrors()) {
            request.addMirror(new org.apache.maven.settings.Mirror(mirror));
        }

        // Collect repositories; are sensitive to ordering
        LinkedHashMap<String, Repository> remoteRepositories = new LinkedHashMap<>();
        LinkedHashMap<String, Repository> remotePluginRepositories = new LinkedHashMap<>();

        // settings/repositories
        for (Repository remoteRepository : settings.getRepositories()) {
            remoteRepositories.put(remoteRepository.getId(), remoteRepository);
        }
        for (Repository pluginRepository : settings.getPluginRepositories()) {
            remotePluginRepositories.put(pluginRepository.getId(), pluginRepository);
        }

        // profiles (if active)
        for (Profile rawProfile : settings.getProfiles()) {
            request.addProfile(
                    new org.apache.maven.model.Profile(SettingsUtilsV4.convertFromSettingsProfile(rawProfile)));

            if (settings.getActiveProfiles().contains(rawProfile.getId())) {
                for (Repository remoteRepository : rawProfile.getRepositories()) {
                    remoteRepositories.put(remoteRepository.getId(), remoteRepository);
                }

                for (Repository pluginRepository : rawProfile.getPluginRepositories()) {
                    remotePluginRepositories.put(pluginRepository.getId(), pluginRepository);
                }
            }
        }

        // pour onto request
        request.setActiveProfiles(settings.getActiveProfiles());
        request.setRemoteRepositories(remoteRepositories.values().stream()
                .map(r -> {
                    try {
                        return MavenRepositorySystem.buildArtifactRepository(
                                new org.apache.maven.settings.Repository(r));
                    } catch (Exception e) {
                        // nothing currently
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList());
        request.setPluginArtifactRepositories(remotePluginRepositories.values().stream()
                .map(r -> {
                    try {
                        return MavenRepositorySystem.buildArtifactRepository(
                                new org.apache.maven.settings.Repository(r));
                    } catch (Exception e) {
                        // nothing currently
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList());
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
        String ciEnv = context.protoSession.getSystemProperties().get("env.CI");
        return ciEnv != null && !"false".equals(ciEnv);
    }

    protected abstract int execute(C context) throws Exception;
}
