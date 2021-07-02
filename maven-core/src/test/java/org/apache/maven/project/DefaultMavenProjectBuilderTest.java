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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DefaultMavenProjectBuilderTest
    extends AbstractMavenProjectTestCase
{
    @TempDir
    File localRepoDir;

    // only use by reread()
    @TempDir
    Path projectRoot;

    @Override
    @BeforeEach
    public void setUp()
        throws Exception
    {
        projectBuilder = getContainer().lookup( ProjectBuilder.class );
    }

    protected MavenProject getProject( Artifact pom, boolean allowStub )
        throws Exception
    {
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setLocalRepository( getLocalRepository() );
        initRepoSession( configuration );

        return projectBuilder.build( pom, allowStub, configuration ).getProject();
    }

    /**
     * Check that we can build ok from the middle pom of a (parent,child,grandchild) hierarchy
     *
     * @throws Exception in case of issue
     */
    @Test
    public void testBuildFromMiddlePom() throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/grandchild-check/child/pom.xml");
        File f2 = getTestFile( "src/test/resources/projects/grandchild-check/child/grandchild/pom.xml");

        getProject( f1 );

        // it's the building of the grandchild project, having already cached the child project
        // (but not the parent project), which causes the problem.
        getProject( f2 );
    }

    @Disabled( "Maven 4 does not allow duplicate plugin declarations" )
    @Test
    public void testDuplicatePluginDefinitionsMerged()
        throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/duplicate-plugins-merged-pom.xml" );

        MavenProject project = getProject( f1 );
        assertEquals( 2, project.getBuildPlugins().get( 0 ).getDependencies().size() );
        assertEquals( 2, project.getBuildPlugins().get( 0 ).getExecutions().size() );
        assertEquals( "first", project.getBuildPlugins().get( 0 ).getExecutions().get( 0 ).getId() );
    }

    @Test
    public void testFutureModelVersion()
        throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/future-model-version-pom.xml" );

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject( f1 ),
                "Expected to fail for future versions" );
        assertThat(  e.getMessage(), containsString( "Building this project requires a newer version of Maven" ) );
    }

    @Test
    public void testPastModelVersion()
        throws Exception
    {
        // a Maven 1.x pom will not even
        // update the resource if we stop supporting modelVersion 4.0.0
        File f1 = getTestFile( "src/test/resources/projects/past-model-version-pom.xml" );

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject( f1 ),
                "Expected to fail for past versions" );
        assertThat( e.getMessage(), containsString( "Building this project requires an older version of Maven" ) );
    }

    @Test
    public void testFutureSchemaModelVersion()
        throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/future-schema-model-version-pom.xml" );

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject( f1 ),
                "Expected to fail for future versions" );
        assertThat( e.getMessage(), containsString( "Building this project requires a newer version of Maven" ) );
    }

    @Test
    public void testBuildStubModelForMissingRemotePom()
        throws Exception
    {
        Artifact pom = repositorySystem.createProjectArtifact( "org.apache.maven.its", "missing", "0.1" );
        MavenProject project = getProject( pom, true );

        assertNotNull( project.getArtifactId() );

        assertNotNull( project.getRemoteArtifactRepositories() );
        assertFalse( project.getRemoteArtifactRepositories().isEmpty() );

        assertNotNull( project.getPluginArtifactRepositories() );
        assertFalse( project.getPluginArtifactRepositories().isEmpty() );

        assertNull( project.getParent() );
        assertNull( project.getParentArtifact() );

        assertFalse( project.isExecutionRoot() );
    }

    @Override
    protected ArtifactRepository getLocalRepository()
        throws Exception
    {
        return repositorySystem.createLocalRepository( getLocalRepositoryPath() );
    }

    @Test
    public void testPartialResultUponBadDependencyDeclaration()
        throws Exception
    {
        File pomFile = getTestFile( "src/test/resources/projects/bad-dependency.xml" );

        ProjectBuildingRequest request = newBuildingRequest();
        request.setProcessPlugins( false );
        request.setResolveDependencies( true );
        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> projectBuilder.build( pomFile, request ),
                "Project building did not fail despite invalid POM" );
        List<ProjectBuildingResult> results = e.getResults();
        assertNotNull( results );
        assertEquals( 1, results.size() );
        ProjectBuildingResult result = results.get( 0 );
        assertNotNull( result );
        assertNotNull( result.getProject() );
        assertEquals( 1, result.getProblems().size() );
        assertEquals( 1, result.getProject().getArtifacts().size() );
        assertNotNull( result.getDependencyResolutionResult() );
    }

    public void testImportScopePomResolvesFromPropertyBasedRepository()
            throws Exception
    {
        File pomFile = getTestFile( "src/test/resources/projects/import-scope-pom-resolves-from-property-based-repository.xml" );
        ProjectBuildingRequest request = newBuildingRequest();
        request.setProcessPlugins( false );
        request.setResolveDependencies( true );
        projectBuilder.build( pomFile, request );
    }

    /**
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    public void testBuildValidParentVersionRangeLocally() throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/parent-version-range-local-valid/child/pom.xml" );

        final MavenProject childProject = getProject( f1 );

        assertNotNull( childProject.getParentArtifact() );
        assertEquals( childProject.getParentArtifact().getVersion(), "1" );
        assertNotNull( childProject.getParent() );
        assertEquals( childProject.getParent().getVersion(), "1" );
        assertNotNull( childProject.getModel().getParent() );
        assertEquals( childProject.getModel().getParent().getVersion(), "[1,10]" );
    }

    /**
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    public void testBuildParentVersionRangeLocallyWithoutChildVersion() throws Exception
    {
        File f1 =
            getTestFile( "src/test/resources/projects/parent-version-range-local-child-without-version/child/pom.xml" );

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject( f1 ),
                "Expected 'ProjectBuildingException' not thrown." );
        assertThat( e.getMessage(), containsString( "Version must be a constant" ) );
    }

    /**
     * Tests whether local version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    public void testBuildParentVersionRangeLocallyWithChildVersionExpression() throws Exception
    {
        File f1 =
            getTestFile(
                "src/test/resources/projects/parent-version-range-local-child-version-expression/child/pom.xml" );

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProject( f1 ),
                "Expected 'ProjectBuildingException' not thrown." );
        assertThat( e.getMessage(), containsString( "Version must be a constant" ) );
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    public void testBuildParentVersionRangeExternally() throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/parent-version-range-external-valid/pom.xml" );

        final MavenProject childProject = this.getProjectFromRemoteRepository( f1 );

        assertNotNull( childProject.getParentArtifact() );
        assertEquals( childProject.getParentArtifact().getVersion(), "1" );
        assertNotNull( childProject.getParent() );
        assertEquals( childProject.getParent().getVersion(), "1" );
        assertNotNull( childProject.getModel().getParent() );
        assertEquals( childProject.getModel().getParent().getVersion(), "[1,1]" );
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    public void testBuildParentVersionRangeExternallyWithoutChildVersion() throws Exception
    {
        File f1 =
            getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-without-version/pom.xml" );

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProjectFromRemoteRepository( f1 ),
                "Expected 'ProjectBuildingException' not thrown." );
        assertThat( e.getMessage(), containsString( "Version must be a constant" ) );
    }

    /**
     * Tests whether external version range parent references are build correctly.
     *
     * @throws Exception in case of issue
     */
    @Test
    public void testBuildParentVersionRangeExternallyWithChildVersionExpression() throws Exception
    {
        File f1 =
            getTestFile(
                "src/test/resources/projects/parent-version-range-external-child-version-expression/pom.xml" );

        ProjectBuildingException e = assertThrows(
                ProjectBuildingException.class,
                () -> getProjectFromRemoteRepository( f1 ),
                "Expected 'ProjectBuildingException' not thrown." );
        assertThat( e.getMessage(), containsString( "Version must be a constant" ) );
    }

    /**
     * Ensure that when re-reading a pom, it should not use the cached Model
     *
     * @throws Exception in case of issue
     */
    @Test
    public void rereadPom_mng7063() throws Exception
    {
        final Path pom = projectRoot.resolve( "pom.xml" );
        final ProjectBuildingRequest buildingRequest = newBuildingRequest();

        try ( InputStream pomResource =
            DefaultMavenProjectBuilderTest.class.getResourceAsStream( "/projects/reread/pom1.xml" ) )
        {
            Files.copy( pomResource, pom, StandardCopyOption.REPLACE_EXISTING );
        }

        MavenProject project = projectBuilder.build( pom.toFile(), buildingRequest ).getProject();
        assertThat( project.getName(), is( "aid" ) ); // inherited from artifactId

        try ( InputStream pomResource =
            DefaultMavenProjectBuilderTest.class.getResourceAsStream( "/projects/reread/pom2.xml" ) )
        {
            Files.copy( pomResource, pom, StandardCopyOption.REPLACE_EXISTING );
        }

        project = projectBuilder.build( pom.toFile(), buildingRequest ).getProject();
        assertThat( project.getName(), is( "PROJECT NAME" ) );
    }

}
