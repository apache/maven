package org.apache.maven.project.imports.t01;

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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.imports.AbstractProjectImportsTestCase;

import java.io.File;
import java.util.Map;


/**
 * Verifies managed dependencies are imported into other projects correctly.
 *
 * @version $Id$
 */
public class ProjectImportsTest extends AbstractProjectImportsTestCase
{
    // ----------------------------------------------------------------------
    //
    // p1 inherits from p0
    // p0 inhertis from super model
    //
    // or we can show it graphically as:
    //
    // p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    public void testDependencyManagementImportsVersions() throws Exception
    {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File( localRepo, "p0/pom.xml" );
        File pom0Basedir = pom0.getParentFile();
        System.out.println("basedir " + pom0Basedir.getAbsolutePath());
        File pom1 = new File( pom0Basedir, "p1/pom.xml" );
        File pom2 = new File( pom0Basedir, "p2/pom.xml" );
        File pom3 = new File( pom0Basedir, "p3/pom.xml" );
        File pom4 = new File( pom0Basedir, "p4/pom.xml" );

        getProjectWithDependencies( pom0 );
        // load the child project, which inherits from p0...
        // MavenProject project0 = getProjectWithDependencies( pom0 );
        MavenProject project1 = getProjectWithDependencies( pom1 );

        assertEquals( pom0Basedir, project1.getParent().getBasedir().getCanonicalFile() );

        Map map = project1.getArtifactMap();
        assertNotNull("No artifacts", map);
        assertTrue("No Artifacts", map.size() > 0);
        assertTrue("Set size should be 2, is " + map.size(), map.size() == 2);

        Artifact a = (Artifact) map.get("maven-test:maven-test-a");
        Artifact b = (Artifact) map.get("maven-test:maven-test-b");

        assertTrue("Incorrect version for " + a.getDependencyConflictId(), a.getVersion().equals("1.0"));
        assertTrue("Incorrect version for " + b.getDependencyConflictId(), b.getVersion().equals("1.0"));

                // load the child project, which inherits from p0...
        // MavenProject project0 = getProjectWithDependencies( pom0 );
        MavenProject project2 = getProjectWithDependencies( pom2 );

        map = project2.getArtifactMap();
        assertNotNull("No artifacts", map);
        assertTrue("No Artifacts", map.size() > 0);
        assertTrue("Set size should be 3, is " + map.size(), map.size() == 3);

        a = (Artifact) map.get("maven-test:maven-test-a");
        b = (Artifact) map.get("maven-test:maven-test-b");
        Artifact c = (Artifact) map.get("maven-test:maven-test-c");

        assertTrue("Incorrect version for " + a.getDependencyConflictId(), a.getVersion().equals("1.0"));
        assertTrue("Incorrect version for " + b.getDependencyConflictId(), b.getVersion().equals("1.0"));
        assertTrue("Incorrect version for " + c.getDependencyConflictId(), c.getVersion().equals("1.0"));

        // load the child project, which inherits from p0...
        // MavenProject project0 = getProjectWithDependencies( pom0 );
        MavenProject project3 = getProjectWithDependencies( pom3 );

        map = project3.getArtifactMap();
        assertNotNull("No artifacts", map);
        assertTrue("No Artifacts", map.size() > 0);
        assertTrue("Set size should be 3, is " + map.size(), map.size() == 3);

        a = (Artifact) map.get("maven-test:maven-test-a");
        c = (Artifact) map.get("maven-test:maven-test-c");
        Artifact d = (Artifact) map.get("maven-test:maven-test-d");

        assertTrue("Incorrect version for " + a.getDependencyConflictId(), a.getVersion().equals("1.1"));
        assertTrue("Incorrect version for " + c.getDependencyConflictId(), c.getVersion().equals("1.1"));
        assertTrue("Incorrect version for " + d.getDependencyConflictId(), d.getVersion().equals("1.0"));

        // load the child project, which inherits from p0...
        // MavenProject project0 = getProjectWithDependencies( pom0 );
        MavenProject project4 = getProjectWithDependencies( pom4 );

        map = project4.getArtifactMap();
        assertNotNull("No artifacts", map);
        assertTrue("No Artifacts", map.size() > 0);
        assertTrue("Set size should be 4, is " + map.size(), map.size() == 4);

        a = (Artifact) map.get("maven-test:maven-test-a");
        b = (Artifact) map.get("maven-test:maven-test-b");
        c = (Artifact) map.get("maven-test:maven-test-c");
        d = (Artifact) map.get("maven-test:maven-test-d");

        assertTrue("Incorrect version for " + a.getDependencyConflictId(), a.getVersion().equals("1.0"));
        assertTrue("Incorrect version for " + b.getDependencyConflictId(), b.getVersion().equals("1.1"));
        assertTrue("Incorrect version for " + c.getDependencyConflictId(), c.getVersion().equals("1.0"));
        assertTrue("Incorrect version for " + d.getDependencyConflictId(), d.getVersion().equals("1.0"));
    }
}