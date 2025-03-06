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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.apache.maven.api.Constants;
import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.cli.CoreExtensions;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.cli.extensions.InputLocation;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.cling.extensions.BootstrapCoreExtensionManager;
import org.apache.maven.cling.extensions.ExtensionConfigurationModule;
import org.apache.maven.cling.extensions.LoadedCoreExtension;
import org.apache.maven.cling.logging.Slf4jLoggerManager;
import org.apache.maven.di.Injector;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.execution.scope.internal.MojoExecutionScopeModule;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.internal.impl.DefaultLookup;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.session.scope.internal.SessionScopeModule;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.logging.LoggerManager;
import org.slf4j.ILoggerFactory;

import static org.apache.maven.cling.invoker.Utils.toPlexusLoggingLevel;

/**
 * Container capsule backed by Plexus Container.
 *
 * @param <C> The context type.
 */
public class PlexusContainerCapsuleFactory<C extends LookupContext> implements ContainerCapsuleFactory<C> {
    @Override
    public ContainerCapsule createContainerCapsule(LookupInvoker<C> invoker, C context) throws Exception {
        return new PlexusContainerCapsule(
                context, Thread.currentThread().getContextClassLoader(), container(invoker, context));
    }

    protected DefaultPlexusContainer container(LookupInvoker<C> invoker, C context) throws Exception {
        ClassWorld classWorld = invoker.protoLookup.lookup(ClassWorld.class);
        ClassRealm coreRealm = classWorld.getClassRealm("plexus.core");
        List<Path> extClassPath = parseExtClasspath(context);
        CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom(coreRealm);
        List<LoadedCoreExtension> loadedExtensions =
                loadCoreExtensions(invoker, context, coreRealm, coreEntry.getExportedArtifacts());
        List<CoreExtensionEntry> loadedExtensionsEntries =
                loadedExtensions.stream().map(LoadedCoreExtension::entry).toList();
        ClassRealm containerRealm =
                setupContainerRealm(context.logger, classWorld, coreRealm, extClassPath, loadedExtensionsEntries);
        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(classWorld)
                .setRealm(containerRealm)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setJSR250Lifecycle(true)
                .setStrictClassPathScanning(true)
                .setName("maven");
        customizeContainerConfiguration(context, cc);

        CoreExports exports = new CoreExports(
                containerRealm,
                collectExportedArtifacts(coreEntry, loadedExtensionsEntries),
                collectExportedPackages(coreEntry, loadedExtensionsEntries));
        Thread.currentThread().setContextClassLoader(containerRealm);
        DefaultPlexusContainer container = new DefaultPlexusContainer(cc, getCustomModule(context, exports));

        // NOTE: To avoid inconsistencies, we'll use the TCCL exclusively for lookups
        container.setLookupRealm(null);
        Thread.currentThread().setContextClassLoader(container.getContainerRealm());

        container.setLoggerManager(createLoggerManager());
        ProtoSession protoSession = context.protoSession;
        UnaryOperator<String> extensionSource = expression -> {
            String value = protoSession.getUserProperties().get(expression);
            if (value == null) {
                value = protoSession.getSystemProperties().get(expression);
            }
            return value;
        };
        List<Throwable> failures = new ArrayList<>();
        for (LoadedCoreExtension extension : loadedExtensions) {
            container.discoverComponents(
                    extension.entry().getClassRealm(),
                    new AbstractModule() {
                        @Override
                        protected void configure() {
                            try {
                                container
                                        .lookup(Injector.class)
                                        .discover(extension.entry().getClassRealm());
                            } catch (Throwable e) {
                                failures.add(new IllegalStateException(
                                        "Injection failure in "
                                                + extension.coreExtension().getId(),
                                        e));
                            }
                        }
                    },
                    new SessionScopeModule(container.lookup(SessionScope.class)),
                    new MojoExecutionScopeModule(container.lookup(MojoExecutionScope.class)),
                    new ExtensionConfigurationModule(extension.entry(), extensionSource));
        }
        if (!failures.isEmpty()) {
            IllegalStateException mavenDiFailed = new IllegalStateException(
                    "Maven dependency injection failed for at least one of the registered core extension");
            failures.forEach(mavenDiFailed::addSuppressed);
            throw mavenDiFailed;
        }
        container.getLoggerManager().setThresholds(toPlexusLoggingLevel(context.loggerLevel));
        customizeContainer(context, container);

        return container;
    }

    protected Set<String> collectExportedArtifacts(
            CoreExtensionEntry coreEntry, List<CoreExtensionEntry> extensionEntries) {
        Set<String> exportedArtifacts = new HashSet<>(coreEntry.getExportedArtifacts());
        for (CoreExtensionEntry extension : extensionEntries) {
            exportedArtifacts.addAll(extension.getExportedArtifacts());
        }
        return exportedArtifacts;
    }

    protected Set<String> collectExportedPackages(
            CoreExtensionEntry coreEntry, List<CoreExtensionEntry> extensionEntries) {
        Set<String> exportedPackages = new HashSet<>(coreEntry.getExportedPackages());
        for (CoreExtensionEntry extension : extensionEntries) {
            exportedPackages.addAll(extension.getExportedPackages());
        }
        return exportedPackages;
    }

