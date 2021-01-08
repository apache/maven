package org.apache.maven.project.inheritance.t10;

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
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies scope inheritance of direct and transitive dependencies.
 *
 * Should show three behaviors:
 *
 * 1. dependencyManagement should override the scope of transitive dependencies.
 * 2. Direct dependencies should override the scope of dependencyManagement.
 * 3. Direct dependencies should inherit scope from dependencyManagement when
 *    they do not explicitly state a scope.
 *
 * @author <a href="mailto:pschneider@gmail.com">Patrick Schneider</a>
 */
public class ProjectInheritanceTest
    extends AbstractProjectInheritanceTestCase
{
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

    @Test
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

        assertEquals( pom0Basedir, project1.getParent().getBasedir() );
        System.out.println("Project " + project1.getId() + " " + project1);
        Map map = project1.getArtifactMap();
        assertNotNull( map, "No artifacts" );
        assertTrue( map.size() > 0, "No Artifacts" );
        assertTrue( map.size() == 3, "Set size should be 3, is " + map.size() );

        Artifact a = (Artifact) map.get("maven-test:t10-a");
        Artifact b = (Artifact) map.get("maven-test:t10-b");
        Artifact c = (Artifact) map.get("maven-test:t10-c");

        assertNotNull( a );
        assertNotNull( b );
        assertNotNull( c );

        // inherited from depMgmt
        System.out.println(a.getScope());
        assertTrue( a.getScope().equals("test"), "Incorrect scope for " + a.getDependencyConflictId() );

        // transitive dep, overridden b depMgmt
        assertTrue( b.getScope().equals("runtime"), "Incorrect scope for " + b.getDependencyConflictId() );

        // direct dep, overrides depMgmt
        assertTrue( c.getScope().equals("runtime"), "Incorrect scope for " + c.getDependencyConflictId() );

    }
}
