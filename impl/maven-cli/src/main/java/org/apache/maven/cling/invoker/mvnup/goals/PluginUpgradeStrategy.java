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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.RepositoryPolicy;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.Sources;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.apache.maven.impl.standalone.ApiRunner;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.LegacyDispatcher;
import org.eclipse.aether.internal.impl.DefaultPathProcessor;
import org.eclipse.aether.internal.impl.DefaultTransporterProvider;
import org.eclipse.aether.internal.impl.transport.http.DefaultChecksumExtractor;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PARENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROPERTIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.MAVEN_4_COMPATIBILITY_REASON;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.MAVEN_PLUGIN_PREFIX;

/**
 * Strategy for upgrading Maven plugins to recommended versions.
 * Handles plugin version upgrades in build/plugins and build/pluginManagement sections.
 */
@Named
@Singleton
@Priority(10)
public class PluginUpgradeStrategy extends AbstractUpgradeStrategy {

    private static final List<PluginUpgrade> PLUGIN_UPGRADES = List.of(
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-compiler-plugin", "3.2.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-exec-plugin", "3.2.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-enforcer-plugin", "3.0.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade("org.codehaus.mojo", "flatten-maven-plugin", "1.2.7", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-shade-plugin", "3.5.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID,
                    "maven-remote-resources-plugin",
                    "3.0.0",
                    MAVEN_4_COMPATIBILITY_REASON));

    private Session session;

