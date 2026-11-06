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
package org.apache.maven.cling.invoker.mvn;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.settings.PhaseInjection;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.PhaseEnrichedLifecycle;
import org.apache.maven.plugin.LegacySupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SettingsLifecycleProcessor#validate(PhaseInjection)} and
 * {@link SettingsLifecycleProcessor#process(Lifecycle)}.
 */
class SettingsLifecycleProcessorTest {

    // -------------------------------------------------------------------------
    // validate() tests (unchanged from earlier round)
    // -------------------------------------------------------------------------

    @Test
    void validPhaseWithAfterOnly() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("my-phase").after("compile").build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void validPhaseWithBeforeOnly() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("my-phase").before("test").build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void validPhaseWithParentOnly() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("my-phase").parent("default").build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void validPhaseWithAfterAndBefore() {
        PhaseInjection phase = PhaseInjection.newBuilder()
                .name("my-phase")
                .after("compile")
                .before("test")
                .build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void validPhaseWithParentAndAfter() {
        PhaseInjection phase = PhaseInjection.newBuilder()
                .name("my-phase")
                .parent("default")
                .after("compile")
                .build();
        assertDoesNotThrow(() -> SettingsLifecycleProcessor.validate(phase));
    }

    @Test
    void missingNameThrows() {
        PhaseInjection phase = PhaseInjection.newBuilder().after("compile").build();
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SettingsLifecycleProcessor.validate(phase));
        assertTrue(ex.getMessage().contains("non-blank 'name'"));
    }

    @Test
    void blankNameThrows() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("   ").after("compile").build();
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SettingsLifecycleProcessor.validate(phase));
        assertTrue(ex.getMessage().contains("non-blank 'name'"));
    }

    @Test
    void noAnchorThrows() {
        PhaseInjection phase = PhaseInjection.newBuilder().name("my-phase").build();
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SettingsLifecycleProcessor.validate(phase));
        assertTrue(ex.getMessage().contains("anchor it in the lifecycle DAG"));
    }

    @Test
    void blankAfterAndBeforeWithNoParentThrows() {
        PhaseInjection phase = PhaseInjection.newBuilder()
                .name("my-phase")
                .after("  ")
                .before("")
                .build();
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SettingsLifecycleProcessor.validate(phase));
        assertTrue(ex.getMessage().contains("anchor it in the lifecycle DAG"));
    }

    // -------------------------------------------------------------------------
    // process() tests
    // -------------------------------------------------------------------------

    /** Builds a minimal Lifecycle stub with the given id and top-level phase names. */
    private static Lifecycle stubLifecycle(String id, List<String> phaseNames) {
        List<Lifecycle.Phase> phases = phaseNames.stream()
                .map(name -> (Lifecycle.Phase) new Lifecycle.Phase() {
                    @Override
                    public String name() {
                        return name;
                    }

                    @Override
                    public List<Plugin> plugins() {
                        return List.of();
                    }

                    @Override
                    public Collection<Lifecycle.Link> links() {
                        return Collections.emptyList();
                    }

                    @Override
                    public List<Lifecycle.Phase> phases() {
                        return List.of();
                    }

                    @Override
                    public Stream<Lifecycle.Phase> allPhases() {
                        return Stream.of(this);
                    }
                })
                .collect(Collectors.toList());
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
                return phases;
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

    /** Creates a {@link SettingsLifecycleProcessor} whose session returns the given data map. */
    private static SettingsLifecycleProcessor processorWithData(Map<String, Object> data) {
        MavenExecutionRequest request = mock(MavenExecutionRequest.class);
        when(request.getData()).thenReturn(data);
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);
        LegacySupport legacySupport = mock(LegacySupport.class);
        when(legacySupport.getSession()).thenReturn(session);
        return new SettingsLifecycleProcessor(legacySupport);
    }

    @Test
    void processNoSessionReturnsOriginal() {
        // getSession() returns null → no enrichment
        LegacySupport legacySupport = mock(LegacySupport.class);
        when(legacySupport.getSession()).thenReturn(null);
        SettingsLifecycleProcessor processor = new SettingsLifecycleProcessor(legacySupport);

        Lifecycle lc = stubLifecycle(Lifecycle.DEFAULT, List.of("compile", "test"));
        Lifecycle result = processor.process(lc);
        assertSame(lc, result, "No session → original lifecycle returned unchanged");
    }

    @Test
    void processNoPhasesDataKeyReturnsOriginal() {
        Map<String, Object> data = new HashMap<>(); // no PHASES_DATA_KEY entry
        SettingsLifecycleProcessor processor = processorWithData(data);

        Lifecycle lc = stubLifecycle(Lifecycle.DEFAULT, List.of("compile", "test"));
        Lifecycle result = processor.process(lc);
        assertSame(lc, result, "No phases in data → original lifecycle returned unchanged");
    }

    @Test
    void processEmptyPhasesListReturnsOriginal() {
        Map<String, Object> data = new HashMap<>();
        data.put(SettingsLifecycleProcessor.PHASES_DATA_KEY, List.of());
        SettingsLifecycleProcessor processor = processorWithData(data);

        Lifecycle lc = stubLifecycle(Lifecycle.DEFAULT, List.of("compile", "test"));
        Lifecycle result = processor.process(lc);
        assertSame(lc, result, "Empty phases list → original lifecycle returned unchanged");
    }

    @Test
    void processNonDefaultLifecycleReturnsOriginal() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("my-phase").after("pre-clean").build();
        Map<String, Object> data = new HashMap<>();
        data.put(SettingsLifecycleProcessor.PHASES_DATA_KEY, List.of(phase));
        SettingsLifecycleProcessor processor = processorWithData(data);

        Lifecycle lc = stubLifecycle("clean", List.of("pre-clean", "clean"));
        Lifecycle result = processor.process(lc);
        assertSame(lc, result, "Non-default lifecycle → not enriched");
    }

    @Test
    void processDefaultLifecycleWithInjection() {
        PhaseInjection phase = PhaseInjection.newBuilder()
                .name("pre-integration")
                .after("test")
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put(SettingsLifecycleProcessor.PHASES_DATA_KEY, List.of(phase));
        SettingsLifecycleProcessor processor = processorWithData(data);

        Lifecycle lc = stubLifecycle(Lifecycle.DEFAULT, List.of("compile", "test", "package"));
        Lifecycle result = processor.process(lc);

        assertInstanceOf(PhaseEnrichedLifecycle.class, result, "Default lifecycle should be enriched");
        List<String> names = result.phases().stream().map(Lifecycle.Phase::name).collect(Collectors.toList());
        assertTrue(names.contains("pre-integration"), "Injected phase must be present");
        // pre-integration should be right after test
        int testIdx = names.indexOf("test");
        int injIdx = names.indexOf("pre-integration");
        assertTrue(injIdx == testIdx + 1, "pre-integration should be inserted directly after test");
    }

    @Test
    void processInjectedPhaseHasAfterLink() {
        PhaseInjection phase =
                PhaseInjection.newBuilder().name("mid").after("compile").build();
        Map<String, Object> data = new HashMap<>();
        data.put(SettingsLifecycleProcessor.PHASES_DATA_KEY, List.of(phase));
        SettingsLifecycleProcessor processor = processorWithData(data);

        Lifecycle lc = stubLifecycle(Lifecycle.DEFAULT, List.of("compile", "test"));
        Lifecycle result = processor.process(lc);
        assertInstanceOf(PhaseEnrichedLifecycle.class, result);

        Lifecycle.Phase injected = result.phases().stream()
                .filter(p -> "mid".equals(p.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Injected phase 'mid' not found"));

        assertTrue(
                injected.links().stream().anyMatch(l -> l.kind() == Lifecycle.Link.Kind.AFTER),
                "Injected phase must carry an AFTER link");
    }
}
