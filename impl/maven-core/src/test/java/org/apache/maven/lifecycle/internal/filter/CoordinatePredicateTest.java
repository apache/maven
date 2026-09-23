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
package org.apache.maven.lifecycle.internal.filter;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CoordinatePredicate}.
 */
class CoordinatePredicateTest {

    private static MojoExecution execution(
            String groupId, String artifactId, String version, String goalPrefix, String goal, String executionId) {
        PluginDescriptor descriptor = mock(PluginDescriptor.class);
        when(descriptor.getGroupId()).thenReturn(groupId);
        when(descriptor.getArtifactId()).thenReturn(artifactId);
        when(descriptor.getVersion()).thenReturn(version);
        when(descriptor.getGoalPrefix()).thenReturn(goalPrefix);

        Plugin plugin = mock(Plugin.class);
        when(plugin.getDescriptor()).thenReturn(descriptor);

        MojoExecution exec = mock(MojoExecution.class);
        when(exec.getPlugin()).thenReturn(plugin);
        when(exec.getGoal()).thenReturn(goal);
        when(exec.getExecutionId()).thenReturn(executionId);
        return exec;
    }

    private static MojoExecution surefire() {
        return execution(
                "org.apache.maven.plugins", "maven-surefire-plugin", "3.2.5", "surefire", "test", "default-test");
    }

    private static MojoExecution enforcer() {
        return execution(
                "org.apache.maven.plugins", "maven-enforcer-plugin", "3.4.1", "enforcer", "enforce", "enforce-rules");
    }

    @Test
    void wildcardMatchesAll() {
        assertTrue(CoordinatePredicate.parse("*").matches(surefire()));
        assertTrue(CoordinatePredicate.parse("*").matches(enforcer()));
    }

    @Test
    void artifactIdOnlyMatchesByArtifactId() {
        CoordinatePredicate p = CoordinatePredicate.parse(":maven-surefire-plugin");
        assertTrue(p.matches(surefire()));
        assertFalse(p.matches(enforcer()));
    }

    @Test
    void colonAloneMatchesAll() {
        CoordinatePredicate p = CoordinatePredicate.parse(":");
        assertTrue(p.matches(surefire()));
        assertTrue(p.matches(enforcer()));
    }

    @Test
    void groupAndArtifactMatchesExact() {
        CoordinatePredicate p = CoordinatePredicate.parse("org.apache.maven.plugins:maven-surefire-plugin");
        assertTrue(p.matches(surefire()));
        assertFalse(p.matches(enforcer()));
    }

    @Test
    void groupWithWildcardArtifact() {
        CoordinatePredicate p = CoordinatePredicate.parse("org.apache.maven.plugins:");
        assertTrue(p.matches(surefire()));
        assertTrue(p.matches(enforcer()));
    }

    @Test
    void groupMismatchDoesNotMatch() {
        CoordinatePredicate p = CoordinatePredicate.parse("com.example:maven-surefire-plugin");
        assertFalse(p.matches(surefire()));
    }

    @Test
    void prefixMatchesByGoalPrefix() {
        CoordinatePredicate p = CoordinatePredicate.parse("surefire");
        assertTrue(p.matches(surefire()));
        assertFalse(p.matches(enforcer()));
    }

    @Test
    void prefixWithGoalMatchesGoal() {
        CoordinatePredicate p = CoordinatePredicate.parse("surefire::test");
        assertTrue(p.matches(surefire()));
        assertFalse(p.matches(enforcer()));
    }

    @Test
    void prefixWithVersionGoalAndExecution() {
        CoordinatePredicate p = CoordinatePredicate.parse("surefire:3.2.5:test@default-test");
        assertTrue(p.matches(surefire()));
    }

    @Test
    void prefixVersionMismatch() {
        CoordinatePredicate p = CoordinatePredicate.parse("surefire:99.0:test@default-test");
        assertFalse(p.matches(surefire()));
    }

    @Test
    void prefixExecutionIdMismatch() {
        CoordinatePredicate p = CoordinatePredicate.parse("surefire::test@other-id");
        assertFalse(p.matches(surefire()));
    }

    @Test
    void fullCoordinateWithGoalAndExecution() {
        CoordinatePredicate p =
                CoordinatePredicate.parse("org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test@default-test");
        assertTrue(p.matches(surefire()));
    }

    @Test
    void coordinateGoalMismatch() {
        CoordinatePredicate p =
                CoordinatePredicate.parse("org.apache.maven.plugins:maven-surefire-plugin:3.2.5:other@default-test");
        assertFalse(p.matches(surefire()));
    }

    @Test
    void noDescriptorDoesNotMatchSpecificPlugin() {
        MojoExecution exec = mock(MojoExecution.class);
        when(exec.getPlugin()).thenReturn(null);
        when(exec.getGoal()).thenReturn("test");
        when(exec.getExecutionId()).thenReturn("default-test");
        assertFalse(CoordinatePredicate.parse(":maven-surefire-plugin").matches(exec));
    }

    @Test
    void wildcardMatchesEvenWithNullPlugin() {
        MojoExecution exec = mock(MojoExecution.class);
        when(exec.getPlugin()).thenReturn(null);
        assertTrue(CoordinatePredicate.MATCH_ALL.matches(exec));
    }
}
