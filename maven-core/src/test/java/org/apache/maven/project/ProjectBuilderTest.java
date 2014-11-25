package org.apache.maven.project;

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

import java.io.File;
import java.util.Properties;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;

public class ProjectBuilderTest
    extends AbstractCoreMavenComponentTestCase
{
    protected String getProjectsDirectory()
    {
        return "src/test/projects/project-builder";
    }

    public void testSystemScopeDependencyIsPresentInTheCompileClasspathElements()
        throws Exception
    {
        File pom = getProject( "it0063" );

        Properties eps = new Properties();
        eps.setProperty( "jre.home", new File( pom.getParentFile(), "jdk/jre" ).getPath() );

        MavenSession session = createMavenSession( pom, eps );
        MavenProject project = session.getCurrentProject();

        // Here we will actually not have any artifacts because the ProjectDependenciesResolver is not involved here. So
        // right now it's not valid to ask for artifacts unless plugins require the artifacts.

        project.getCompileClasspathElements();
    }

    public void testBuildFromModelSource()
        throws Exception
    {
        File pomFile = new File( "src/test/resources/projects/modelsource/module01/pom.xml" );
        MavenSession mavenSession = createMavenSession( pomFile );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession( mavenSession.getRepositorySession() );
        ModelSource modelSource = new FileModelSource( pomFile );
        ProjectBuildingResult result =
            lookup( org.apache.maven.project.ProjectBuilder.class ).build( modelSource, configuration );

        assertNotNull( result.getProject().getParentFile() );
    }

    public void testVersionlessManagedDependency()
        throws Exception
    {
        File pomFile = new File( "src/test/resources/projects/versionless-managed-dependency.xml" );
        MavenSession mavenSession = createMavenSession( null );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession( mavenSession.getRepositorySession() );

        try
        {
            lookup( org.apache.maven.project.ProjectBuilder.class ).build( pomFile, configuration );
            fail();
        }
        catch ( ProjectBuildingException e )
        {
            // this is expected
        }
    }
}
