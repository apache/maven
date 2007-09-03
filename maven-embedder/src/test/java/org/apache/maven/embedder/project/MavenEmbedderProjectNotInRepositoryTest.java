package org.apache.maven.embedder.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.embedder.AbstractEmbedderTestCase;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * We want to make sure when projects are newly created and have dependencies between them that
 * projects will still resolve and allow good IDE integration. Currently if you have newly
 * created projects that have interdependencies and you have not run "mvn install" then
 * none of the dependencies will be resolved for projects with interdependencies.
 *
 * @author Jason van Zyl
 */
public class MavenEmbedderProjectNotInRepositoryTest
    extends AbstractEmbedderTestCase
{
    public void testThatNewlyCreatedProjectsWithInterdependenciesStillResolveCorrectly()
        throws Exception
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Configuration configuration = new DefaultConfiguration()
            .setClassLoader( classLoader )
            .setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );

        ConfigurationValidationResult cr = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( cr.isValid() );

        MavenEmbedder embedder = new MavenEmbedder( configuration );

        File pom = new File( getBasedir(), "src/test/projects/no-artifact-in-repository-test" );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBaseDirectory( pom );

        MavenExecutionResult result = embedder.readProjectWithDependencies( request );        

        List projects = result.getTopologicallySortedProjects();

        MavenProject project;

        project = (MavenProject) projects.get( 0 );

        assertEquals( "child-1", project.getArtifactId() );

        project = (MavenProject) projects.get( 1 );

        assertEquals( "child-2", project.getArtifactId() );

        List deps = project.getDependencies();

        assertEquals( 2, deps.size() );

        project = (MavenProject) projects.get( 2 );

        assertEquals( "parent", project.getArtifactId() );        
    }
}