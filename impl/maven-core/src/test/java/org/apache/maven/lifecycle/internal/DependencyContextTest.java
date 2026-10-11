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
package org.apache.maven.lifecycle.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyContextTest {

    private final Set<String> compile = Set.of("compile");

    private final Set<String> test = Set.of("compile", "test");

    private final DependencyContext context = new DependencyContext(new MavenProject(), compile, compile);

    @Test
    void testResolutionIsRequiredUntilTheProjectWasResolved() {
        assertTrue(context.isResolutionRequiredForCurrentProject(compile, compile));
    }

    @Test
    void testResolutionIsSkippedForTheScopesTheProjectWasResolvedFor() {
        context.synchronizeWithProjectState(compile, compile);

        assertFalse(context.isResolutionRequiredForCurrentProject(compile, compile));
        assertFalse(
                context.isResolutionRequiredForCurrentProject(List.of("compile"), Collections.singleton("compile")));
    }

    @Test
    void testResolutionIsRequiredForOtherScopes() {
        context.synchronizeWithProjectState(compile, compile);

        assertTrue(context.isResolutionRequiredForCurrentProject(test, test));
        assertTrue(context.isResolutionRequiredForCurrentProject(compile, test));
    }

    @Test
    void testResolutionIsRequiredAgainAfterOtherScopes() {
        context.synchronizeWithProjectState(compile, compile);
        context.synchronizeWithProjectState(test, test);

        assertTrue(context.isResolutionRequiredForCurrentProject(compile, compile));
        assertFalse(context.isResolutionRequiredForCurrentProject(test, test));
    }

    @Test
    void testResolutionIsRequiredAfterAResolutionWithoutScopes() {
        context.synchronizeWithProjectState(compile, compile);
        context.synchronizeWithProjectState();

        assertTrue(context.isResolutionRequiredForCurrentProject(compile, compile));
    }
}
