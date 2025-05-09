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
package org.apache.maven.project.inheritance.t10;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void dependencyManagementOverridesTransitiveDependencyVersion() throws Exception {
        File localRepo = getLocalRepositoryPath();

        File pom0 = new File(localRepo, "p0/pom.xml");
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File(pom0Basedir, "p1/pom.xml");

        // load the child project, which inherits from p0...
        MavenProject project0 = getProjectWithDependencies(pom0);
        MavenProject project1 = getProjectWithDependencies(pom1);

        assertThat(project1.getParent().getBasedir()).isEqualTo(pom0Basedir);
        System.out.println("Project " + project1.getId() + " " + project1);
        Map map = project1.getArtifactMap();
        assertThat(map).as("No artifacts").isNotNull();
        assertThat(map.size() > 0).as("No Artifacts").isTrue();
        assertThat(map.size()).as("Set size should be 3, is " + map.size()).isEqualTo(3);

        Artifact a = (Artifact) map.get("maven-test:t10-a");
        Artifact b = (Artifact) map.get("maven-test:t10-b");
        Artifact c = (Artifact) map.get("maven-test:t10-c");

        assertThat(a).isNotNull();
        assertThat(b).isNotNull();
        assertThat(c).isNotNull();

        // inherited from depMgmt
        System.out.println(a.getScope());
        assertThat(a.getScope()).as("Incorrect scope for " + a.getDependencyConflictId()).isEqualTo("test");

        // transitive dep, overridden b depMgmt
        assertThat(b.getScope()).as("Incorrect scope for " + b.getDependencyConflictId()).isEqualTo("runtime");

        // direct dep, overrides depMgmt
        assertThat(c.getScope()).as("Incorrect scope for " + c.getDependencyConflictId()).isEqualTo("runtime");
    }
}
