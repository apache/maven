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
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.util.ArrayList;
import java.util.Arrays;
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

    public void testBasicSingleProject()
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        MavenProject project = createProject( "group", "artifactA", "1.0" );

        List projects = new ProjectSorter( Collections.singletonList( project ) ).getSortedProjects();
        
        assertEquals( "Wrong number of projects: " + projects, 1, projects.size() );
        assertEquals( "Didn't match project", project, projects.get( 0 ) );
    }
    
    public void testBasicMultiProject()
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        MavenProject projectA = createProject( "group", "artifactA", "1.0" );
        MavenProject projectB = createProject( "group", "artifactB", "1.0" );
        MavenProject projectC = createProject( "group", "artifactC", "1.0" );
        
        projectA.getDependencies().add( createDependency( projectB ) );
        projectB.getDependencies().add( createDependency( projectC ) );
        
        List projects = Arrays.asList( new Object[] { projectA, projectB, projectC} );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( "Wrong number of projects: " + projects, 3, projects.size() );
        assertEquals( "Didn't match project", projectC, projects.get( 0 ) );
        assertEquals( "Didn't match project", projectB, projects.get( 1 ) );
        assertEquals( "Didn't match project", projectA, projects.get( 2 ) );
    }

    public void testResumeFrom()
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        MavenProject projectA = createProject( "group", "artifactA", "1.0" );
        MavenProject projectB = createProject( "group", "artifactB", "1.0" );
        MavenProject projectC = createProject( "group", "artifactC", "1.0" );

        projectA.getDependencies().add( createDependency( projectB ) );
        projectB.getDependencies().add( createDependency( projectC ) );

        List projects = Arrays.asList( new Object[] { projectA, projectB, projectC } );

        projects = new ProjectSorter( projects, null, "group:artifactB", false, false ).getSortedProjects();

        assertEquals( "Wrong number of projects: " + projects, 2, projects.size() );
        assertEquals( "Didn't match project", projectB, projects.get( 0 ) );
        assertEquals( "Didn't match project", projectA, projects.get( 1 ) );
    }

    public void testSelectedProjects()
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        MavenProject projectA = createProject( "group", "artifactA", "1.0" );
        MavenProject projectB = createProject( "group", "artifactB", "1.0" );
        MavenProject projectC = createProject( "group", "artifactC", "1.0" );

        projectA.getDependencies().add( createDependency( projectB ) );
        projectB.getDependencies().add( createDependency( projectC ) );

        List projects = Arrays.asList( new Object[] { projectA, projectB, projectC } );
        List selectedProjects = Arrays.asList( new Object[] { "group:artifactB" } );

        projects = new ProjectSorter( projects, selectedProjects, null, false, false ).getSortedProjects();

        assertEquals( "Wrong number of projects: " + projects, 1, projects.size() );
        assertEquals( "Didn't match project", projectB, projects.get( 0 ) );
    }

    public void testMake()
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        MavenProject projectA = createProject( "group", "artifactA", "1.0" );
        MavenProject projectB = createProject( "group", "artifactB", "1.0" );
        MavenProject projectC = createProject( "group", "artifactC", "1.0" );

        projectA.getDependencies().add( createDependency( projectB ) );
        projectB.getDependencies().add( createDependency( projectC ) );

        List projects = Arrays.asList( new Object[] { projectA, projectB, projectC } );
        List selectedProjects = Arrays.asList( new Object[] { "group:artifactB" } );

        projects = new ProjectSorter( projects, selectedProjects, null, true/* make */, false ).getSortedProjects();

        assertEquals( "Wrong number of projects: " + projects, 2, projects.size() );
        assertEquals( "Didn't match project", projectC, projects.get( 0 ) );
        assertEquals( "Didn't match project", projectB, projects.get( 1 ) );
    }

    public void testMakeDependents()
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        MavenProject projectA = createProject( "group", "artifactA", "1.0" );
        MavenProject projectB = createProject( "group", "artifactB", "1.0" );
        MavenProject projectC = createProject( "group", "artifactC", "1.0" );

        projectA.getDependencies().add( createDependency( projectB ) );
        projectB.getDependencies().add( createDependency( projectC ) );

        List projects = Arrays.asList( new Object[] { projectA, projectB, projectC } );
        List selectedProjects = Arrays.asList( new Object[] { "group:artifactB" } );

        projects = new ProjectSorter( projects, selectedProjects, null, false/* make */, true/*makeDependents*/ ).getSortedProjects();

        assertEquals( "Wrong number of projects: " + projects, 2, projects.size() );
        assertEquals( "Didn't match project", projectB, projects.get( 0 ) );
        assertEquals( "Didn't match project", projectA, projects.get( 1 ) );
    }

    public void testMakeBoth()
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        MavenProject projectA = createProject( "group", "artifactA", "1.0" );
        MavenProject projectB = createProject( "group", "artifactB", "1.0" );
        MavenProject projectC = createProject( "group", "artifactC", "1.0" );
        MavenProject projectD = createProject( "group", "artifactD", "1.0" );
        MavenProject projectE = createProject( "group", "artifactE", "1.0" );

        projectA.getDependencies().add( createDependency( projectB ) );
        projectB.getDependencies().add( createDependency( projectC ) );
        projectD.getDependencies().add( createDependency( projectE ) );
        projectE.getDependencies().add( createDependency( projectB ) );

        List projects = Arrays.asList( new Object[] { projectA, projectB, projectC, projectD, projectE } );
        List selectedProjects = Arrays.asList( new Object[] { "group:artifactE" } );

        projects =
            new ProjectSorter( projects, selectedProjects, null, true/* make */, true/* makeDependents */).getSortedProjects();

        assertEquals( "Wrong number of projects: " + projects, 4, projects.size() );
        assertEquals( "Didn't match project", projectC, projects.get( 0 ) );
        assertEquals( "Didn't match project", projectB, projects.get( 1 ) );
        assertEquals( "Didn't match project", projectE, projects.get( 2 ) );
        assertEquals( "Didn't match project", projectD, projects.get( 3 ) );
    }
    
    public void testShouldNotFailWhenProjectReferencesNonExistentProject()
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
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
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
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
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
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
        throws CycleDetectedException, MissingProjectException
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
        throws CycleDetectedException, MissingProjectException
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

    public void testPluginDependenciesInfluenceSorting()
        throws Exception {
      List projects = new ArrayList();

      MavenProject parentProject = createProject( "groupId", "parent", "1.0" );

      MavenProject project1 = createProject( "groupId", "artifactId1", "1.0" );
      project1.setParent(parentProject);
      projects.add( project1 );

      MavenProject project2 = createProject( "groupId", "artifactId2", "1.0" );
      project2.setParent(parentProject);
      projects.add( project2 );

      MavenProject pluginProject = createProject( "groupId", "pluginArtifact", "1.0" );
      pluginProject.setParent(parentProject);
      projects.add( pluginProject );

      Plugin plugin = new Plugin();
      plugin.setGroupId(pluginProject.getGroupId());
      plugin.setArtifactId(pluginProject.getArtifactId());
      plugin.setVersion(pluginProject.getVersion());

      plugin.addDependency( createDependency( project2 ) );

      Model model = project1.getModel();
      Build build = model.getBuild();

      if ( build == null )
      {
          build = new Build();
          model.setBuild( build );
      }

      build.addPlugin( plugin );

      projects = new ProjectSorter( projects ).getSortedProjects();

      assertEquals( project1, projects.get( 2 ) );
      assertTrue( projects.contains( project2 ) );
      assertTrue( projects.contains( pluginProject ) );
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
