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
package org.apache.maven.project.inheritance.t12scm;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;

/**
 * Verifies SCM inheritance uses modules statement from parent.
 *
 * @author jdcasey
 */
public class ProjectInheritanceTest extends AbstractProjectInheritanceTestCase {
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

    public void testScmInfoCalculatedCorrectlyOnParentAndChildRead() throws Exception {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File(localRepo, "p0/pom.xml");
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File(pom0Basedir, "modules/p1/pom.xml");

        // load the child project, which inherits from p0...
        MavenProject project0 = getProject(pom0);
        MavenProject project1 = getProject(pom1);

        System.out.println("\n\n");
        System.out.println("Parent SCM URL is: " + project0.getScm().getUrl());
        System.out.println("Child SCM URL is: " + project1.getScm().getUrl());
        System.out.println();
        System.out.println("Parent SCM connection is: " + project0.getScm().getConnection());
        System.out.println("Child SCM connection is: " + project1.getScm().getConnection());
        System.out.println();
        System.out.println(
                "Parent SCM developer connection is: " + project0.getScm().getDeveloperConnection());
        System.out.println(
                "Child SCM developer connection is: " + project1.getScm().getDeveloperConnection());

        assertEquals(project1.getScm().getUrl(), project0.getScm().getUrl() + "/modules/p1");
        assertEquals(project1.getScm().getConnection(), project0.getScm().getConnection() + "/modules/p1");
        assertEquals(
                project1.getScm().getDeveloperConnection(), project0.getScm().getDeveloperConnection() + "/modules/p1");
    }

    public void testScmInfoCalculatedCorrectlyOnChildOnlyRead() throws Exception {
        File localRepo = getLocalRepositoryPath();

        File pom1 = new File(localRepo, "p0/modules/p1/pom.xml");

        // load the child project, which inherits from p0...
        MavenProject project1 = getProject(pom1);

        System.out.println("\n\n");
        System.out.println("Child SCM URL is: " + project1.getScm().getUrl());
        System.out.println("Child SCM connection is: " + project1.getScm().getConnection());
        System.out.println(
                "Child SCM developer connection is: " + project1.getScm().getDeveloperConnection());

        assertEquals("http://host/viewer?path=/p0/modules/p1", project1.getScm().getUrl());
        assertEquals("scm:svn:http://host/p0/modules/p1", project1.getScm().getConnection());
        assertEquals("scm:svn:https://host/p0/modules/p1", project1.getScm().getDeveloperConnection());
    }

    //    public void testScmInfoCalculatedCorrectlyOnChildReadFromLocalRepository()
    //        throws Exception
    //    {
    //        File localRepo = getLocalRepositoryPath();
    //
    //        ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.class );
    //        Artifact artifact = factory.createProjectArtifact( "maven", "p1", "1.0" );
    //
    //        ArtifactRepositoryFactory repoFactory = (ArtifactRepositoryFactory) lookup(
    // ArtifactRepositoryFactory.class );
    //        ArtifactRepository localArtifactRepo = repoFactory.createLocalRepository( localRepo );
    //
    //        MavenProject project1 = getProjectBuilder().buildFromRepository( artifact, Collections.EMPTY_LIST,
    // localArtifactRepo );
    //
    //        System.out.println( "\n\n" );
    //        System.out.println( "Child SCM URL is: " + project1.getScm().getUrl() );
    //        System.out.println( "Child SCM connection is: " + project1.getScm().getConnection() );
    //        System.out.println( "Child SCM developer connection is: "
    //                            + project1.getScm().getDeveloperConnection() );
    //
    //        assertEquals( project1.getScm().getUrl(), "http://host/viewer?path=/p0/modules/p1" );
    //        assertEquals( project1.getScm().getConnection(), "scm:svn:http://host/p0/modules/p1" );
    //        assertEquals( project1.getScm().getDeveloperConnection(),
    //                      "scm:svn:https://host/p0/modules/p1" );
    //    }

}
