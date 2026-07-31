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
package org.apache.maven.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.maven.api.JavaToolchain;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.Source;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ToolchainFactory;
import org.apache.maven.api.services.ToolchainFactoryException;
import org.apache.maven.api.services.ToolchainManager;
import org.apache.maven.api.services.ToolchainManagerException;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.api.xml.XmlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DefaultToolchainManager implements ToolchainManager {
    private final Map<String, ToolchainFactory> factories;
    private final JdkToolchainDiscoverer discoverer;
    private final Logger logger;

    @Inject
    public DefaultToolchainManager(Map<String, ToolchainFactory> factories, JdkToolchainDiscoverer discoverer) {
        this(factories, discoverer, null);
    }

    /**
     * Convenience constructor without a discoverer — auto-selection will skip
     * filesystem discovery. Used by tests and IT harnesses.
     */
    public DefaultToolchainManager(Map<String, ToolchainFactory> factories) {
        this(factories, null, null);
    }

    /**
     * Convenience constructor without a discoverer, with custom logger.
     * Used by tests.
     */
    DefaultToolchainManager(Map<String, ToolchainFactory> factories, Logger logger) {
        this(factories, null, logger);
    }

    /**
     * Full-control constructor. Used by tests.
     */
    DefaultToolchainManager(Map<String, ToolchainFactory> factories, JdkToolchainDiscoverer discoverer, Logger logger) {
        this.factories = factories;
        this.discoverer = discoverer;
        this.logger = logger != null ? logger : LoggerFactory.getLogger(DefaultToolchainManager.class);
    }

    @Nonnull
    @Override
    public List<Toolchain> getToolchains(
            @Nonnull Session session, @Nonnull String type, @Nullable Map<String, String> requirements)
            throws ToolchainManagerException {
        ToolchainFactory factory = factories.get(Objects.requireNonNull(type, "type"));
        if (factory == null) {
            logger.error("Missing toolchain factory for type: " + type + ". Possibly caused by misconfigured project.");
            return List.of();
        }
        return Stream.concat(
                        session.getToolchains().stream()
                                .filter(model -> Objects.equals(type, model.getType()))
                                .map(this::createToolchain)
                                .flatMap(Optional::stream),
                        factory.createDefaultToolchain().stream())
                .filter(toolchain -> requirements == null || toolchain.matchesRequirements(requirements))
                .toList();
    }

    @Nonnull
    @Override
    public Optional<Toolchain> getToolchainFromBuildContext(@Nonnull Session session, @Nonnull String type)
            throws ToolchainManagerException {
        Map<String, Object> context = retrieveContext(session);
        ToolchainModel model = (ToolchainModel) context.get("toolchain-" + type);
        if (model != null) {
            return createToolchain(model);
        }

        // For JDK type, try auto-selection based on project's target version
        if ("jdk".equals(type)) {
            Optional<Toolchain> autoSelected = autoSelectJdkToolchain(session);
            if (autoSelected.isPresent()) {
                // Cache the selection so subsequent calls for this project return the same toolchain
                context.put("toolchain-" + type, autoSelected.get().getModel());
            }
            return autoSelected;
        }

        return Optional.empty();
    }

    @Override
    public void storeToolchainToBuildContext(@Nonnull Session session, @Nonnull Toolchain toolchain) {
        Map<String, Object> context = retrieveContext(session);
        context.put("toolchain-" + toolchain.getType(), toolchain.getModel());
    }

    /**
     * Attempts to automatically select a JDK toolchain when the running JDK
     * does not support the project's required {@code --source}/{@code --release} level.
     * <p>
     * First searches configured toolchains (from {@code toolchains.xml}), then falls back
     * to lazy filesystem discovery. Normal builds pay zero cost — discovery only runs
     * when the running JDK is incompatible and no configured toolchain matches.
     */
    Optional<Toolchain> autoSelectJdkToolchain(Session session) {
        int requiredSourceLevel = getProjectRequiredSourceLevel(session);
        logger.debug("Auto-select JDK toolchain: requiredSourceLevel={}", requiredSourceLevel);
        if (requiredSourceLevel <= 0) {
            return Optional.empty();
        }

        int runningJdkMajor = getRunningJdkMajor();
        logger.debug(
                "Auto-select JDK toolchain: runningJdkMajor={}, supportsLevel={}",
                runningJdkMajor,
                JdkSourceLevelSupport.supportsSourceLevel(runningJdkMajor, requiredSourceLevel));
        if (JdkSourceLevelSupport.supportsSourceLevel(runningJdkMajor, requiredSourceLevel)) {
            return Optional.empty();
        }

        // 1. Search configured toolchains (from toolchains.xml)
        List<Toolchain> configuredToolchains = getToolchains(session, "jdk", null);
        Toolchain bestMatch = findNewestCompatible(configuredToolchains, requiredSourceLevel);

        // 2. Fall back to lazy filesystem discovery
        if (bestMatch == null && discoverer != null) {
            logger.debug("No compatible JDK in configured toolchains, discovering JDKs from filesystem...");
            List<ToolchainModel> discoveredModels = discoverer.discoverToolchains(session.getSystemProperties());
            List<Toolchain> discoveredToolchains = discoveredModels.stream()
                    .map(this::createToolchain)
                    .flatMap(Optional::stream)
                    .toList();
            bestMatch = findNewestCompatible(discoveredToolchains, requiredSourceLevel);
        }

        if (bestMatch != null) {
            JavaToolchain jtc = (JavaToolchain) bestMatch;
            logger.warn(
                    "Project requires --source {} which is not supported by JDK {}.",
                    requiredSourceLevel,
                    runningJdkMajor);
            logger.warn(
                    "Automatically selected JDK {} (discovered at {}) for compilation.",
                    jtc.getJavaVersion(),
                    jtc.getJavaHome());
            logger.warn("To suppress this warning, configure the maven-toolchains-plugin explicitly");
            logger.warn("or set <targetVersion> to a value supported by your JDK.");
            return Optional.of(bestMatch);
        }

        return Optional.empty();
    }

    /**
     * Finds the newest JDK toolchain that supports the given source level.
     */
    private Toolchain findNewestCompatible(List<Toolchain> toolchains, int requiredSourceLevel) {
        Toolchain bestMatch = null;
        int bestVersion = 0;
        for (Toolchain tc : toolchains) {
            if (tc instanceof JavaToolchain jtc && jtc.getJavaVersion() != null) {
                int tcMajor = JdkSourceLevelSupport.normalizeSourceLevel(
                        jtc.getJavaVersion().toString());
                if (tcMajor > 0 && JdkSourceLevelSupport.supportsSourceLevel(tcMajor, requiredSourceLevel)) {
                    if (tcMajor > bestVersion) {
                        bestVersion = tcMajor;
                        bestMatch = tc;
                    }
                }
            }
        }
        return bestMatch;
    }

    /**
     * Reads the project's required source level from either Model 4.1.0
     * {@code <source><targetVersion>} elements or legacy properties
     * ({@code maven.compiler.release}, {@code maven.compiler.source}).
     *
     * @return the required source level as a major version, or {@code -1} if none is specified
     */
    int getProjectRequiredSourceLevel(Session session) {
        Optional<Project> current = session.getService(Lookup.class).lookupOptional(Project.class);
        if (current.isEmpty()) {
            return -1;
        }

        Project project = current.get();

        // Check Model 4.1.0 <source><targetVersion> elements
        Build build = project.getModel().getBuild();
        if (build != null) {
            List<Source> sources = build.getSources();
            if (sources != null) {
                for (Source source : sources) {
                    String targetVersion = source.getTargetVersion();
                    if (targetVersion != null && !targetVersion.isEmpty()) {
                        int level = JdkSourceLevelSupport.normalizeSourceLevel(targetVersion);
                        if (level > 0) {
                            return level;
                        }
                    }
                }
            }
        }

        // Fall back to legacy properties
        Map<String, String> properties = project.getModel().getProperties();
        if (properties != null) {
            // maven.compiler.release takes precedence
            String release = properties.get("maven.compiler.release");
            if (release != null && !release.isEmpty()) {
                int level = JdkSourceLevelSupport.normalizeSourceLevel(release);
                if (level > 0) {
                    return level;
                }
            }

            // Then maven.compiler.source
            String source = properties.get("maven.compiler.source");
            if (source != null && !source.isEmpty()) {
                int level = JdkSourceLevelSupport.normalizeSourceLevel(source);
                if (level > 0) {
                    return level;
                }
            }
        }

        // Fall back to compiler plugin configuration (<release>, <source>)
        int pluginLevel = getSourceLevelFromCompilerPlugin(build);
        if (pluginLevel > 0) {
            return pluginLevel;
        }

        return -1;
    }

    /**
     * Reads the source level from the maven-compiler-plugin configuration.
     * Checks both {@code <release>} and {@code <source>} elements in the plugin's
     * {@code <configuration>} block.
     *
     * @return the source level, or {@code -1} if not configured
     */
    private int getSourceLevelFromCompilerPlugin(Build build) {
        if (build == null) {
            return -1;
        }
        for (Plugin plugin : build.getPlugins()) {
            if ("maven-compiler-plugin".equals(plugin.getArtifactId())
                    && (plugin.getGroupId() == null
                            || plugin.getGroupId().isEmpty()
                            || "org.apache.maven.plugins".equals(plugin.getGroupId()))) {
                XmlNode config = plugin.getConfiguration();
                if (config != null) {
                    // <release> takes precedence over <source>
                    XmlNode releaseNode = config.child("release");
                    if (releaseNode != null
                            && releaseNode.value() != null
                            && !releaseNode.value().isBlank()) {
                        int level = JdkSourceLevelSupport.normalizeSourceLevel(
                                releaseNode.value().trim());
                        if (level > 0) {
                            return level;
                        }
                    }
                    XmlNode sourceNode = config.child("source");
                    if (sourceNode != null
                            && sourceNode.value() != null
                            && !sourceNode.value().isBlank()) {
                        int level = JdkSourceLevelSupport.normalizeSourceLevel(
                                sourceNode.value().trim());
                        if (level > 0) {
                            return level;
                        }
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Returns the major version of the running JDK.
     * Extracted as a method so tests can override it.
     */
    int getRunningJdkMajor() {
        return JdkSourceLevelSupport.getRunningJdkMajor();
    }

    private Optional<Toolchain> createToolchain(ToolchainModel model) {
        String type = Objects.requireNonNull(model.getType(), "model.getType()");
        ToolchainFactory factory = factories.get(type);
        if (factory != null) {
            try {
                return Optional.of(factory.createToolchain(model));
            } catch (ToolchainFactoryException e) {
                throw new ToolchainManagerException("Error creating toolchain of type " + type, e);
            }
        } else {
            logger.error("Missing toolchain factory for type: " + type + ". Possibly caused by misconfigured project.");
        }
        return Optional.empty();
    }

    private static final SessionData.Key<ConcurrentHashMap<Project, ConcurrentHashMap<String, Object>>>
            TOOLCHAIN_CONTEXT_KEY = (SessionData.Key) SessionData.key(ConcurrentHashMap.class, "toolchain-context");

    protected Map<String, Object> retrieveContext(Session session) {
        Optional<Project> current = session.getService(Lookup.class).lookupOptional(Project.class);
        if (current.isPresent()) {
            var map = session.getData().computeIfAbsent(TOOLCHAIN_CONTEXT_KEY, ConcurrentHashMap::new);
            return map.computeIfAbsent(current.get(), p -> new ConcurrentHashMap<>());
        }
        return new HashMap<>();
    }
}
