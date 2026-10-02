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
package org.apache.maven.logging.internal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.maven.logging.OutputCapabilities.Destination;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutputCapabilitiesMapTest {
    private final DefaultOutputCapabilities component = new DefaultOutputCapabilities();

    @Test
    void destinationsAndCanonicalEncodingAreIndependent() throws Exception {
        Map<String, String> initial = component.asMap();
        assertEquals(Collections.singletonMap("destination", "UNKNOWN"), initial);
        for (Destination destination : Destination.values()) {
            try (AutoCloseable cleanup = component.install(DefaultOutputCapabilities.snapshot(destination, null))) {
                assertEquals(Collections.singletonMap("destination", destination.name()), component.asMap());
                assertFalse(component.asMap().containsKey("encoding"));
            }
            try (AutoCloseable cleanup =
                    component.install(DefaultOutputCapabilities.snapshot(destination, Charset.forName("latin1")))) {
                Map<String, String> captured = component.asMap();
                assertEquals(destination.name(), captured.get("destination"));
                assertEquals("ISO-8859-1", captured.get("encoding"));
                assertEquals(2, captured.size());
                assertEquals(Collections.singletonMap("destination", "UNKNOWN"), initial);
            }
        }
        assertSame(initial, component.asMap());
    }

    @Test
    void retainedMapsAndCollectionsSurviveReconfigurationAndCleanup() throws Exception {
        Map<String, String> initial = component.asMap();
        Map<String, String> file;
        Set<Map.Entry<String, String>> entries;
        Set<String> keys;
        Collection<String> values;
        Iterator<Map.Entry<String, String>> iterator;
        try (AutoCloseable cleanup =
                component.install(DefaultOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8))) {
            file = component.asMap();
            entries = file.entrySet();
            keys = file.keySet();
            values = file.values();
            iterator = entries.iterator();
            assertNotSame(initial, file);
            try (AutoCloseable nested = component.install(
                    DefaultOutputCapabilities.snapshot(Destination.CONSOLE, StandardCharsets.ISO_8859_1))) {
                assertEquals("CONSOLE", component.asMap().get("destination"));
                assertEquals("ISO-8859-1", component.asMap().get("encoding"));
                assertEquals(Arrays.asList("FILE", "UTF-8"), new ArrayList<>(values));
            }
            assertSame(file, component.asMap());
        }
        assertSame(initial, component.asMap());
        assertEquals(Collections.singletonMap("destination", "UNKNOWN"), initial);
        assertEquals("FILE", file.get("destination"));
        assertEquals("UTF-8", file.get("encoding"));
        assertEquals(file.entrySet(), entries);
        assertEquals(2, entries.size());
        assertEquals(Arrays.asList("destination", "encoding"), new ArrayList<>(keys));
        assertEquals(Arrays.asList("FILE", "UTF-8"), new ArrayList<>(values));
        Map<String, String> fromIterator = new HashMap<>();
        iterator.forEachRemaining(entry -> fromIterator.put(entry.getKey(), entry.getValue()));
        assertEquals(file, fromIterator);
    }

    @Test
    void mutationIsRejectedThroughMapCollectionsAndEntries() throws Exception {
        try (AutoCloseable cleanup =
                component.install(DefaultOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8))) {
            Map<String, String> captured = component.asMap();
            assertThrows(UnsupportedOperationException.class, () -> captured.put("destination", "CONSOLE"));
            assertThrows(
                    UnsupportedOperationException.class, () -> captured.compute("encoding", (key, value) -> "ASCII"));
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> captured.entrySet().iterator().next().setValue("changed"));
            for (Collection<?> collection : Arrays.asList(captured.entrySet(), captured.keySet(), captured.values())) {
                assertThrows(UnsupportedOperationException.class, collection::clear);
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> collection.remove(collection.iterator().next()));
                Iterator<?> iterator = collection.iterator();
                iterator.next();
                assertThrows(UnsupportedOperationException.class, iterator::remove);
            }
        }
    }

    @Test
    void failureAndRepeatedCleanupDoNotChangeCapturedMaps() throws Exception {
        AutoCloseable first =
                component.install(DefaultOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8));
        Map<String, String> file = component.asMap();
        first.close();
        Map<String, String> redirected;
        List<Map<String, String>> failed = new ArrayList<>();
        try (AutoCloseable second = component.install(
                DefaultOutputCapabilities.snapshot(Destination.REDIRECTED, StandardCharsets.US_ASCII))) {
            redirected = component.asMap();
            first.close();
            assertSame(redirected, component.asMap());
            assertThrows(IllegalStateException.class, () -> {
                try (AutoCloseable nested = component.install(
                        DefaultOutputCapabilities.snapshot(Destination.CONSOLE, StandardCharsets.ISO_8859_1))) {
                    failed.add(component.asMap());
                    throw new IllegalStateException("logging setup failed");
                }
            });
            assertSame(redirected, component.asMap());
        }
        assertEquals(Collections.singletonMap("destination", "UNKNOWN"), component.asMap());
        assertEquals("FILE", file.get("destination"));
        assertEquals("UTF-8", file.get("encoding"));
        assertEquals("REDIRECTED", redirected.get("destination"));
        assertEquals("US-ASCII", redirected.get("encoding"));
        assertEquals("CONSOLE", failed.get(0).get("destination"));
        assertEquals("ISO-8859-1", failed.get(0).get("encoding"));
    }

    @Test
    void parallelReadersRetainTheirOriginalInformation() throws Exception {
        Map<String, String> file;
        try (AutoCloseable cleanup =
                component.install(DefaultOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8))) {
            file = component.asMap();
        }
        CompletableFuture<Void> ready = new CompletableFuture<>();
        List<CompletableFuture<Map<String, String>>> readers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            readers.add(ready.thenApplyAsync(ignored -> new HashMap<>(file)));
        }
        try (AutoCloseable cleanup = component.install(
                DefaultOutputCapabilities.snapshot(Destination.CONSOLE, StandardCharsets.ISO_8859_1))) {
            ready.complete(null);
            for (CompletableFuture<Map<String, String>> reader : readers) {
                Map<String, String> copy = reader.get(10, TimeUnit.SECONDS);
                assertEquals("FILE", copy.get("destination"));
                assertEquals("UTF-8", copy.get("encoding"));
            }
            assertEquals("CONSOLE", component.asMap().get("destination"));
            assertEquals("ISO-8859-1", component.asMap().get("encoding"));
        }
    }
}
