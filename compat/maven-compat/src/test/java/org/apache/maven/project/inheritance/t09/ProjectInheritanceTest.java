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
package org.apache.maven.project.inheritance.t09;

import java.io.File;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies exclusions listed in dependencyManagement are valid for
 * transitive dependencies.
 *
 */
@Deprecated
@SuppressWarnings("checkstyle:UnusedLocalVariable")
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

    /**
     * How the test project is set up:
     *
     * 1. dependencyManagement lists dependencies on a &amp; b,
     *    with an exclusion on c in b.
     * 2. the child project lists a dependency on project a only
     * 3. a depends on b (which is transitive to the child project),
     *    and b depends on c.
     *
     * We should see that the resulting size of collected artifacts is two:
     * a &amp; b only.
     */
    @Test
    void dependencyManagementExclusionsExcludeTransitively() throws Exception {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File(localRepo, "p0/pom.xml");
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File(pom0Basedir, "p1/pom.xml");

        // load the child project, which inherits from p0...
        MavenProject project0 = getProjectWithDependencies(pom0);
        MavenProject project1 = getProjectWithDependencies(pom1);

        assertThat(project1.getParent()).as("Parent is null").isNotNull();
        assertThat(project1.getParent().getBasedir()).isEqualTo(pom0Basedir);
        Map map = project1.getArtifactMap();

        assertThat(map).as("No artifacts").isNotNull();
        assertThat(map.size() > 0).as("No Artifacts").isTrue();
        assertThat(map.size()).as("Set size should be 2, is " + map.size()).isEqualTo(2);

        assertThat(map.containsKey("maven-test:t09-a")).as("maven-test:t09-a is not in the project").isTrue();
        assertThat(map.containsKey("maven-test:t09-b")).as("maven-test:t09-b is not in the project").isTrue();
        assertThat(map.containsKey("maven-test:t09-c")).as("maven-test:t09-c is in the project").isFalse();
    }

    /**
     * Setup exactly the same as the above test, except that the child project
     * now depends upon d, which has a transitive dependency on c.  Even though
     * we did list an exclusion on c, it was only from within the context of
     * project b.  We will pick up project c in this case because no
     * restrictions were placed on d.  This demonstrates that a, b, c, &amp; d will
     * all be collected.
     *
     * @throws Exception
     */
    @Test
    void dependencyManagementExclusionDoesNotOverrideGloballyForTransitives() throws Exception {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File(localRepo, "p0/pom.xml");
        File pom0Basedir = pom0.getParentFile();
        File pom2 = new File(pom0Basedir, "p2/pom.xml");

        // load the child project, which inherits from p0...
        MavenProject project0 = getProjectWithDependencies(pom0);
        MavenProject project2 = getProjectWithDependencies(pom2);

        assertThat(project2.getParent().getBasedir()).isEqualTo(pom0Basedir);
        Map map = project2.getArtifactMap();
        assertThat(map).as("No artifacts").isNotNull();
        assertThat(map.size() > 0).as("No Artifacts").isTrue();
        assertThat(map.size()).as("Set size should be 4, is " + map.size()).isEqualTo(4);

        assertThat(map.containsKey("maven-test:t09-a")).as("maven-test:t09-a is not in the project").isTrue();
        assertThat(map.containsKey("maven-test:t09-b")).as("maven-test:t09-b is not in the project").isTrue();
        assertThat(map.containsKey("maven-test:t09-c")).as("maven-test:t09-c is not in the project").isTrue();
        assertThat(map.containsKey("maven-test:t09-d")).as("maven-test:t09-d is not in the project").isTrue();
    }
}
