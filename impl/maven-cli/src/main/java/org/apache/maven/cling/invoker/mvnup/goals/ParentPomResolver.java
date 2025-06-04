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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.Version;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.RepositoryPolicy;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.VersionParser;
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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.MAVEN_PLUGIN_PREFIX;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ARTIFACT_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.BUILD;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PARENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGINS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.VERSION;

/**
 * Utility class for resolving and analyzing parent POMs for plugin upgrades.
 * This class uses the Maven 4 API to compute effective POMs and check if they
 * contain plugins that need to be managed locally. For external parents, it
 * leverages Maven's model building capabilities to resolve parent hierarchies
 * and compute the effective model. Falls back to direct HTTP download if needed.
 *
 * This is a singleton that caches the Maven 4 session for performance.
 */
@Named
@Singleton
public class ParentPomResolver {

    private Session cachedSession;
    private final Object sessionLock = new Object();

    /**
     * Gets or creates the cached Maven 4 session.
     */
    private Session getSession() {
        if (cachedSession == null) {
            synchronized (sessionLock) {
                if (cachedSession == null) {
                    cachedSession = createMaven4Session();
                }
            }
        }
        return cachedSession;
    }

    /**
     * Creates a new Maven 4 session with proper configuration.
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
     * Checks parent POMs for plugins that need to be managed locally.
     * Uses Maven 4 API to compute effective POM and checks if it contains
     * any of the target plugins that need version management.
     */
    public boolean checkParentPomsForPlugins(
            UpgradeContext context,
            Document pomDocument,
            Map<String, PluginUpgrade> pluginUpgrades,
            Map<Path, Document> pomMap) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Get parent information
        Element parentElement = root.getChild(PARENT, namespace);
        if (parentElement == null) {
            return false; // No parent to check
        }

        String parentGroupId = getChildText(parentElement, GROUP_ID, namespace);
        String parentArtifactId = getChildText(parentElement, ARTIFACT_ID, namespace);
        String parentVersion = getChildText(parentElement, VERSION, namespace);

        if (parentGroupId == null || parentArtifactId == null || parentVersion == null) {
            context.debug("Parent POM has incomplete coordinates, skipping parent plugin check");
            return false;
        }

        // Check if parent is external (not in current project)
        if (isExternalParent(context, parentGroupId, parentArtifactId, parentVersion, pomMap)) {
            // Use Maven 4 API to compute effective POM
            Set<String> parentPlugins = findPluginsUsingMaven4Api(context, pomDocument, pluginUpgrades);

            if (!parentPlugins.isEmpty()) {
                // Add plugin management entries for plugins found in parent
                hasUpgrades = addPluginManagementForParentPlugins(context, pomDocument, parentPlugins, pluginUpgrades);
            }
        } else {
            context.debug("Parent POM is local, skipping Maven 4 API check");
        }

