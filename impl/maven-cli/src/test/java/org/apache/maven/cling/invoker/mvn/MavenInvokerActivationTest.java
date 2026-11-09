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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.execution.ProfileActivation;
import org.apache.maven.execution.ProjectActivation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * How {@code -P} and {@code -pl} tokens are split into active, inactive and optional entries.
 */
class MavenInvokerActivationTest {

    private static MavenContext context(MavenOptions options) {
        MavenContext context = mock(MavenContext.class);
        when(context.options()).thenReturn(options);
        return context;
    }

    @Test
    void profilePrefixesAreReadAfterTrimming() {
        MavenOptions options = mock(MavenOptions.class);
        when(options.activatedProfiles()).thenReturn(Optional.of(List.of("a, +b, -c, !d, +?e, -?f")));
        ProfileActivation activation = new ProfileActivation();

        new MavenInvoker(mock(Lookup.class), null).performProfileActivation(context(options), activation);

        assertEquals(Set.of("a", "b"), activation.getRequiredActiveProfileIds());
        assertEquals(Set.of("e"), activation.getOptionalActiveProfileIds());
        assertEquals(Set.of("c", "d"), activation.getRequiredInactiveProfileIds());
        assertEquals(Set.of("f"), activation.getOptionalInactiveProfileIds());
    }

    @Test
    void projectPrefixesAreReadAfterTrimming() {
        MavenOptions options = mock(MavenOptions.class);
        when(options.projects()).thenReturn(Optional.of(List.of(":a, +:b, -:c, !:d, +?:e")));
        ProjectActivation activation = new ProjectActivation();

        new MavenInvoker(mock(Lookup.class), null).performProjectActivation(context(options), activation);

        assertEquals(Set.of(":a", ":b"), activation.getRequiredActiveProjectSelectors());
        assertEquals(Set.of(":e"), activation.getOptionalActiveProjectSelectors());
        assertEquals(Set.of(":c", ":d"), activation.getRequiredInactiveProjectSelectors());
    }
}
