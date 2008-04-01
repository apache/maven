package org.apache.maven.project.inheritance.t13;

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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;

import java.io.File;
import java.util.List;

/**
 * Verifies that plugin execution sections in the parent POM that have
 * inherit == false are not inherited to the child POM.
 */
public class ProjectInheritanceTest
    extends AbstractProjectInheritanceTestCase
{
    protected ArtifactRepository getLocalRepository()
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "default" );

        ArtifactRepository r = new DefaultArtifactRepository( "local",
                                                              "file://" + getLocalRepositoryPath().getAbsolutePath() + "/repo",
                                                              repoLayout );

        return r;
    }

    // ----------------------------------------------------------------------
    //
    // p1 inherits from p0
    // p0 inherits from super model
    //
    // or we can show it graphically as:
    //
    // p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    public void testChildDependenciesAddedAheadOfParentDependencies()
        throws Exception
    {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File( localRepo, "p0/pom.xml" );
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File( pom0Basedir, "p1/pom.xml" );

        getProjectWithDependencies( pom0 );
        MavenProject project1 = getProjectWithDependencies( pom1 );

        List dependencies = project1.getDependencies();

        assertNotNull( "Must contain dependencies.", dependencies );
        assertEquals( "Must contain 2 dependencies.", 2, dependencies.size() );

        Dependency dep1 = (Dependency) dependencies.get( 0 );
        assertEquals( "Child dependency should be listed first.", "test-from-child", dep1.getArtifactId() );
        assertEquals( "Child dependency should have version '1'.", "1", dep1.getVersion() );

        Dependency dep2 = (Dependency) dependencies.get( 1 );
        assertEquals( "Parent dependency should be listed last.", "test-from-parent", dep2.getArtifactId() );

        List compileArtifacts = project1.getCompileArtifacts();
        assertNotNull( "Must contain compile-scoped artifacts.", compileArtifacts );
        assertEquals( "Must contain 2 compile-scoped artifacts.", 2, compileArtifacts.size() );

        Artifact artifact1 = (Artifact) compileArtifacts.get( 0 );
        assertEquals( "Child dependency should be listed first in compile-scoped artifacts list.", "test-from-child", artifact1.getArtifactId() );

        Artifact artifact2 = (Artifact) compileArtifacts.get( 1 );
        assertEquals( "Parent dependency should be listed last in compile-scoped artifacts list.", "test-from-parent", artifact2.getArtifactId() );
    }
}