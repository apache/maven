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
import javax.xml.stream.XMLStreamReader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.internal.xml.XmlNodeBuilder;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.plugin.descriptor.io.PluginDescriptorStaxReader;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

/**
 */
public class PluginDescriptorBuilder {

    public static final String PLUGIN_2_0_0 = "http://maven.apache.org/PLUGIN/2.0.0";
    private static final int BUFFER_SIZE = 8192;

    public interface StreamSupplier {
        InputStream open() throws IOException;
    }

    public interface ReaderSupplier {
        Reader open() throws IOException;
    }

    /**
     * @deprecated use {@link #build(ReaderSupplier)}
     */
    @Deprecated
    public PluginDescriptor build(Reader reader) throws PlexusConfigurationException {
        return build(reader, null);
    }

    /**
     * @deprecated use {@link #build(ReaderSupplier, String)}
     */
    @Deprecated
    public PluginDescriptor build(Reader reader, String source) throws PlexusConfigurationException {
        return build(() -> reader, source);
    }

    public PluginDescriptor build(ReaderSupplier readerSupplier) throws PlexusConfigurationException {
        return build(readerSupplier, null);
    }

    public PluginDescriptor build(ReaderSupplier readerSupplier, String source) throws PlexusConfigurationException {
        try (BufferedReader br = new BufferedReader(readerSupplier.open(), BUFFER_SIZE)) {
            br.mark(BUFFER_SIZE);
            XMLStreamReader xsr = WstxInputFactory.newFactory().createXMLStreamReader(br);
            xsr.nextTag();
            String nsUri = xsr.getNamespaceURI();
            try (BufferedReader br2 = reset(readerSupplier, br)) {
                xsr = WstxInputFactory.newFactory().createXMLStreamReader(br2);
                return build(source, nsUri, xsr);
            }
        } catch (XMLStreamException | IOException e) {
            throw new PlexusConfigurationException(e.getMessage(), e);
        }
    }

    /**
     * @deprecated use {@link #build(StreamSupplier, String)}
     */
    @Deprecated
    public PluginDescriptor build(InputStream input, String source) throws PlexusConfigurationException {
        return build(() -> input, source);
    }

    public PluginDescriptor build(StreamSupplier inputSupplier) throws PlexusConfigurationException {
        return build(inputSupplier, null);
    }

    public PluginDescriptor build(StreamSupplier inputSupplier, String source) throws PlexusConfigurationException {
        try (BufferedInputStream bis = new BufferedInputStream(inputSupplier.open(), BUFFER_SIZE)) {
            bis.mark(BUFFER_SIZE);
            XMLStreamReader xsr = WstxInputFactory.newFactory().createXMLStreamReader(bis);
            xsr.nextTag();
            String nsUri = xsr.getNamespaceURI();
            try (BufferedInputStream bis2 = reset(inputSupplier, bis)) {
                xsr = WstxInputFactory.newFactory().createXMLStreamReader(bis2);
                return build(source, nsUri, xsr);
            }
        } catch (XMLStreamException | IOException e) {
            throw new PlexusConfigurationException(e.getMessage(), e);
        }
    }

    private static BufferedInputStream reset(StreamSupplier inputSupplier, BufferedInputStream bis) throws IOException {
        try {
            bis.reset();
            return bis;
        } catch (IOException e) {
            return new BufferedInputStream(inputSupplier.open(), BUFFER_SIZE);
        }
    }

    private static BufferedReader reset(ReaderSupplier readerSupplier, BufferedReader br) throws IOException {
        try {
            br.reset();
            return br;
        } catch (IOException e) {
            return new BufferedReader(readerSupplier.open(), BUFFER_SIZE);
        }
    }

    private PluginDescriptor build(String source, String nsUri, XMLStreamReader xsr)
            throws XMLStreamException, PlexusConfigurationException {
        if (PLUGIN_2_0_0.equals(nsUri)) {
            org.apache.maven.api.plugin.descriptor.PluginDescriptor pd =
                    new PluginDescriptorStaxReader().read(xsr, true);
            return new PluginDescriptor(pd);
        } else {
            XmlNode node = XmlNodeBuilder.build(xsr, true, null);
            PlexusConfiguration cfg = XmlPlexusConfiguration.toPlexusConfiguration(node);
            return build(source, cfg);
        }
    }

    private PluginDescriptor build(String source, PlexusConfiguration c) throws PlexusConfigurationException {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        pluginDescriptor.setSource(source);
        pluginDescriptor.setGroupId(extractGroupId(c));
        pluginDescriptor.setArtifactId(extractArtifactId(c));
        pluginDescriptor.setVersion(extractVersion(c));
        pluginDescriptor.setGoalPrefix(extractGoalPrefix(c));

        pluginDescriptor.setName(extractName(c));
        pluginDescriptor.setDescription(extractDescription(c));

        pluginDescriptor.setIsolatedRealm(extractIsolatedRealm(c));
        pluginDescriptor.setInheritedByDefault(extractInheritedByDefault(c));
        pluginDescriptor.setRequiredJavaVersion(extractRequiredJavaVersion(c).orElse(null));
        pluginDescriptor.setRequiredMavenVersion(extractRequiredMavenVersion(c).orElse(null));

        pluginDescriptor.addMojos(extractMojos(c, pluginDescriptor));

        pluginDescriptor.setDependencies(extractComponentDependencies(c));

        return pluginDescriptor;
    }

