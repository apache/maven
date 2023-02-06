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
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.apache.maven.plugin.MojoExecution;

/**
 * @author Kristian Rosenvold
 */
public class PhaseRecorderTest extends TestCase {
    public void testObserveExecution() throws Exception {
        PhaseRecorder phaseRecorder = new PhaseRecorder(ProjectDependencyGraphStub.A);
        MavenExecutionPlan plan = LifecycleExecutionPlanCalculatorStub.getProjectAExceutionPlan();
        final List<MojoExecution> executions = plan.getMojoExecutions();

        final MojoExecution mojoExecution1 = executions.get(0);
        final MojoExecution mojoExecution2 = executions.get(1);
        phaseRecorder.observeExecution(mojoExecution1);

        assertTrue(ProjectDependencyGraphStub.A.hasLifecyclePhase(mojoExecution1.getLifecyclePhase()));
        assertFalse(ProjectDependencyGraphStub.A.hasLifecyclePhase(mojoExecution2.getLifecyclePhase()));

        assertFalse(phaseRecorder.isDifferentPhase(mojoExecution1));
        assertTrue(phaseRecorder.isDifferentPhase(mojoExecution2));
    }
}
