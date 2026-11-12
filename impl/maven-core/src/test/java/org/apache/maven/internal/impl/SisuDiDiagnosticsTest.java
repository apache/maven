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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import org.apache.maven.api.Constants;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.InjectorImpl;
import org.apache.maven.impl.DefaultMessageBuilder;
import org.apache.maven.impl.DefaultMessageBuilderFactory;
import org.apache.maven.logging.impl.DefaultLoggerManager;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.sisu.inject.BeanLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
class SisuDiDiagnosticsTest {
    private static final String MAVEN_DI_DEBUG = Constants.MAVEN_DI_DEBUG;

    @Test
    void indexedContainersStillDiscoverTheNativeBridge() throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        ClassLoader loader = new ClassLoader(previous) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return name.equals("META-INF/maven/org.apache.maven.api.di.Inject")
                        ? Collections.emptyEnumeration()
                        : super.getResources(name);
            }
        };
        Thread.currentThread().setContextClassLoader(loader);
        try {
            DefaultPlexusContainer container = new DefaultPlexusContainer(new DefaultContainerConfiguration()
                    .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                    .setAutoWiring(true));
            try {
                assertNotNull(container.lookup(Injector.class));
            } finally {
                container.dispose();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    @Test
    void explicitBridgeKeepsItsFactoryWhenTheIndexAlsoContainsTheModule() throws Exception {
        MessageBuilderFactory factory = new DefaultMessageBuilderFactory() {
            @Override
            public boolean isColorEnabled() {
                return true;
            }

            @Override
            public MessageBuilder builder() {
                return new DefaultMessageBuilder().a("request:");
            }
        };
        DefaultPlexusContainer container = new DefaultPlexusContainer(
                new DefaultContainerConfiguration()
                        .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                        .setAutoWiring(true),
                new SisuDiBridgeModule(false, factory));
        try {
            InjectorImpl injector = (InjectorImpl) container.lookup(Injector.class);
            assertSame(injector, container.lookup(SisuDiBridgeModule.class).injector);
            injector.bindInstance(String.class, "fixture");
            assertTrue(injector.dumpBindings().contains("request:java.lang.String"));
        } finally {
            container.dispose();
        }
    }

    @Test
    void bridgeProvidersAndLazyCollectionsObserveLateRegistrations() throws Exception {
        String previous = System.getProperty(MAVEN_DI_DEBUG);
        List<String> captured = new ArrayList<>();
        LogSink capturingSink = event -> captured.add(event.message());
        DefaultLoggerManager manager = new DefaultLoggerManager();
        manager.setLogSink(capturingSink);
        manager.install();
        LazyNative.CALLS.set(0);
        try {
            System.setProperty(MAVEN_DI_DEBUG, "true");
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
                captured.clear();
                assertNotNull(container.lookup(NativeService.class, "native"));
                assertEquals(1, LazyNative.CALLS.get());
                assertEquals(1, blocks(captured));
                assertTrue(captured.stream().anyMatch(m -> m.contains(LazyNative.class.getName())));

                List<NativeService> list = injector.getInstance(new Key<List<NativeService>>() {});
                Map<String, NativeService> map = injector.getInstance(new Key<Map<String, NativeService>>() {});
                captured.clear();
                injector.bindInstance(Extra.class, new Extra());
                assertNotNull(list.get(0));
                assertEquals(1, blocks(captured));
                assertTrue(captured.stream().anyMatch(m -> m.contains(Extra.class.getName())));
                captured.clear();
                injector.bindInstance(Another.class, new Another());
                assertNotNull(map.get("native"));
                assertEquals(1, blocks(captured));
                assertTrue(captured.stream().anyMatch(m -> m.contains(Another.class.getName())));

                captured.clear();
                injector.bindInstance(Extra.class, new Extra());
                Client client = new Client();
                injector.injectInstance(client);
                assertNotNull(client.service);
                assertEquals(1, blocks(captured));
                assertTrue(captured.stream().anyMatch(m -> m.contains("+ candidate: " + Extra.class.getName())));
            } finally {
                container.dispose();
            }
        } finally {
            manager.uninstall();
            if (previous == null) {
                System.clearProperty(MAVEN_DI_DEBUG);
            } else {
                System.setProperty(MAVEN_DI_DEBUG, previous);
            }
        }
    }

    @Test
    void bridgeUsesEachRequestsFactoryBeforeDiscoveryCompletes(@TempDir Path directory) throws Exception {
        Path index = directory.resolve("META-INF/maven/org.apache.maven.api.di.Inject");
        Files.createDirectories(index.getParent());
        Files.writeString(index, LazyNative.class.getName() + "\nmissing.NativeComponent\n");
        ClassLoader previousLoader = Thread.currentThread().getContextClassLoader();
        String previous = System.getProperty(MAVEN_DI_DEBUG);
        try (URLClassLoader loader =
                new URLClassLoader(new URL[] {directory.toUri().toURL()}, previousLoader) {
                    @Override
                    public Enumeration<URL> getResources(String name) throws IOException {
                        return findResources(name);
                    }
                }) {
            Thread.currentThread().setContextClassLoader(loader);
            System.setProperty(MAVEN_DI_DEBUG, "true");
            for (String tag : List.of("first", "second")) {
                MessageBuilderFactory factory = new DefaultMessageBuilderFactory() {
                    @Override
                    public boolean isColorEnabled() {
                        return true;
                    }

                    @Override
                    public MessageBuilder builder() {
                        return new DefaultMessageBuilder().a(tag);
                    }
                };
                List<String> captured = new ArrayList<>();
                LogSink capturingSink = event -> captured.add(event.message());
                DefaultLoggerManager manager = new DefaultLoggerManager();
                manager.setLogSink(capturingSink);
                manager.install();
                LazyNative.CALLS.set(0);
                try {
                    SisuDiBridgeModule module = new SisuDiBridgeModule(true, factory);
                    assertThrows(CreationException.class, () -> bridge(factory, module));
                    assertTrue(captured.stream().anyMatch(m -> m.contains(", partial")), captured.toString());
                    assertTrue(
                            captured.stream().anyMatch(m -> m.contains(tag + LazyNative.class.getName())),
                            captured.toString());
                    assertEquals(0, LazyNative.CALLS.get());
                } finally {
                    manager.uninstall();
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previousLoader);
            if (previous == null) {
                System.clearProperty(MAVEN_DI_DEBUG);
            } else {
                System.setProperty(MAVEN_DI_DEBUG, previous);
            }
        }
    }

    @Test
    void standaloneBridgeStaysPlainEvenWhenTheContainerHasAFactory() {
        MessageBuilderFactory factory = new DefaultMessageBuilderFactory() {
            @Override
            public boolean isColorEnabled() {
                return true;
            }

            @Override
            public MessageBuilder builder() {
                throw new AssertionError("standalone bridge used the container's factory");
            }
        };
        var plainContainer = bridge(factory, new SisuDiBridgeModule(false));
        InjectorImpl plain = (InjectorImpl) plainContainer.getInstance(Injector.class);
        plain.bindInstance(String.class, "fixture");
        assertTrue(plain.dumpBindings().contains("key: java.lang.String [loader=bootstrap]"));
    }

    private static com.google.inject.Injector bridge(MessageBuilderFactory factory, SisuDiBridgeModule module) {
        return Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PlexusContainer.class).toProvider(() -> null);
                        bind(BeanLocator.class).toProvider(() -> null);
                        bind(MessageBuilderFactory.class).toInstance(factory);
                    }
                },
                module);
    }

    private static long blocks(List<String> messages) {
        return messages.stream()
                .filter(m -> m.startsWith("Maven DI native registry "))
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
