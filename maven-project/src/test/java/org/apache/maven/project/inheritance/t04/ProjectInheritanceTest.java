package org.apache.maven.project.inheritance.t04;

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
import java.util.Set;
import java.util.Iterator;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.apache.maven.artifact.Artifact;

/**
 * Verifies the version of a dependency listed in a parent's
 * dependencyManagement section is chosen over another version of the same
 * dependency, listed transitively.
 *
 * @author <a href="mailto:pschneider@gmail.com">Patrick Schneider</a>
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
    // p1 has a depMgmt section that specifies versions 1.0 of jars "a" & "b"
    // jar "a" has a transitive dependency on 2.0 of jar "b", but maven should
    // prefer to use version 1.0.
    //
    // ----------------------------------------------------------------------

    public void testDependencyManagementOverridesTransitiveDependencyVersion()
        throws Exception
    {
        File localRepo = getLocalRepositoryPath();
        File pom0 = new File( localRepo, "p0/pom.xml" );
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File( pom0Basedir, "p1/pom.xml" );

        // load the child project, which inherits from p0...
        MavenProject project0 = getProjectWithDependencies( pom0 );
        MavenProject project1 = getProjectWithDependencies( pom1 );

        assertEquals( pom0Basedir, project1.getParent().getBasedir().getCanonicalFile() );
        Set set = project1.getArtifacts();
        assertNotNull("No artifacts", set);
        assertTrue("No Artifacts", set.size() > 0);
        assertTrue("Set size should be 3, is " + set.size(), set.size() == 3);

        Iterator iter = set.iterator();
        while (iter.hasNext()) {
            Artifact artifact = (Artifact)iter.next();
            System.out.println("Artifact: " + artifact.getDependencyConflictId() + " " + artifact.getVersion() +
              " Optional=" + (artifact.isOptional() ? "true" : "false"));
            assertTrue("Incorrect version for " + artifact.getDependencyConflictId(), artifact.getVersion().equals("1.0"));
        }

    }
}