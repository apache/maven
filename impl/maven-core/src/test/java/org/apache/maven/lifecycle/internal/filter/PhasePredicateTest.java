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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PhasePredicate}.
 */
class PhasePredicateTest {

    @Test
    void matchesExecutionBoundToThatPhase() {
        MojoExecution exec = mock(MojoExecution.class);
        when(exec.getLifecyclePhase()).thenReturn("test");
        assertTrue(new PhasePredicate("test").matches(exec));
    }

    @Test
    void doesNotMatchDifferentPhase() {
        MojoExecution exec = mock(MojoExecution.class);
        when(exec.getLifecyclePhase()).thenReturn("compile");
        assertFalse(new PhasePredicate("test").matches(exec));
    }

    @Test
    void doesNotMatchNullPhase() {
        MojoExecution exec = mock(MojoExecution.class);
        when(exec.getLifecyclePhase()).thenReturn(null);
        assertFalse(new PhasePredicate("test").matches(exec));
    }

    @Test
    void matchesIntegrationTestPhase() {
        MojoExecution exec = mock(MojoExecution.class);
        when(exec.getLifecyclePhase()).thenReturn("integration-test");
        assertTrue(new PhasePredicate("integration-test").matches(exec));
    }

    @Test
    void toStringReturnsPhaseExpression() {
        org.junit.jupiter.api.Assertions.assertEquals("phase(test)", new PhasePredicate("test").toString());
    }
}
