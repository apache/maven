package org.apache.maven.project.inheritance.t07;

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
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

/**
 * A test which demonstrates maven's dependency management
 *
 * @author Jason van Zyl
 * @version $Id$
 */
public class ProjectInheritanceTest
    extends AbstractProjectInheritanceTestCase
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

    public void testDependencyManagement()
        throws Exception
    {
        File localRepo = getLocalRepositoryPath();
        File pom0 = new File( localRepo, "p0/pom.xml" );

        File pom0Basedir = pom0.getParentFile();

        File pom1 = new File( pom0Basedir, "p1/pom.xml" );

        // load everything...
        MavenProject project0 = getProjectWithDependencies( pom0 );
        MavenProject project1 = getProjectWithDependencies( pom1 );

        assertEquals( pom0Basedir, project1.getParent().getBasedir().getCanonicalFile() );
        System.out.println("Project " + project1.getId() + " " + project1);
        Set set = project1.getArtifacts();
        assertNotNull("No artifacts", set);
        assertTrue("No Artifacts", set.size() > 0);
        Iterator iter = set.iterator();
        assertTrue("Set size should be 3, is " + set.size(), set.size() == 3);

        while (iter.hasNext())
        {
            Artifact artifact = (Artifact)iter.next();
            assertFalse("", artifact.getArtifactId().equals("maven-test-d"));
            System.out.println("Artifact: " + artifact.getDependencyConflictId() + " " + artifact.getVersion() +
              " Optional=" + (artifact.isOptional() ? "true" : "false"));
            assertTrue("Incorrect version for " + artifact.getDependencyConflictId(), artifact.getVersion().equals("1.0"));
        }

    }
}
