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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.AbstractModule;
import org.apache.maven.InternalErrorException;
import org.apache.maven.Maven;
import org.apache.maven.api.Constants;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.mvn.MavenInvoker;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Problem;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.cli.ExtensionConfigurationModule;
import org.apache.maven.cli.event.DefaultEventSpyContext;
import org.apache.maven.cli.event.ExecutionEventLogger;
import org.apache.maven.cli.internal.BootstrapCoreExtensionManager;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.cli.logging.Slf4jConfigurationFactory;
import org.apache.maven.cli.logging.Slf4jLoggerManager;
import org.apache.maven.cli.transfer.ConsoleMavenTransferListener;
import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.cli.transfer.SimplexTransferListener;
import org.apache.maven.cli.transfer.Slf4jMavenTransferListener;
import org.apache.maven.di.Injector;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ProfileActivation;
import org.apache.maven.execution.ProjectActivation;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.execution.scope.internal.MojoExecutionScopeModule;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.logwrapper.LogLevelRecorder;
import org.apache.maven.logwrapper.MavenSlf4jWrapperFactory;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.session.scope.internal.SessionScopeModule;
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
import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.transfer.TransferListener;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.Utils.toFile;
import static org.apache.maven.cling.invoker.Utils.toProperties;

/**
 * Local invoker implementation, when Maven CLI is being run. System uses ClassWorld launcher, and class world
 * instance is passed in via "enhanced" main method. Hence, this class expects fully setup ClassWorld via constructor.
 */
public class LocalInvoker implements MavenInvoker {

    protected static class LocalContext implements AutoCloseable {
        final MavenInvokerRequest invokerRequest;
        final Function<String, Path> cwdResolver;
        final InputStream stdIn;
        final PrintWriter stdOut;
        final PrintWriter stdErr;

        protected LocalContext(MavenInvokerRequest invokerRequest) {
            this.invokerRequest = requireNonNull(invokerRequest);
            this.cwdResolver = s -> invokerRequest.cwd().resolve(s).normalize().toAbsolutePath();
            this.stdIn = invokerRequest.in().orElse(System.in);
            this.stdOut = new PrintWriter(invokerRequest.out().orElse(System.out), true);
            this.stdErr = new PrintWriter(invokerRequest.err().orElse(System.err), true);
            this.logger = invokerRequest.logger();
        }

        Logger logger;
        DefaultMavenExecutionRequest mavenExecutionRequest;
        ILoggerFactory loggerFactory;
        PlexusContainer plexusContainer;
        EventSpyDispatcher eventSpyDispatcher;
        MavenExecutionRequestPopulator mavenExecutionRequestPopulator;
        SettingsBuilder settingsBuilder;
        ToolchainsBuilder toolchainsBuilder;
        ModelProcessor modelProcessor;
        Maven maven;

        @Override
        public void close() {
            if (plexusContainer != null) {
                plexusContainer.dispose();
            }
        }
    }

    private final ClassWorld classWorld;

    public LocalInvoker(ClassWorld classWorld) {
        this.classWorld = requireNonNull(classWorld);
    }

    @Override
    public int invoke(MavenInvokerRequest invokerRequest) throws InvokerException {
        requireNonNull(invokerRequest);

        try (LocalContext localContext = new LocalContext(invokerRequest)) {
            try {
                defaultMavenExecutionRequest(localContext);
                logging(localContext);

                if (invokerRequest.options().help().isPresent()) {
                    invokerRequest.options().displayHelp(localContext.stdOut);
                    return 0;
                }
                if (invokerRequest.options().showVersionAndExit().isPresent()) {
                    if (invokerRequest.options().quiet().orElse(false)) {
                        localContext.stdOut.println(CLIReportingUtils.showVersionMinimal());
                    } else {
                        localContext.stdOut.println(CLIReportingUtils.showVersion());
                    }
                    return 0;
                }

                validate(localContext);
                preCommands(localContext);
                container(localContext);
                postCommands(localContext);
                settings(localContext, localContext.settingsBuilder);
                toolchains(localContext);
                populateRequest(localContext, localContext.mavenExecutionRequest);
                return execute(localContext);
            } catch (Exception e) {
                CLIReportingUtils.showError(
                        localContext.logger,
                        "Error executing Maven.",
                        e,
                        invokerRequest.options().showErrors().orElse(false));
                throw new InvokerException(e.getMessage(), e);
            }
        }
    }

    protected void validate(LocalContext localContext) throws InvokerException {}

