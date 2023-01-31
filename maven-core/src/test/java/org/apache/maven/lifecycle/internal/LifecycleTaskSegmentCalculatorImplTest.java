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

import java.util.List;

import junit.framework.TestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.stub.LifecycleTaskSegmentCalculatorStub;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;

/**
 * @author Kristian Rosenvold
 */
public class LifecycleTaskSegmentCalculatorImplTest extends TestCase {
    public void testCalculateProjectBuilds() throws Exception {
        LifecycleTaskSegmentCalculator lifecycleTaskSegmentCalculator = getTaskSegmentCalculator();
        BuildListCalculator buildListCalculator = new BuildListCalculator();
        final MavenSession session = ProjectDependencyGraphStub.getMavenSession();
        List<TaskSegment> taskSegments = lifecycleTaskSegmentCalculator.calculateTaskSegments(session);

        final ProjectBuildList buildList = buildListCalculator.calculateProjectBuilds(session, taskSegments);
        final ProjectBuildList segments = buildList.getByTaskSegment(taskSegments.get(0));
        assertEquals("Stub data contains 3 segments", 3, taskSegments.size());
        assertEquals("Stub data contains 6 items", 6, segments.size());
        final ProjectSegment build = segments.get(0);
        assertNotNull(build);
    }

    private static LifecycleTaskSegmentCalculator getTaskSegmentCalculator() {
        return new LifecycleTaskSegmentCalculatorStub();
    }
}
