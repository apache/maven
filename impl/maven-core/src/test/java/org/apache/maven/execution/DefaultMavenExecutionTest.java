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
package org.apache.maven.execution;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
class DefaultMavenExecutionTest {
    @Test
    void testCopyDefault() {
        MavenExecutionRequest original = new DefaultMavenExecutionRequest();
        MavenExecutionRequest copy = DefaultMavenExecutionRequest.copy(original);
        assertNotNull(copy);
        assertNotSame(copy, original);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updateAllIsPreservedWhenCopyingRequest(boolean updateAll) {
        MavenExecutionRequest original = new DefaultMavenExecutionRequest();
        assertFalse(original.isUpdateAll());
        assertSame(original, original.setUpdateAll(updateAll));
        assertEquals(updateAll, original.isUpdateAll());

        MavenExecutionRequest copy = DefaultMavenExecutionRequest.copy(original);
        assertEquals(updateAll, copy.isUpdateAll());
        copy.setUpdateAll(!updateAll);
        assertEquals(updateAll, original.isUpdateAll());
        assertEquals(!updateAll, copy.isUpdateAll());
    }

    @SuppressWarnings("deprecation")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deprecatedUpdateMethodsShareTheUpdateAllFlag(boolean updateAll) {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        assertFalse(request.isUpdateSnapshots());
        assertSame(request, request.setUpdateSnapshots(updateAll));
        assertEquals(updateAll, request.isUpdateAll());
        assertEquals(updateAll, request.isUpdateSnapshots());

        MavenExecutionRequest copy = DefaultMavenExecutionRequest.copy(request);
        assertEquals(updateAll, copy.isUpdateAll());
        assertEquals(updateAll, copy.isUpdateSnapshots());

        assertSame(request, request.setUpdateAll(!updateAll));
        assertEquals(!updateAll, request.isUpdateSnapshots());
        assertEquals(updateAll, copy.isUpdateSnapshots());
    }

    @SuppressWarnings("deprecation")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updateAllSupportsExistingRequestImplementations(boolean updateAll) {
        MavenExecutionRequest request = mock(MavenExecutionRequest.class, CALLS_REAL_METHODS);
        when(request.setUpdateSnapshots(updateAll)).thenReturn(request);
        when(request.isUpdateSnapshots()).thenReturn(updateAll);

        assertSame(request, request.setUpdateAll(updateAll));
        assertEquals(updateAll, request.isUpdateAll());
    }

    @Test
    void testResultWithNullTopologicallySortedProjectsIsEmptyList() {
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.setTopologicallySortedProjects(null);
        List<MavenProject> projects = result.getTopologicallySortedProjects();
        assertNotNull(projects);
        assertTrue(
                projects.isEmpty(),
                "Expected collection to be empty but had " + projects.size() + " elements: " + projects);
    }
}
