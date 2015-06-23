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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Test sorting projects by dependencies.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ProjectSorterTest
    extends TestCase
{

    private Parent createParent( MavenProject project )
    {
        return createParent( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private Parent createParent( String groupId, String artifactId, String version )
    {
        Parent plugin = new Parent();
        plugin.setGroupId( groupId );
        plugin.setArtifactId( artifactId );
        plugin.setVersion( version );
        return plugin;
    }

    private Dependency createDependency( MavenProject project )
    {
        return createDependency( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private Dependency createDependency( String groupId, String artifactId, String version )
    {
        Dependency depdendency = new Dependency();
        depdendency.setGroupId( groupId );
        depdendency.setArtifactId( artifactId );
        depdendency.setVersion( version );
        return depdendency;
    }

    private Plugin createPlugin( MavenProject project )
    {
        return createPlugin( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private Plugin createPlugin( String groupId, String artifactId, String version )
    {
        Plugin plugin = new Plugin();
        plugin.setGroupId( groupId );
        plugin.setArtifactId( artifactId );
        plugin.setVersion( version );
        return plugin;
    }

    private Extension createExtension( String groupId, String artifactId, String version )
    {
        Extension extension = new Extension();
        extension.setGroupId( groupId );
        extension.setArtifactId( artifactId );
        extension.setVersion( version );
        return extension;
    }

    private static MavenProject createProject( String groupId, String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setBuild( new Build() );
        return new MavenProject( model );
    }

    public void testShouldNotFailWhenPluginDepReferencesCurrentProject()
        throws CycleDetectedException, DuplicateProjectException
    {
        MavenProject project = createProject( "group", "artifact", "1.0" );

        Build build = project.getModel().getBuild();

        Plugin plugin = createPlugin( "other.group", "other-artifact", "1.0" );

        Dependency dep = createDependency( "group", "artifact", "1.0" );

        plugin.addDependency( dep );

        build.addPlugin( plugin );

        new ProjectSorter( Collections.singletonList( project ) );
    }

    public void testShouldNotFailWhenManagedPluginDepReferencesCurrentProject()
        throws CycleDetectedException, DuplicateProjectException
    {
        MavenProject project = createProject( "group", "artifact", "1.0" );

        Build build = project.getModel().getBuild();

        PluginManagement pMgmt = new PluginManagement();

        Plugin plugin = createPlugin( "other.group", "other-artifact", "1.0" );

        Dependency dep = createDependency( "group", "artifact", "1.0" );

        plugin.addDependency( dep );

        pMgmt.addPlugin( plugin );

        build.setPluginManagement( pMgmt );

        new ProjectSorter( Collections.singletonList( project ) );
    }

    public void testShouldNotFailWhenProjectReferencesNonExistentProject()
        throws CycleDetectedException, DuplicateProjectException
    {
        MavenProject project = createProject( "group", "artifact", "1.0" );

        Build build = project.getModel().getBuild();

        Extension extension = createExtension( "other.group", "other-artifact", "1.0" );

        build.addExtension( extension );

        new ProjectSorter( Collections.singletonList( project ) );
    }

    public void testMatchingArtifactIdsDifferentGroupIds()
        throws CycleDetectedException, DuplicateProjectException
    {
        List<MavenProject> projects = new ArrayList<>();
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
        List<MavenProject> projects = new ArrayList<>();
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
        List<MavenProject> projects = new ArrayList<>();
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
        throws CycleDetectedException, DuplicateProjectException
    {
        List<MavenProject> projects = new ArrayList<>();
        MavenProject project1 = createProject( "groupId", "artifactId", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId", "artifactId", "2.0" );
        projects.add( project2 );

        projects = new ProjectSorter( projects ).getSortedProjects();
        assertEquals( project1, projects.get( 0 ) );
        assertEquals( project2, projects.get( 1 ) );
    }

    public void testPluginDependenciesInfluenceSorting()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject parentProject = createProject( "groupId", "parent", "1.0" );
        projects.add( parentProject );

        MavenProject declaringProject = createProject( "groupId", "declarer", "1.0" );
        declaringProject.setParent( parentProject );
        declaringProject.getModel().setParent( createParent( parentProject ) );
        projects.add( declaringProject );

        MavenProject pluginLevelDepProject = createProject( "groupId", "plugin-level-dep", "1.0" );
        pluginLevelDepProject.setParent( parentProject );
        pluginLevelDepProject.getModel().setParent( createParent( parentProject ) );
        projects.add( pluginLevelDepProject );

        MavenProject pluginProject = createProject( "groupId", "plugin", "1.0" );
        pluginProject.setParent( parentProject );
        pluginProject.getModel().setParent( createParent( parentProject ) );
        projects.add( pluginProject );

        Plugin plugin = createPlugin( pluginProject );

        plugin.addDependency( createDependency( pluginLevelDepProject ) );

        Build build = declaringProject.getModel().getBuild();

        build.addPlugin( plugin );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( parentProject, projects.get( 0 ) );

        // the order of these two is non-deterministic, based on when they're added to the reactor.
        assertTrue( projects.contains( pluginProject ) );
        assertTrue( projects.contains( pluginLevelDepProject ) );

        // the declaring project MUST be listed after the plugin and its plugin-level dep, though.
        assertEquals( declaringProject, projects.get( 3 ) );
    }

    public void testPluginDependenciesInfluenceSorting_DeclarationInParent()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject parentProject = createProject( "groupId", "parent-declarer", "1.0" );
        projects.add( parentProject );

        MavenProject pluginProject = createProject( "groupId", "plugin", "1.0" );
        pluginProject.setParent( parentProject );
        pluginProject.getModel().setParent( createParent( parentProject ) );
        projects.add( pluginProject );

        MavenProject pluginLevelDepProject = createProject( "groupId", "plugin-level-dep", "1.0" );
        pluginLevelDepProject.setParent( parentProject );
        pluginLevelDepProject.getModel().setParent( createParent( parentProject ) );
        projects.add( pluginLevelDepProject );

        Plugin plugin = createPlugin( pluginProject );

        plugin.addDependency( createDependency( pluginLevelDepProject ) );

        Build build = parentProject.getModel().getBuild();

        build.addPlugin( plugin );

        projects = new ProjectSorter( projects ).getSortedProjects();

        System.out.println( projects );

        assertEquals( parentProject, projects.get( 0 ) );

        // the order of these two is non-deterministic, based on when they're added to the reactor.
        assertTrue( projects.contains( pluginProject ) );
        assertTrue( projects.contains( pluginLevelDepProject ) );
    }

    public void testPluginVersionsAreConsidered()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject pluginProjectA = createProject( "group", "plugin-a", "2.0-SNAPSHOT" );
        projects.add( pluginProjectA );
        pluginProjectA.getModel().getBuild().addPlugin( createPlugin( "group", "plugin-b", "1.0" ) );

        MavenProject pluginProjectB = createProject( "group", "plugin-b", "2.0-SNAPSHOT" );
        projects.add( pluginProjectB );
        pluginProjectB.getModel().getBuild().addPlugin( createPlugin( "group", "plugin-a", "1.0" ) );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertTrue( projects.contains( pluginProjectA ) );
        assertTrue( projects.contains( pluginProjectB ) );
    }

    public void testDependencyPrecedesProjectThatUsesSpecificDependencyVersion()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject usingProject = createProject( "group", "project", "1.0" );
        projects.add( usingProject );
        usingProject.getModel().addDependency( createDependency( "group", "dependency", "1.0" ) );

        MavenProject pluginProject = createProject( "group", "dependency", "1.0" );
        projects.add( pluginProject );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( pluginProject, projects.get( 0 ) );
        assertEquals( usingProject, projects.get( 1 ) );
    }

    public void testDependencyPrecedesProjectThatUsesUnresolvedDependencyVersion()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject usingProject = createProject( "group", "project", "1.0" );
        projects.add( usingProject );
        usingProject.getModel().addDependency( createDependency( "group", "dependency", "[1.0,)" ) );

        MavenProject pluginProject = createProject( "group", "dependency", "1.0" );
        projects.add( pluginProject );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( pluginProject, projects.get( 0 ) );
        assertEquals( usingProject, projects.get( 1 ) );
    }

}