    @Inject
    public PluginUpgradeStrategy() {}

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.plugins(), true);
    }

    @Override
    public String getDescription() {
        return "Upgrading Maven plugins to recommended versions";
    }

    @Override
    public UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        try {
            // Phase 1: Write all modifications to temp directory (keeping project structure)
            Path tempDir = createTempProjectStructure(context, pomMap);

            // Phase 2: For each POM, build effective model using the session and analyze plugins
            Map<Path, Set<String>> pluginsNeedingManagement =
                    analyzePluginsUsingEffectiveModels(context, pomMap, tempDir);

            // Phase 3: Add plugin management to the last local parent in hierarchy
            for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
                Path pomPath = entry.getKey();
                Document pomDocument = entry.getValue();
                processedPoms.add(pomPath);

                context.info(pomPath + " (checking for plugin upgrades)");
                context.indent();

                try {
                    boolean hasUpgrades = false;

                    // Apply direct plugin upgrades in the document
                    hasUpgrades |= upgradePluginsInDocument(pomDocument, context);

                    // Add plugin management based on effective model analysis
                    // Note: pluginsNeedingManagement only contains entries for POMs that should receive plugin
                    // management
                    // (i.e., the "last local parent" for each plugin that needs management)
                    Set<String> pluginsForThisPom = pluginsNeedingManagement.get(pomPath);
                    if (pluginsForThisPom != null && !pluginsForThisPom.isEmpty()) {
                        hasUpgrades |= addPluginManagementForEffectivePlugins(context, pomDocument, pluginsForThisPom);
                        context.detail("Added plugin management to " + pomPath + " (target parent for "
                                + pluginsForThisPom.size() + " plugins)");
                    }

                    if (hasUpgrades) {
                        modifiedPoms.add(pomPath);
                        context.success("Plugin upgrades applied");
                    } else {
                        context.success("No plugin upgrades needed");
                    }
                } catch (Exception e) {
                    context.failure("Failed to upgrade plugins: " + e.getMessage());
                    errorPoms.add(pomPath);
                } finally {
                    context.unindent();
                }
            }

            // Clean up temp directory
            cleanupTempDirectory(tempDir);

        } catch (Exception e) {
            context.failure("Failed to create temp project structure: " + e.getMessage());
            // Mark all POMs as errors
            errorPoms.addAll(pomMap.keySet());
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Upgrades plugins in the document.
     * Checks both build/plugins and build/pluginManagement/plugins sections.
     * Only processes plugins explicitly defined in the current POM document.
     */
    private boolean upgradePluginsInDocument(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        boolean hasUpgrades = false;

        // Define the plugins that need to be upgraded for Maven 4 compatibility
        Map<String, PluginUpgradeInfo> pluginUpgrades = getPluginUpgradesMap();

        // Check build/plugins
        Element buildElement = root.child(BUILD).orElse(null);
        if (buildElement != null) {
            Element pluginsElement = buildElement.child(PLUGINS).orElse(null);
            if (pluginsElement != null) {
                hasUpgrades |= upgradePluginsInSection(
                        pluginsElement, pluginUpgrades, pomDocument, BUILD + "/" + PLUGINS, context);
            }

            // Check build/pluginManagement/plugins
            Element pluginManagementElement =
                    buildElement.child(PLUGIN_MANAGEMENT).orElse(null);
            if (pluginManagementElement != null) {
                Element managedPluginsElement =
                        pluginManagementElement.child(PLUGINS).orElse(null);
                if (managedPluginsElement != null) {
                    hasUpgrades |= upgradePluginsInSection(
                            managedPluginsElement,
                            pluginUpgrades,
                            pomDocument,
                            BUILD + "/" + PLUGIN_MANAGEMENT + "/" + PLUGINS,
                            context);
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Returns the map of plugins that need to be upgraded for Maven 4 compatibility.
     */
    private Map<String, PluginUpgradeInfo> getPluginUpgradesMap() {
        Map<String, PluginUpgradeInfo> upgrades = new HashMap<>();
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-compiler-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-compiler-plugin", "3.2"));
        upgrades.put(
                "org.codehaus.mojo:exec-maven-plugin",
                new PluginUpgradeInfo("org.codehaus.mojo", "exec-maven-plugin", "3.2.0"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-enforcer-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-enforcer-plugin", "3.0.0"));
        upgrades.put(
                "org.codehaus.mojo:flatten-maven-plugin",
                new PluginUpgradeInfo("org.codehaus.mojo", "flatten-maven-plugin", "1.2.7"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-shade-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-shade-plugin", "3.5.0"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-remote-resources-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-remote-resources-plugin", "3.0.0"));
        return upgrades;
    }

    /**
     * Upgrades plugins in a specific plugins section (either build/plugins or build/pluginManagement/plugins).
     */
    private boolean upgradePluginsInSection(
            Element pluginsElement,
            Map<String, PluginUpgradeInfo> pluginUpgrades,
            Document pomDocument,
            String sectionName,
            UpgradeContext context) {

        return pluginsElement
                .children(PLUGIN)
                .map(pluginElement -> {
                    String groupId = getChildText(pluginElement, GROUP_ID);
                    String artifactId = getChildText(pluginElement, ARTIFACT_ID);

                    // Default groupId for Maven plugins
                    if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                        groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
                    }

                    if (groupId != null && artifactId != null) {
                        String pluginKey = groupId + ":" + artifactId;
                        PluginUpgradeInfo upgrade = pluginUpgrades.get(pluginKey);

                        if (upgrade != null) {
                            return upgradePluginVersion(pluginElement, upgrade, pomDocument, sectionName, context);
                        }
                    }
                    return false;
                })
                .reduce(false, Boolean::logicalOr);
    }

    /**
     * Upgrades a specific plugin's version if needed.
     */
    private boolean upgradePluginVersion(
            Element pluginElement,
            PluginUpgradeInfo upgrade,
            Document pomDocument,
            String sectionName,
            UpgradeContext context) {
        Element versionElement = pluginElement.child(VERSION).orElse(null);
        String currentVersion;
        boolean isProperty = false;
        String propertyName = null;

        if (versionElement != null) {
            currentVersion = versionElement.textContentTrimmed();
            // Check if version is a property reference
            if (currentVersion.startsWith("${") && currentVersion.endsWith("}")) {
                isProperty = true;
                propertyName = currentVersion.substring(2, currentVersion.length() - 1);
            }
        } else {
            // Plugin version might be inherited from parent or pluginManagement
            context.debug("Plugin " + upgrade.groupId + ":" + upgrade.artifactId
                    + " has no explicit version, may inherit from parent");
            return false;
        }

        if (isProperty) {
            // Update property value if it's below minimum version
            return upgradePropertyVersion(pomDocument, propertyName, upgrade, sectionName, context);
        } else {
            // Direct version comparison and upgrade
            if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                Editor editor = new Editor(pomDocument);
                editor.setTextContent(versionElement, upgrade.minVersion);
                context.detail("Upgraded " + upgrade.groupId + ":" + upgrade.artifactId + " from " + currentVersion
                        + " to " + upgrade.minVersion + " in " + sectionName);
                return true;
            } else {
                context.debug("Plugin " + upgrade.groupId + ":" + upgrade.artifactId + " version " + currentVersion
                        + " is already >= " + upgrade.minVersion);
            }
        }

        return false;
    }

    /**
     * Upgrades a property value if it represents a plugin version below the minimum.
     */
    private boolean upgradePropertyVersion(
            Document pomDocument,
            String propertyName,
            PluginUpgradeInfo upgrade,
            String sectionName,
            UpgradeContext context) {
        Editor editor = new Editor(pomDocument);
        Element root = editor.root();
        Element propertiesElement = root.child(PROPERTIES).orElse(null);

        if (propertiesElement != null) {
            Element propertyElement = propertiesElement.child(propertyName).orElse(null);
            if (propertyElement != null) {
                String currentVersion = propertyElement.textContentTrimmed();
                if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                    editor.setTextContent(propertyElement, upgrade.minVersion);
                    context.detail("Upgraded property " + propertyName + " (for " + upgrade.groupId
                            + ":"
                            + upgrade.artifactId + ") from " + currentVersion + " to " + upgrade.minVersion
                            + " in "
                            + sectionName);
                    return true;
                } else {
                    context.debug("Property " + propertyName + " version " + currentVersion + " is already >= "
                            + upgrade.minVersion);
                }
            } else {
                context.warning("Property " + propertyName + " not found in POM properties");
            }
        } else {
            context.warning("No properties section found in POM for property " + propertyName);
        }

        return false;
    }

    /**
     * Simple version comparison to check if current version is below minimum version.
     * This is a basic implementation that works for most Maven plugin versions.
     */
    private boolean isVersionBelow(String currentVersion, String minVersion) {
        if (currentVersion == null || minVersion == null) {
            return false;
        }

        // Remove any qualifiers like -SNAPSHOT, -alpha, etc. for comparison
        String cleanCurrent = currentVersion.split("-")[0];
        String cleanMin = minVersion.split("-")[0];

        try {
            String[] currentParts = cleanCurrent.split("\\.");
            String[] minParts = cleanMin.split("\\.");

            int maxLength = Math.max(currentParts.length, minParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int minPart = i < minParts.length ? Integer.parseInt(minParts[i]) : 0;

                if (currentPart < minPart) {
                    return true;
                } else if (currentPart > minPart) {
                    return false;
                }
            }

            return false; // Versions are equal
        } catch (NumberFormatException e) {
            // Fallback to string comparison if parsing fails
            return currentVersion.compareTo(minVersion) < 0;
        }
    }

    /**
     * Helper method to get child element text.
     */
    private String getChildText(Element parent, String childName) {
        Element child = parent.child(childName).orElse(null);
        return child != null ? child.textContentTrimmed() : null;
    }

    /**
     * Gets the list of plugin upgrades to apply.
     */
    public static List<PluginUpgrade> getPluginUpgrades() {
        return PLUGIN_UPGRADES;
    }

    /**
     * Gets or creates the cached Maven 4 session.
     */
    private Session getSession() {
        if (session == null) {
            session = createMaven4Session();
        }
        return session;
    }

    /**
     * Creates a new Maven 4 session for effective POM computation.
     */
    private Session createMaven4Session() {
        Session session = ApiRunner.createSession(injector -> {
            injector.bindInstance(Dispatcher.class, new LegacyDispatcher());

            injector.bindInstance(
                    TransporterProvider.class,
                    new DefaultTransporterProvider(Map.of(
                            "https",
                            new JdkTransporterFactory(
                                    new DefaultChecksumExtractor(Map.of()), new DefaultPathProcessor()),
                            "file",
                            new FileTransporterFactory())));
        });

        // Configure repositories
        // TODO: we should read settings
        RemoteRepository central =
                session.createRemoteRepository(RemoteRepository.CENTRAL_ID, "https://repo.maven.apache.org/maven2");
        RemoteRepository snapshots = session.getService(RepositoryFactory.class)
                .createRemote(Repository.newBuilder()
                        .id("apache-snapshots")
                        .url("https://repository.apache.org/content/repositories/snapshots/")
                        .releases(RepositoryPolicy.newBuilder().enabled("false").build())
                        .snapshots(RepositoryPolicy.newBuilder().enabled("true").build())
                        .build());

        return session.withRemoteRepositories(List.of(central, snapshots));
    }

    /**
     * Creates a temporary project structure with all POMs written to preserve relative paths.
     * This allows Maven 4 API to properly resolve the project hierarchy.
     */
    private Path createTempProjectStructure(UpgradeContext context, Map<Path, Document> pomMap) throws Exception {
        Path tempDir = Files.createTempDirectory("mvnup-project-");
        context.debug("Created temp project directory: " + tempDir);

        // Find the common root of all POM paths to preserve relative structure
        Path commonRoot = findCommonRoot(pomMap.keySet());
        context.debug("Common root: " + commonRoot);

        // Write each POM to the temp directory, preserving relative structure
        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path originalPath = entry.getKey();
            Document document = entry.getValue();

            // Calculate the relative path from common root
            Path relativePath = commonRoot.relativize(originalPath);
            Path tempPomPath = tempDir.resolve(relativePath);

            // Ensure parent directories exist
            Files.createDirectories(tempPomPath.getParent());

            // Write POM to temp location
            writePomToFile(document, tempPomPath);
            context.debug("Wrote POM to temp location: " + tempPomPath);
        }

        return tempDir;
    }

    /**
     * Finds the common root directory of all POM paths.
     */
    private Path findCommonRoot(Set<Path> pomPaths) {
        Path commonRoot = null;
        for (Path pomPath : pomPaths) {
            Path parent = pomPath.getParent();
            if (parent == null) {
                parent = Path.of(".");
            }
            if (commonRoot == null) {
                commonRoot = parent;
            } else {
                // Find common ancestor
                while (!parent.startsWith(commonRoot)) {
                    commonRoot = commonRoot.getParent();
                    if (commonRoot == null) {
                        break;
                    }
                }
            }
        }
        return commonRoot;
    }

    /**
     * Writes a Document to a file using the same format as the existing codebase.
     */
    private void writePomToFile(Document document, Path filePath) throws Exception {
        Files.writeString(filePath, document.toXml());
    }

    /**
     * Analyzes plugins using effective models built from the temp directory.
     * Returns a map of POM path to the set of plugin keys that need management.
     */
    private Map<Path, Set<String>> analyzePluginsUsingEffectiveModels(
            UpgradeContext context, Map<Path, Document> pomMap, Path tempDir) {
        Map<Path, Set<String>> result = new HashMap<>();
        Map<String, PluginUpgrade> pluginUpgrades = getPluginUpgradesAsMap();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path originalPomPath = entry.getKey();

            try {
                // Find the corresponding temp POM path
                Path commonRoot = findCommonRoot(pomMap.keySet());
                Path relativePath = commonRoot.relativize(originalPomPath);
                Path tempPomPath = tempDir.resolve(relativePath);

                // Build effective model using Maven 4 API
                Set<String> pluginsNeedingUpgrade =
                        analyzeEffectiveModelForPlugins(context, tempPomPath, pluginUpgrades);

                // Determine where to add plugin management (last local parent)
                Path targetPomForManagement =
                        findLastLocalParentForPluginManagement(context, tempPomPath, pomMap, tempDir, commonRoot);

                if (targetPomForManagement != null) {
                    result.computeIfAbsent(targetPomForManagement, k -> new HashSet<>())
                            .addAll(pluginsNeedingUpgrade);

                    if (!pluginsNeedingUpgrade.isEmpty()) {
                        context.debug("Will add plugin management to " + targetPomForManagement + " for plugins: "
                                + pluginsNeedingUpgrade);
                    }
                }

            } catch (Exception e) {
                context.debug("Failed to analyze effective model for " + originalPomPath + ": " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Converts PluginUpgradeInfo map to PluginUpgrade map for compatibility.
     */
    private Map<String, PluginUpgrade> getPluginUpgradesAsMap() {
        return PLUGIN_UPGRADES.stream()
                .collect(Collectors.toMap(
                        upgrade -> upgrade.groupId() + ":" + upgrade.artifactId(), upgrade -> upgrade));
    }

    /**
     * Analyzes the effective model for a single POM to find plugins that need upgrades.
     */
    private Set<String> analyzeEffectiveModelForPlugins(
            UpgradeContext context, Path tempPomPath, Map<String, PluginUpgrade> pluginUpgrades) {

        // Use the cached Maven 4 session
        Session session = getSession();
        ModelBuilder modelBuilder = session.getService(ModelBuilder.class);

        // Build effective model
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(tempPomPath))
                .requestType(ModelBuilderRequest.RequestType.BUILD_EFFECTIVE)
                .recursive(false) // We only want this POM, not its modules
                .build();

        ModelBuilderResult result = modelBuilder.newSession().build(request);
        Model effectiveModel = result.getEffectiveModel();

        // Analyze plugins from effective model
        return analyzePluginsFromEffectiveModel(context, effectiveModel, pluginUpgrades);
    }

    /**
     * Analyzes plugins from the effective model and determines which ones need upgrades.
     */
    private Set<String> analyzePluginsFromEffectiveModel(
            UpgradeContext context, Model effectiveModel, Map<String, PluginUpgrade> pluginUpgrades) {
        Set<String> pluginsNeedingUpgrade = new HashSet<>();

        Build build = effectiveModel.getBuild();
        if (build != null) {
            // Check build/plugins - these are the actual plugins used in the build
            for (Plugin plugin : build.getPlugins()) {
                String pluginKey = getPluginKey(plugin);
                PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
                if (upgrade != null) {
                    String effectiveVersion = plugin.getVersion();
                    if (isVersionBelow(effectiveVersion, upgrade.minVersion())) {
                        pluginsNeedingUpgrade.add(pluginKey);
                        context.debug("Plugin " + pluginKey + " version " + effectiveVersion + " needs upgrade to "
                                + upgrade.minVersion());
                    }
                }
            }

            // Check build/pluginManagement/plugins - these provide version management
            PluginManagement pluginManagement = build.getPluginManagement();
            if (pluginManagement != null) {
                for (Plugin plugin : pluginManagement.getPlugins()) {
                    String pluginKey = getPluginKey(plugin);
                    PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
                    if (upgrade != null) {
                        String effectiveVersion = plugin.getVersion();
                        if (isVersionBelow(effectiveVersion, upgrade.minVersion())) {
                            pluginsNeedingUpgrade.add(pluginKey);
                            context.debug("Managed plugin " + pluginKey + " version " + effectiveVersion
                                    + " needs upgrade to " + upgrade.minVersion());
                        }
                    }
                }
            }
        }

        return pluginsNeedingUpgrade;
    }

    /**
     * Gets the plugin key (groupId:artifactId) for a plugin, handling default groupId.
     */
    private String getPluginKey(Plugin plugin) {
        String groupId = plugin.getGroupId();
        String artifactId = plugin.getArtifactId();

        // Default groupId for Maven plugins
        if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
            groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
        }

        return groupId + ":" + artifactId;
    }

    /**
     * Finds the last local parent in the hierarchy where plugin management should be added.
     * This implements the algorithm: start with the effective model, check if parent is in pomMap,
     * if so continue to its parent, else that's the target.
     */
    private Path findLastLocalParentForPluginManagement(
            UpgradeContext context, Path tempPomPath, Map<Path, Document> pomMap, Path tempDir, Path commonRoot) {

        // Build effective model to get parent information
        Session session = getSession();
        ModelBuilder modelBuilder = session.getService(ModelBuilder.class);

        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(tempPomPath))
                .requestType(ModelBuilderRequest.RequestType.BUILD_EFFECTIVE)
                .recursive(false)
                .build();

        ModelBuilderResult result = modelBuilder.newSession().build(request);
        Model effectiveModel = result.getEffectiveModel();

        // Convert the temp path back to the original path
        Path relativePath = tempDir.relativize(tempPomPath);
        Path currentOriginalPath = commonRoot.resolve(relativePath);

        // Start with current POM as the candidate
        Path lastLocalParent = currentOriginalPath;

        // Walk up the parent hierarchy
        Model currentModel = effectiveModel;
        while (currentModel.getParent() != null) {
            Parent parent = currentModel.getParent();

            // Check if this parent is in our local pomMap
            Path parentPath = findParentInPomMap(parent, pomMap);
            if (parentPath != null) {
                // Parent is local, so it becomes our new candidate
                lastLocalParent = parentPath;

                // Load the parent model to continue walking up
                Path parentTempPath = tempDir.resolve(commonRoot.relativize(parentPath));
                ModelBuilderRequest parentRequest = ModelBuilderRequest.builder()
                        .session(session)
                        .source(Sources.buildSource(parentTempPath))
                        .requestType(ModelBuilderRequest.RequestType.BUILD_EFFECTIVE)
                        .recursive(false)
                        .build();

                ModelBuilderResult parentResult = modelBuilder.newSession().build(parentRequest);
                currentModel = parentResult.getEffectiveModel();
            } else {
                // Parent is external, stop here
                break;
            }
        }

        context.debug("Last local parent for " + currentOriginalPath + " is " + lastLocalParent);
        return lastLocalParent;
    }

    /**
     * Finds a parent POM in the pomMap based on its coordinates.
     */
    private Path findParentInPomMap(Parent parent, Map<Path, Document> pomMap) {
        String parentGroupId = parent.getGroupId();
        String parentArtifactId = parent.getArtifactId();
        String parentVersion = parent.getVersion();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Document doc = entry.getValue();
            Element root = doc.root();

            // Extract GAV from this POM
            String groupId = getChildText(root, GROUP_ID);
            String artifactId = getChildText(root, ARTIFACT_ID);
            String version = getChildText(root, VERSION);

            // Handle inheritance from parent
            Element parentElement = root.child(PARENT).orElse(null);
            if (parentElement != null) {
                if (groupId == null) {
                    groupId = getChildText(parentElement, GROUP_ID);
                }
                if (version == null) {
                    version = getChildText(parentElement, VERSION);
                }
            }

            // Check if this POM matches the parent coordinates
            if (parentGroupId.equals(groupId) && parentArtifactId.equals(artifactId) && parentVersion.equals(version)) {
                return entry.getKey();
            }
        }

        return null; // Parent not found in local project
    }

    /**
     * Adds plugin management entries for plugins found through effective model analysis.
     */
    private boolean addPluginManagementForEffectivePlugins(
            UpgradeContext context, Document pomDocument, Set<String> pluginKeys) {

        Map<String, PluginUpgrade> pluginUpgrades = getPluginUpgradesAsMap();
        boolean hasUpgrades = false;

        Element root = pomDocument.root();

        // Ensure build/pluginManagement/plugins structure exists
        Element buildElement = root.child(BUILD).orElse(null);
        if (buildElement == null) {
            buildElement = DomUtils.insertNewElement(BUILD, root);
        }

        Element pluginManagementElement = buildElement.child(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagementElement == null) {
            pluginManagementElement = DomUtils.insertNewElement(PLUGIN_MANAGEMENT, buildElement);
        }

        Element managedPluginsElement = pluginManagementElement.child(PLUGINS).orElse(null);
        if (managedPluginsElement == null) {
            managedPluginsElement = DomUtils.insertNewElement(PLUGINS, pluginManagementElement);
        }

        // Add plugin management entries for each plugin
        for (String pluginKey : pluginKeys) {
            PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
            if (upgrade != null) {
                // Check if plugin is already managed
                if (!isPluginAlreadyManagedInElement(managedPluginsElement, upgrade)) {
                    addPluginManagementEntryFromUpgrade(managedPluginsElement, upgrade, context);
                    hasUpgrades = true;
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Checks if a plugin is already managed in the given plugins element.
     */
    private boolean isPluginAlreadyManagedInElement(Element pluginsElement, PluginUpgrade upgrade) {
        List<Element> pluginElements = pluginsElement.children(PLUGIN).toList();
        for (Element pluginElement : pluginElements) {
            String groupId = getChildText(pluginElement, GROUP_ID);
            String artifactId = getChildText(pluginElement, ARTIFACT_ID);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
            }

            if (upgrade.groupId().equals(groupId) && upgrade.artifactId().equals(artifactId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a plugin management entry from a PluginUpgrade.
     */
    private void addPluginManagementEntryFromUpgrade(
            Element managedPluginsElement, PluginUpgrade upgrade, UpgradeContext context) {
        // Create plugin element using DomUtils convenience method for proper formatting
        DomUtils.createPlugin(managedPluginsElement, upgrade.groupId(), upgrade.artifactId(), upgrade.minVersion());

        context.detail("Added plugin management for " + upgrade.groupId() + ":" + upgrade.artifactId() + " version "
                + upgrade.minVersion() + " (found through effective model analysis)");
    }

    /**
     * Cleans up the temporary directory.
     */
    private void cleanupTempDirectory(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            // Best effort cleanup - don't fail the whole operation
        }
    }

    /**
     * Holds plugin upgrade information for Maven 4 compatibility.
     * This class contains the minimum version requirements for plugins
     * that need to be upgraded to work properly with Maven 4.
     */
    public static class PluginUpgradeInfo {
        /** The Maven groupId of the plugin */
        final String groupId;

        /** The Maven artifactId of the plugin */
        final String artifactId;

        /** The minimum version required for Maven 4 compatibility */
        final String minVersion;

        /**
         * Creates a new plugin upgrade information holder.
         *
         * @param groupId the Maven groupId of the plugin
         * @param artifactId the Maven artifactId of the plugin
         * @param minVersion the minimum version required for Maven 4 compatibility
         */
        PluginUpgradeInfo(String groupId, String artifactId, String minVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.minVersion = minVersion;
        }
    }
}
