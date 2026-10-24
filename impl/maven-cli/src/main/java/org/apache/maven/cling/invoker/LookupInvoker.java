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
import java.io.OutputStream;
import java.io.PrintStream;
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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.apache.maven.api.Constants;
import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.cisupport.CIInfo;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.Logger;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.api.services.BuilderProblem;
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
import org.apache.maven.logging.jul.MavenJulHandler;
import org.codehaus.plexus.PlexusContainer;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.AbstractPosixTerminal;
import org.jline.terminal.spi.TerminalExt;
import org.jline.utils.OSUtils;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.CliUtils.toMavenExecutionRequestLoggingLevel;
import static org.apache.maven.cling.invoker.CliUtils.toProperties;

/**
 * Lookup invoker implementation, that boots up DI container.
 *
 * @param <C> The context type.
 */
public abstract class LookupInvoker<C extends LookupContext> implements Invoker {
    private static final int SIGINT_EXIT_CODE = 130;

    protected final Lookup protoLookup;

    @Nullable
    protected final Consumer<LookupContext> contextConsumer;

    public LookupInvoker(Lookup protoLookup, @Nullable Consumer<LookupContext> contextConsumer) {
        this.protoLookup = requireNonNull(protoLookup);
        this.contextConsumer = contextConsumer;
    }

    @Override
    public final int invoke(InvokerRequest invokerRequest) {
        requireNonNull(invokerRequest);

        Properties oldProps = new Properties();
        oldProps.putAll(System.getProperties());
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        // activateLogging() replaces them with MavenJulHandler, whose classes live in Maven's realm
        java.util.logging.Logger julRoot = java.util.logging.Logger.getLogger("");
        // Only manipulate JUL root handlers when the default LogManager is active.
        // Embedded-Maven scenarios (IDE plugins, app servers) may run a custom
        // LogManager (e.g. Log4j2-JUL); trampling their handler list causes
        // the host's logging to break after the Maven invocation completes.
        boolean isDefaultLogManager = LogManager.getLogManager().getClass() == LogManager.class;
        Handler[] oldRootLoggerHandlers = isDefaultLogManager ? julRoot.getHandlers() : new Handler[0];
        java.util.logging.Level oldRootLoggerLevel = isDefaultLogManager ? julRoot.getLevel() : null;
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
            } catch (InvokerException.ExitException e) {
                // contract of ExitException is that nothing needed by us
                throw e;
            } catch (Exception e) {
                // other exceptions (including InvokerException but sans Exit, see above): we need to inform user
                throw handleException(context, e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
            System.setProperties(oldProps);
            restoreRootLoggerHandlers(oldRootLoggerHandlers, oldRootLoggerLevel);
        }
    }

    /**
     * Puts back the handlers and root level the {@code java.util.logging} root logger had before the invocation.
     * When Maven runs inside another JVM, the root logger belongs to that JVM, and a {@code MavenJulHandler} left
     * on it would route the host's logging to Maven and fail once Maven's class realm is closed. Similarly, the
     * level change (e.g. {@code SEVERE} in quiet mode) must be reverted so the host's logging is not suppressed
     * after the invocation completes.
     */
    private static void restoreRootLoggerHandlers(Handler[] handlers, java.util.logging.Level level) {
        if (handlers.length == 0 && level == null) {
            return; // custom LogManager — we didn't touch anything
        }
        java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        for (Handler handler : handlers) {
            root.addHandler(handler);
        }
        if (level != null) {
            root.setLevel(level);
        }
    }

