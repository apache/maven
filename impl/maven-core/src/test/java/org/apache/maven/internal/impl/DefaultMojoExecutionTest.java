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
package org.apache.maven.internal.impl;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultMojoExecution} immutable snapshot semantics.
 */
class DefaultMojoExecutionTest {

    /**
     * Builds a minimal {@link MojoExecution} backed by a real {@link MojoDescriptor}
     * with the given goal; executionId and lifecyclePhase are left at their
     * legacy-default values (null) to simulate the CLI-invocation path.
     */
    private static MojoExecution cliExecution(String goal) {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.plugins");
        pluginDescriptor.setArtifactId("maven-test-plugin");
        pluginDescriptor.setVersion("1.0");
        pluginDescriptor.setGoalPrefix("test");
        // No plugin artifact, no dependency node → simulates a minimal CLI-invoked execution

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal(goal);
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        return new MojoExecution(mojoDescriptor);
    }

    /**
     * Builds a lifecycle-bound {@link MojoExecution} with all fields populated
     * (executionId and lifecyclePhase present).
     */
    private static MojoExecution lifecycleExecution(String goal, String executionId, String phase) {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.plugins");
        pluginDescriptor.setArtifactId("maven-test-plugin");
        pluginDescriptor.setVersion("1.0");
        pluginDescriptor.setGoalPrefix("test");

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal(goal);
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        MojoExecution execution = new MojoExecution(mojoDescriptor, executionId);
        execution.setLifecyclePhase(phase);
        return execution;
    }

    @Test
    void cliInvocationPathExecutionIdAndLifecyclePhaseAreEmpty() {
        InternalMavenSession session = mock(InternalMavenSession.class);
        MojoExecution delegate = cliExecution("help");

        org.apache.maven.api.MojoExecution snapshot = new DefaultMojoExecution(session, delegate);

        assertEquals("help", snapshot.goal());
        assertFalse(snapshot.executionId().isPresent(), "executionId should be empty for CLI invocation");
        assertFalse(snapshot.lifecyclePhase().isPresent(), "lifecyclePhase should be empty for CLI invocation");
        assertFalse(snapshot.model().isPresent(), "model should be empty when no plugin in model");
        assertFalse(snapshot.configuration().isPresent(), "configuration should be empty when not set");
    }

    @Test
    void lifecycleBoundExecutionExecutionIdAndLifecyclePhasePresentAndImmutable() {
        InternalMavenSession session = mock(InternalMavenSession.class);
        MojoExecution delegate = lifecycleExecution("compile", "default-compile", "compile");

        org.apache.maven.api.MojoExecution snapshot = new DefaultMojoExecution(session, delegate);

        assertEquals("compile", snapshot.goal());
        assertTrue(snapshot.executionId().isPresent());
        assertEquals("default-compile", snapshot.executionId().get());
        assertTrue(snapshot.lifecyclePhase().isPresent());
        assertEquals("compile", snapshot.lifecyclePhase().get());

        // Mutating the delegate after snapshot construction must not affect the snapshot
        delegate.setLifecyclePhase("package");
        assertEquals("compile", snapshot.lifecyclePhase().get(), "snapshot must be immutable after construction");
    }

    @Test
    void snapshotDescriptorMatchesLegacyDescriptor() {
        InternalMavenSession session = mock(InternalMavenSession.class);
        MojoExecution delegate = cliExecution("clean");

        org.apache.maven.api.MojoExecution snapshot = new DefaultMojoExecution(session, delegate);

        assertEquals(
                delegate.getMojoDescriptor().getMojoDescriptorV4(),
                snapshot.descriptor(),
                "descriptor must be a copy of the legacy V4 descriptor");
    }
}
