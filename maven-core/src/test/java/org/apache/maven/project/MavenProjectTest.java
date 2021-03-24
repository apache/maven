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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenProjectTest
    extends AbstractMavenProjectTestCase
{

    @Test
    public void testShouldInterpretChildPathAdjustmentBasedOnModulePaths()
        throws IOException
    {
        Model parentModel = new Model();
        parentModel.addModule( "../child" );

        MavenProject parentProject = new MavenProject( parentModel );

        Model childModel = new Model();
        childModel.setArtifactId( "artifact" );

        MavenProject childProject = new MavenProject( childModel );

        File childFile =
            new File( System.getProperty( "java.io.tmpdir" ), "maven-project-tests" + System.currentTimeMillis()
                + "/child/pom.xml" );

        childProject.setFile( childFile );

        String adjustment = parentProject.getModulePathAdjustment( childProject );

        assertNotNull( adjustment );

        assertEquals( "..", adjustment );
    }

    @Test
    public void testIdentityProtoInheritance()
    {
        Parent parent = new Parent();

        parent.setGroupId( "test-group" );
        parent.setVersion( "1000" );
        parent.setArtifactId( "test-artifact" );

        Model model = new Model();

        model.setParent( parent );
        model.setArtifactId( "real-artifact" );

        MavenProject project = new MavenProject( model );

        assertEquals( "test-group", project.getGroupId(), "groupId proto-inheritance failed." );
        assertEquals( "real-artifact", project.getArtifactId(), "artifactId is masked." );
        assertEquals( "1000", project.getVersion(), "version proto-inheritance failed." );

        // draw the NPE.
        project.getId();
    }

    @Test
    public void testEmptyConstructor()
    {
        MavenProject project = new MavenProject();

        assertEquals( MavenProject.EMPTY_PROJECT_GROUP_ID + ":" + MavenProject.EMPTY_PROJECT_ARTIFACT_ID + ":jar:"
                        + MavenProject.EMPTY_PROJECT_VERSION, project.getId() );
    }

    @Test
    public void testClone()
        throws Exception
    {
        File f = getFileForClasspathResource( "canonical-pom.xml" );
        MavenProject projectToClone = getProject( f );

        MavenProject clonedProject = projectToClone.clone();
        assertEquals( "maven-core", clonedProject.getArtifactId() );
        Map<?, ?> clonedMap = clonedProject.getManagedVersionMap();
        assertNotNull( clonedMap, "ManagedVersionMap not copied" );
        assertTrue( clonedMap.isEmpty(), "ManagedVersionMap is not empty" );
    }

    @Test
    public void testCloneWithDependencyManagement()
        throws Exception
    {
        File f = getFileForClasspathResource( "dependencyManagement-pom.xml" );
        MavenProject projectToClone = getProjectWithDependencies( f );
        DependencyManagement dep = projectToClone.getDependencyManagement();
        assertNotNull( dep, "No dependencyManagement" );
        List<?> list = dep.getDependencies();
        assertNotNull( list, "No dependencies" );
        assertTrue( !list.isEmpty(), "Empty dependency list" );

        Map<?, ?> map = projectToClone.getManagedVersionMap();
        assertNotNull( map, "No ManagedVersionMap" );
        assertTrue( !map.isEmpty(), "ManagedVersionMap is empty" );

        MavenProject clonedProject = projectToClone.clone();
        assertEquals( "maven-core", clonedProject.getArtifactId() );
        Map<?, ?> clonedMap = clonedProject.getManagedVersionMap();
        assertNotNull( clonedMap, "ManagedVersionMap not copied" );
        assertTrue( !clonedMap.isEmpty(), "ManagedVersionMap is empty" );
        assertTrue( clonedMap.containsKey( "maven-test:maven-test-b:jar" ), "ManagedVersionMap does not contain test key" );
    }

    @Test
    public void testGetModulePathAdjustment()
        throws IOException
    {
        Model moduleModel = new Model();

        MavenProject module = new MavenProject( moduleModel );
        module.setFile( new File( "module-dir/pom.xml" ) );

        Model parentModel = new Model();
        parentModel.addModule( "../module-dir" );

        MavenProject parent = new MavenProject( parentModel );
        parent.setFile( new File( "parent-dir/pom.xml" ) );

        String pathAdjustment = parent.getModulePathAdjustment( module );

        assertEquals( "..", pathAdjustment );
    }

    @Test
    public void testCloneWithDistributionManagement()
        throws Exception
    {

        File f = getFileForClasspathResource( "distributionManagement-pom.xml" );
        MavenProject projectToClone = getProject( f );

        MavenProject clonedProject = projectToClone.clone();
        assertNotNull( clonedProject.getDistributionManagementArtifactRepository(), "clonedProject - distributionManagement" );
    }

    @Test
    public void testCloneWithActiveProfile()
        throws Exception
    {

        File f = getFileForClasspathResource( "withActiveByDefaultProfile-pom.xml" );
        MavenProject projectToClone = getProject( f );
        List<Profile> activeProfilesOrig = projectToClone.getActiveProfiles();

        assertEquals( 1, activeProfilesOrig.size(), "Expecting 1 active profile" );

        MavenProject clonedProject = projectToClone.clone();

        List<Profile> activeProfilesClone = clonedProject.getActiveProfiles();

        assertEquals( 1, activeProfilesClone.size(), "Expecting 1 active profile" );

        assertNotSame( activeProfilesOrig, activeProfilesClone,
                      "The list of active profiles should have been cloned too but is same" );
    }

    @Test
    public void testCloneWithBaseDir()
        throws Exception
    {
        File f = getFileForClasspathResource( "canonical-pom.xml" );
        MavenProject projectToClone = getProject( f );
        projectToClone.setPomFile( new File( new File( f.getParentFile(), "target" ), "flattened.xml" ) );
        MavenProject clonedProject = projectToClone.clone();
        assertEquals( projectToClone.getFile(), clonedProject.getFile(), "POM file is preserved across clone" );
        assertEquals( projectToClone.getBasedir(), clonedProject.getBasedir(), "Base directory is preserved across clone" );
    }

    @Test
    public void testUndefinedOutputDirectory()
        throws Exception
    {
        MavenProject p = new MavenProject();
        assertNoNulls( p.getCompileClasspathElements() );
        assertNoNulls( p.getSystemClasspathElements() );
        assertNoNulls( p.getRuntimeClasspathElements() );
        assertNoNulls( p.getTestClasspathElements() );
    }

    @Test
    public void testAddDotFile()
    {
        MavenProject project = new MavenProject();

        File basedir = new File( System.getProperty( "java.io.tmpdir" ) );
        project.setFile( new File( basedir, "file" ) );

        project.addCompileSourceRoot( basedir.getAbsolutePath() );
        project.addCompileSourceRoot( "." );

        assertEquals( 1, project.getCompileSourceRoots().size() );
    }

    private void assertNoNulls( List<String> elements )
    {
        assertFalse( elements.contains( null ) );
    }

}
