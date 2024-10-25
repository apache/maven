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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void testBuildReader() throws Exception {
        PluginDescriptor pd = build("/plugin.xml");

        assertEquals("org.apache.maven.plugins", pd.getGroupId());
        assertEquals("maven-jar-plugin", pd.getArtifactId());
        assertEquals("2.3-SNAPSHOT", pd.getVersion());
        assertEquals("jar", pd.getGoalPrefix());
        assertEquals("plugin-description", pd.getDescription());
        assertFalse(pd.isIsolatedRealm());
        assertTrue(pd.isInheritedByDefault());
        assertEquals(2, pd.getMojos().size());
        assertEquals(1, pd.getDependencies().size());

        MojoDescriptor md = pd.getMojos().get(0);

        assertEquals("jar", md.getGoal());
        assertEquals("mojo-description", md.getDescription());
        assertEquals("runtime", md.getDependencyResolutionRequired());
        assertEquals("test", md.getDependencyCollectionRequired());
        assertFalse(md.isAggregator());
        assertFalse(md.isDirectInvocationOnly());
        assertTrue(md.isInheritedByDefault());
        assertFalse(md.isOnlineRequired());
        assertTrue(md.isProjectRequired());
        assertFalse(md.isThreadSafe());
        assertEquals("package", md.getPhase());
        assertEquals("org.apache.maven.plugin.jar.JarMojo", md.getImplementation());
        assertEquals("antrun", md.getComponentConfigurator());
        assertEquals("java", md.getLanguage());
        assertEquals("per-lookup", md.getInstantiationStrategy());
        assertEquals("some-goal", md.getExecuteGoal());
        assertEquals("generate-sources", md.getExecutePhase());
        assertEquals("cobertura", md.getExecuteLifecycle());
        assertEquals("2.2", md.getSince());
        assertEquals("deprecated-mojo", md.getDeprecated());
        assertEquals(1, md.getRequirements().size());
        assertEquals(1, md.getParameters().size());

        assertNotNull(md.getMojoConfiguration());
        assertEquals(1, md.getMojoConfiguration().getChildCount());

        PlexusConfiguration pc = md.getMojoConfiguration().getChild(0);

        assertEquals("${jar.finalName}", pc.getValue());
        assertEquals("${project.build.finalName}", pc.getAttribute("default-value"));
        assertEquals("java.lang.String", pc.getAttribute("implementation"));

        Parameter mp = md.getParameters().get(0);

        assertEquals("finalName", mp.getName());
        assertEquals("jarName", mp.getAlias());
        assertEquals("java.lang.String", mp.getType());
        assertEquals("java.lang.String", mp.getImplementation());
        assertTrue(mp.isEditable());
        assertFalse(mp.isRequired());
        assertEquals("parameter-description", mp.getDescription());
        assertEquals("deprecated-parameter", mp.getDeprecated());
        assertEquals("${jar.finalName}", mp.getExpression());
        assertEquals("${project.build.finalName}", mp.getDefaultValue());
        assertEquals("3.0.0", mp.getSince());

        ComponentRequirement cr = md.getRequirements().get(0);

        assertEquals("org.codehaus.plexus.archiver.Archiver", cr.getRole());
        assertEquals("jar", cr.getRoleHint());
        assertEquals("jarArchiver", cr.getFieldName());

        ComponentDependency cd = pd.getDependencies().get(0);

        assertEquals("org.apache.maven", cd.getGroupId());
        assertEquals("maven-plugin-api", cd.getArtifactId());
        assertEquals("2.0.6", cd.getVersion());
        assertEquals("jar", cd.getType());

        md = pd.getMojos().get(1);

        assertEquals("war", md.getGoal());
        assertNull(md.getDependencyResolutionRequired());
        assertNull(md.getDependencyCollectionRequired());
        assertTrue(md.isThreadSafe());
    }
}
