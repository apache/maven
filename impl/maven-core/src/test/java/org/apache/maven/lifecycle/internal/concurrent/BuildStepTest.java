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
package org.apache.maven.lifecycle.internal.concurrent;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildStepTest {

    @Test
    void stepWithoutMojosDoesNoWork() {
        BuildStep step = new BuildStep("compile", new MavenProject(), null);

        assertFalse(step.hasExecutions());
    }

    @Test
    void stepCarryingAMojoDoesWork() {
        BuildStep step = new BuildStep("compile", new MavenProject(), null);
        step.addMojo(mojoExecution("compile"), 0);

        assertTrue(step.hasExecutions());
    }

    @Test
    void skippedStepDoesNoWork() {
        BuildStep step = new BuildStep("compile", new MavenProject(), null);
        step.addMojo(mojoExecution("compile"), 0);
        step.skip();

        assertFalse(step.hasExecutions());
    }

    private static MojoExecution mojoExecution(String goal) {
        MojoDescriptor descriptor = new MojoDescriptor();
        descriptor.setGoal(goal);
        return new MojoExecution(descriptor, "default-" + goal);
    }
}
