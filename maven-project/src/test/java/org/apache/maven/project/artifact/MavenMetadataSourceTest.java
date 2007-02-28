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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.codehaus.plexus.PlexusTestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MavenMetadataSourceTest
    extends PlexusTestCase
{
    
    public void testShouldNotCarryExclusionsOverFromDependencyToDependency()
        throws Exception
    {
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
        
        List deps = new ArrayList();
        deps.add( dep1 );
        deps.add( dep2 );
        
        ArtifactFactory factory = ( ArtifactFactory ) lookup( ArtifactFactory.ROLE );
        
        ArtifactFilter dependencyFilter = new ScopeArtifactFilter( Artifact.SCOPE_COMPILE );
        
        MavenProject project = new MavenProject( new Model() );
        
        Set result = MavenMetadataSource.createArtifacts( factory, deps, null, dependencyFilter, project );
        
        for ( Iterator it = result.iterator(); it.hasNext(); )
        {
            Artifact artifact = ( Artifact ) it.next();
            
            if ( "test-artifact2".equals( artifact.getArtifactId() ) )
            {
                ArtifactFilter filter = artifact.getDependencyFilter();
                
                assertSame( dependencyFilter, filter );
            }
        }
    }

    public void testShouldUseCompileScopeIfDependencyScopeEmpty()
        throws Exception
    {
        String groupId = "org.apache.maven";
        String artifactId = "maven-model";

        Dependency dep = new Dependency();

        dep.setGroupId( groupId );
        dep.setArtifactId( artifactId );
        dep.setVersion( "2.0-alpha-3" );

        Model model = new Model();

        model.addDependency( dep );

        MavenProject project = new MavenProject( model );

        ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        project.setArtifacts( project.createArtifacts( factory, null, null ) );

        String key = ArtifactUtils.versionlessKey( groupId, artifactId );

        Map artifactMap = project.getArtifactMap();

        assertNotNull( "artifact-map should not be null.", artifactMap );
        assertEquals( "artifact-map should contain 1 element.", 1, artifactMap.size() );

        Artifact artifact = (Artifact) artifactMap.get( key );

        assertNotNull( "dependency artifact not found in map.", artifact );
        assertEquals( "dependency artifact has wrong scope.", Artifact.SCOPE_COMPILE, artifact.getScope() );

        //check for back-propagation of default scope.
        assertEquals( "default scope NOT back-propagated to dependency.", Artifact.SCOPE_COMPILE, dep.getScope() );
    }

    public void testShouldUseInjectedTestScopeFromDependencyManagement()
        throws Exception
    {
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

        MavenProject project = new MavenProject( model );

        ModelDefaultsInjector injector = (ModelDefaultsInjector) lookup( ModelDefaultsInjector.ROLE );

        injector.injectDefaults( model );

        ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        project.setArtifacts( project.createArtifacts( factory, null, null ) );

        String key = ArtifactUtils.versionlessKey( groupId, artifactId );

        Map artifactMap = project.getArtifactMap();

        assertNotNull( "artifact-map should not be null.", artifactMap );
        assertEquals( "artifact-map should contain 1 element.", 1, artifactMap.size() );

        Artifact artifact = (Artifact) artifactMap.get( key );

        assertNotNull( "dependency artifact not found in map.", artifact );
        assertEquals( "dependency artifact has wrong scope.", Artifact.SCOPE_TEST, artifact.getScope() );

        //check for back-propagation of default scope.
        assertEquals( "default scope NOT back-propagated to dependency.", Artifact.SCOPE_TEST, dep.getScope() );
    }

}
