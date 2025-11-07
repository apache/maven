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
package org.apache.maven.internal.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.services.TempFileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for DefaultTempFileService.
 */
class DefaultTempFileServiceTest {

    private static final String KEEP_PROP = "maven.tempfile.keep";

    @AfterEach
    void clearKeepProp() {
        System.clearProperty(KEEP_PROP);
    }

    @Test
    void createsFilesAndDirectoriesAndCleansThemUp() throws IOException {
        final TempFileService svc = new DefaultTempFileService();
        final SessionData data = new MapBackedSessionData();
        final Session session = mock(Session.class);
        when(session.getData()).thenReturn(data);

        final Path f1 = svc.createTempFile(session, "tfs-", ".bin");
        final Path d1 = svc.createTempDirectory(session, "tfs-");
        final Path nested = Files.createTempFile(d1, "inner-", ".tmp");

        assertTrue(Files.exists(f1), "temp file must exist");
        assertTrue(Files.exists(d1), "temp dir must exist");
        assertTrue(Files.exists(nested), "nested file must exist");

        svc.cleanup(session);

        assertFalse(Files.exists(f1), "temp file must be deleted");
        assertFalse(Files.exists(d1), "temp dir tree must be deleted");
        assertFalse(Files.exists(nested), "nested file must be deleted");
    }

    @Test
    void registerExternalPathIsAlsoDeleted() throws IOException {
        final TempFileService svc = new DefaultTempFileService();
        final SessionData data = new MapBackedSessionData();
        final Session session = mock(Session.class);
        when(session.getData()).thenReturn(data);

        final Path externalDir = Files.createTempDirectory("ext-");
        final Path nested = Files.createTempFile(externalDir, "ext-inner-", ".tmp");
        assertTrue(Files.exists(externalDir));
        assertTrue(Files.exists(nested));

        svc.register(session, externalDir);
        svc.cleanup(session);

        assertFalse(Files.exists(externalDir), "registered external dir must be deleted recursively");
        assertFalse(Files.exists(nested));
    }

    @Test
    void keepPropertySkipsCleanup() throws IOException {
        final TempFileService svc = new DefaultTempFileService();
        final SessionData data = new MapBackedSessionData();
        final Session session = mock(Session.class);
        when(session.getData()).thenReturn(data);

        final Path f = svc.createTempFile(session, "keep-", ".tmp");
        assertTrue(Files.exists(f));

        System.setProperty(KEEP_PROP, "true");
        svc.cleanup(session);

        assertTrue(Files.exists(f), "cleanup must be skipped when -Dmaven.tempfile.keep=true");

        // turn cleanup back on and verify it deletes
        System.clearProperty(KEEP_PROP);
        svc.cleanup(session);
        assertFalse(Files.exists(f));
    }

    /**
     * Minimal, thread-safe SessionData backed by a ConcurrentHashMap.
     * Keeps generics safe at the call sites of DefaultTempFileService.
     */
    static final class MapBackedSessionData implements SessionData {
        private final ConcurrentHashMap<Key<?>, Object> map = new ConcurrentHashMap<>();

        @Override
        public <T> void set(final Key<T> key, final T value) {
            Objects.requireNonNull(key, "key");
            if (value == null) {
                map.remove(key);
            } else {
                map.put(key, value);
            }
        }

        @Override
        public <T> boolean replace(final Key<T> key, final T oldValue, final T newValue) {
            Objects.requireNonNull(key, "key");
            if (newValue == null) {
                return map.remove(key, oldValue);
            }
            return map.replace(key, oldValue, newValue);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(final Key<T> key) {
            Objects.requireNonNull(key, "key");
            return (T) map.get(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T computeIfAbsent(final Key<T> key, final Supplier<T> supplier) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(supplier, "supplier");
            return (T) map.computeIfAbsent(key, k -> supplier.get());
        }
    }
}
