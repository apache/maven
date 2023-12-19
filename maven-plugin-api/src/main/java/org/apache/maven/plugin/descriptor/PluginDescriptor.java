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
package org.apache.maven.plugin.descriptor;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;
import org.apache.maven.api.plugin.descriptor.lifecycle.LifecycleConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.lifecycle.io.LifecycleStaxReader;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.eclipse.aether.graph.DependencyNode;

/**
 */
public class PluginDescriptor extends ComponentSetDescriptor implements Cloneable {

    private static final String LIFECYCLE_DESCRIPTOR = "META-INF/maven/lifecycle.xml";

    private static final Pattern PATTERN_FILTER_1 = Pattern.compile("-?(maven|plugin)-?");

    private String groupId;

    private String artifactId;

    private String version;

    private String goalPrefix;

    private String source;

    private boolean inheritedByDefault = true;

    private List<Artifact> artifacts;

    private DependencyNode dependencyNode;

    private ClassRealm classRealm;

    // calculated on-demand.
    private Map<String, Artifact> artifactMap;

    private Set<Artifact> introducedDependencyArtifacts;

    private String name;

    private String description;

    private String requiredMavenVersion;

    private String requiredJavaVersion;

    private Plugin plugin;

    private Artifact pluginArtifact;

    private Map<String, Lifecycle> lifecycleMappings;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public PluginDescriptor() {}

    public PluginDescriptor(PluginDescriptor original) {
        this.setGroupId(original.getGroupId());
        this.setArtifactId(original.getArtifactId());
        this.setVersion(original.getVersion());
        this.setGoalPrefix(original.getGoalPrefix());
        this.setInheritedByDefault(original.isInheritedByDefault());
        this.setName(original.getName());
        this.setDescription(original.getDescription());
        this.setRequiredMavenVersion(original.getRequiredMavenVersion());
        this.setRequiredJavaVersion(original.getRequiredJavaVersion());
        this.setPluginArtifact(ArtifactUtils.copyArtifactSafe(original.getPluginArtifact()));
        this.setComponents(clone(original.getMojos(), this));
        this.setId(original.getId());
        this.setIsolatedRealm(original.isIsolatedRealm());
        this.setSource(original.getSource());
        this.setDependencies(original.getDependencies());
        this.setDependencyNode(original.getDependencyNode());
    }

    private static List<ComponentDescriptor<?>> clone(List<MojoDescriptor> mojos, PluginDescriptor pluginDescriptor) {
        List<ComponentDescriptor<?>> clones = null;
        if (mojos != null) {
            clones = new ArrayList<>(mojos.size());
            for (MojoDescriptor mojo : mojos) {
                MojoDescriptor clone = mojo.clone();
                clone.setPluginDescriptor(pluginDescriptor);
                clones.add(clone);
            }
        }
        return clones;
    }