    protected int doInvoke(C context) throws Exception {
        validate(context);
        pushCoreProperties(context);
        pushUserProperties(context);
        setupGuiceClassLoading(context);
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

    protected InvokerException.ExitException handleException(C context, Exception e) {
        printError(context, context.options().showErrors().orElse(false), e.getMessage(), e.getCause());
        return new InvokerException.ExitException(2);
    }

    protected void printError(C context, boolean showStackTrace, String message, Throwable error) {
        // Write to stderr directly — this is the fatal error exit path and must
        // be visible even if the logging infrastructure is not fully set up.
        PrintWriter err = new PrintWriter(context.invokerRequest.stdErr().orElse(System.err), true);
        err.println("[ERROR] Error executing "
                + context.invokerRequest.parserRequest().commandName() + ".");
        if (showStackTrace && error != null) {
            err.println("[ERROR] " + message);
            error.printStackTrace(err);
        } else {
            err.println("[ERROR] " + message);
            for (Throwable cause = error; cause != null && cause != cause.getCause(); cause = cause.getCause()) {
                err.println("[ERROR] Caused by: " + cause.getMessage());
            }
        }
    }

    protected abstract C createContext(InvokerRequest invokerRequest);

    @SuppressWarnings("deprecation")
    protected void validate(C context) throws Exception {
        if (context.invokerRequest.parsingFailed()) {
            boolean showStackTrace = context.invokerRequest
                    .parserRequest()
                    .args()
                    .contains(CommonsCliOptions.CLIManager.SHOW_ERRORS_CLI_ARG);
            // Drain parser errors and write to stderr
            List<org.apache.maven.api.cli.Logger.Entry> entries =
                    context.invokerRequest.parserRequest().logger().drain();
            PrintWriter err = new PrintWriter(context.invokerRequest.stdErr().orElse(System.err), true);
            err.println("[ERROR] Error executing "
                    + context.invokerRequest.parserRequest().commandName() + ".");
            for (org.apache.maven.api.cli.Logger.Entry entry : entries) {
                err.println("[ERROR] " + entry.message());
                if (showStackTrace && entry.error() != null) {
                    entry.error().printStackTrace(err);
                }
            }
            throw new InvokerException.ExitException(1);
        }

        // warn about deprecated options
        context.options().warnAboutDeprecatedOptions(context.invokerRequest.parserRequest(), context.logger::warn);
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

    /**
     * Sets up Guice class loading mode to CHILD, if not already set.
     * Default Guice class loading mode uses a terminally deprecated JDK memory-access classes.
     */
    protected void setupGuiceClassLoading(C context) {
        if (System.getProperty("guice_custom_class_loading", "").isBlank()) {
            System.setProperty("guice_custom_class_loading", "CHILD");
        }
    }

    protected void configureLogging(C context) throws Exception {
        // LOG COLOR
        Map<String, String> effectiveProperties = context.protoSession.getEffectiveProperties();
        String styleColor = context.options()
                .color()
                .orElse(effectiveProperties.getOrDefault(
                        Constants.MAVEN_STYLE_COLOR_PROPERTY, effectiveProperties.getOrDefault("style.color", "auto")))
                .toLowerCase(Locale.ENGLISH);
        if ("always".equals(styleColor) || "yes".equals(styleColor) || "force".equals(styleColor)) {
            context.coloredOutput = true;
        } else if ("never".equals(styleColor) || "no".equals(styleColor) || "none".equals(styleColor)) {
            context.coloredOutput = false;
        } else if (!"auto".equals(styleColor) && !"tty".equals(styleColor) && !"if-tty".equals(styleColor)) {
            throw new IllegalArgumentException(
                    "Invalid color configuration value '" + styleColor + "'. Supported are 'auto', 'always', 'never'.");
        } else {
            boolean isBatchMode = !context.options().forceInteractive().orElse(false)
                    && context.options().nonInteractive().orElse(false);
            if (isBatchMode || context.options().logFile().isPresent()) {
                context.coloredOutput = false;
            }
        }

        context.loggerFactory = LoggerFactory.getILoggerFactory();

        if (context.invokerRequest.effectiveVerbose()) {
            context.loggerLevel = LogLevel.DEBUG;
            LoggerManager.get().setRootLoggerLevel(context.loggerLevel);
        } else if (context.options().quiet().orElse(false)) {
            context.loggerLevel = LogLevel.ERROR;
            LoggerManager.get().setRootLoggerLevel(context.loggerLevel);
            // Install the JUL handler early and clamp the JUL root to SEVERE
            // so that JLine terminal-init DEBUG events (emitted via java.util.logging
            // before activateLogging() runs) are suppressed at source.
            // createTerminal() executes after configureLogging() but before
            // activateLogging(), so without this guard those DEBUG lines leak
            // into the build output even when -q is in effect, causing
            // MavenITmng4387QuietLoggingTest to fail on Windows.
            // Setting SEVERE here is safe: it only blocks events, so it
            // cannot trigger the SLF4J-bootstrap reentrancy flood that
            // Level.ALL would cause.
            LogManager logManager = LogManager.getLogManager();
            if (logManager.getClass() == LogManager.class && !MavenJulHandler.isInstalled()) {
                MavenJulHandler.install();
            }
            if (logManager.getClass() == LogManager.class) {
                logManager.getLogger("").setLevel(Level.SEVERE);
            }
        } else {
            // fall back to default log level specified in conf
            // see https://issues.apache.org/jira/browse/MNG-2570 and https://github.com/apache/maven/issues/11199
            context.loggerLevel = LogLevel.INFO; // default for display purposes
        }
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

    protected final void createTerminal(C context) {
        if (context.terminal == null) {
            MessageUtils.systemInstall(builder -> doCreateTerminal(context, builder), terminal -> {
                if (OSUtils.IS_WINDOWS && !context.invokerRequest.embedded()) {
                    // JLine may consume Ctrl+C as terminal input, bypassing the JVM's shutdown hooks.
                    terminal.handle(Terminal.Signal.INT, signal -> System.exit(SIGINT_EXIT_CODE));
                }
                doConfigureWithTerminal(context, terminal);
            });

            context.terminal = MessageUtils.getTerminal();
            context.closeables.add(MessageUtils::systemUninstall);
            MessageUtils.registerShutdownHook(); // safety belt

            // Create the build log appender; also sets MavenSimpleLogger sink
            ProjectBuildLogAppender projectBuildLogAppender =
                    new ProjectBuildLogAppender(determineBuildEventListener(context));
            context.closeables.add(projectBuildLogAppender);
        } else {
            doConfigureWithTerminal(context, context.terminal);
        }
    }

    /**
     * Override this method to create Terminal as you want.
     *
     * @see #createTerminal(LookupContext)
     */
    protected void doCreateTerminal(C context, TerminalBuilder builder) {
        if (context.invokerRequest.embedded()) {
            InputStream in = context.invokerRequest.stdIn().orElse(InputStream.nullInputStream());
            OutputStream out = context.invokerRequest.stdOut().orElse(OutputStream.nullOutputStream());
            builder.streams(in, out);
            builder.provider(TerminalBuilder.PROP_PROVIDER_EXEC);
            context.coloredOutput = context.coloredOutput != null ? context.coloredOutput : false;
            context.closeables.add(out::flush);
        } else {
            builder.systemOutput(TerminalBuilder.SystemOutput.SysOut);
        }
        if (context.coloredOutput != null) {
            builder.color(context.coloredOutput);
        }
    }

    /**
     * Called from {@link #createTerminal(LookupContext)} when Terminal was built.
     */
    protected final void doConfigureWithTerminal(C context, Terminal terminal) {
        context.terminal = terminal;
        // Align Maven's color setting with JLine's terminal detection:
        // if embedded, we default to context.coloredOutput=false unless overridden (see above)
        // if not embedded, JLine detects redirection via SysOut and will create dumb terminal.
        // To align Maven with outcomes, we set here color enabled based on these premises.
        MessageUtils.setColorEnabled(
                context.coloredOutput != null ? context.coloredOutput : !Terminal.TYPE_DUMB.equals(terminal.getType()));

        // handle rawStreams: some would like to act on true, some on false
        // quiet mode implies raw streams — plugins that write to System.out (e.g. help:evaluate
        // -DforceStdout) must not be decorated with "[INFO] [stdout]" prefix, otherwise scripting
        // use cases like $(mvn -q help:evaluate ...) break (see GH-12730)
        if (context.options().rawStreams().orElse(false)
                || context.options().quiet().orElse(false)) {
            doConfigureWithTerminalWithRawStreamsEnabled(context);
        } else {
            doConfigureWithTerminalWithRawStreamsDisabled(context);
        }
    }

    /**
     * Override this method to add some special handling for "raw streams" <em>enabled</em> option.
     */
    protected void doConfigureWithTerminalWithRawStreamsEnabled(C context) {
        context.invokerRequest.stdIn().ifPresent(System::setIn);
        context.invokerRequest
                .stdOut()
                .ifPresent(out -> System.setOut(out instanceof PrintStream pw ? pw : new PrintStream(out, true)));
        context.invokerRequest
                .stdErr()
                .ifPresent(err -> System.setErr(err instanceof PrintStream pw ? pw : new PrintStream(err, true)));
    }

    /**
     * Override this method to add some special handling for "raw streams" <em>disabled</em> option.
     */
    protected void doConfigureWithTerminalWithRawStreamsDisabled(C context) {
        Logger stdout = LoggerManager.get().getLogger("stdout");
        Logger stderr = LoggerManager.get().getLogger("stderr");
        PrintStream psOut = new LoggingOutputStream(s -> stdout.atInfo().log("[stdout] " + s)).printStream();
        context.closeables.add(() -> LoggingOutputStream.forceFlush(psOut));
        PrintStream psErr = new LoggingOutputStream(s -> stderr.atWarn().log("[stderr] " + s)).printStream();
        context.closeables.add(() -> LoggingOutputStream.forceFlush(psErr));
        System.setOut(psOut);
        System.setErr(psErr);
    }

    protected Consumer<String> determineWriter(C context) {
        if (context.writer == null) {
            context.writer = doDetermineWriter(context);
        }
        return context.writer;
    }

    protected Consumer<String> doDetermineWriter(C context) {
        Consumer<String> raw;
        if (context.options().logFile().isPresent()) {
            Path logFile = context.cwd.resolve(context.options().logFile().get());
            try {
                PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(logFile), true);
                context.closeables.add(printWriter);
                raw = printWriter::println;
            } catch (IOException e) {
                throw new MavenException("Unable to redirect logging to " + logFile, e);
            }
        } else {
            // Given the terminal creation has been offloaded to a different thread,
            // do not pass directly the terminal writer
            raw = msg -> {
                PrintWriter pw = context.terminal.writer();
                pw.println(msg);
                pw.flush();
            };
        }
        return raw;
    }

    protected void activateLogging(C context) throws Exception {
        LogManager logManager = LogManager.getLogManager();
        if (logManager.getClass() == LogManager.class && !MavenJulHandler.isInstalled()) {
            MavenJulHandler.install();
        }

        LoggerManager.get().reconfigure();

        // Now that SLF4J is fully initialized, set the JUL root logger level
        // to match the effective log level.  This must happen AFTER install()
        // + activate() to avoid flooding JUL events during SLF4J bootstrap
        // (ConcurrentHashMap.computeIfAbsent reentrancy).
        // In quiet mode keep the JUL root at SEVERE so that INFO/WARN/DEBUG JUL
        // events are suppressed at source — relying solely on the SLF4J-level
        // check in MavenJulHandler.isLevelEnabled() is racy: newly created
        // SLF4J loggers may briefly see the default INFO level before
        // quiet-mode propagation completes, leaking output that
        // MavenITmng4387QuietLoggingTest detects as a flaky failure.
        // SEVERE matches the SLF4J ERROR threshold exactly, blocking both
        // INFO and WARNING JUL events at source during the race window.
        Level julRootLevel;
        if (context.options().quiet().orElse(false)) {
            julRootLevel = Level.SEVERE;
        } else if (context.invokerRequest.effectiveVerbose()) {
            julRootLevel = Level.ALL;
        } else {
            julRootLevel = Level.INFO;
        }
        if (logManager.getClass() == LogManager.class) {
            logManager.getLogger("").setLevel(julRootLevel);
        }
        if (context.options().failOnSeverity().isPresent()) {
            String logLevelThreshold = context.options().failOnSeverity().get();
            context.failOnSeverity = switch (logLevelThreshold.toLowerCase(Locale.ENGLISH)) {
                case "warn", "warning" -> LogLevel.WARN;
                case "error" -> LogLevel.ERROR;
                default ->
                    throw new IllegalArgumentException(logLevelThreshold
                            + " is not a valid log severity threshold. Valid severities are WARN/WARNING and ERROR.");
            };
            context.logger.info("Enabled to break the build on log level " + logLevelThreshold + ".");
        }
    }

    protected void helpOrVersionAndMayExit(C context) throws Exception {
        if (context.options().help().isPresent()) {
            Consumer<String> writer = determineWriter(context);
            context.options().displayHelp(context.invokerRequest.parserRequest(), writer);
            throw new InvokerException.ExitException(0);
        }
        if (context.options().showVersionAndExit().isPresent()) {
            showVersion(context);
            throw new InvokerException.ExitException(0);
        }
    }

    protected void showVersion(C context) {
        Consumer<String> writer = determineWriter(context);
        if (context.options().quiet().orElse(false)) {
            writer.accept(CLIReportingUtils.showVersionMinimal());
        } else if (context.invokerRequest.effectiveVerbose()) {
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
        boolean verbose = context.invokerRequest.effectiveVerbose();
        boolean version = context.options().showVersion().orElse(false) && !Boolean.getBoolean("maven.version.printed");
        if (verbose || version) {
            showVersion(context);
        }
    }

    protected void container(C context) throws Exception {
        if (context.lookup == null) {
            context.containerCapsule = createContainerCapsuleFactory()
                    .createContainerCapsule(this, context, createCoreExtensionSelector());
            context.closeables.add(context::closeContainer);
            context.lookup = context.containerCapsule.getLookup();
        } else {
            context.containerCapsule.updateLogging(context);
        }
    }

    protected CoreExtensionSelector<C> createCoreExtensionSelector() {
        return new PrecedenceCoreExtensionSelector<>();
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
        Map<String, Object> data = new HashMap<>();
        data.put("plexus", context.lookup.lookup(PlexusContainer.class));
        data.put("workingDirectory", context.cwd.get().toString());
        data.put("systemProperties", toProperties(context.protoSession.getSystemProperties()));
        data.put("userProperties", toProperties(context.protoSession.getUserProperties()));
        data.put("versionProperties", CLIReportingUtils.getBuildProperties());
        context.eventSpyDispatcher.init(() -> data);
    }

    protected void postCommands(C context) throws Exception {
        Logger logger = context.logger;
        if (context.options().showErrors().orElse(false)) {
            logger.info("Error stacktraces are turned on.");
        }
        if (context.options().verbose().orElse(false)) {
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
        Path userSettingsFile = null;
        if (context.options().altUserSettings().isPresent()) {
            userSettingsFile =
                    context.cwd.resolve(context.options().altUserSettings().get());

            if (!Files.isRegularFile(userSettingsFile)) {
                throw new FileNotFoundException("The specified user settings file does not exist: " + userSettingsFile);
            }
        } else {
            String userSettingsFileStr =
                    context.protoSession.getEffectiveProperties().get(Constants.MAVEN_USER_SETTINGS);
            if (userSettingsFileStr != null) {
                userSettingsFile =
                        context.userDirectory.resolve(userSettingsFileStr).normalize();
            }
        }

        Path projectSettingsFile = null;
        if (context.options().altProjectSettings().isPresent()) {
            projectSettingsFile =
                    context.cwd.resolve(context.options().altProjectSettings().get());

            if (!Files.isRegularFile(projectSettingsFile)) {
                throw new FileNotFoundException(
                        "The specified project settings file does not exist: " + projectSettingsFile);
            }
        } else {
            String projectSettingsFileStr =
                    context.protoSession.getEffectiveProperties().get(Constants.MAVEN_PROJECT_SETTINGS);
            if (projectSettingsFileStr != null) {
                projectSettingsFile = context.cwd.resolve(projectSettingsFileStr);
            }
        }

        Path installationSettingsFile = null;
        if (context.options().altInstallationSettings().isPresent()) {
            installationSettingsFile = context.cwd.resolve(
                    context.options().altInstallationSettings().get());

            if (!Files.isRegularFile(installationSettingsFile)) {
                throw new FileNotFoundException(
                        "The specified installation settings file does not exist: " + installationSettingsFile);
            }
        } else {
            String installationSettingsFileStr =
                    context.protoSession.getEffectiveProperties().get(Constants.MAVEN_INSTALLATION_SETTINGS);
            if (installationSettingsFileStr != null) {
                installationSettingsFile = context.installationDirectory
                        .resolve(installationSettingsFileStr)
                        .normalize();
            }
        }

        context.installationSettingsPath = installationSettingsFile;
        context.projectSettingsPath = projectSettingsFile;
        context.userSettingsPath = userSettingsFile;

        UnaryOperator<String> interpolationSource = context.protoSession.getEffectiveProperties()::get;
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

            if (context.options().showErrors().orElse(false)) {
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
        if (!context.options().forceInteractive().orElse(false)) {
            if (context.options().nonInteractive().orElse(false)) {
                return false;
            } else {
                if (context.invokerRequest.ciInfo().isPresent()) {
                    CIInfo ci = context.invokerRequest.ciInfo().get();
                    context.logger.info(
                            "Making this build non-interactive, because CI detected. Disable this detection by adding --force-interactive.");
                    context.logger.info("Detected CI system: '" + ci.name() + "': " + ci.message());
                    return false;
                }
            }
        }
        return proposedInteractive;
    }

    protected Path localRepositoryPath(C context) {
        // user override
        String userDefinedLocalRepo =
                context.protoSession.getEffectiveProperties().get(Constants.MAVEN_REPO_LOCAL);
        if (userDefinedLocalRepo == null) {
            userDefinedLocalRepo = context.protoSession.getEffectiveProperties().get(Constants.MAVEN_REPO_LOCAL);
            if (userDefinedLocalRepo != null) {
                context.logger.warn("The property '" + Constants.MAVEN_REPO_LOCAL
                        + "' has been set using a JVM system property which is deprecated. "
                        + "The property can be passed as a Maven argument or in the Maven project configuration file,"
                        + "usually located at ${session.rootDirectory}/.mvn/maven-user.properties.");
            }
        }
        if (userDefinedLocalRepo != null) {
            return context.cwd.resolve(userDefinedLocalRepo);
        }
        // settings
        userDefinedLocalRepo = context.effectiveSettings.getLocalRepository();
        if (userDefinedLocalRepo != null && !userDefinedLocalRepo.isEmpty()) {
            return context.userDirectory.resolve(userDefinedLocalRepo).normalize();
        }
        // defaults
        return context.userDirectory
                .resolve(context.protoSession.getEffectiveProperties().get(Constants.MAVEN_USER_CONF))
                .resolve("repository")
                .normalize();
    }

    protected void populateRequest(C context, Lookup lookup, MavenExecutionRequest request) throws Exception {
        populateRequestFromSettings(request, context.effectiveSettings);

        request.setLoggingLevel(toMavenExecutionRequestLoggingLevel(context.loggerLevel));
        request.setLocalRepositoryPath(context.localRepositoryPath.toFile());
        request.setLocalRepository(createLocalArtifactRepository(context.localRepositoryPath));

        request.setInteractiveMode(context.interactive);
        request.setShowErrors(context.options().showErrors().orElse(false));
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
        if ("max".equalsIgnoreCase(threadConfiguration)) {
            return Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        }
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
                    + "'. Supported are 'max', int and float values ending with C.");
        }
    }

    protected abstract int execute(C context) throws Exception;
}
