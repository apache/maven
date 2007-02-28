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

import junit.framework.TestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test sorting projects by dependencies.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ProjectSorterTest
    extends TestCase
{
    
    public void testShouldNotFailWhenProjectReferencesNonExistentProject()
        throws CycleDetectedException, DuplicateProjectException
    {
        MavenProject project = createProject( "group", "artifact", "1.0" );
        Model model = project.getModel();
        
        Build build = model.getBuild();
        
        if ( build == null )
        {
            build = new Build();
            model.setBuild( build );
        }
        
        Extension extension = new Extension();
        
        extension.setArtifactId( "other-artifact" );
        extension.setGroupId( "other.group" );
        extension.setVersion( "1.0" );
        
        build.addExtension( extension );
        
        new ProjectSorter( Collections.singletonList( project ) );
    }
    
    public void testMatchingArtifactIdsDifferentGroupIds()
        throws CycleDetectedException, DuplicateProjectException
    {
        List projects = new ArrayList();
        MavenProject project1 = createProject( "groupId1", "artifactId", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId2", "artifactId", "1.0" );
        projects.add( project2 );
        project1.getDependencies().add( createDependency( project2 ) );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( project2, projects.get( 0 ) );
        assertEquals( project1, projects.get( 1 ) );
    }

    public void testMatchingGroupIdsDifferentArtifactIds()
        throws CycleDetectedException, DuplicateProjectException
    {
        List projects = new ArrayList();
        MavenProject project1 = createProject( "groupId", "artifactId1", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId", "artifactId2", "1.0" );
        projects.add( project2 );
        project1.getDependencies().add( createDependency( project2 ) );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( project2, projects.get( 0 ) );
        assertEquals( project1, projects.get( 1 ) );
    }

    public void testMatchingIdsAndVersions()
        throws CycleDetectedException
    {
        List projects = new ArrayList();
        MavenProject project1 = createProject( "groupId", "artifactId", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId", "artifactId", "1.0" );
        projects.add( project2 );

        try 
        {
            projects = new ProjectSorter( projects ).getSortedProjects();
            fail( "Duplicate projects should fail" );
        }
        catch ( DuplicateProjectException e )
        {
            // expected
            assertTrue( true );
        }
    }

    public void testMatchingIdsAndDifferentVersions()
        throws CycleDetectedException
    {
        List projects = new ArrayList();
        MavenProject project1 = createProject( "groupId", "artifactId", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId", "artifactId", "2.0" );
        projects.add( project2 );

        try 
        {
            projects = new ProjectSorter( projects ).getSortedProjects();
            fail( "Duplicate projects should fail" );
        }
        catch ( DuplicateProjectException e )
        {
            // expected
            assertTrue( true );
        }
    }

    private Dependency createDependency( MavenProject project )
    {
        Dependency depdendency = new Dependency();
        depdendency.setArtifactId( project.getArtifactId() );
        depdendency.setGroupId( project.getGroupId() );
        depdendency.setVersion( project.getVersion() );
        return depdendency;
    }

    private static MavenProject createProject( String groupId, String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return new MavenProject( model );
    }
}
