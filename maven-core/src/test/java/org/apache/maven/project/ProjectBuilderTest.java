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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ProjectBuilderTest
    extends AbstractCoreMavenComponentTestCase
{
    @Override
    protected String getProjectsDirectory()
    {
        return "src/test/projects/project-builder";
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

        // Here we will actually not have any artifacts because the ProjectDependenciesResolver is not involved here. So
        // right now it's not valid to ask for artifacts unless plugins require the artifacts.

        project.getCompileClasspathElements();
    }

    @Test
    public void testBuildFromModelSource()
        throws Exception
    {
        File pomFile = new File( "src/test/resources/projects/modelsource/module01/pom.xml" );
        MavenSession mavenSession = createMavenSession( pomFile );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession( mavenSession.getRepositorySession() );
        ModelSource modelSource = new FileModelSource( pomFile );
        ProjectBuildingResult result =
            getContainer().lookup( org.apache.maven.project.ProjectBuilder.class ).build( modelSource, configuration );

        assertNotNull( result.getProject().getParentFile() );
    }

    @Test
    public void testVersionlessManagedDependency()
        throws Exception
    {
        File pomFile = new File( "src/test/resources/projects/versionless-managed-dependency.xml" );
        MavenSession mavenSession = createMavenSession( null );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession( mavenSession.getRepositorySession() );

        ProjectBuildingException e = assertThrows( ProjectBuildingException.class,
                      () -> getContainer().lookup( org.apache.maven.project.ProjectBuilder.class ).build( pomFile, configuration ) );
        assertThat( e.getMessage(),
                    containsString( "[ERROR] 'dependencies.dependency.version' for org.apache.maven.its:a:jar is missing. "
                        + "@ line 9, column 17" ) );
    }

    @Test
    public void testResolveDependencies()
        throws Exception
    {
        File pomFile = new File( "src/test/resources/projects/basic-resolveDependencies.xml" );
        MavenSession mavenSession = createMavenSession( null );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession( mavenSession.getRepositorySession() );
        configuration.setResolveDependencies( true );

        // single project build entry point
        ProjectBuildingResult result = getContainer().lookup( org.apache.maven.project.ProjectBuilder.class ).build( pomFile, configuration );
        assertEquals( 1, result.getProject().getArtifacts().size() );
        // multi projects build entry point
        List<ProjectBuildingResult> results =
                getContainer().lookup( org.apache.maven.project.ProjectBuilder.class ).build( Collections.singletonList( pomFile ), false,
                                                                           configuration );
        assertEquals( 1, results.size() );
        MavenProject mavenProject = results.get( 0 ).getProject();
        assertEquals( 1, mavenProject.getArtifacts().size() );
    }

    @Test
    public void testDontResolveDependencies()
        throws Exception
    {
        File pomFile = new File( "src/test/resources/projects/basic-resolveDependencies.xml" );
        MavenSession mavenSession = createMavenSession( null );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession( mavenSession.getRepositorySession() );
        configuration.setResolveDependencies( false );

        // single project build entry point
        ProjectBuildingResult result = getContainer().lookup( org.apache.maven.project.ProjectBuilder.class ).build( pomFile, configuration );
        assertEquals( 0, result.getProject().getArtifacts().size() );
        // multi projects build entry point
        List<ProjectBuildingResult> results = getContainer().lookup( org.apache.maven.project.ProjectBuilder.class ).build( Collections.singletonList( pomFile ), false, configuration );
        assertEquals( 1, results.size() );
        MavenProject mavenProject = results.get( 0 ).getProject();
        assertEquals( 0, mavenProject.getArtifacts().size() );
    }

    @Test
    public void testReadModifiedPoms() throws Exception {
        // TODO a similar test should be created to test the dependency management (basically all usages
        // of DefaultModelBuilder.getCache() are affected by MNG-6530

        Path tempDir = Files.createTempDirectory( null );
        FileUtils.copyDirectoryStructure ( new File( "src/test/resources/projects/grandchild-check" ), tempDir.toFile() );
        try
        {
            MavenSession mavenSession = createMavenSession( null );
            ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
            configuration.setRepositorySession( mavenSession.getRepositorySession() );
            org.apache.maven.project.ProjectBuilder projectBuilder = getContainer().lookup( org.apache.maven.project.ProjectBuilder.class );
            File child = new File( tempDir.toFile(), "child/pom.xml" );
            // build project once
            projectBuilder.build( child, configuration );
            // modify parent
            File parent = new File( tempDir.toFile(), "pom.xml" );
            String parentContent = FileUtils.fileRead( parent );
            parentContent = parentContent.replaceAll( "<packaging>pom</packaging>",
                     "<packaging>pom</packaging><properties><addedProperty>addedValue</addedProperty></properties>" );
            FileUtils.fileWrite( parent, "UTF-8", parentContent );
            // re-build pom with modified parent
            ProjectBuildingResult result = projectBuilder.build( child, configuration );
            assertThat( result.getProject().getProperties(), hasKey( (Object) "addedProperty" ) );
        }
        finally
        {
            FileUtils.deleteDirectory( tempDir.toFile() );
        }
    }

    @Test
    public void testReadErroneousMavenProjectContainsReference()
        throws Exception
    {
        File pomFile = new File( "src/test/resources/projects/artifactMissingVersion.xml" ).getAbsoluteFile();
        MavenSession mavenSession = createMavenSession( null );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        configuration.setRepositorySession( mavenSession.getRepositorySession() );
        org.apache.maven.project.ProjectBuilder projectBuilder =
                getContainer().lookup( org.apache.maven.project.ProjectBuilder.class );

        // single project build entry point
        ProjectBuildingException ex1 =
            assertThrows( ProjectBuildingException.class, () -> projectBuilder.build( pomFile, configuration ) );

        assertEquals( 1, ex1.getResults().size() );
        MavenProject project1 = ex1.getResults().get( 0 ).getProject();
        assertNotNull( project1 );
        assertEquals( "testArtifactMissingVersion", project1.getArtifactId() );
        assertEquals( pomFile, project1.getFile() );

        // multi projects build entry point
        ProjectBuildingException ex2 =
            assertThrows( ProjectBuildingException.class,
                          () -> projectBuilder.build( Collections.singletonList( pomFile ), false, configuration ) );

        assertEquals( 1, ex2.getResults().size() );
        MavenProject project2 = ex2.getResults().get( 0 ).getProject();
        assertNotNull( project2 );
        assertEquals( "testArtifactMissingVersion", project2.getArtifactId() );
        assertEquals( pomFile, project2.getFile() );
    }

    @Test
    public void testReadInvalidPom()
        throws Exception
    {
        File pomFile = new File( "src/test/resources/projects/badPom.xml" ).getAbsoluteFile();
        MavenSession mavenSession = createMavenSession( null );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        configuration.setRepositorySession( mavenSession.getRepositorySession() );
        org.apache.maven.project.ProjectBuilder projectBuilder =
                getContainer().lookup( org.apache.maven.project.ProjectBuilder.class );

        // single project build entry point
        Exception ex = assertThrows( Exception.class, () -> projectBuilder.build( pomFile, configuration ) );
        assertThat( ex.getMessage(), containsString( "expected START_TAG or END_TAG not TEXT" ) );

        // multi projects build entry point
        ProjectBuildingException pex =
            assertThrows( ProjectBuildingException.class,
                          () -> projectBuilder.build( Collections.singletonList( pomFile ), false, configuration ) );
        assertEquals( 1, pex.getResults().size() );
        assertNotNull( pex.getResults().get( 0 ).getPomFile() );
        assertThat( pex.getResults().get( 0 ).getProblems().size(), greaterThan( 0 ) );
        assertThat( pex.getMessage(), containsString( "expected START_TAG or END_TAG not TEXT" ) );
    }

    @Test
    public void testReadParentAndChildWithRegularVersionSetParentFile()
        throws Exception
    {
        List<File> toRead = new ArrayList<>( 2 );
        File parentPom = getProject( "MNG-6723" );
        toRead.add( parentPom );
        toRead.add( new File( parentPom.getParentFile(), "child/pom.xml" ) );
        MavenSession mavenSession = createMavenSession( null );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
        configuration.setRepositorySession( mavenSession.getRepositorySession() );
        org.apache.maven.project.ProjectBuilder projectBuilder =
                getContainer().lookup( org.apache.maven.project.ProjectBuilder.class );

        // read poms separately
        boolean parentFileWasFoundOnChild = false;
        for ( File file : toRead )
        {
            List<ProjectBuildingResult> results = projectBuilder.build( Collections.singletonList( file ), false, configuration );
            assertResultShowNoError( results );
            MavenProject project = findChildProject( results );
            if ( project != null )
            {
                assertEquals( parentPom, project.getParentFile() );
                parentFileWasFoundOnChild = true;
            }
        }
        assertTrue( parentFileWasFoundOnChild );

        // read projects together
        List<ProjectBuildingResult> results = projectBuilder.build( toRead, false, configuration );
        assertResultShowNoError( results );
        assertEquals( parentPom, findChildProject( results ).getParentFile() );
        Collections.reverse( toRead );
        results = projectBuilder.build( toRead, false, configuration );
        assertResultShowNoError( results );
        assertEquals( parentPom, findChildProject( results ).getParentFile() );
    }

    private MavenProject findChildProject( List<ProjectBuildingResult> results )
    {
        for ( ProjectBuildingResult result : results )
        {
            if ( result.getPomFile().getParentFile().getName().equals( "child" ) )
            {
                return result.getProject();
            }
        }
        return null;
    }

    private void assertResultShowNoError( List<ProjectBuildingResult> results )
    {
        for ( ProjectBuildingResult result : results )
        {
            assertThat( result.getProblems(), is( empty() ) );
            assertNotNull( result.getProject() );
        }
    }

    @Test
    public void testBuildProperties()
            throws Exception
    {
        File file = new File( getProject( "MNG-6716" ).getParentFile(), "project/pom.xml" );
        MavenSession mavenSession = createMavenSession( null );
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession( mavenSession.getRepositorySession() );
        configuration.setResolveDependencies( true );
        List<ProjectBuildingResult> result = projectBuilder.build( Collections.singletonList( file ), true, configuration );
        MavenProject project = result.get( 0 ).getProject();
        // verify a few typical parameters are not duplicated
        assertEquals( 1, project.getTestCompileSourceRoots().size() );
        assertEquals( 1, project.getCompileSourceRoots().size() );
        assertEquals( 1, project.getMailingLists().size() );
        assertEquals( 1, project.getResources().size() );
    }

    @Test
    public void testPropertyInPluginManagementGroupId()
            throws Exception
    {
        File pom = getProject( "MNG-6983" );

        MavenSession session = createMavenSession( pom );
        MavenProject project = session.getCurrentProject();

        for (Plugin buildPlugin : project.getBuildPlugins()) {
            assertNotNull( "Missing version for build plugin " + buildPlugin.getKey(), buildPlugin.getVersion() );
        }
    }
}
