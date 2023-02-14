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
package org.apache.maven.plugin.testing;

import java.io.File;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;

public class ParametersMojoTest extends AbstractMojoTestCase {
    public void testDefault() throws Exception {
        MavenProject project = readMavenProject(new File("src/test/projects/default"));

        ParametersMojo mojo = (ParametersMojo) lookupConfiguredMojo(project, "parameters");

        assertNull(mojo.plain);
        assertNull(mojo.withProperty);
        assertEquals("default", mojo.withDefault);
        assertEquals("default", mojo.withPropertyAndDefault);
    }

    public void testExplicit() throws Exception {
        MavenProject project = readMavenProject(new File("src/test/projects/explicit"));

        ParametersMojo mojo = (ParametersMojo) lookupConfiguredMojo(project, "parameters");

        assertEquals("explicitValue", mojo.plain);
        assertEquals("explicitWithPropertyValue", mojo.withProperty);
        assertEquals("explicitWithDefaultValue", mojo.withDefault);
        assertEquals("explicitWithPropertyAndDefaultValue", mojo.withPropertyAndDefault);
    }

    public void testDefaultWithProperty() throws Exception {
        MavenProject project = readMavenProject(new File("src/test/projects/default"));
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution("parameters");

        session.getUserProperties().put("property", "propertyValue");
        ParametersMojo mojo = (ParametersMojo) lookupConfiguredMojo(session, execution);

        assertNull(mojo.plain);
        assertEquals("propertyValue", mojo.withProperty);
        assertEquals("default", mojo.withDefault);
        assertEquals("propertyValue", mojo.withPropertyAndDefault);
    }

    public void testExplicitWithProperty() throws Exception {
        MavenProject project = readMavenProject(new File("src/test/projects/explicit"));
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution("parameters");

        session.getUserProperties().put("property", "propertyValue");
        ParametersMojo mojo = (ParametersMojo) lookupConfiguredMojo(session, execution);

        assertEquals("explicitValue", mojo.plain);
        assertEquals("explicitWithPropertyValue", mojo.withProperty);
        assertEquals("explicitWithDefaultValue", mojo.withDefault);
        assertEquals("explicitWithPropertyAndDefaultValue", mojo.withPropertyAndDefault);
    }

    protected MavenProject readMavenProject(File basedir) throws ProjectBuildingException, Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());
        MavenProject project =
                lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        assertNotNull(project);
        return project;
    }
}
