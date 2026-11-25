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
package org.apache.maven.impl;

import java.util.stream.IntStream;

import org.apache.maven.api.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MutableProjectTest {

    @Test
    void exposesInitialProject() {
        Project project = project(0);

        assertSame(project, new MutableProject(project).project());
    }

    @Test
    void rejectsNullProjectsAndUpdates() {
        assertThrows(NullPointerException.class, () -> new MutableProject(null));

        MutableProject project = new MutableProject(project(0));
        assertThrows(NullPointerException.class, () -> project.update(null));
        assertThrows(NullPointerException.class, () -> project.update(current -> null));
    }

    @Test
    void updatesAtomically() {
        MutableProject project = new MutableProject(project(0));

        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> project.update(current -> project(Integer.parseInt(current.version()) + 1)));

        assertEquals("100", project.project().version());
    }

    private static Project project(int version) {
        Project project = mock(Project.class);
        when(project.version()).thenReturn(Integer.toString(version));
        return project;
    }
}
