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

import javax.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.maven.logging.OutputCapabilities;
import org.apache.maven.logging.OutputCapabilities.Destination;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@PlexusTest
class DefaultOutputCapabilitiesTest {
    @Inject
    private OutputCapabilities capabilities;

    @Inject
    private PlexusContainer container;

    @Test
    void injectableSingletonWithoutSession() throws Exception {
        assertSame(capabilities, container.lookup(OutputCapabilities.class));
        assertEquals(Destination.UNKNOWN, capabilities.getDestination());
        assertEquals(Optional.empty(), capabilities.getEncoding());
    }

    @Test
    void updatesAreVisibleToParallelClientsAndRestoredOnClose() throws Exception {
        DefaultOutputCapabilities component = (DefaultOutputCapabilities) capabilities;
        try (AutoCloseable cleanup =
                component.install(DefaultOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8))) {
            assertEquals(
                    Destination.FILE,
                    CompletableFuture.supplyAsync(capabilities::getDestination).get(10, TimeUnit.SECONDS));
            assertEquals(
                    Optional.of(StandardCharsets.UTF_8),
                    CompletableFuture.supplyAsync(capabilities::getEncoding).get(10, TimeUnit.SECONDS));
            try (AutoCloseable nested = component.install(
                    DefaultOutputCapabilities.snapshot(Destination.CONSOLE, StandardCharsets.ISO_8859_1))) {
                assertEquals(Destination.CONSOLE, capabilities.getDestination());
                assertEquals(Optional.of(StandardCharsets.ISO_8859_1), capabilities.getEncoding());
            }
            assertEquals(Destination.FILE, capabilities.getDestination());
            assertEquals(Optional.of(StandardCharsets.UTF_8), capabilities.getEncoding());
        }
        assertEquals(Destination.UNKNOWN, capabilities.getDestination());
        assertEquals(Optional.empty(), capabilities.getEncoding());
    }

    @Test
    void unknownDestinationCanHaveKnownEncoding() throws Exception {
        try (AutoCloseable cleanup = ((DefaultOutputCapabilities) capabilities)
                .install(DefaultOutputCapabilities.snapshot(Destination.UNKNOWN, StandardCharsets.UTF_8))) {
            assertEquals(Destination.UNKNOWN, capabilities.getDestination());
            assertEquals(Optional.of(StandardCharsets.UTF_8), capabilities.getEncoding());
        }
    }

    @Test
    void repeatedCleanupDoesNotResetNextInvocation() throws Exception {
        DefaultOutputCapabilities component = (DefaultOutputCapabilities) capabilities;
        AutoCloseable first =
                component.install(DefaultOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8));
        first.close();
        try (AutoCloseable second = component.install(
                DefaultOutputCapabilities.snapshot(Destination.REDIRECTED, StandardCharsets.US_ASCII))) {
            first.close();
            assertEquals(Destination.REDIRECTED, capabilities.getDestination());
            assertEquals(Optional.of(StandardCharsets.US_ASCII), capabilities.getEncoding());
        }
        assertEquals(Destination.UNKNOWN, capabilities.getDestination());
    }
}
