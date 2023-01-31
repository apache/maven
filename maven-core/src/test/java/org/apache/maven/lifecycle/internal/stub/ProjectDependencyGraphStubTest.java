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
package org.apache.maven.lifecycle.internal.stub;

import java.util.List;

import junit.framework.TestCase;
import org.apache.maven.project.MavenProject;

/**
 * Tests the stub. Yeah, I know.
 *
 * @author Kristian Rosenvold
 */
public class ProjectDependencyGraphStubTest extends TestCase {
    public void testADependencies() {
        ProjectDependencyGraphStub stub = new ProjectDependencyGraphStub();
        final List<MavenProject> mavenProjects = stub.getUpstreamProjects(ProjectDependencyGraphStub.A, false);
        assertEquals(0, mavenProjects.size());
    }

    public void testBDepenencies(ProjectDependencyGraphStub stub) {
        final List<MavenProject> bProjects = stub.getUpstreamProjects(ProjectDependencyGraphStub.B, false);
        assertEquals(1, bProjects.size());
        assertTrue(bProjects.contains(ProjectDependencyGraphStub.A));
    }

    public void testCDepenencies(ProjectDependencyGraphStub stub) {
        final List<MavenProject> cProjects = stub.getUpstreamProjects(ProjectDependencyGraphStub.C, false);
        assertEquals(1, cProjects.size());
        assertTrue(cProjects.contains(ProjectDependencyGraphStub.C));
    }

    public void testXDepenencies(ProjectDependencyGraphStub stub) {
        final List<MavenProject> cProjects = stub.getUpstreamProjects(ProjectDependencyGraphStub.X, false);
        assertEquals(2, cProjects.size());
        assertTrue(cProjects.contains(ProjectDependencyGraphStub.C));
        assertTrue(cProjects.contains(ProjectDependencyGraphStub.B));
    }
}
