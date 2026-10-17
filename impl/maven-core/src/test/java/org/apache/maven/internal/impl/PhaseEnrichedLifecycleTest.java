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

import java.util.Collection;
import java.util.List;

import org.apache.maven.api.Lifecycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PhaseEnrichedLifecycle}.
 *
 * <p>In Maven 4 the "default" lifecycle is a <em>tree</em> of nested phases.  Phases like
 * {@code unit-test} and {@code integration-test} are children of {@code verify}, which is itself
 * a child of {@code each}.  Custom phases must therefore declare their {@code parent} explicitly.
 *
 * <p>Ordering is verified via {@link DefaultLifecycleRegistry#computePhases(Lifecycle)}, which
 * performs the full topological sort on the lifecycle DAG.
 */
class PhaseEnrichedLifecycleTest {

    /** InjectedPhase(name, parent, after, before) */
    private static PhaseEnrichedLifecycle.InjectedPhase ip(String name, String parent, String after, String before) {
        return new PhaseEnrichedLifecycle.InjectedPhase(name, parent, after, before);
    }

    private static Lifecycle defaultLifecycle() {
        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(List.of());
        return registry.stream()
                .filter(lc -> Lifecycle.DEFAULT.equals(lc.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Default lifecycle not found"));
    }

    private static DefaultLifecycleRegistry registry() {
        return new DefaultLifecycleRegistry(List.of());
    }

    // -------------------------------------------------------------------------
    // Phase presence
    // -------------------------------------------------------------------------

    @Test
    void injectedPhaseAppearsInAllPhases() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("pre-integration", "verify", "unit-test", null)));

        List<String> names = enriched.allPhases().map(Lifecycle.Phase::name).toList();
        assertTrue(names.contains("pre-integration"), "'pre-integration' must appear in allPhases()");
        assertTrue(names.contains("unit-test"), "'unit-test' must still be present");
        assertTrue(names.contains("integration-test"), "'integration-test' must still be present");
    }

    @Test
    void injectedPhaseAppearsInV3Phases() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("pre-integration", "verify", "unit-test", null)));

        List<String> names = enriched.v3phases().stream()
                .flatMap(Lifecycle.Phase::allPhases)
                .map(Lifecycle.Phase::name)
                .toList();
        assertTrue(names.contains("pre-integration"), "'pre-integration' must appear in v3phases() tree");
    }

    // -------------------------------------------------------------------------
    // Topological ordering via computePhases()
    // -------------------------------------------------------------------------

    @Test
    void computedPhasesRespectAfterConstraint() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("pre-integration", "verify", "unit-test", null)));

        List<String> computed = registry().computePhases(enriched);

        int unitTestIdx = computed.indexOf("unit-test");
        int newIdx = computed.indexOf("pre-integration");
        int itIdx = computed.indexOf("integration-test");
        assertTrue(unitTestIdx >= 0, "'unit-test' must appear in computePhases()");
        assertTrue(newIdx >= 0, "'pre-integration' must appear in computePhases()");
        assertTrue(itIdx >= 0, "'integration-test' must appear in computePhases()");
        assertTrue(unitTestIdx < newIdx, "'pre-integration' must come after 'unit-test'");
        assertTrue(newIdx < itIdx, "'pre-integration' must come before 'integration-test'");
    }

    @Test
    void computedPhasesRespectBeforeConstraint() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("pre-integration", "verify", null, "integration-test")));

        List<String> computed = registry().computePhases(enriched);

        int itIdx = computed.indexOf("integration-test");
        int newIdx = computed.indexOf("pre-integration");
        assertTrue(itIdx >= 0, "'integration-test' must appear in computePhases()");
        assertTrue(newIdx >= 0, "'pre-integration' must appear in computePhases()");
        assertTrue(newIdx < itIdx, "'pre-integration' must precede 'integration-test' in topological order");
    }

    @Test
    void computedPhasesWithAfterAndBefore() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched = new PhaseEnrichedLifecycle(
                base, List.of(ip("pre-integration", "verify", "unit-test", "integration-test")));

        List<String> computed = registry().computePhases(enriched);

        int unitTestIdx = computed.indexOf("unit-test");
        int itIdx = computed.indexOf("integration-test");
        int newIdx = computed.indexOf("pre-integration");
        assertTrue(unitTestIdx >= 0, "'unit-test' must appear");
        assertTrue(itIdx >= 0, "'integration-test' must appear");
        assertTrue(newIdx >= 0, "'pre-integration' must appear");
        assertTrue(unitTestIdx < newIdx, "'pre-integration' must come after 'unit-test'");
        assertTrue(newIdx < itIdx, "'pre-integration' must come before 'integration-test'");
    }

    // -------------------------------------------------------------------------
    // DAG links on the injected phase
    // -------------------------------------------------------------------------

    @Test
    void injectedPhaseHasAfterLink() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("pre-integration", "verify", "unit-test", null)));

        Lifecycle.Phase phase = enriched.allPhases()
                .filter(p -> "pre-integration".equals(p.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'pre-integration' not found in allPhases()"));

        Collection<Lifecycle.Link> links = phase.links();
        assertEquals(1, links.size(), "injected phase must have exactly one AFTER link");
        Lifecycle.Link link = links.iterator().next();
        assertEquals(Lifecycle.Link.Kind.AFTER, link.kind(), "link must be AFTER");
        assertEquals("unit-test", link.pointer().phase(), "AFTER link must point to 'unit-test'");
    }

    @Test
    void injectedPhaseBeforeHasAfterLinkToLeftSibling() {
        // When only 'before' is specified, the AFTER link points to the left sibling (not the before anchor)
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("pre-integration", "verify", null, "integration-test")));

        Lifecycle.Phase phase = enriched.allPhases()
                .filter(p -> "pre-integration".equals(p.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'pre-integration' not found"));

        Collection<Lifecycle.Link> links = phase.links();
        assertEquals(1, links.size(), "injected phase must have exactly one AFTER link");
        assertEquals(Lifecycle.Link.Kind.AFTER, links.iterator().next().kind());
        // Left sibling when inserting before 'integration-test' in [unit-test, integration-test] is 'unit-test'
        assertEquals(
                "unit-test",
                links.iterator().next().pointer().phase(),
                "AFTER link must point to 'unit-test' (left sibling)");
    }

    // -------------------------------------------------------------------------
    // Identity / delegation
    // -------------------------------------------------------------------------

    @Test
    void idIsPreserved() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("x", "verify", "unit-test", null)));
        assertEquals(Lifecycle.DEFAULT, enriched.id());
    }

    @Test
    void aliasesAreDelegated() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("x", "verify", "unit-test", null)));
        List<String> baseV3 =
                base.aliases().stream().map(Lifecycle.Alias::v3Phase).toList();
        List<String> enrichedV3 =
                enriched.aliases().stream().map(Lifecycle.Alias::v3Phase).toList();
        assertEquals(baseV3, enrichedV3, "aliases must be fully delegated to the base lifecycle");
    }

    @Test
    void originalPhasesAreRetained() {
        Lifecycle base = defaultLifecycle();
        long originalCount = base.allPhases().count();

        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("x", "verify", "unit-test", null)));
        long enrichedCount = enriched.allPhases().count();

        assertEquals(originalCount + 1, enrichedCount, "exactly one phase should be added");
    }

    // -------------------------------------------------------------------------
    // Unknown parent — graceful fallback
    // -------------------------------------------------------------------------

    @Test
    void unknownParentAppendsPhaseWithoutCrash() {
        Lifecycle base = defaultLifecycle();
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("orphan", "no-such-parent", null, null)));

        assertTrue(
                enriched.allPhases().anyMatch(p -> "orphan".equals(p.name())),
                "phase with unknown parent should still appear (appended at top level)");
    }

    @Test
    void appendsToEndOfParentWhenNoAnchor() {
        Lifecycle base = defaultLifecycle();
        // parent=verify, no after/before → appended after integration-test
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(base, List.of(ip("post-verify", "verify", null, null)));

        List<String> computed = registry().computePhases(enriched);
        int itIdx = computed.indexOf("integration-test");
        int newIdx = computed.indexOf("post-verify");
        assertTrue(itIdx >= 0, "'integration-test' must appear");
        assertTrue(newIdx >= 0, "'post-verify' must appear");
        assertTrue(itIdx < newIdx, "'post-verify' appended at end should come after 'integration-test'");
    }
}
