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

import org.apache.maven.api.services.OutputCapabilities;
import org.apache.maven.api.services.OutputCapabilities.Destination;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@PlexusTest
class CliOutputCapabilitiesTest {
    @Inject
    private OutputCapabilities capabilities;

    @Inject
    private PlexusContainer container;

    @Test
    void injectableSingletonWithoutSession() throws Exception {
        assertSame(capabilities, container.lookup(OutputCapabilities.class));
        assertEquals(Optional.empty(), capabilities.getDestination());
        assertEquals(Optional.empty(), capabilities.getEncoding());
    }

    @Test
    void availableThroughMaven4LookupWithoutSession() throws Exception {
        assertSame(
                capabilities,
                container.lookup(org.apache.maven.di.Injector.class).getInstance(CliOutputCapabilities.class));
        assertSame(
                capabilities,
                container.lookup(org.apache.maven.api.services.Lookup.class).lookup(OutputCapabilities.class));
        assertSame(
                capabilities,
                container.lookup(org.apache.maven.di.Injector.class).getInstance(OutputCapabilities.class));
    }

    @Test
    void updatesAreVisibleToParallelClientsAndRestoredOnClose() throws Exception {
        CliOutputCapabilities component = (CliOutputCapabilities) capabilities;
        try (AutoCloseable cleanup =
                component.install(CliOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8, null))) {
            assertEquals(
                    Optional.of(Destination.FILE),
                    CompletableFuture.supplyAsync(capabilities::getDestination).get(10, TimeUnit.SECONDS));
            assertEquals(
                    Optional.of(StandardCharsets.UTF_8),
                    CompletableFuture.supplyAsync(capabilities::getEncoding).get(10, TimeUnit.SECONDS));
            try (AutoCloseable nested = component.install(
                    CliOutputCapabilities.snapshot(Destination.CONSOLE, StandardCharsets.ISO_8859_1, null))) {
                assertEquals(Optional.of(Destination.CONSOLE), capabilities.getDestination());
                assertEquals(Optional.of(StandardCharsets.ISO_8859_1), capabilities.getEncoding());
            }
            assertEquals(Optional.of(Destination.FILE), capabilities.getDestination());
            assertEquals(Optional.of(StandardCharsets.UTF_8), capabilities.getEncoding());
        }
        assertEquals(Optional.empty(), capabilities.getDestination());
        assertEquals(Optional.empty(), capabilities.getEncoding());
    }

    @Test
    void unknownDestinationCanHaveKnownEncoding() throws Exception {
        try (AutoCloseable cleanup = ((CliOutputCapabilities) capabilities)
                .install(CliOutputCapabilities.snapshot(null, StandardCharsets.UTF_8, null))) {
            assertEquals(Optional.empty(), capabilities.getDestination());
            assertEquals(Optional.of(StandardCharsets.UTF_8), capabilities.getEncoding());
        }
    }

    @Test
    void outOfOrderCleanupSkipsClosedInstallations() throws Exception {
        CliOutputCapabilities component = (CliOutputCapabilities) capabilities;
        AutoCloseable first = component.install(CliOutputCapabilities.snapshot(
                Destination.FILE, StandardCharsets.UTF_8, OutputCapabilities.Format.HUMAN_READABLE));
        AutoCloseable second = component.install(CliOutputCapabilities.snapshot(
                Destination.CONSOLE, StandardCharsets.US_ASCII, OutputCapabilities.Format.MACHINE_READABLE));
        try {
            first.close();
            assertEquals(Optional.of(Destination.CONSOLE), component.getDestination());
            assertEquals(Optional.of(OutputCapabilities.Format.MACHINE_READABLE), component.getFormat());
            second.close();
            assertEquals(Optional.empty(), component.getDestination());
            assertEquals(Optional.empty(), component.getEncoding());
            assertEquals(Optional.empty(), component.getFormat());
            first.close();
            second.close();
            assertEquals(Optional.empty(), component.getFormat());
        } finally {
            second.close();
            first.close();
        }
    }

    @Test
    void repeatedCleanupDoesNotResetNextInvocation() throws Exception {
        CliOutputCapabilities component = (CliOutputCapabilities) capabilities;
        AutoCloseable first =
                component.install(CliOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8, null));
        first.close();
        try (AutoCloseable second = component.install(
                CliOutputCapabilities.snapshot(Destination.REDIRECTED, StandardCharsets.US_ASCII, null))) {
            first.close();
            assertEquals(Optional.of(Destination.REDIRECTED), capabilities.getDestination());
            assertEquals(Optional.of(StandardCharsets.US_ASCII), capabilities.getEncoding());
        }
        assertEquals(Optional.empty(), capabilities.getDestination());
    }
}
