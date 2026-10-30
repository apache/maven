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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.lifecycle.internal.stub.LoggerStub;
import org.apache.maven.lifecycle.providers.lifecycle.CleanLifecycleProvider;
import org.apache.maven.lifecycle.providers.lifecycle.DefaultLifecycleProvider;
import org.apache.maven.lifecycle.providers.lifecycle.SiteLifecycleProvider;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Kristian Rosenvold
 */
@PlexusTest
public class DefaultLifecyclesTest {
    @Inject
    private DefaultLifecycles defaultLifeCycles;

    @Test
    public void testLifecycle() throws Exception {
        final List<Lifecycle> cycles = defaultLifeCycles.getLifeCycles();
        assertNotNull(cycles);
        final Lifecycle lifecycle0 = cycles.get(0);
        assertEquals("clean", lifecycle0.getId());
        final Lifecycle lifecycle1 = cycles.get(1);
        assertEquals("default", lifecycle1.getId());
        assertEquals(23, lifecycle1.getPhases().size());
    }

    @Test
    public void testSiteLifecycleDisabled() {
        Map<String, Lifecycle> all = new LinkedHashMap<>();
        for (String id :
                new String[] {CleanLifecycleProvider.NAME, DefaultLifecycleProvider.NAME, SiteLifecycleProvider.NAME}) {
            all.put(id, new Lifecycle(id, Collections.singletonList(id + "-phase"), Collections.emptyMap()));
        }
        DefaultLifecycles lifecycles = new DefaultLifecycles(all, new LoggerStub());

        assertEquals(3, lifecycles.getLifeCycles().size());

        System.setProperty("maven.site.lifecycle.disabled", "true");
        try {
            List<String> ids =
                    lifecycles.getLifeCycles().stream().map(Lifecycle::getId).collect(Collectors.toList());
            assertEquals(Arrays.asList(CleanLifecycleProvider.NAME, DefaultLifecycleProvider.NAME), ids);
            assertNull(lifecycles.get("site-phase"));
        } finally {
            System.clearProperty("maven.site.lifecycle.disabled");
        }
    }
}