    protected void defaultMavenExecutionRequest(LocalContext localContext) {
        // explicitly fill in "defaults"?
        DefaultMavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();
        mavenExecutionRequest.setRepositoryCache(new DefaultRepositoryCache());
        mavenExecutionRequest.setInteractiveMode(true);
        mavenExecutionRequest.setIgnoreInvalidArtifactDescriptor(true);
        mavenExecutionRequest.setIgnoreMissingArtifactDescriptor(true);
        mavenExecutionRequest.setProjectPresent(true);
        mavenExecutionRequest.setRecursive(true);
        mavenExecutionRequest.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_FAST);
        mavenExecutionRequest.setStartTime(new Date());
        mavenExecutionRequest.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_INFO);
        mavenExecutionRequest.setDegreeOfConcurrency(1);
        mavenExecutionRequest.setBuilderId("singlethreaded");

        localContext.mavenExecutionRequest = mavenExecutionRequest;
    }

    protected void logging(LocalContext localContext) throws Exception {
        MavenInvokerRequest invokerRequest = localContext.invokerRequest;
        // LOG COLOR
        MavenOptions mavenOptions = invokerRequest.options();
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

        localContext.loggerFactory = LoggerFactory.getILoggerFactory();
        Slf4jConfiguration slf4jConfiguration = Slf4jConfigurationFactory.getConfiguration(localContext.loggerFactory);

        if (mavenOptions.verbose().orElse(false)) {
            localContext.mavenExecutionRequest.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_DEBUG);
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.DEBUG);
        } else if (mavenOptions.quiet().orElse(false)) {
            localContext.mavenExecutionRequest.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_ERROR);
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.ERROR);
        }
        // else fall back to default log level specified in conf
        // see https://issues.apache.org/jira/browse/MNG-2570

        // LOG STREAMS
        if (mavenOptions.logFile().isPresent()) {
            Path logFile = localContext.cwdResolver.apply(mavenOptions.logFile().get());
            // redirect stdout and stderr to file
            try {
                PrintStream ps = new PrintStream(Files.newOutputStream(logFile));
                System.setOut(ps);
                System.setErr(ps);
            } catch (IOException e) {
                throw new InvokerException("Cannot set up log " + e.getMessage(), e);
            }
        }

        slf4jConfiguration.activate();
        localContext.logger =
                localContext.loggerFactory.getLogger(this.getClass().getName());

        if (mavenOptions.failOnSeverity().isPresent()) {
            String logLevelThreshold = mavenOptions.failOnSeverity().get();

            if (localContext.loggerFactory instanceof MavenSlf4jWrapperFactory) {
                LogLevelRecorder logLevelRecorder = new LogLevelRecorder(logLevelThreshold);
                ((MavenSlf4jWrapperFactory) localContext.loggerFactory).setLogLevelRecorder(logLevelRecorder);
                localContext.logger.info("Enabled to break the build on log level {}.", logLevelThreshold);
            } else {
                localContext.logger.warn(
                        "Expected LoggerFactory to be of type '{}', but found '{}' instead. "
                                + "The --fail-on-severity flag will not take effect.",
                        MavenSlf4jWrapperFactory.class.getName(),
                        localContext.loggerFactory.getClass().getName());
            }
        }
    }

    protected void container(LocalContext localContext) throws Exception {
        MavenInvokerRequest invokerRequest = localContext.invokerRequest;
        ClassRealm coreRealm = classWorld.getClassRealm("plexus.core");
        List<Path> extClassPath = parseExtClasspath(localContext);
        CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom(coreRealm);
        List<CoreExtensionEntry> extensions =
                loadCoreExtensions(localContext, coreRealm, coreEntry.getExportedArtifacts());
        ClassRealm containerRealm =
                setupContainerRealm(localContext.logger, classWorld, coreRealm, extClassPath, extensions);
        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(classWorld)
                .setRealm(containerRealm)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setJSR250Lifecycle(true)
                .setStrictClassPathScanning(true)
                .setName("maven");
        customizeContainerConfiguration(cc);

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
                bind(ILoggerFactory.class).toInstance(localContext.loggerFactory);
                bind(CoreExports.class).toInstance(exports);
                bind(MessageBuilderFactory.class).toInstance(localContext.invokerRequest.messageBuilderFactory());
            }
        });

        // NOTE: To avoid inconsistencies, we'll use the TCCL exclusively for lookups
        container.setLookupRealm(null);
        Thread.currentThread().setContextClassLoader(container.getContainerRealm());

        container.setLoggerManager(new Slf4jLoggerManager());
        AbstractValueSource extensionSource = new AbstractValueSource(false) {
            @Override
            public Object getValue(String expression) {
                Object value = invokerRequest.userProperties().get(expression);
                if (value == null) {
                    value = invokerRequest.systemProperties().get(expression);
                }
                return value;
            }
        };
        for (CoreExtensionEntry extension : extensions) {
            container.discoverComponents(
                    extension.getClassRealm(),
                    new AbstractModule() {
                        @Override
                        protected void configure() {
                            try {
                                container.lookup(Injector.class).discover(extension.getClassRealm());
                            } catch (Throwable e) {
                                localContext.logger.warn("Maven DI failure", e);
                            }
                        }
                    },
                    new SessionScopeModule(container.lookup(SessionScope.class)),
                    new MojoExecutionScopeModule(container.lookup(MojoExecutionScope.class)),
                    new ExtensionConfigurationModule(extension, extensionSource));
        }

        customizeContainer(container);

        container.getLoggerManager().setThresholds(localContext.mavenExecutionRequest.getLoggingLevel());

        localContext.plexusContainer = container;
        localContext.eventSpyDispatcher = container.lookup(EventSpyDispatcher.class);

        DefaultEventSpyContext eventSpyContext = new DefaultEventSpyContext();
        Map<String, Object> data = eventSpyContext.getData();
        data.put("plexus", container);
        data.put("workingDirectory", invokerRequest.cwd().toString());
        data.put("systemProperties", toProperties(invokerRequest.systemProperties()));
        data.put("userProperties", toProperties(invokerRequest.userProperties()));
        data.put("versionProperties", CLIReportingUtils.getBuildProperties());
        localContext.eventSpyDispatcher.init(eventSpyContext);

        // refresh logger in case container got customized by spy
        localContext.logger =
                localContext.loggerFactory.getLogger(this.getClass().getName());

        // lookup the rest
        localContext.maven = container.lookup(Maven.class);
        localContext.mavenExecutionRequestPopulator = container.lookup(MavenExecutionRequestPopulator.class);
        localContext.modelProcessor = createModelProcessor(localContext);
        localContext.settingsBuilder = container.lookup(SettingsBuilder.class);
        localContext.toolchainsBuilder = container.lookup(ToolchainsBuilder.class);
    }

    protected ModelProcessor createModelProcessor(LocalContext localContext) throws Exception {
        return localContext.plexusContainer.lookup(ModelProcessor.class);
    }

    protected List<Path> parseExtClasspath(LocalContext localContext) throws Exception {
        MavenInvokerRequest invokerRequest = localContext.invokerRequest;
        String extClassPath = invokerRequest.userProperties().get(Constants.MAVEN_EXT_CLASS_PATH);
        if (extClassPath == null) {
            extClassPath = invokerRequest.systemProperties().get(Constants.MAVEN_EXT_CLASS_PATH);
            if (extClassPath != null) {
                localContext.logger.warn(
                        "The property '{}' has been set using a JVM system property which is deprecated. "
                                + "The property can be passed as a Maven argument or in the Maven project configuration file,"
                                + "usually located at ${session.rootDirectory}/.mvn/maven.properties.",
                        Constants.MAVEN_EXT_CLASS_PATH);
            }
        }
        ArrayList<Path> jars = new ArrayList<>();
        if (extClassPath != null && !extClassPath.isEmpty()) {
            for (String jar : extClassPath.split(File.pathSeparator)) {
                Path file = localContext.cwdResolver.apply(jar);
                localContext.logger.debug("  included '{}'", file);
                jars.add(file);
            }
        }
        return jars;
    }

    protected ClassRealm setupContainerRealm(
            Logger logger,
            ClassWorld classWorld,
            ClassRealm coreRealm,
            List<Path> extClassPath,
            List<CoreExtensionEntry> extensions)
            throws Exception {
        if (!extClassPath.isEmpty() || !extensions.isEmpty()) {
            ClassRealm extRealm = classWorld.newRealm("maven.ext", null);

            extRealm.setParentRealm(coreRealm);

            logger.debug("Populating class realm '{}'", extRealm.getId());

            for (Path file : extClassPath) {
                logger.debug("  included '{}'", file);
                extRealm.addURL(file.toUri().toURL());
            }

            ArrayList<CoreExtensionEntry> reversed = new ArrayList<>(extensions);
            Collections.reverse(reversed);
            for (CoreExtensionEntry entry : reversed) {
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

    protected List<CoreExtensionEntry> loadCoreExtensions(
            LocalContext localContext, ClassRealm containerRealm, Set<String> providedArtifacts) throws Exception {
        MavenInvokerRequest invokerRequest = localContext.invokerRequest;
        if (invokerRequest.coreExtensions().isEmpty()
                || invokerRequest.coreExtensions().get().isEmpty()) {
            return Collections.emptyList();
        }

        List<CoreExtension> extensions = invokerRequest.coreExtensions().get();
        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(containerRealm.getWorld())
                .setRealm(containerRealm)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setJSR250Lifecycle(true)
                .setStrictClassPathScanning(true)
                .setName("maven");

        DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ILoggerFactory.class).toInstance(localContext.loggerFactory);
            }
        });

        try {
            container.setLookupRealm(null);
            container.setLoggerManager(new Slf4jLoggerManager());
            container.getLoggerManager().setThresholds(localContext.mavenExecutionRequest.getLoggingLevel());
            Thread.currentThread().setContextClassLoader(container.getContainerRealm());

            settings(localContext, container.lookup(SettingsBuilder.class));
            MavenExecutionRequest mavenExecutionRequest =
                    DefaultMavenExecutionRequest.copy(localContext.mavenExecutionRequest);
            populateRequest(localContext, mavenExecutionRequest);
            mavenExecutionRequest =
                    container.lookup(MavenExecutionRequestPopulator.class).populateDefaults(mavenExecutionRequest);
            BootstrapCoreExtensionManager resolver = container.lookup(BootstrapCoreExtensionManager.class);
            return Collections.unmodifiableList(
                    resolver.loadCoreExtensions(mavenExecutionRequest, providedArtifacts, extensions));
        } finally {
            container.dispose();
        }
    }

    protected void customizeContainerConfiguration(ContainerConfiguration configuration) {}

    protected void customizeContainer(PlexusContainer container) {}

    protected void preCommands(LocalContext localContext) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.verbose().orElse(false) || mavenOptions.showVersion().orElse(false)) {
            localContext.stdOut.println(CLIReportingUtils.showVersion());
        }
    }

    protected void postCommands(LocalContext localContext) {
        MavenInvokerRequest invokerRequest = localContext.invokerRequest;
        Logger logger = localContext.logger;
        if (invokerRequest.options().showErrors().orElse(false)) {
            logger.info("Error stacktraces are turned on.");
        }
        if (invokerRequest.options().relaxedChecksums().orElse(false)) {
            logger.info("Disabling strict checksum verification on all artifact downloads.");
        } else if (invokerRequest.options().strictChecksums().orElse(false)) {
            logger.info("Enabling strict checksum verification on all artifact downloads.");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Message scheme: {}", (MessageUtils.isColorEnabled() ? "color" : "plain"));
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

    protected void settings(LocalContext localContext, SettingsBuilder settingsBuilder) throws Exception {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        Path userSettingsFile = null;

        if (mavenOptions.altUserSettings().isPresent()) {
            userSettingsFile = localContext.cwdResolver.apply(
                    mavenOptions.altUserSettings().get());

            if (!Files.isRegularFile(userSettingsFile)) {
                throw new FileNotFoundException("The specified user settings file does not exist: " + userSettingsFile);
            }
        } else {
            String userSettingsFileStr =
                    localContext.invokerRequest.userProperties().get(Constants.MAVEN_USER_SETTINGS);
            if (userSettingsFileStr != null) {
                userSettingsFile = localContext.cwdResolver.apply(userSettingsFileStr);
            }
        }

        Path projectSettingsFile = null;

        if (mavenOptions.altProjectSettings().isPresent()) {
            projectSettingsFile = localContext.cwdResolver.apply(
                    mavenOptions.altProjectSettings().get());

            if (!Files.isRegularFile(projectSettingsFile)) {
                throw new FileNotFoundException(
                        "The specified project settings file does not exist: " + projectSettingsFile);
            }
        } else {
            String projectSettingsFileStr =
                    localContext.invokerRequest.userProperties().get(Constants.MAVEN_PROJECT_SETTINGS);
            if (projectSettingsFileStr != null) {
                projectSettingsFile = localContext.cwdResolver.apply(projectSettingsFileStr);
            }
        }

        Path installationSettingsFile = null;

        if (mavenOptions.altInstallationSettings().isPresent()) {
            installationSettingsFile = localContext.cwdResolver.apply(
                    mavenOptions.altInstallationSettings().get());

            if (!Files.isRegularFile(installationSettingsFile)) {
                throw new FileNotFoundException(
                        "The specified installation settings file does not exist: " + installationSettingsFile);
            }
        } else {
            String installationSettingsFileStr =
                    localContext.invokerRequest.userProperties().get(Constants.MAVEN_INSTALLATION_SETTINGS);
            if (installationSettingsFileStr != null) {
                installationSettingsFile = localContext.cwdResolver.apply(installationSettingsFileStr);
            }
        }

        localContext.mavenExecutionRequest.setInstallationSettingsFile(toFile(installationSettingsFile));
        localContext.mavenExecutionRequest.setProjectSettingsFile(toFile(projectSettingsFile));
        localContext.mavenExecutionRequest.setUserSettingsFile(toFile(userSettingsFile));

        SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setGlobalSettingsFile(toFile(installationSettingsFile));
        settingsRequest.setProjectSettingsFile(toFile(projectSettingsFile));
        settingsRequest.setUserSettingsFile(toFile(userSettingsFile));
        settingsRequest.setSystemProperties(toProperties(localContext.invokerRequest.systemProperties()));
        Properties props = toProperties(localContext.invokerRequest.userProperties());
        if (localContext.invokerRequest.rootDirectory() != null) {
            props.put(
                    "session.rootDirectory",
                    localContext.invokerRequest.rootDirectory().toString());
        }
        settingsRequest.setUserProperties(props);

        if (localContext.eventSpyDispatcher != null) {
            localContext.eventSpyDispatcher.onEvent(settingsRequest);
        }

        localContext.logger.debug(
                "Reading installation settings from '{}'",
                settingsRequest.getGlobalSettingsSource() != null
                        ? settingsRequest.getGlobalSettingsSource().getLocation()
                        : settingsRequest.getGlobalSettingsFile());
        localContext.logger.debug(
                "Reading project settings from '{}'",
                settingsRequest.getProjectSettingsSource() != null
                        ? settingsRequest.getProjectSettingsSource().getLocation()
                        : settingsRequest.getProjectSettingsFile());
        localContext.logger.debug(
                "Reading user settings from '{}'",
                settingsRequest.getUserSettingsSource() != null
                        ? settingsRequest.getUserSettingsSource().getLocation()
                        : settingsRequest.getUserSettingsFile());

        SettingsBuildingResult settingsResult = settingsBuilder.build(settingsRequest);

        if (localContext.eventSpyDispatcher != null) {
            localContext.eventSpyDispatcher.onEvent(settingsResult);
        }

        populateRequestFromSettings(localContext.mavenExecutionRequest, settingsResult.getEffectiveSettings());

        if (!settingsResult.getProblems().isEmpty() && localContext.logger.isWarnEnabled()) {
            localContext.logger.warn("");
            localContext.logger.warn("Some problems were encountered while building the effective settings");

            for (SettingsProblem problem : settingsResult.getProblems()) {
                localContext.logger.warn("{} @ {}", problem.getMessage(), problem.getLocation());
            }
            localContext.logger.warn("");
        }
    }

    protected void populateRequestFromSettings(MavenExecutionRequest request, Settings settings) {
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

    protected void toolchains(LocalContext localContext) throws Exception {
        Path userToolchainsFile = null;

        if (localContext.invokerRequest.options().altUserToolchains().isPresent()) {
            userToolchainsFile = localContext.cwdResolver.apply(
                    localContext.invokerRequest.options().altUserToolchains().get());

            if (!Files.isRegularFile(userToolchainsFile)) {
                throw new FileNotFoundException(
                        "The specified user toolchains file does not exist: " + userToolchainsFile);
            }
        } else {
            String userToolchainsFileStr =
                    localContext.invokerRequest.userProperties().get(Constants.MAVEN_USER_TOOLCHAINS);
            if (userToolchainsFileStr != null) {
                userToolchainsFile = localContext.cwdResolver.apply(userToolchainsFileStr);
            }
        }

        Path installationToolchainsFile = null;

        if (localContext.invokerRequest.options().altInstallationToolchains().isPresent()) {
            installationToolchainsFile = localContext.cwdResolver.apply(localContext
                    .invokerRequest
                    .options()
                    .altInstallationToolchains()
                    .get());

            if (!Files.isRegularFile(installationToolchainsFile)) {
                throw new FileNotFoundException(
                        "The specified installation toolchains file does not exist: " + installationToolchainsFile);
            }
        } else {
            String installationToolchainsFileStr =
                    localContext.invokerRequest.userProperties().get(Constants.MAVEN_INSTALLATION_TOOLCHAINS);
            if (installationToolchainsFileStr != null) {
                installationToolchainsFile = localContext.cwdResolver.apply(installationToolchainsFileStr);
            }
        }

        localContext.mavenExecutionRequest.setInstallationToolchainsFile(
                installationToolchainsFile != null ? installationToolchainsFile.toFile() : null);
        localContext.mavenExecutionRequest.setUserToolchainsFile(
                userToolchainsFile != null ? userToolchainsFile.toFile() : null);

        DefaultToolchainsBuildingRequest toolchainsRequest = new DefaultToolchainsBuildingRequest();
        if (installationToolchainsFile != null && Files.isRegularFile(installationToolchainsFile)) {
            toolchainsRequest.setGlobalToolchainsSource(new FileSource(installationToolchainsFile));
        }
        if (userToolchainsFile != null && Files.isRegularFile(userToolchainsFile)) {
            toolchainsRequest.setUserToolchainsSource(new FileSource(userToolchainsFile));
        }

        localContext.eventSpyDispatcher.onEvent(toolchainsRequest);

        localContext.logger.debug(
                "Reading installation toolchains from '{}'",
                toolchainsRequest.getGlobalToolchainsSource() != null
                        ? toolchainsRequest.getGlobalToolchainsSource().getLocation()
                        : installationToolchainsFile);
        localContext.logger.debug(
                "Reading user toolchains from '{}'",
                toolchainsRequest.getUserToolchainsSource() != null
                        ? toolchainsRequest.getUserToolchainsSource().getLocation()
                        : userToolchainsFile);

        ToolchainsBuildingResult toolchainsResult = localContext.toolchainsBuilder.build(toolchainsRequest);

        localContext.eventSpyDispatcher.onEvent(toolchainsResult);

        localContext.mavenExecutionRequestPopulator.populateFromToolchains(
                localContext.mavenExecutionRequest, toolchainsResult.getEffectiveToolchains());

        if (!toolchainsResult.getProblems().isEmpty() && localContext.logger.isWarnEnabled()) {
            localContext.logger.warn("");
            localContext.logger.warn("Some problems were encountered while building the effective toolchains");

            for (Problem problem : toolchainsResult.getProblems()) {
                localContext.logger.warn("{} @ {}", problem.getMessage(), problem.getLocation());
            }

            localContext.logger.warn("");
        }
    }

    protected void populateRequest(LocalContext localContext, MavenExecutionRequest request) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        request.setShowErrors(mavenOptions.showErrors().orElse(false));
        disableInteractiveModeIfNeeded(localContext);
        request.setNoSnapshotUpdates(mavenOptions.suppressSnapshotUpdates().orElse(false));
        request.setGoals(mavenOptions.goals().orElse(List.of()));
        request.setReactorFailureBehavior(determineReactorFailureBehaviour(localContext));
        request.setRecursive(!mavenOptions.nonRecursive().orElse(!request.isRecursive()));
        request.setOffline(mavenOptions.offline().orElse(request.isOffline()));
        request.setUpdateSnapshots(mavenOptions.updateSnapshots().orElse(false));
        request.setGlobalChecksumPolicy(determineGlobalChecksumPolicy(localContext));
        request.setBaseDirectory(localContext.invokerRequest.cwd().toFile());
        request.setSystemProperties(toProperties(localContext.invokerRequest.systemProperties()));
        request.setUserProperties(toProperties(localContext.invokerRequest.userProperties()));
        request.setMultiModuleProjectDirectory(
                localContext.invokerRequest.rootDirectory().toFile());

        request.setRootDirectory(localContext.invokerRequest.rootDirectory());
        request.setTopDirectory(localContext.invokerRequest.topDirectory());

        Path pom = determinePom(localContext);
        request.setPom(pom != null ? pom.toFile() : null);
        request.setTransferListener(determineTransferListener(localContext));
        request.setExecutionListener(determineExecutionListener(localContext));

        if ((request.getPom() != null) && (request.getPom().getParentFile() != null)) {
            request.setBaseDirectory(request.getPom().getParentFile());
        }

        request.setResumeFrom(mavenOptions.resumeFrom().orElse(null));
        request.setResume(mavenOptions.resume().orElse(false));
        request.setMakeBehavior(determineMakeBehavior(localContext));
        request.setCacheNotFound(mavenOptions.cacheArtifactNotFound().orElse(true));
        request.setCacheTransferError(false);

        if (mavenOptions.strictArtifactDescriptorPolicy().orElse(false)) {
            request.setIgnoreMissingArtifactDescriptor(false);
            request.setIgnoreInvalidArtifactDescriptor(false);
        } else {
            request.setIgnoreMissingArtifactDescriptor(true);
            request.setIgnoreInvalidArtifactDescriptor(true);
        }

        request.setIgnoreTransitiveRepositories(
                mavenOptions.ignoreTransitiveRepositories().orElse(false));

        performProjectActivation(localContext, request.getProjectActivation());
        performProfileActivation(localContext, request.getProfileActivation());

        final String localRepositoryPath = determineLocalRepositoryPath(localContext);
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
        if (localContext.invokerRequest.options().threads().isPresent()) {
            int degreeOfConcurrency = calculateDegreeOfConcurrency(
                    localContext.invokerRequest.options().threads().get());
            if (degreeOfConcurrency > 1) {
                request.setBuilderId("multithreaded");
                request.setDegreeOfConcurrency(degreeOfConcurrency);
            }
        }

        //
        // Allow the builder to be overridden by the user if requested. The builders are now pluggable.
        //
        if (localContext.invokerRequest.options().builder().isPresent()) {
            request.setBuilderId(localContext.invokerRequest.options().builder().get());
        }
    }

    protected void disableInteractiveModeIfNeeded(LocalContext localContext) {
        if (localContext.invokerRequest.options().forceInteractive().orElse(false)) {
            return;
        }

        if (localContext.invokerRequest.options().nonInteractive().orElse(false)) {
            localContext.mavenExecutionRequest.setInteractiveMode(false);
        } else {
            boolean runningOnCI = isRunningOnCI(localContext);
            if (runningOnCI) {
                localContext.logger.info(
                        "Making this build non-interactive, because the environment variable CI equals \"true\"."
                                + " Disable this detection by removing that variable or adding --force-interactive.");
                localContext.mavenExecutionRequest.setInteractiveMode(false);
            }
        }
    }

    protected Path determinePom(LocalContext localContext) {
        Path current = localContext.invokerRequest.cwd();
        if (localContext.invokerRequest.options().alternatePomFile().isPresent()) {
            current = localContext.cwdResolver.apply(
                    localContext.invokerRequest.options().alternatePomFile().get());
        }
        if (localContext.modelProcessor != null) {
            return localContext.modelProcessor.locateExistingPom(current);
        } else {
            return Files.isRegularFile(current) ? current : null;
        }
    }

    protected String determineLocalRepositoryPath(LocalContext localContext) {
        String userDefinedLocalRepo =
                localContext.invokerRequest.userProperties().get(Constants.MAVEN_REPO_LOCAL);
        if (userDefinedLocalRepo == null) {
            userDefinedLocalRepo =
                    localContext.invokerRequest.systemProperties().get(Constants.MAVEN_REPO_LOCAL);
            if (userDefinedLocalRepo != null) {
                localContext.logger.warn(
                        "The property '{}' has been set using a JVM system property which is deprecated. "
                                + "The property can be passed as a Maven argument or in the Maven project configuration file,"
                                + "usually located at ${session.rootDirectory}/.mvn/maven.properties.",
                        Constants.MAVEN_REPO_LOCAL);
            }
        }
        return userDefinedLocalRepo;
    }

    protected String determineReactorFailureBehaviour(LocalContext localContext) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.failFast().isPresent()) {
            return MavenExecutionRequest.REACTOR_FAIL_FAST;
        } else if (mavenOptions.failAtEnd().isPresent()) {
            return MavenExecutionRequest.REACTOR_FAIL_AT_END;
        } else if (mavenOptions.failNever().isPresent()) {
            return MavenExecutionRequest.REACTOR_FAIL_NEVER;
        } else {
            return MavenExecutionRequest.REACTOR_FAIL_FAST;
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

    protected boolean isRunningOnCI(LocalContext localContext) {
        String ciEnv = localContext.invokerRequest.systemProperties().get("env.CI");
        return ciEnv != null && !"false".equals(ciEnv);
    }

    protected String determineGlobalChecksumPolicy(LocalContext localContext) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.strictChecksums().orElse(false)) {
            return MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
        } else if (mavenOptions.relaxedChecksums().orElse(false)) {
            return MavenExecutionRequest.CHECKSUM_POLICY_WARN;
        } else {
            return null;
        }
    }

    protected TransferListener determineTransferListener(LocalContext localContext) {
        boolean quiet = localContext.invokerRequest.options().quiet().orElse(false);
        boolean logFile = localContext.invokerRequest.options().logFile().isPresent();
        boolean noTransferProgress =
                localContext.invokerRequest.options().noTransferProgress().orElse(false);
        boolean runningOnCI = isRunningOnCI(localContext);
        boolean quietCI = runningOnCI
                && !localContext.invokerRequest.options().forceInteractive().orElse(false);

        if (quiet || noTransferProgress || quietCI) {
            return new QuietMavenTransferListener();
        } else if (localContext.mavenExecutionRequest.isInteractiveMode() && !logFile) {
            //
            // If we're logging to a file then we don't want the console transfer listener as it will spew
            // download progress all over the place
            //
            return new SimplexTransferListener(new ConsoleMavenTransferListener(
                    localContext.invokerRequest.messageBuilderFactory(),
                    localContext.stdOut,
                    localContext.invokerRequest.options().verbose().orElse(false)));
        } else {
            // default: batch mode which goes along with interactive
            return new Slf4jMavenTransferListener();
        }
    }

    protected ExecutionListener determineExecutionListener(LocalContext localContext) {
        ExecutionListener executionListener =
                new ExecutionEventLogger(localContext.invokerRequest.messageBuilderFactory());
        if (localContext.eventSpyDispatcher != null) {
            return localContext.eventSpyDispatcher.chainListener(executionListener);
        } else {
            return executionListener;
        }
    }

    protected String determineMakeBehavior(LocalContext localContext) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.alsoMake().isPresent()
                && mavenOptions.alsoMakeDependents().isEmpty()) {
            return MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;
        } else if (mavenOptions.alsoMake().isEmpty()
                && mavenOptions.alsoMakeDependents().isPresent()) {
            return MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM;
        } else if (mavenOptions.alsoMake().isPresent()
                && mavenOptions.alsoMakeDependents().isPresent()) {
            return MavenExecutionRequest.REACTOR_MAKE_BOTH;
        } else {
            return null;
        }
    }

    protected void performProjectActivation(LocalContext localContext, ProjectActivation projectActivation) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.projects().isPresent()
                && !mavenOptions.projects().get().isEmpty()) {
            List<String> optionValues = mavenOptions.projects().get();
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

    protected void performProfileActivation(LocalContext localContext, ProfileActivation profileActivation) {
        MavenOptions mavenOptions = localContext.invokerRequest.options();
        if (mavenOptions.activatedProfiles().isPresent()
                && !mavenOptions.activatedProfiles().get().isEmpty()) {
            List<String> optionValues = mavenOptions.activatedProfiles().get();
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

    protected int execute(LocalContext localContext) throws Exception {
        MavenExecutionRequest request =
                localContext.mavenExecutionRequestPopulator.populateDefaults(localContext.mavenExecutionRequest);

        // why? No way to disable caching?
        if (localContext.mavenExecutionRequest.getRepositoryCache() == null) {
            localContext.mavenExecutionRequest.setRepositoryCache(new DefaultRepositoryCache());
        }

        localContext.eventSpyDispatcher.onEvent(request);

        MavenExecutionResult result = localContext.maven.execute(request);

        localContext.eventSpyDispatcher.onEvent(result);

        localContext.eventSpyDispatcher.close();

        if (result.hasExceptions()) {
            ExceptionHandler handler = new DefaultExceptionHandler();

            Map<String, String> references = new LinkedHashMap<>();

            List<MavenProject> failedProjects = new ArrayList<>();

            for (Throwable exception : result.getExceptions()) {
                ExceptionSummary summary = handler.handleException(exception);

                logSummary(localContext, summary, references, "");

                if (exception instanceof LifecycleExecutionException) {
                    failedProjects.add(((LifecycleExecutionException) exception).getProject());
                }
            }

            localContext.logger.error("");

            if (!localContext.invokerRequest.options().showErrors().orElse(false)) {
                localContext.logger.error(
                        "To see the full stack trace of the errors, re-run Maven with the '{}' switch",
                        MessageUtils.builder().strong("-e"));
            }
            if (!localContext.logger.isDebugEnabled()) {
                localContext.logger.error(
                        "Re-run Maven using the '{}' switch to enable verbose output",
                        MessageUtils.builder().strong("-X"));
            }

            if (!references.isEmpty()) {
                localContext.logger.error("");
                localContext.logger.error("For more information about the errors and possible solutions"
                        + ", please read the following articles:");

                for (Map.Entry<String, String> entry : references.entrySet()) {
                    localContext.logger.error("{} {}", MessageUtils.builder().strong(entry.getValue()), entry.getKey());
                }
            }

            if (result.canResume()) {
                logBuildResumeHint(localContext, "mvn [args] -r");
            } else if (!failedProjects.isEmpty()) {
                List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();

                // Sort the failedProjects list in the topologically sorted order.
                failedProjects.sort(comparing(sortedProjects::indexOf));

                MavenProject firstFailedProject = failedProjects.get(0);
                if (!firstFailedProject.equals(sortedProjects.get(0))) {
                    String resumeFromSelector = getResumeFromSelector(sortedProjects, firstFailedProject);
                    logBuildResumeHint(localContext, "mvn [args] -rf " + resumeFromSelector);
                }
            }

            if (localContext.invokerRequest.options().failNever().orElse(false)) {
                localContext.logger.info("Build failures were ignored.");
                return 0;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

    protected void logBuildResumeHint(LocalContext localContext, String resumeBuildHint) {
        localContext.logger.error("");
        localContext.logger.error("After correcting the problems, you can resume the build with the command");
        localContext.logger.error(
                MessageUtils.builder().a("  ").strong(resumeBuildHint).toString());
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
    protected String getResumeFromSelector(List<MavenProject> mavenProjects, MavenProject firstFailedProject) {
        boolean hasOverlappingArtifactId = mavenProjects.stream()
                        .filter(project -> firstFailedProject.getArtifactId().equals(project.getArtifactId()))
                        .count()
                > 1;

        if (hasOverlappingArtifactId) {
            return firstFailedProject.getGroupId() + ":" + firstFailedProject.getArtifactId();
        }

        return ":" + firstFailedProject.getArtifactId();
    }

    protected static final Pattern NEXT_LINE = Pattern.compile("\r?\n");

    protected static final Pattern LAST_ANSI_SEQUENCE = Pattern.compile("(\u001B\\[[;\\d]*[ -/]*[@-~])[^\u001B]*$");

    protected static final String ANSI_RESET = "\u001B\u005Bm";

    protected void logSummary(
            LocalContext localContext, ExceptionSummary summary, Map<String, String> references, String indent) {
        String referenceKey = "";

        if (summary.getReference() != null && !summary.getReference().isEmpty()) {
            referenceKey =
                    references.computeIfAbsent(summary.getReference(), k -> "[Help " + (references.size() + 1) + "]");
        }

        String msg = summary.getMessage();

        if (!referenceKey.isEmpty()) {
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

            if ((i == lines.length - 1)
                    && (localContext.invokerRequest.options().showErrors().orElse(false)
                            || (summary.getException() instanceof InternalErrorException))) {
                localContext.logger.error(line, summary.getException());
            } else {
                localContext.logger.error(line);
            }

            currentColor = nextColor;
        }

        indent += "  ";

        for (ExceptionSummary child : summary.getChildren()) {
            logSummary(localContext, child, references, indent);
        }
    }
}
