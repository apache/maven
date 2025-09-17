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
package org.apache.maven.lifecycle.internal;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
class ProjectBuildListTest {
    @Test
    void testGetByTaskSegment() throws Exception {
        final MavenSession session = ProjectDependencyGraphStub.getMavenSession();
        ProjectBuildList projectBuildList = ProjectDependencyGraphStub.getProjectBuildList(session);
        TaskSegment taskSegment = projectBuildList.get(0).getTaskSegment();
        assertTrue(
                projectBuildList.size() >= 6,
                "Expected size " + projectBuildList.size() + " to be >= 6 for collection: " + projectBuildList);

        final ProjectBuildList byTaskSegment = projectBuildList.getByTaskSegment(taskSegment);
        assertEquals(projectBuildList.size(), byTaskSegment.size()); // TODO Make multiple segments on projectBuildList
    }
}
