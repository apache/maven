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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MojoExecutionFilter}.
 */
class MojoExecutionFilterTest {

    // ── parse ─────────────────────────────────────────────────────────────────

    @Test
    void parseNullReturnsEmpty() {
        assertTrue(MojoExecutionFilter.parse(null).isEmpty());
    }

    @Test
    void parseBlankReturnsEmpty() {
        assertTrue(MojoExecutionFilter.parse("  ").isEmpty());
    }

    @Test
    void parseSinglePhase() {
        List<FilterPredicate> predicates = MojoExecutionFilter.parse("phase(test)");
        assertEquals(1, predicates.size());
        assertInstanceOf(PhasePredicate.class, predicates.get(0));
    }

    @Test
    void parseMultiplePredicates() {
        List<FilterPredicate> predicates =
                MojoExecutionFilter.parse("phase(test), phase(integration-test), :maven-enforcer-plugin");
        assertEquals(3, predicates.size());
        assertInstanceOf(PhasePredicate.class, predicates.get(0));
        assertInstanceOf(PhasePredicate.class, predicates.get(1));
        assertInstanceOf(CoordinatePredicate.class, predicates.get(2));
    }

    @Test
    void parseSkipsBlankTokens() {
        List<FilterPredicate> predicates = MojoExecutionFilter.parse("phase(test),,phase(compile),");
        assertEquals(2, predicates.size());
    }

    @Test
    void parseWildcard() {
        List<FilterPredicate> predicates = MojoExecutionFilter.parse("*");
        assertEquals(1, predicates.size());
        assertEquals(CoordinatePredicate.MATCH_ALL, predicates.get(0));
    }

    @Test
    void parsePrefix() {
        List<FilterPredicate> predicates = MojoExecutionFilter.parse("surefire");
        assertEquals(1, predicates.size());
        assertInstanceOf(CoordinatePredicate.class, predicates.get(0));
    }

    @Test
    void parsePrefixWithVersionAndGoalAndExecution() {
        List<FilterPredicate> predicates = MojoExecutionFilter.parse("surefire:3.2.5:test@default-test");
        assertEquals(1, predicates.size());
        assertInstanceOf(CoordinatePredicate.class, predicates.get(0));
    }

    @Test
    void parseGroupArtifact() {
        List<FilterPredicate> predicates = MojoExecutionFilter.parse("org.apache.maven.plugins:maven-surefire-plugin");
        assertEquals(1, predicates.size());
        assertInstanceOf(CoordinatePredicate.class, predicates.get(0));
    }

    @Test
    void parseResultIsImmutable() {
        List<FilterPredicate> predicates = MojoExecutionFilter.parse("phase(test)");
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> predicates.add(CoordinatePredicate.MATCH_ALL));
    }

    @Test
    void emptyPhaseParensThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> MojoExecutionFilter.parse("phase()"));
    }

    @Test
    void blankPhaseParensThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> MojoExecutionFilter.parse("phase(  )"));
    }

    // ── propertyName ─────────────────────────────────────────────────────────

    @Test
    void propertyNameMatchesConstant() {
        assertEquals(org.apache.maven.api.Constants.MAVEN_LIFECYCLE_FILTER, MojoExecutionFilter.PROPERTY_NAME);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void assertInstanceOf(Class<?> expected, Object actual) {
        assertTrue(
                expected.isInstance(actual),
                "Expected instance of " + expected.getSimpleName() + " but got "
                        + actual.getClass().getSimpleName());
    }
}
