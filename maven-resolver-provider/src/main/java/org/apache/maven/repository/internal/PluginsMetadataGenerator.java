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
package org.apache.maven.repository.internal;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.repository.internal.PluginsMetadata.PluginInfo;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven G level metadata generator.
 * <p>
 * Plugin metadata contains G level list of "prefix" to A mapping for plugins present under this G.
 */
class PluginsMetadataGenerator implements MetadataGenerator {
    private static final String PLUGIN_DESCRIPTOR_LOCATION = "META-INF/maven/plugin.xml";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<Object, PluginsMetadata> processedPlugins;

    private final Date timestamp;

    PluginsMetadataGenerator(RepositorySystemSession session, InstallRequest request) {
        this(session, request.getMetadata());
    }

    PluginsMetadataGenerator(RepositorySystemSession session, DeployRequest request) {
        this(session, request.getMetadata());
    }

    private PluginsMetadataGenerator(RepositorySystemSession session, Collection<? extends Metadata> metadatas) {
        this.processedPlugins = new LinkedHashMap<>();
        this.timestamp = (Date) ConfigUtils.getObject(session, new Date(), "maven.startTime");

        /*
         * NOTE: This should be considered a quirk to support interop with Maven's legacy ArtifactDeployer which
         * processes one artifact at a time and hence cannot associate the artifacts from the same project to use the
         * same version index. Allowing the caller to pass in metadata from a previous deployment allows to re-establish
         * the association between the artifacts of the same project.
         */
        for (Iterator<? extends Metadata> it = metadatas.iterator(); it.hasNext(); ) {
            Metadata metadata = it.next();
            if (metadata instanceof PluginsMetadata) {
                it.remove();
                PluginsMetadata pluginMetadata = (PluginsMetadata) metadata;
                processedPlugins.put(pluginMetadata.getGroupId(), pluginMetadata);
            }
        }
    }

    @Override
    public Collection<? extends Metadata> prepare(Collection<? extends Artifact> artifacts) {
        return Collections.emptyList();
    }

    @Override
    public Artifact transformArtifact(Artifact artifact) {
        return artifact;
    }

    @Override
    public Collection<? extends Metadata> finish(Collection<? extends Artifact> artifacts) {
        LinkedHashMap<String, PluginsMetadata> plugins = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            PluginInfo pluginInfo = extractPluginInfo(artifact);
            if (pluginInfo != null) {
                String key = pluginInfo.groupId;
                if (processedPlugins.get(key) == null) {
                    PluginsMetadata pluginMetadata = plugins.get(key);
                    if (pluginMetadata == null) {
                        pluginMetadata = new PluginsMetadata(pluginInfo, timestamp);
                        plugins.put(key, pluginMetadata);
                    }
                }
            }
        }
        return plugins.values();
    }

    private PluginInfo extractPluginInfo(Artifact artifact) {
        // sanity: jar, no classifier and file exists
        if (artifact != null
                && "jar".equals(artifact.getExtension())
                && "".equals(artifact.getClassifier())
                && artifact.getFile() != null) {
            Path artifactPath = artifact.getFile().toPath();
            if (Files.isRegularFile(artifactPath)) {
                try (JarFile artifactJar = new JarFile(artifactPath.toFile(), false)) {
                    ZipEntry pluginDescriptorEntry = artifactJar.getEntry(PLUGIN_DESCRIPTOR_LOCATION);

                    if (pluginDescriptorEntry != null) {
                        try (Reader reader =
                                ReaderFactory.newXmlReader(artifactJar.getInputStream(pluginDescriptorEntry))) {
                            // Note: using DOM instead of use of
                            // org.apache.maven.plugin.descriptor.PluginDescriptor
                            // as it would pull in dependency on:
                            // - maven-plugin-api (for model)
                            // - Plexus Container (for model supporting classes and exceptions)
                            Xpp3Dom root = Xpp3DomBuilder.build(reader);
                            String groupId = mayGetChild(root, "groupId");
                            String artifactId = mayGetChild(root, "artifactId");
                            String goalPrefix = mayGetChild(root, "goalPrefix");
                            String name = mayGetChild(root, "name");
                            // sanity check: plugin descriptor extracted from artifact must have same GA
                            if (Objects.equals(artifact.getGroupId(), groupId)
                                    && Objects.equals(artifact.getArtifactId(), artifactId)) {
                                // here groupId and artifactId cannot be null
                                return new PluginInfo(groupId, artifactId, goalPrefix, name);
                            } else {
                                logger.warn(
                                        "Artifact {}:{}"
                                                + " JAR (about to be installed/deployed) contains Maven Plugin metadata for"
                                                + " conflicting coordinates: {}:{}."
                                                + " Your JAR contains rogue Maven Plugin metadata."
                                                + " Possible causes may be: shaded into this JAR some Maven Plugin or some rogue resource.",
                                        artifact.getGroupId(),
                                        artifact.getArtifactId(),
                                        groupId,
                                        artifactId);
                            }
                        }
                    }
                } catch (Exception e) {
                    // here we can have: IO. ZIP or Plexus Conf Ex: but we should not interfere with user intent
                }
            }
        }
        return null;
    }

    private static String mayGetChild(Xpp3Dom node, String child) {
        Xpp3Dom c = node.getChild(child);
        if (c != null) {
            return c.getValue();
        }
        return null;
    }
}
