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
package org.apache.maven.project.inheritance.t07;

import java.io.File;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test which demonstrates maven's dependency management
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
    void dependencyManagement() throws Exception {
        File localRepo = getLocalRepositoryPath();
        File pom0 = new File(localRepo, "p0/pom.xml");

        File pom0Basedir = pom0.getParentFile();

        File pom1 = new File(pom0Basedir, "p1/pom.xml");

        // load everything...
        MavenProject project1 = getProjectWithDependencies(pom1);

        assertThat(project1.getParent().getBasedir()).isEqualTo(pom0Basedir);
        System.out.println("Project " + project1.getId() + " " + project1);
        Set set = project1.getArtifacts();
        assertThat(set).as("No artifacts").isNotNull();
        assertThat(set.size() > 0).as("No Artifacts").isTrue();
        assertThat(set.size()).as("Set size should be 3, is " + set.size()).isEqualTo(3);

        for (Object aSet : set) {
            Artifact artifact = (Artifact) aSet;
            assertThat(artifact.getArtifactId()).isNotEqualTo("t07-d");
            System.out.println("Artifact: " + artifact.getDependencyConflictId() + " " + artifact.getVersion()
                    + " Optional=" + (artifact.isOptional() ? "true" : "false"));
            assertThat(artifact.getVersion()).as("Incorrect version for " + artifact.getDependencyConflictId()).isEqualTo("1.0");
        }
    }
}
