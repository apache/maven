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

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link PluginDescriptorBuilder}.
 *
 */
class PluginDescriptorBuilderTest {

    private PluginDescriptor build(String resource) throws IOException, PlexusConfigurationException {
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            return new PluginDescriptorBuilder().build(is, null);
        }
    }

    @Test
    void buildReader() throws Exception {
        PluginDescriptor pd = build("/plugin.xml");

        assertThat(pd.getGroupId()).isEqualTo("org.apache.maven.plugins");
        assertThat(pd.getArtifactId()).isEqualTo("maven-jar-plugin");
        assertThat(pd.getVersion()).isEqualTo("2.3-SNAPSHOT");
        assertThat(pd.getGoalPrefix()).isEqualTo("jar");
        assertThat(pd.getDescription()).isEqualTo("plugin-description");
        assertThat(pd.isIsolatedRealm()).isFalse();
        assertThat(pd.isInheritedByDefault()).isTrue();
        assertThat(pd.getMojos().size()).isEqualTo(2);
        assertThat(pd.getDependencies().size()).isEqualTo(1);

        MojoDescriptor md = pd.getMojos().get(0);

        assertThat(md.getGoal()).isEqualTo("jar");
        assertThat(md.getDescription()).isEqualTo("mojo-description");
        assertThat(md.getDependencyResolutionRequired()).isEqualTo("runtime");
        assertThat(md.getDependencyCollectionRequired()).isEqualTo("test");
        assertThat(md.isAggregator()).isFalse();
        assertThat(md.isDirectInvocationOnly()).isFalse();
        assertThat(md.isInheritedByDefault()).isTrue();
        assertThat(md.isOnlineRequired()).isFalse();
        assertThat(md.isProjectRequired()).isTrue();
        assertThat(md.isThreadSafe()).isFalse();
        assertThat(md.getPhase()).isEqualTo("package");
        assertThat(md.getImplementation()).isEqualTo("org.apache.maven.plugin.jar.JarMojo");
        assertThat(md.getComponentConfigurator()).isEqualTo("antrun");
        assertThat(md.getLanguage()).isEqualTo("java");
        assertThat(md.getInstantiationStrategy()).isEqualTo("per-lookup");
        assertThat(md.getExecuteGoal()).isEqualTo("some-goal");
        assertThat(md.getExecutePhase()).isEqualTo("generate-sources");
        assertThat(md.getExecuteLifecycle()).isEqualTo("cobertura");
        assertThat(md.getSince()).isEqualTo("2.2");
        assertThat(md.getDeprecated()).isEqualTo("deprecated-mojo");
        assertThat(md.getRequirements().size()).isEqualTo(1);
        assertThat(md.getParameters().size()).isEqualTo(1);

        assertThat(md.getMojoConfiguration()).isNotNull();
        assertThat(md.getMojoConfiguration().getChildCount()).isEqualTo(1);

        PlexusConfiguration pc = md.getMojoConfiguration().getChild(0);

        assertThat(pc.getValue()).isEqualTo("${jar.finalName}");
        assertThat(pc.getAttribute("default-value")).isEqualTo("${project.build.finalName}");
        assertThat(pc.getAttribute("implementation")).isEqualTo("java.lang.String");

        Parameter mp = md.getParameters().get(0);

        assertThat(mp.getName()).isEqualTo("finalName");
        assertThat(mp.getAlias()).isEqualTo("jarName");
        assertThat(mp.getType()).isEqualTo("java.lang.String");
        assertThat(mp.getImplementation()).isEqualTo("java.lang.String");
        assertThat(mp.isEditable()).isTrue();
        assertThat(mp.isRequired()).isFalse();
        assertThat(mp.getDescription()).isEqualTo("parameter-description");
        assertThat(mp.getDeprecated()).isEqualTo("deprecated-parameter");
        assertThat(mp.getExpression()).isEqualTo("${jar.finalName}");
        assertThat(mp.getDefaultValue()).isEqualTo("${project.build.finalName}");
        assertThat(mp.getSince()).isEqualTo("3.0.0");

        ComponentRequirement cr = md.getRequirements().get(0);

        assertThat(cr.getRole()).isEqualTo("org.codehaus.plexus.archiver.Archiver");
        assertThat(cr.getRoleHint()).isEqualTo("jar");
        assertThat(cr.getFieldName()).isEqualTo("jarArchiver");

        ComponentDependency cd = pd.getDependencies().get(0);

        assertThat(cd.getGroupId()).isEqualTo("org.apache.maven");
        assertThat(cd.getArtifactId()).isEqualTo("maven-plugin-api");
        assertThat(cd.getVersion()).isEqualTo("2.0.6");
        assertThat(cd.getType()).isEqualTo("jar");

        md = pd.getMojos().get(1);

        assertThat(md.getGoal()).isEqualTo("war");
        assertThat(md.getDependencyResolutionRequired()).isNull();
        assertThat(md.getDependencyCollectionRequired()).isNull();
        assertThat(md.isThreadSafe()).isTrue();
    }
}
