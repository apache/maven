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
package org.apache.maven.project.inheritance.t01;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test which demonstrates maven's recursive inheritance where
 * we are testing to make sure that elements stated in a model are
 * not clobbered by the same elements elsewhere in the lineage.
 *
 */
@Deprecated
@SuppressWarnings("checkstyle:UnusedLocalVariable")
class ProjectInheritanceTest extends AbstractProjectInheritanceTestCase {
    // ----------------------------------------------------------------------
    //
    // p4 inherits from p3
    // p3 inherits from p2
    // p2 inherits from p1
    // p1 inherits from p0
    // p0 inherits from super model
    //
    // or we can show it graphically as:
    //
    // p4 ---> p3 ---> p2 ---> p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    @Test
    void projectInheritance() throws Exception {
        // ----------------------------------------------------------------------
        // Check p0 value for org name
        // ----------------------------------------------------------------------

        MavenProject p0 = getProject(projectFile("maven.t01", "p0"));

        assertThat(p0.getOrganization().getName()).isEqualTo("p0-org");

        // ----------------------------------------------------------------------
        // Check p1 value for org name
        // ----------------------------------------------------------------------

        MavenProject p1 = getProject(projectFile("maven.t01", "p1"));

        assertThat(p1.getOrganization().getName()).isEqualTo("p1-org");

        // ----------------------------------------------------------------------
        // Check p2 value for org name
        // ----------------------------------------------------------------------

        MavenProject p2 = getProject(projectFile("maven.t01", "p2"));

        assertThat(p2.getOrganization().getName()).isEqualTo("p2-org");

        // ----------------------------------------------------------------------
        // Check p2 value for org name
        // ----------------------------------------------------------------------

        MavenProject p3 = getProject(projectFile("maven.t01", "p3"));

        assertThat(p3.getOrganization().getName()).isEqualTo("p3-org");

        // ----------------------------------------------------------------------
        // Check p4 value for org name
        // ----------------------------------------------------------------------

        MavenProject p4 = getProject(projectFile("maven.t01", "p4"));

        assertThat(p4.getOrganization().getName()).isEqualTo("p4-org");
    }
}