    /**
     * Note: overriding this method should be avoided. Preferred way to replace Maven components is the "normal" way
     * where the components are on index (are annotated with JSR330 annotations and Sisu index is created) and, they
     * have priorities set.
     */
    protected Module getCustomModule(C context, CoreExports exports) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(ILoggerFactory.class).toInstance(context.loggerFactory);
                bind(CoreExports.class).toInstance(exports);
                bind(MessageBuilderFactory.class).toInstance(context.invokerRequest.messageBuilderFactory());
            }
        };
    }

    protected LoggerManager createLoggerManager() {
        return new Slf4jLoggerManager();
    }

    protected void customizeContainerConfiguration(C context, ContainerConfiguration configuration) throws Exception {}

    protected void customizeContainer(C context, PlexusContainer container) throws Exception {}

    protected List<Path> parseExtClasspath(C context) throws Exception {
        ProtoSession protoSession = context.protoSession;
        String extClassPath = protoSession.getUserProperties().get(Constants.MAVEN_EXT_CLASS_PATH);
        if (extClassPath == null) {
            extClassPath = protoSession.getSystemProperties().get(Constants.MAVEN_EXT_CLASS_PATH);
            if (extClassPath != null) {
                context.logger.warn("The property '" + Constants.MAVEN_EXT_CLASS_PATH
                        + "' has been set using a JVM system property which is deprecated. "
                        + "The property can be passed as a Maven argument or in the Maven project configuration file,"
                        + "usually located at ${session.rootDirectory}/.mvn/maven.properties.");
            }
        }
        ArrayList<Path> jars = new ArrayList<>();
        if (extClassPath != null && !extClassPath.isEmpty()) {
            for (String jar : extClassPath.split(File.pathSeparator)) {
                Path file = context.cwd.resolve(jar);
                context.logger.debug("  included '" + file + "'");
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

            logger.debug("Populating class realm '" + extRealm.getId() + "'");

            for (Path file : extClassPath) {
                logger.debug("  included '" + file + "'");
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

    /**
     * Selects extensions to load discovered from various sources. Also reports conflicts.
     */
    protected List<CoreExtension> selectCoreExtensions(C context, List<CoreExtensions> configuredCoreExtensions) {
        context.logger.debug("Configured core extensions:");
        for (CoreExtensions source : configuredCoreExtensions) {
            context.logger.debug("* " + source.source() + ":");
            for (CoreExtension extension : source.coreExtensions()) {
                context.logger.debug("  - " + extension.getId() + " -> " + formatLocation(extension.getLocation("")));
            }
        }

        Map<CoreExtensions.Source, CoreExtensions> coreExtensionsBySource = configuredCoreExtensions.stream()
                .collect(Collectors.toMap(CoreExtensions::source, Function.identity()));
        LinkedHashMap<String, CoreExtension> selectedExtensions = new LinkedHashMap<>();
        List<String> conflicts = new ArrayList<>();
        for (CoreExtensions.Source source : CoreExtensions.Source.values()) {
            CoreExtensions coreExtensions = coreExtensionsBySource.get(source);
            if (coreExtensions != null) {
                for (CoreExtension coreExtension : coreExtensions.coreExtensions()) {
                    String key = coreExtension.getGroupId() + ":" + coreExtension.getArtifactId();
                    CoreExtension conflict = selectedExtensions.putIfAbsent(key, coreExtension);
                    if (conflict != null) {
                        conflicts.add(String.format(
                                "Conflicting extension %s: %s vs %s",
                                key,
                                formatLocation(conflict.getLocation("")),
                                formatLocation(coreExtension.getLocation(""))));
                    }
                }
            }
        }
        if (!conflicts.isEmpty()) {
            context.logger.warn("Found " + conflicts.size() + " extension conflict(s):");
            for (String conflict : conflicts) {
                context.logger.warn("* " + conflict);
            }
            context.logger.warn("");
            context.logger.warn(
                    "Order of core extensions precedence is project > user > installation. Selected extensions are:");
            for (CoreExtension extension : selectedExtensions.values()) {
                context.logger.warn(
                        "* " + extension.getId() + " configured in " + formatLocation(extension.getLocation("")));
            }
        }

        context.logger.debug("Selected core extensions:");
        for (CoreExtension source : selectedExtensions.values()) {
            context.logger.debug("* " + source.getId() + ": " + formatLocation(source.getLocation("")));
        }
        return List.copyOf(selectedExtensions.values());
    }

    private String formatLocation(InputLocation location) {
        return location.getSource().getLocation() + ":" + location.getLineNumber();
    }

    protected List<LoadedCoreExtension> loadCoreExtensions(
            LookupInvoker<C> invoker, C context, ClassRealm containerRealm, Set<String> providedArtifacts)
            throws Exception {
        InvokerRequest invokerRequest = context.invokerRequest;
        if (invokerRequest.coreExtensions().isEmpty()
                || invokerRequest.coreExtensions().get().isEmpty()) {
            return Collections.emptyList();
        }

        List<CoreExtension> extensions =
                selectCoreExtensions(context, invokerRequest.coreExtensions().get());
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
                bind(ILoggerFactory.class).toProvider(() -> context.loggerFactory);
            }
        });

        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        Runnable settingsCleaner = null;
        try {
            container.setLookupRealm(null);
            container.setLoggerManager(createLoggerManager());
            container.getLoggerManager().setThresholds(toPlexusLoggingLevel(context.loggerLevel));
            Thread.currentThread().setContextClassLoader(container.getContainerRealm());

            settingsCleaner = invoker.settings(context, false, container.lookup(SettingsBuilder.class));

            MavenExecutionRequest mer = new DefaultMavenExecutionRequest();
            invoker.populateRequest(context, new DefaultLookup(container), mer);
            mer = container.lookup(MavenExecutionRequestPopulator.class).populateDefaults(mer);
            return Collections.unmodifiableList(container
                    .lookup(BootstrapCoreExtensionManager.class)
                    .loadCoreExtensions(mer, providedArtifacts, extensions));
        } finally {
            if (settingsCleaner != null) {
                settingsCleaner.run();
            }
            try {
                container.dispose();
            } finally {
                Thread.currentThread().setContextClassLoader(oldCL);
            }
        }
    }
}
