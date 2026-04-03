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
package org.apache.maven.lifecycle;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.internal.impl.DefaultLifecycleRegistry;
import org.apache.maven.internal.impl.DefaultLookup;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
@PlexusTest
class DefaultLifecyclesTest {
    @Inject
    private DefaultLifecycles defaultLifeCycles;

    @Test
    void testDefaultLifecycles() {
        final List<Lifecycle> lifecycles = defaultLifeCycles.getLifeCycles();
        assertEquals(3, lifecycles.size());
        assertEquals(3, DefaultLifecycles.STANDARD_LIFECYCLES.length);
    }

    @Test
    void testDefaultLifecycle() {
        final Lifecycle lifecycle = getLifeCycleById("default");
        assertEquals("default", lifecycle.getId());
        assertEquals(54, lifecycle.getPhases().size());
    }

    @Test
    void testCleanLifecycle() {
        final Lifecycle lifecycle = getLifeCycleById("clean");
        assertEquals("clean", lifecycle.getId());
        assertEquals(3, lifecycle.getPhases().size());
    }

    @Test
    void testSiteLifecycle() {
        final Lifecycle lifecycle = getLifeCycleById("site");
        assertEquals("site", lifecycle.getId());
        assertEquals(6, lifecycle.getPhases().size());
    }

    @Test
    void testCustomLifecycle() throws ComponentLookupException {
        List<Lifecycle> myLifecycles = new ArrayList<>();
        Lifecycle myLifecycle =
                new Lifecycle("etl", Arrays.asList("extract", "transform", "load"), Collections.emptyMap());
        myLifecycles.add(myLifecycle);
        myLifecycles.addAll(defaultLifeCycles.getLifeCycles());

        Map<String, Lifecycle> lifeCycles = myLifecycles.stream().collect(Collectors.toMap(Lifecycle::getId, l -> l));
        PlexusContainer mockedPlexusContainer = mock(PlexusContainer.class);
        when(mockedPlexusContainer.lookupMap(Lifecycle.class)).thenReturn(lifeCycles);

        DefaultLifecycles dl = new DefaultLifecycles(
                new DefaultLifecycleRegistry(
                        List.of(new DefaultLifecycleRegistry.LifecycleWrapperProvider(mockedPlexusContainer))),
                new DefaultLookup(mockedPlexusContainer));

        assertEquals("clean", dl.getLifeCycles().get(0).getId());
        assertEquals("default", dl.getLifeCycles().get(1).getId());
        assertEquals("site", dl.getLifeCycles().get(2).getId());
        assertEquals("etl", dl.getLifeCycles().get(3).getId());
    }

    @Test
    void testCustomLifecycleWithCrossLifecycleDefaultPhases() throws ComponentLookupException {
        // Simulates a plugin that registers a custom lifecycle via components.xml
        // with <default-phases> binding goals to standard lifecycle phases (e.g. process-sources)
        // rather than to phases of the custom lifecycle itself. This is the Maven 3 mechanism
        // for extension plugins to bind goals to standard phases without requiring <executions>.
        Map<String, LifecyclePhase> defaultPhases = new HashMap<>();
        defaultPhases.put("process-sources", new LifecyclePhase("com.example:my-plugin:1.0:touch"));

        Lifecycle customLifecycle = new Lifecycle("my-custom-lifecycle", Arrays.asList("custom-phase"), defaultPhases);

        List<Lifecycle> myLifecycles = new ArrayList<>();
        myLifecycles.add(customLifecycle);
        myLifecycles.addAll(defaultLifeCycles.getLifeCycles());

        Map<String, Lifecycle> lifeCycles = myLifecycles.stream().collect(Collectors.toMap(Lifecycle::getId, l -> l));
        PlexusContainer mockedPlexusContainer = mock(PlexusContainer.class);
        when(mockedPlexusContainer.lookupMap(Lifecycle.class)).thenReturn(lifeCycles);

        DefaultLifecycles dl = new DefaultLifecycles(
                new DefaultLifecycleRegistry(
                        List.of(new DefaultLifecycleRegistry.LifecycleWrapperProvider(mockedPlexusContainer))),
                new DefaultLookup(mockedPlexusContainer));

        Lifecycle resolved = dl.getLifeCycles().stream()
                .filter(l -> "my-custom-lifecycle".equals(l.getId()))
                .findFirst()
                .orElseThrow();

        // Cross-lifecycle default phase bindings must survive the round-trip conversion
        Map<String, LifecyclePhase> resolvedDefaultPhases = resolved.getDefaultLifecyclePhases();
        assertNotNull(resolvedDefaultPhases);
        assertTrue(
                resolvedDefaultPhases.containsKey("process-sources"),
                "Cross-lifecycle binding to 'process-sources' should be preserved");

        // The lifecycle's own phase list should NOT include cross-lifecycle phases
        assertFalse(
                resolved.getPhases().contains("process-sources"),
                "Cross-lifecycle phase should not appear in the lifecycle's own phase list");
        assertTrue(resolved.getPhases().contains("custom-phase"), "Lifecycle's own phase should be present");
    }

    private Lifecycle getLifeCycleById(String id) {
        return defaultLifeCycles.getLifeCycles().stream()
                .filter(l -> id.equals(l.getId()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
