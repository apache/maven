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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.InjectorImpl;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
@ResourceLock(SYSTEM_OUT)
class SisuDiDiagnosticsTest {
    private static final String MAVEN_DI_DEBUG = "maven.di.debug";

    @Test
    void bridgeProvidersAndLazyCollectionsObserveLateRegistrations() throws Exception {
        String previous = System.getProperty(MAVEN_DI_DEBUG);
        PrintStream output = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        LazyNative.CALLS.set(0);
        try (PrintStream sink = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setProperty(MAVEN_DI_DEBUG, "true");
            System.setOut(sink);
            DefaultPlexusContainer container =
                    new DefaultPlexusContainer(new DefaultContainerConfiguration(), new SisuDiBridgeModule(false) {
                        @Override
                        protected void configure() {
                            super.configure();
                            injector.bindImplicit(LazyNative.class);
                        }
                    });
            try {
                InjectorImpl injector = (InjectorImpl) container.lookup(Injector.class);
                assertEquals(0, LazyNative.CALLS.get());
                captured.reset();
                assertNotNull(container.lookup(NativeService.class, "native"));
                assertEquals(1, LazyNative.CALLS.get());
                assertEquals(1, blocks(captured));
                assertTrue(captured.toString(StandardCharsets.UTF_8).contains(LazyNative.class.getName()));

                List<NativeService> list = injector.getInstance(new Key<List<NativeService>>() {});
                Map<String, NativeService> map = injector.getInstance(new Key<Map<String, NativeService>>() {});
                captured.reset();
                injector.bindInstance(Extra.class, new Extra());
                assertNotNull(list.get(0));
                assertEquals(1, blocks(captured));
                assertTrue(captured.toString(StandardCharsets.UTF_8).contains(Extra.class.getName()));
                captured.reset();
                injector.bindInstance(Another.class, new Another());
                assertNotNull(map.get("native"));
                assertEquals(1, blocks(captured));

                captured.reset();
                injector.bindInstance(Extra.class, new Extra());
                Client client = new Client();
                injector.injectInstance(client);
                assertNotNull(client.service);
                assertEquals(1, blocks(captured));
            } finally {
                container.dispose();
            }
        } finally {
            System.setOut(output);
            if (previous == null) {
                System.clearProperty(MAVEN_DI_DEBUG);
            } else {
                System.setProperty(MAVEN_DI_DEBUG, previous);
            }
        }
    }

    private static long blocks(ByteArrayOutputStream output) {
        return output.toString(StandardCharsets.UTF_8)
                .lines()
                .filter(line -> line.startsWith("Maven DI native registry "))
                .count();
    }

    public interface NativeService {}

    @Named("native")
    public static class LazyNative implements NativeService {
        static final AtomicInteger CALLS = new AtomicInteger();

        LazyNative() {
            CALLS.incrementAndGet();
        }
    }

    static class Extra {}

    static class Another {}

    static class Client {
        @Inject
        NativeService service;
    }
}
