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
package org.apache.maven.project.inheritance.t11;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies scope of root project is preserved regardless of parent dependency management.
 *
 * @see <a href="https://issues.apache.org/jira/browse/MNG-2919">MNG-2919</a>
 */
@Deprecated
class ProjectInheritanceTest extends AbstractProjectInheritanceTestCase {
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
    void testDependencyManagementDoesNotOverrideScopeOfCurrentArtifact() throws Exception {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File(localRepo, "p0/pom.xml");
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File(pom0Basedir, "p1/pom.xml");

        // load the child project, which inherits from p0...
        MavenProject project0 = getProjectWithDependencies(pom0);
        MavenProject project1 = getProjectWithDependencies(pom1);

        assertEquals(pom0Basedir, project1.getParent().getBasedir());
        assertNull(
                project1.getArtifact().getScope(),
                "dependencyManagement has overwritten the scope of the currently building child project");
    }
}
