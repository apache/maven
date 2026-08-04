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
package org.apache.maven.internal.build.incremental.impl;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultIncrementalContextStateTest {
    /** Temporary directory for tests. */
    @TempDir
    private Path temp;

    @Test
    void testRoundtrip() throws Exception {
        Path file = Files.createTempFile(temp, "", "");
        DefaultIncrementalContextState state = DefaultIncrementalContextState.withConfiguration(new HashMap<>());
        BasicFileAttributes attrs = DefaultIncrementalContext.readAttributes(file);
        state.putResource(file, new FileState(file, attrs.lastModifiedTime(), attrs.size()));

        Path stateFile = Files.createTempFile(temp, "", "");
        try (OutputStream os = Files.newOutputStream(stateFile)) {
            state.storeTo(os);
        }

        state = DefaultIncrementalContextState.loadFrom(stateFile);

        assertNotNull(state.getResource(file));
    }

    @Test
    void testStateDoesNotExist() throws Exception {
        DefaultIncrementalContextState state = DefaultIncrementalContextState.loadFrom(temp.resolve("does-not-exist"));
        assertTrue(state.configuration.isEmpty());
    }

    @Test
    void testEmptyState() throws Exception {
        Path stateFile = Files.createTempFile(temp, "", "");
        assertTrue(
                DefaultIncrementalContextState.loadFrom(stateFile).configuration.isEmpty());
    }

    @Test
    void testCorruptedState() throws Exception {
        Path corrupted = Files.createTempFile(temp, "", "");
        Files.write(corrupted, Collections.singletonList("test"), StandardOpenOption.APPEND);
        assertTrue(
                DefaultIncrementalContextState.loadFrom(corrupted).configuration.isEmpty());
    }

    @Test
    void testIncompatibleState() throws Exception {
        Path incompatible = Files.createTempFile(temp, "", "");
        ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(incompatible));
        oos.writeUTF("incompatible");
        oos.close();
        assertTrue(DefaultIncrementalContextState.loadFrom(incompatible)
                .configuration
                .isEmpty());
    }
}