    private String extractGroupId(PlexusConfiguration c) {
        return c.getChild("groupId").getValue();
    }

    private String extractArtifactId(PlexusConfiguration c) {
        return c.getChild("artifactId").getValue();
    }

    private String extractVersion(PlexusConfiguration c) {
        return c.getChild("version").getValue();
    }

    private String extractGoalPrefix(PlexusConfiguration c) {
        return c.getChild("goalPrefix").getValue();
    }

    private String extractName(PlexusConfiguration c) {
        return c.getChild("name").getValue();
    }

    private String extractDescription(PlexusConfiguration c) {
        return c.getChild("description").getValue();
    }

    private List<MojoDescriptor> extractMojos(PlexusConfiguration c, PluginDescriptor pluginDescriptor)
            throws PlexusConfigurationException {
        List<MojoDescriptor> mojos = new ArrayList<>();

        PlexusConfiguration[] mojoConfigurations = c.getChild("mojos").getChildren("mojo");

        for (PlexusConfiguration component : mojoConfigurations) {
            mojos.add(buildComponentDescriptor(component, pluginDescriptor));
        }
        return mojos;
    }

    private boolean extractInheritedByDefault(PlexusConfiguration c) {
        String inheritedByDefault = c.getChild("inheritedByDefault").getValue();

        if (inheritedByDefault != null) {
            return Boolean.parseBoolean(inheritedByDefault);
        }
        return false;
    }

    private boolean extractIsolatedRealm(PlexusConfiguration c) {
        String isolatedRealm = c.getChild("isolatedRealm").getValue();

        if (isolatedRealm != null) {
            return Boolean.parseBoolean(isolatedRealm);
        }
        return false;
    }

    private Optional<String> extractRequiredJavaVersion(PlexusConfiguration c) {
        return Optional.ofNullable(c.getChild("requiredJavaVersion")).map(PlexusConfiguration::getValue);
    }

    private Optional<String> extractRequiredMavenVersion(PlexusConfiguration c) {
        return Optional.ofNullable(c.getChild("requiredMavenVersion")).map(PlexusConfiguration::getValue);
    }

    private List<ComponentDependency> extractComponentDependencies(PlexusConfiguration c) {

        PlexusConfiguration[] dependencyConfigurations =
                c.getChild("dependencies").getChildren("dependency");

        List<ComponentDependency> dependencies = new ArrayList<>();

        for (PlexusConfiguration d : dependencyConfigurations) {
            dependencies.add(extractComponentDependency(d));
        }
        return dependencies;
    }

    private ComponentDependency extractComponentDependency(PlexusConfiguration d) {
        ComponentDependency cd = new ComponentDependency();

        cd.setArtifactId(extractArtifactId(d));

        cd.setGroupId(extractGroupId(d));

        cd.setType(d.getChild("type").getValue());

        cd.setVersion(extractVersion(d));
        return cd;
    }

