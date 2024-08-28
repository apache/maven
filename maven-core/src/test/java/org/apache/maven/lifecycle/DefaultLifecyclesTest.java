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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.internal.impl.DefaultLifecycleRegistry;
import org.apache.maven.internal.impl.DefaultLookup;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
        assertThat(lifecycles, hasSize(4));
        assertThat(DefaultLifecycles.STANDARD_LIFECYCLES, arrayWithSize(4));
    }

    @Test
    void testDefaultLifecycle() {
        final Lifecycle lifecycle = getLifeCycleById("default");
        assertThat(lifecycle.getId(), is("default"));
        assertThat(lifecycle.getPhases(), hasSize(54));
    }

    @Test
    void testCleanLifecycle() {
        final Lifecycle lifecycle = getLifeCycleById("clean");
        assertThat(lifecycle.getId(), is("clean"));
        assertThat(lifecycle.getPhases(), hasSize(3));
    }

    @Test
    void testSiteLifecycle() {
        final Lifecycle lifecycle = getLifeCycleById("site");
        assertThat(lifecycle.getId(), is("site"));
        assertThat(lifecycle.getPhases(), hasSize(6));
    }

    @Test
    void testWrapperLifecycle() {
        final Lifecycle lifecycle = getLifeCycleById("wrapper");
        assertThat(lifecycle.getId(), is("wrapper"));
        assertThat(lifecycle.getPhases(), hasSize(3));
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

        assertThat(dl.getLifeCycles().get(0).getId(), is("clean"));
        assertThat(dl.getLifeCycles().get(1).getId(), is("default"));
        assertThat(dl.getLifeCycles().get(2).getId(), is("site"));
        assertThat(dl.getLifeCycles().get(3).getId(), is("wrapper"));
        assertThat(dl.getLifeCycles().get(4).getId(), is("etl"));
    }

    private Lifecycle getLifeCycleById(String id) {
        return defaultLifeCycles.getLifeCycles().stream()
                .filter(l -> id.equals(l.getId()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
