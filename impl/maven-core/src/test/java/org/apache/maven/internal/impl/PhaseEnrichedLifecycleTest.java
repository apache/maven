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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.junit.jupiter.api.Test;

import static org.apache.maven.internal.impl.Lifecycles.phase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PhaseEnrichedLifecycle}.
 *
 * <p>Tests cover phase injection ordering, before/after anchor placement, unknown parent fallback,
 * and conflict detection.
 */
class PhaseEnrichedLifecycleTest {

    // -------------------------------------------------------------------------
    // Helper: simple lifecycle stub
    // -------------------------------------------------------------------------

    /** Builds a minimal Lifecycle stub with the given top-level phases. */
    private static Lifecycle stub(String id, List<Lifecycle.Phase> phases) {
        return new Lifecycle() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Collection<Phase> phases() {
                return phases;
            }

            @Override
            public Collection<Phase> v3phases() {
                return phases; // flat list – sufficient for v3 injection tests
            }

            @Override
            public Stream<Phase> allPhases() {
                return phases.stream().flatMap(Phase::allPhases);
            }

            @Override
            public Collection<Alias> aliases() {
                return Collections.emptyList();
            }
        };
    }

    /** Returns phase names from the top-level list. */
    private static List<String> names(Collection<Lifecycle.Phase> phases) {
        return phases.stream().map(Lifecycle.Phase::name).collect(Collectors.toList());
    }

    /** Returns phase names from allPhases() (DFS order). */
    private static List<String> allNames(Lifecycle lc) {
        return lc.allPhases().map(Lifecycle.Phase::name).collect(Collectors.toList());
    }

    private static PhaseEnrichedLifecycle.InjectedPhase inj(String name, String parent, String after, String before) {
        return new PhaseEnrichedLifecycle.InjectedPhase(name, parent, after, before);
    }

    // -------------------------------------------------------------------------
    // Maven 4 tree injection (phases())
    // -------------------------------------------------------------------------

    @Test
    void injectAfterAnchorNoParent() {
        // Flat list: [compile, test, package]
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test"), phase("package")));

        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(lc, List.of(inj("my-phase", null, "compile", null)));

        List<String> names = names(enriched.phases());
        assertEquals(List.of("compile", "my-phase", "test", "package"), names);
    }

    @Test
    void injectBeforeAnchorNoParent() {
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test"), phase("package")));

        PhaseEnrichedLifecycle enriched = new PhaseEnrichedLifecycle(lc, List.of(inj("pre-test", null, null, "test")));

        List<String> names = names(enriched.phases());
        assertEquals(List.of("compile", "pre-test", "test", "package"), names);
    }

    @Test
    void injectAfterAndBeforeConstraint() {
        // Insert between compile and package (after compile, before package)
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test"), phase("package")));

        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(lc, List.of(inj("mid", null, "compile", "package")));

        List<String> names = names(enriched.phases());
        // Should land at index 1 (after compile=0, before test=1 or package=2)
        // after compile (idx 0) → lowerBound=0; before package (idx 2) → upperBound=2
        // insertAt = 1
        assertEquals(List.of("compile", "mid", "test", "package"), names);
    }

    @Test
    void injectIntoKnownParent() {
        // Tree: [pre-build, build(children=[compile, test]), post-build]
        Lifecycle.Phase build = phase("build", phase("compile"), phase("test"));
        Lifecycle lc = stub("default", List.of(phase("pre-build"), build, phase("post-build")));

        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(lc, List.of(inj("my-phase", "build", "compile", null)));

        // Top-level names unchanged
        assertEquals(List.of("pre-build", "build", "post-build"), names(enriched.phases()));
        // Children of "build" should contain the injected phase
        Lifecycle.Phase enrichedBuild = enriched.phases().stream()
                .filter(p -> "build".equals(p.name()))
                .findFirst()
                .orElseThrow();
        List<String> childNames = names(enrichedBuild.phases());
        assertEquals(List.of("compile", "my-phase", "test"), childNames);
    }

    @Test
    void unknownParentFallsBackToTopLevel() {
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test")));

        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(lc, List.of(inj("orphan", "nonexistent-parent", null, null)));

        List<String> names = names(enriched.phases());
        // Falls back: appended at the top level
        assertEquals(List.of("compile", "test", "orphan"), names);
    }

    @Test
    void conflictingConstraintsThrows() {
        // after="package" before="compile" — after comes after before in the list
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test"), phase("package")));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new PhaseEnrichedLifecycle(lc, List.of(inj("bad", null, "package", "compile"))));

        assertTrue(ex.getMessage().contains("conflicting constraints"));
    }

    @Test
    void duplicatePhaseNameAppended() {
        // Injecting a phase with the same name as an existing one — should still inject (no dedup)
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test")));

        PhaseEnrichedLifecycle enriched = new PhaseEnrichedLifecycle(lc, List.of(inj("compile", null, "test", null)));

        List<String> names = names(enriched.phases());
        assertEquals(List.of("compile", "test", "compile"), names);
    }

    @Test
    void multipleInjectionsOrdered() {
        // Inject two phases; both after "compile"
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test")));

        PhaseEnrichedLifecycle enriched = new PhaseEnrichedLifecycle(
                lc, List.of(inj("phase-a", null, "compile", null), inj("phase-b", null, "phase-a", null)));

        List<String> names = names(enriched.phases());
        // phase-a after compile; phase-b after phase-a (which is at idx 1)
        assertEquals(List.of("compile", "phase-a", "phase-b", "test"), names);
    }

    // -------------------------------------------------------------------------
    // v3phases() injection
    // -------------------------------------------------------------------------

    @Test
    void v3InjectAfter() {
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test"), phase("package")));

        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(lc, List.of(inj("my-phase", null, "compile", null)));

        List<String> names = names(enriched.v3phases());
        assertEquals(List.of("compile", "my-phase", "test", "package"), names);
        // Verify AFTER link present
        Lifecycle.Phase injected = enriched.v3phases().stream()
                .filter(p -> "my-phase".equals(p.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(injected.links().stream().anyMatch(l -> l.kind() == Lifecycle.Link.Kind.AFTER));
    }

    @Test
    void v3UnknownAnchorFallsBackToTopLevel() {
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test")));

        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(lc, List.of(inj("orphan", null, "nosuchphase", null)));

        // Should be appended
        List<String> names = names(enriched.v3phases());
        assertTrue(names.contains("orphan"));
        assertEquals("orphan", names.get(names.size() - 1));
    }

    // -------------------------------------------------------------------------
    // Delegation — id(), aliases()
    // -------------------------------------------------------------------------

    @Test
    void delegatesId() {
        Lifecycle lc = stub("clean", List.of(phase("pre-clean")));
        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(lc, List.of(inj("mid-clean", null, "pre-clean", null)));
        assertEquals("clean", enriched.id());
    }

    @Test
    void delegatesAliases() {
        Lifecycle lc = stub("default", List.of(phase("compile")));
        PhaseEnrichedLifecycle enriched = new PhaseEnrichedLifecycle(lc, List.of());
        assertTrue(enriched.aliases().isEmpty());
    }

    // -------------------------------------------------------------------------
    // allPhases() — DFS flat stream
    // -------------------------------------------------------------------------

    @Test
    void allPhasesIncludesInjected() {
        Lifecycle.Phase build = phase("build", phase("compile"), phase("test"));
        Lifecycle lc = stub("default", List.of(build));

        PhaseEnrichedLifecycle enriched =
                new PhaseEnrichedLifecycle(lc, List.of(inj("my-phase", "build", "compile", null)));

        List<String> all = allNames(enriched);
        assertTrue(all.contains("my-phase"), "allPhases() must include injected phase");
    }

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    @Test
    void emptyInjectionsReturnsSamePhaseCount() {
        Lifecycle lc = stub("default", List.of(phase("compile"), phase("test")));
        PhaseEnrichedLifecycle enriched = new PhaseEnrichedLifecycle(lc, List.of());
        assertEquals(2, enriched.phases().size());
        assertInstanceOf(PhaseEnrichedLifecycle.class, enriched);
    }
}