    @SuppressWarnings("checkstyle:methodlength")
    public MojoDescriptor buildComponentDescriptor(PlexusConfiguration c, PluginDescriptor pluginDescriptor)
            throws PlexusConfigurationException {
        MojoDescriptor mojo = new MojoDescriptor();
        mojo.setPluginDescriptor(pluginDescriptor);

        mojo.setGoal(c.getChild("goal").getValue());

        mojo.setImplementation(c.getChild("implementation").getValue());

        PlexusConfiguration langConfig = c.getChild("language");

        if (langConfig != null) {
            mojo.setLanguage(langConfig.getValue());
        }

        PlexusConfiguration configuratorConfig = c.getChild("configurator");

        if (configuratorConfig != null) {
            mojo.setComponentConfigurator(configuratorConfig.getValue());
        }

        PlexusConfiguration composerConfig = c.getChild("composer");

        if (composerConfig != null) {
            mojo.setComponentComposer(composerConfig.getValue());
        }

        String since = c.getChild("since").getValue();

        if (since != null) {
            mojo.setSince(since);
        }

        PlexusConfiguration deprecated = c.getChild("deprecated", false);

        if (deprecated != null) {
            mojo.setDeprecated(deprecated.getValue());
        }

        String phase = c.getChild("phase").getValue();

        if (phase != null) {
            mojo.setPhase(phase);
        }

        String executePhase = c.getChild("executePhase").getValue();

        if (executePhase != null) {
            mojo.setExecutePhase(executePhase);
        }

        String executeMojo = c.getChild("executeGoal").getValue();

        if (executeMojo != null) {
            mojo.setExecuteGoal(executeMojo);
        }

        String executeLifecycle = c.getChild("executeLifecycle").getValue();

        if (executeLifecycle != null) {
            mojo.setExecuteLifecycle(executeLifecycle);
        }

        mojo.setInstantiationStrategy(c.getChild("instantiationStrategy").getValue());

        mojo.setDescription(extractDescription(c));

        PlexusConfiguration dependencyResolution = c.getChild("requiresDependencyResolution", false);

        if (dependencyResolution != null) {
            mojo.setDependencyResolutionRequired(dependencyResolution.getValue());
        }

        PlexusConfiguration dependencyCollection = c.getChild("requiresDependencyCollection", false);

        if (dependencyCollection != null) {
            mojo.setDependencyCollectionRequired(dependencyCollection.getValue());
        }

        String directInvocationOnly = c.getChild("requiresDirectInvocation").getValue();

        if (directInvocationOnly != null) {
            mojo.setDirectInvocationOnly(Boolean.parseBoolean(directInvocationOnly));
        }

        String requiresProject = c.getChild("requiresProject").getValue();

        if (requiresProject != null) {
            mojo.setProjectRequired(Boolean.parseBoolean(requiresProject));
        }

        String requiresReports = c.getChild("requiresReports").getValue();

        if (requiresReports != null) {
            mojo.setRequiresReports(Boolean.parseBoolean(requiresReports));
        }

        String aggregator = c.getChild("aggregator").getValue();

        if (aggregator != null) {
            mojo.setAggregator(Boolean.parseBoolean(aggregator));
        }

        String requiresOnline = c.getChild("requiresOnline").getValue();

        if (requiresOnline != null) {
            mojo.setOnlineRequired(Boolean.parseBoolean(requiresOnline));
        }

        String inheritedByDefault = c.getChild("inheritedByDefault").getValue();

        if (inheritedByDefault != null) {
            mojo.setInheritedByDefault(Boolean.parseBoolean(inheritedByDefault));
        }

        String threadSafe = c.getChild("threadSafe").getValue();

        if (threadSafe != null) {
            mojo.setThreadSafe(Boolean.parseBoolean(threadSafe));
        }

        String v4Api = c.getChild("v4Api").getValue();

        if (v4Api != null) {
            mojo.setV4Api(Boolean.parseBoolean(v4Api));
        }

        // ----------------------------------------------------------------------
        // Configuration
        // ----------------------------------------------------------------------

        PlexusConfiguration mojoConfig = c.getChild("configuration");
        mojo.setMojoConfiguration(mojoConfig);

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        PlexusConfiguration[] parameterConfigurations = c.getChild("parameters").getChildren("parameter");

        List<Parameter> parameters = new ArrayList<>();

        for (PlexusConfiguration d : parameterConfigurations) {
            Parameter parameter = new Parameter();

            parameter.setName(extractName(d));

            parameter.setAlias(d.getChild("alias").getValue());

            parameter.setType(d.getChild("type").getValue());

            String required = d.getChild("required").getValue();

            parameter.setRequired(Boolean.parseBoolean(required));

            PlexusConfiguration editableConfig = d.getChild("editable");

            // we need the null check for pre-build legacy plugins...
            if (editableConfig != null) {
                String editable = d.getChild("editable").getValue();

                parameter.setEditable(editable == null || Boolean.parseBoolean(editable));
            }

            parameter.setDescription(extractDescription(d));

            parameter.setDeprecated(d.getChild("deprecated").getValue());

            parameter.setImplementation(d.getChild("implementation").getValue());

            parameter.setSince(d.getChild("since").getValue());

            PlexusConfiguration paramConfig = mojoConfig.getChild(parameter.getName(), false);
            if (paramConfig != null) {
                parameter.setExpression(paramConfig.getValue(null));
                parameter.setDefaultValue(paramConfig.getAttribute("default-value"));
            }

            parameters.add(parameter);
        }

        mojo.setParameters(parameters);

        // TODO this should not need to be handed off...

        // ----------------------------------------------------------------------
        // Requirements
        // ----------------------------------------------------------------------

        PlexusConfiguration[] requirements = c.getChild("requirements").getChildren("requirement");

        for (PlexusConfiguration requirement : requirements) {
            ComponentRequirement cr = new ComponentRequirement();

            cr.setRole(requirement.getChild("role").getValue());

            cr.setRoleHint(requirement.getChild("role-hint").getValue());

            cr.setFieldName(requirement.getChild("field-name").getValue());

            mojo.addRequirement(cr);
        }

        return mojo;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public PlexusConfiguration buildConfiguration(Reader configuration) throws PlexusConfigurationException {
        try {
            XMLStreamReader reader = WstxInputFactory.newFactory().createXMLStreamReader(configuration);
            return XmlPlexusConfiguration.toPlexusConfiguration(XmlNodeBuilder.build(reader, true, null));
        } catch (XMLStreamException e) {
            throw new PlexusConfigurationException(e.getMessage(), e);
        }
    }

    public PlexusConfiguration buildConfiguration(InputStream configuration) throws PlexusConfigurationException {
        try {
            XMLStreamReader reader = WstxInputFactory.newFactory().createXMLStreamReader(configuration);
            return XmlPlexusConfiguration.toPlexusConfiguration(XmlNodeBuilder.build(reader, true, null));
        } catch (XMLStreamException e) {
            throw new PlexusConfigurationException(e.getMessage(), e);
        }
    }
}