        return hasUpgrades;
    }

    /**
     * Adds plugin management entries for plugins found in parent POMs.
     */
    public boolean addPluginManagementForParentPlugins(
            UpgradeContext context,
            Document pomDocument,
            Set<String> parentPlugins,
            Map<String, PluginUpgrade> pluginUpgrades) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Ensure build/pluginManagement/plugins structure exists using proper JDom utilities
        Element buildElement = root.getChild(BUILD, namespace);
        if (buildElement == null) {
            buildElement = JDomUtils.insertNewElement(BUILD, root);
        }

        Element pluginManagementElement = buildElement.getChild(PLUGIN_MANAGEMENT, namespace);
        if (pluginManagementElement == null) {
            pluginManagementElement = JDomUtils.insertNewElement(PLUGIN_MANAGEMENT, buildElement);
        }

        Element pluginsElement = pluginManagementElement.getChild(PLUGINS, namespace);
        if (pluginsElement == null) {
            pluginsElement = JDomUtils.insertNewElement(PLUGINS, pluginManagementElement);
        }

        // Add plugin management entries for each parent plugin
        for (String pluginKey : parentPlugins) {
            PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
            if (upgrade != null) {
                // Check if plugin is already managed
                Element existingPlugin = findExistingManagedPlugin(pluginsElement, namespace, upgrade);
                if (existingPlugin == null) {
                    // Plugin not managed - add new entry
                    addPluginManagementEntry(context, pluginsElement, upgrade);
                    hasUpgrades = true;
                } else {
                    // Plugin already managed - check if it needs version upgrade
                    if (upgradeExistingPluginManagement(context, existingPlugin, namespace, upgrade)) {
                        hasUpgrades = true;
                    }
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Finds an existing managed plugin element.
     */
    public Element findExistingManagedPlugin(Element pluginsElement, Namespace namespace, PluginUpgrade upgrade) {
        List<Element> pluginElements = pluginsElement.getChildren(PLUGIN, namespace);

        for (Element pluginElement : pluginElements) {
            String groupId = getChildText(pluginElement, GROUP_ID, namespace);
            String artifactId = getChildText(pluginElement, ARTIFACT_ID, namespace);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
            }

            if (upgrade.groupId().equals(groupId) && upgrade.artifactId().equals(artifactId)) {
                return pluginElement;
            }
        }

        return null;
    }

    /**
     * Upgrades an existing plugin management entry if needed.
     */
    public boolean upgradeExistingPluginManagement(
            UpgradeContext context, Element pluginElement, Namespace namespace, PluginUpgrade upgrade) {
        Element versionElement = pluginElement.getChild(VERSION, namespace);

        if (versionElement == null) {
            // No version element - add one
            JDomUtils.insertContentElement(pluginElement, VERSION, upgrade.minVersion());
            context.detail("Added version " + upgrade.minVersion() + " to plugin management for " + upgrade.groupId()
                    + ":" + upgrade.artifactId() + " (found in parent POM)");
            return true;
        } else {
            String currentVersion = versionElement.getTextTrim();
            if (currentVersion == null || currentVersion.isEmpty()) {
                // Empty version - set it
                versionElement.setText(upgrade.minVersion());
                context.detail("Set version " + upgrade.minVersion() + " for plugin management " + upgrade.groupId()
                        + ":" + upgrade.artifactId() + " (found in parent POM)");
                return true;
            } else {
                // Version exists - check if it needs upgrading
                if (isVersionBelow(currentVersion, upgrade.minVersion())) {
                    versionElement.setText(upgrade.minVersion());
                    context.detail("Upgraded plugin management " + upgrade.groupId() + ":"
                            + upgrade.artifactId() + " from " + currentVersion + " to " + upgrade.minVersion()
                            + " (found in parent POM)");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Adds a plugin management entry for a plugin found in parent POM.
     */
    public static void addPluginManagementEntry(UpgradeContext context, Element pluginsElement, PluginUpgrade upgrade) {

        // Create plugin element using JDomUtils for proper formatting
        Element pluginElement = JDomUtils.insertNewElement(PLUGIN, pluginsElement);

        // Add child elements using JDomUtils for proper formatting
        JDomUtils.insertContentElement(pluginElement, GROUP_ID, upgrade.groupId());
        JDomUtils.insertContentElement(pluginElement, ARTIFACT_ID, upgrade.artifactId());
        JDomUtils.insertContentElement(pluginElement, VERSION, upgrade.minVersion());

        context.detail("Added plugin management for " + upgrade.groupId() + ":" + upgrade.artifactId() + " version "
                + upgrade.minVersion() + " (found in parent POM)");
    }

    /**
     * Checks if the parent is external (not part of the current project).
     * A parent is considered external if it's not found in the current project's pomMap.
     */
    private boolean isExternalParent(
            UpgradeContext context,
            String parentGroupId,
            String parentArtifactId,
            String parentVersion,
            Map<Path, Document> pomMap) {

        // Check if any POM in the current project matches the parent coordinates
        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Document doc = entry.getValue();
            Element root = doc.getRootElement();
            Namespace namespace = root.getNamespace();

            // Extract GAV from this POM
            String groupId = getChildText(root, GROUP_ID, namespace);
            String artifactId = getChildText(root, ARTIFACT_ID, namespace);
            String version = getChildText(root, VERSION, namespace);

            // Handle inheritance from parent
            Element parentElement = root.getChild(PARENT, namespace);
            if (parentElement != null) {
                if (groupId == null) {
                    groupId = getChildText(parentElement, GROUP_ID, namespace);
                }
                if (version == null) {
                    version = getChildText(parentElement, VERSION, namespace);
                }
            }

            // Check if this POM matches the parent coordinates
            if (parentGroupId.equals(groupId) && parentArtifactId.equals(artifactId) && parentVersion.equals(version)) {
                context.debug("Found parent " + parentGroupId + ":" + parentArtifactId + ":" + parentVersion
                        + " in local project at " + entry.getKey());
                return false; // Parent is local
            }
        }

        context.debug("Parent " + parentGroupId + ":" + parentArtifactId + ":" + parentVersion + " is external");
        return true; // Parent not found in local project, so it's external
    }

    /**
     * Uses Maven 4 API to compute the effective POM and find plugins that need management.
     * This method uses the cached session, builds the effective model, and analyzes plugin versions.
     */
    private Set<String> findPluginsUsingMaven4Api(
            UpgradeContext context, Document pomDocument, Map<String, PluginUpgrade> pluginUpgrades) {
        Set<String> pluginsNeedingUpgrade = new HashSet<>();

        try {
            // Create a temporary POM file from the JDOM document
            Path tempPomPath = createTempPomFile(pomDocument);

            // Use cached session
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

            // Analyze plugins from effective model and determine which need upgrades
            pluginsNeedingUpgrade.addAll(analyzePluginsForUpgrades(context, session, effectiveModel, pluginUpgrades));

            context.debug("Found " + pluginsNeedingUpgrade.size()
                    + " target plugins needing upgrades in effective POM using Maven 4 API");

            // Clean up temp file
            tempPomPath.toFile().delete();

        } catch (Exception e) {
            context.debug("Failed to use Maven 4 API for effective POM computation: " + e.getMessage());
            throw new RuntimeException("Maven 4 API failed", e);
        }

        return pluginsNeedingUpgrade;
    }

    /**
     * Creates a temporary POM file from a JDOM document.
     */
    private static Path createTempPomFile(Document pomDocument) throws Exception {
        Path tempFile = java.nio.file.Files.createTempFile("mvnup-", ".pom");
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile.toFile())) {
            org.jdom2.output.XMLOutputter outputter = new org.jdom2.output.XMLOutputter();
            outputter.output(pomDocument, writer);
        }
        return tempFile;
    }

    /**
     * Analyzes plugins from the effective model and determines which ones need upgrades.
     * This method compares the effective plugin versions against the minimum required versions
     * and only returns plugins that actually need to be upgraded.
     */
    private Set<String> analyzePluginsForUpgrades(
            UpgradeContext context, Session session, Model effectiveModel, Map<String, PluginUpgrade> pluginUpgrades) {
        Set<String> pluginsNeedingUpgrade = new HashSet<>();

        Build build = effectiveModel.getBuild();
        if (build != null) {
            // Check build/plugins - these are the actual plugins used in the build
            for (Plugin plugin : build.getPlugins()) {
                String pluginKey = getPluginKey(plugin);
                PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
                if (upgrade != null) {
                    String effectiveVersion = plugin.getVersion();
                    if (effectiveVersion != null
                            && needsVersionUpgrade(context, effectiveVersion, upgrade.minVersion())) {
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
                        if (effectiveVersion != null
                                && needsVersionUpgrade(context, effectiveVersion, upgrade.minVersion())) {
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
     * Checks if a plugin version needs to be upgraded based on our minimum requirements.
     */
    private boolean needsVersionUpgrade(UpgradeContext context, String currentVersion, String minVersion) {
        // Compare versions using Maven 4 API
        boolean needsUpgrade = isVersionBelow(currentVersion, minVersion);
        if (needsUpgrade) {
            context.debug("Current version " + currentVersion + " is below minimum " + minVersion);
        }
        return needsUpgrade;
    }

    /**
     * Compares two version strings to determine if the first is below the second.
     */
    private boolean isVersionBelow(String currentVersion, String targetVersion) {
        try {
            VersionParser parser = getSession().getService(VersionParser.class);
            Version cur = parser.parseVersion(currentVersion);
            Version tgt = parser.parseVersion(targetVersion);
            return cur.compareTo(tgt) < 0; // Changed from <= to < so equal versions don't trigger upgrades
        } catch (Exception e) {
            // Fallback to string comparison if version parsing fails
            return currentVersion.compareTo(targetVersion) < 0;
        }
    }

    /**
     * Gets the plugin key (groupId:artifactId) for a plugin, handling default groupId.
     */
    private static String getPluginKey(Plugin plugin) {
        String groupId = plugin.getGroupId();
        String artifactId = plugin.getArtifactId();

        // Default groupId for Maven plugins
        if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
            groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
        }

        return groupId + ":" + artifactId;
    }

    /**
     * Helper method to get child text content safely.
     */
    private static String getChildText(Element parent, String childName, Namespace namespace) {
        Element child = parent.getChild(childName, namespace);
        return child != null ? child.getTextTrim() : null;
    }
}
