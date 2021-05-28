package org.apache.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProjectDependenciesResolverTest
    extends AbstractCoreMavenComponentTestCase
{
    @Inject
    private ProjectDependenciesResolver resolver;

    protected String getProjectsDirectory()
    {
        return "src/test/projects/project-dependencies-resolver";
    }

    /*
    @Test
    public void testExclusionsInDependencies()
        throws Exception
    {
        MavenSession session = createMavenSession( null );
        MavenProject project = session.getCurrentProject();

        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( "org.apache.maven.its" );
        exclusion.setArtifactId( "a" );

        new ProjectBuilder( project ).addDependency( "org.apache.maven.its", "b", "0.1", Artifact.SCOPE_RUNTIME,
                                                     exclusion );

        Set<Artifact> artifactDependencies =
            resolver.resolve( project, Collections.singleton( Artifact.SCOPE_COMPILE ), session );
        assertEquals( 0, artifactDependencies.size() );

        artifactDependencies = resolver.resolve( project, Collections.singleton( Artifact.SCOPE_RUNTIME ), session );
        assertEquals( 1, artifactDependencies.size() );
        assertEquals( "b", artifactDependencies.iterator().next().getArtifactId() );
    }
    */

    @Test
    public void testSystemScopeDependencies()
        throws Exception
    {
        MavenSession session = createMavenSession( null );
        MavenProject project = session.getCurrentProject();

        new ProjectBuilder( project )
            .addDependency( "com.mycompany", "system-dependency", "1.0", Artifact.SCOPE_SYSTEM, new File( getBasedir(), "pom.xml" ).getAbsolutePath() );

        Set<Artifact> artifactDependencies =
            resolver.resolve( project, Collections.singleton( Artifact.SCOPE_COMPILE ), session );
        assertEquals( 1, artifactDependencies.size() );
    }

    @Test
    public void testSystemScopeDependencyIsPresentInTheCompileClasspathElements()
        throws Exception
    {
        File pom = getProject( "it0063" );

        Properties eps = new Properties();
        eps.setProperty( "jre.home", new File( pom.getParentFile(), "jdk/jre" ).getPath() );

        MavenSession session = createMavenSession( pom, eps );
        MavenProject project = session.getCurrentProject();

        project.setArtifacts( resolver.resolve( project, Collections.singleton( Artifact.SCOPE_COMPILE ), session ) );

        List<String> elements = project.getCompileClasspathElements();
        assertEquals( 2, elements.size() );

        @SuppressWarnings( "deprecation" )
        List<Artifact> artifacts = project.getCompileArtifacts();
        assertEquals( 1, artifacts.size() );
        assertThat( artifacts.get( 0 ).getFile().getName(), endsWith( "tools.jar" ) );
    }
}
