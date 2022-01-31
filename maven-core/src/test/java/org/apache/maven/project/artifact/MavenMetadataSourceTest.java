package org.apache.maven.project.artifact;

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

import javax.inject.Inject;

import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.PlexusContainer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@PlexusTest
public class MavenMetadataSourceTest
{
    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    PlexusContainer container;

    @Test
    @Disabled
    public void testShouldNotCarryExclusionsOverFromDependencyToDependency()
        throws Exception
    {
        /*
        Dependency dep1 = new Dependency();
        dep1.setGroupId( "test" );
        dep1.setArtifactId( "test-artifact" );
        dep1.setVersion( "1" );
        dep1.setType( "jar" );

        Exclusion exc = new Exclusion();
        exc.setGroupId( "test" );
        exc.setArtifactId( "test-artifact3" );

        dep1.addExclusion( exc );

        Dependency dep2 = new Dependency();
        dep2.setGroupId( "test" );
        dep2.setArtifactId( "test-artifact2" );
        dep2.setVersion( "1" );
        dep2.setType( "jar" );

        List<Dependency> deps = new ArrayList<>();
        deps.add( dep1 );
        deps.add( dep2 );

        ArtifactFactory factory = container.lookup( ArtifactFactory.class );

        ArtifactFilter dependencyFilter = new ScopeArtifactFilter( Artifact.SCOPE_COMPILE );

        MavenProject project = new MavenProject( new Model() );

        Set<Artifact> result = project.createArtifacts( dependencyFilter );

        for (Iterator<Artifact> it = result.iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();

            if ( "test-artifact2".equals( artifact.getArtifactId() ) )
            {
                ArtifactFilter filter = artifact.getDependencyFilter();

                assertSame( dependencyFilter, filter );
            }
        }
        */
    }

    @Test
    @Disabled("TODO restore these if it makes sense")
    public void testShouldUseCompileScopeIfDependencyScopeEmpty()
        throws Exception
    {
        /*
        String groupId = "org.apache.maven";
        String artifactId = "maven-model";

        Dependency dep = new Dependency();

        dep.setGroupId( groupId );
        dep.setArtifactId( artifactId );
        dep.setVersion( "2.0-alpha-3" );

        Model model = new Model();

        model.addDependency( dep );

        MavenProject project = new MavenProject( model, repositorySystem );

        project.setArtifacts( project.createArtifacts( null ) );

        String key = ArtifactUtils.versionlessKey( groupId, artifactId );

        Map artifactMap = project.getArtifactMap();

        assertNotNull( artifactMap, "artifact-map should not be null." );
        assertEquals( 1, artifactMap.size(), "artifact-map should contain 1 element." );

        Artifact artifact = (Artifact) artifactMap.get( key );

        assertNotNull( artifact, "dependency artifact not found in map." );
        assertEquals( Artifact.SCOPE_COMPILE, artifact.getScope(), "dependency artifact has wrong scope." );

        //check for back-propagation of default scope.
        assertEquals( Artifact.SCOPE_COMPILE, dep.getScope(), "default scope NOT back-propagated to dependency." );
        */
    }

    @Test
    @Disabled
    public void testShouldUseInjectedTestScopeFromDependencyManagement()
        throws Exception
    {
        /*
        String groupId = "org.apache.maven";
        String artifactId = "maven-model";

        Dependency dep = new Dependency();

        dep.setGroupId( groupId );
        dep.setArtifactId( artifactId );
        dep.setVersion( "2.0-alpha-3" );

        Model model = new Model();

        model.addDependency( dep );

        Dependency mgd = new Dependency();
        mgd.setGroupId( groupId );
        mgd.setArtifactId( artifactId );
        mgd.setScope( Artifact.SCOPE_TEST );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( mgd );

        model.setDependencyManagement( depMgmt );

        MavenProject project = new MavenProject( model, repositorySystem );

        TestModelDefaultsInjector injector = new TestModelDefaultsInjector();

        injector.injectDefaults( model );

        project.setArtifacts( project.createArtifacts( null ) );

        String key = ArtifactUtils.versionlessKey( groupId, artifactId );

        Map artifactMap = project.getArtifactMap();

        assertNotNull( artifactMap, "artifact-map should not be null." );
        assertEquals( 1, artifactMap.size(), "artifact-map should contain 1 element." );

        Artifact artifact = (Artifact) artifactMap.get( key );

        assertNotNull( artifact, "dependency artifact not found in map." );
        assertEquals( "dependency artifact has wrong scope.", Artifact.SCOPE_TEST, artifact.getScope() );

        //check for back-propagation of default scope.
        assertEquals( "default scope NOT back-propagated to dependency.", Artifact.SCOPE_TEST, dep.getScope() );
        */
    }

}
