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

    /**
     * Test for <a href="https://github.com/apache/maven/issues/11796">MNG-11796</a>:
     * custom lifecycle with {@code <default-phases>} binding goals to standard lifecycle phases.
     */
    @Test
    void testCustomLifecycleDefaultPhasesForStandardPhases() throws ComponentLookupException {
        // Create a custom lifecycle with default-phases mapping to a standard lifecycle phase
        Map<String, LifecyclePhase> defaultPhases = new HashMap<>();
        defaultPhases.put("process-sources", new LifecyclePhase("com.example:my-plugin:touch"));

        Lifecycle customLifecycle = new Lifecycle("my-extension", Arrays.asList("my-dummy-phase"), defaultPhases);

        List<Lifecycle> allLifecycles = new ArrayList<>();
        allLifecycles.add(customLifecycle);
        allLifecycles.addAll(defaultLifeCycles.getLifeCycles());

        Map<String, Lifecycle> lifeCycles = allLifecycles.stream().collect(Collectors.toMap(Lifecycle::getId, l -> l));
        PlexusContainer mockedPlexusContainer = mock(PlexusContainer.class);
        when(mockedPlexusContainer.lookupMap(Lifecycle.class)).thenReturn(lifeCycles);

        DefaultLifecycles dl = new DefaultLifecycles(
                new DefaultLifecycleRegistry(
                        List.of(new DefaultLifecycleRegistry.LifecycleWrapperProvider(mockedPlexusContainer))),
                new DefaultLookup(mockedPlexusContainer));

        // Find the custom lifecycle
        Lifecycle result = dl.getLifeCycles().stream()
                .filter(l -> "my-extension".equals(l.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Custom lifecycle not found"));

        // Verify that getPhases() only contains the custom phase (not standard phases)
        assertTrue(result.getPhases().contains("my-dummy-phase"), "Custom lifecycle should contain its own phase");

        // Verify that defaultLifecyclePhases includes the standard phase binding
        Map<String, LifecyclePhase> resultDefaultPhases = result.getDefaultLifecyclePhases();
        assertNotNull(resultDefaultPhases, "Default lifecycle phases should not be null");
        assertTrue(
                resultDefaultPhases.containsKey("process-sources"),
                "Default lifecycle phases should contain 'process-sources' binding");
        String goalSpec = resultDefaultPhases.get("process-sources").toString();
        assertTrue(
                goalSpec.contains("com.example") && goalSpec.contains("my-plugin") && goalSpec.contains("touch"),
                "The process-sources binding should map to the correct goal, got: " + goalSpec);
    }

    private Lifecycle getLifeCycleById(String id) {
        return defaultLifeCycles.getLifeCycles().stream()
                .filter(l -> id.equals(l.getId()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