    public PluginDescriptor(org.apache.maven.api.plugin.descriptor.PluginDescriptor original) {
        this.setGroupId(original.getGroupId());
        this.setArtifactId(original.getArtifactId());
        this.setVersion(original.getVersion());
        this.setGoalPrefix(original.getGoalPrefix());
        this.setInheritedByDefault(original.isInheritedByDefault());
        this.setName(original.getName());
        this.setDescription(original.getDescription());
        this.setRequiredMavenVersion(original.getRequiredMavenVersion());
        this.setRequiredJavaVersion(original.getRequiredJavaVersion());
        this.setPluginArtifact(null); // TODO: v4
        this.setComponents(original.getMojos().stream()
                .map(m -> new MojoDescriptor(this, m))
                .collect(Collectors.toList()));
        this.setId(original.getId());
        this.setIsolatedRealm(original.isIsolatedRealm());
        this.setSource(null);
        this.setDependencies(Collections.emptyList()); // TODO: v4
        this.setDependencyNode(null); // TODO: v4
        this.pluginDescriptorV4 = original;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<MojoDescriptor> getMojos() {
        return (List) getComponents();
    }

    public void addMojo(MojoDescriptor mojoDescriptor) throws DuplicateMojoDescriptorException {
        MojoDescriptor existing = null;
        // this relies heavily on the equals() and hashCode() for ComponentDescriptor,
        // which uses role:roleHint for identity...and roleHint == goalPrefix:goal.
        // role does not vary for Mojos.
        List<MojoDescriptor> mojos = getMojos();

        if (mojos != null && mojos.contains(mojoDescriptor)) {
            int indexOf = mojos.indexOf(mojoDescriptor);

            existing = mojos.get(indexOf);
        }

        if (existing != null) {
            throw new DuplicateMojoDescriptorException(
                    getGoalPrefix(),
                    mojoDescriptor.getGoal(),
                    existing.getImplementation(),
                    mojoDescriptor.getImplementation());
        } else {
            addComponentDescriptor(mojoDescriptor);
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    // ----------------------------------------------------------------------
    // Dependencies
    // ----------------------------------------------------------------------

    public static String constructPluginKey(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    public String getPluginLookupKey() {
        return groupId + ":" + artifactId;
    }

    public String getId() {
        return constructPluginKey(groupId, artifactId, version);
    }

    public static String getDefaultPluginArtifactId(String id) {
        return "maven-" + id + "-plugin";
    }

    public static String getDefaultPluginGroupId() {
        return "org.apache.maven.plugins";
    }

    /**
     * Parse maven-...-plugin.
     *
     * TODO move to plugin-tools-api as a default only
     */
    public static String getGoalPrefixFromArtifactId(String artifactId) {
        if ("maven-plugin-plugin".equals(artifactId)) {
            return "plugin";
        } else {
            return PATTERN_FILTER_1.matcher(artifactId).replaceAll("");
        }
    }

    public String getGoalPrefix() {
        return goalPrefix;
    }

    public void setGoalPrefix(String goalPrefix) {
        this.goalPrefix = goalPrefix;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public boolean isInheritedByDefault() {
        return inheritedByDefault;
    }

    public void setInheritedByDefault(boolean inheritedByDefault) {
        this.inheritedByDefault = inheritedByDefault;
    }

    /**
     * Gets the artifacts that make up the plugin's class realm, excluding artifacts shadowed by the Maven core realm
     * like {@code maven-project}.
     *
     * @return The plugin artifacts, never {@code null}.
     */
    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;

        // clear the calculated artifactMap
        artifactMap = null;
    }

    public DependencyNode getDependencyNode() {
        return dependencyNode;
    }

    public void setDependencyNode(DependencyNode dependencyNode) {
        this.dependencyNode = dependencyNode;
    }

    /**
     * The map of artifacts accessible by the versionlessKey, i.e. groupId:artifactId
     *
     * @return a Map of artifacts, never {@code null}
     * @see #getArtifacts()
     */
    public Map<String, Artifact> getArtifactMap() {
        if (artifactMap == null) {
            artifactMap = ArtifactUtils.artifactMapByVersionlessId(getArtifacts());
        }

        return artifactMap;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        return object instanceof PluginDescriptor && getId().equals(((PluginDescriptor) object).getId());
    }

    public int hashCode() {
        return 10 + getId().hashCode();
    }

    public MojoDescriptor getMojo(String goal) {
        if (getMojos() == null) {
            return null; // no mojo in this POM
        }

        // TODO could we use a map? Maybe if the parent did that for components too, as this is too vulnerable to
        // changes above not being propagated to the map
        for (MojoDescriptor desc : getMojos()) {
            if (goal.equals(desc.getGoal())) {
                return desc;
            }
        }
        return null;
    }

    public void setClassRealm(ClassRealm classRealm) {
        this.classRealm = classRealm;
    }

    public ClassRealm getClassRealm() {
        return classRealm;
    }

    public void setIntroducedDependencyArtifacts(Set<Artifact> introducedDependencyArtifacts) {
        this.introducedDependencyArtifacts = introducedDependencyArtifacts;
    }

    public Set<Artifact> getIntroducedDependencyArtifacts() {
        return (introducedDependencyArtifacts != null) ? introducedDependencyArtifacts : Collections.emptySet();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setRequiredMavenVersion(String requiredMavenVersion) {
        this.requiredMavenVersion = requiredMavenVersion;
    }

    public String getRequiredMavenVersion() {
        return requiredMavenVersion;
    }

    public void setRequiredJavaVersion(String requiredJavaVersion) {
        this.requiredJavaVersion = requiredJavaVersion;
    }

    public String getRequiredJavaVersion() {
        return requiredJavaVersion;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Artifact getPluginArtifact() {
        return pluginArtifact;
    }

    public void setPluginArtifact(Artifact pluginArtifact) {
        this.pluginArtifact = pluginArtifact;
    }

    public Lifecycle getLifecycleMapping(String lifecycleId) throws IOException, XMLStreamException {
        return getLifecycleMappings().get(lifecycleId);
    }

    public Map<String, Lifecycle> getLifecycleMappings() throws IOException, XMLStreamException {
        if (lifecycleMappings == null) {
            LifecycleConfiguration lifecycleConfiguration;

            try (InputStream input = getDescriptorStream(LIFECYCLE_DESCRIPTOR)) {
                lifecycleConfiguration = new LifecycleStaxReader().read(input);
            }

            lifecycleMappings = new HashMap<>();

            for (Lifecycle lifecycle : lifecycleConfiguration.getLifecycles()) {
                lifecycleMappings.put(lifecycle.getId(), lifecycle);
            }
        }
        return lifecycleMappings;
    }

    private InputStream getDescriptorStream(String descriptor) throws IOException {
        File pluginFile = (pluginArtifact != null) ? pluginArtifact.getFile() : null;
        if (pluginFile == null) {
            throw new IllegalStateException("plugin main artifact has not been resolved for " + getId());
        }

        if (pluginFile.isFile()) {
            try {
                return new URL("jar:" + pluginFile.toURI() + "!/" + descriptor).openStream();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        } else {
            return Files.newInputStream(new File(pluginFile, descriptor).toPath());
        }
    }

    /**
     * Creates a shallow copy of this plugin descriptor.
     */
    @Override
    public PluginDescriptor clone() {
        try {
            return (PluginDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public void addMojos(List<MojoDescriptor> mojos) throws DuplicateMojoDescriptorException {
        for (MojoDescriptor mojoDescriptor : mojos) {
            addMojo(mojoDescriptor);
        }
    }

    private volatile org.apache.maven.api.plugin.descriptor.PluginDescriptor pluginDescriptorV4;

    public org.apache.maven.api.plugin.descriptor.PluginDescriptor getPluginDescriptorV4() {
        if (pluginDescriptorV4 == null) {
            synchronized (this) {
                if (pluginDescriptorV4 == null) {
                    pluginDescriptorV4 = org.apache.maven.api.plugin.descriptor.PluginDescriptor.newBuilder()
                            .namespaceUri(null)
                            .modelEncoding(null)
                            .name(name)
                            .description(description)
                            .groupId(groupId)
                            .artifactId(artifactId)
                            .version(version)
                            .goalPrefix(goalPrefix)
                            .isolatedRealm(isIsolatedRealm())
                            .inheritedByDefault(inheritedByDefault)
                            .requiredJavaVersion(requiredJavaVersion)
                            .requiredMavenVersion(requiredMavenVersion)
                            .mojos(getMojos().stream()
                                    .map(MojoDescriptor::getMojoDescriptorV4)
                                    .collect(Collectors.toList()))
                            .build();
                }
            }
        }
        return pluginDescriptorV4;
    }
}
