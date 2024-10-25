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
package org.apache.maven.di;

import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.apache.maven.internal.impl.SisuDiBridgeModule;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DiTest {

    // return true to run the test
    static boolean testShouldNotHaveDuplicates() {
        return true;
    }

    @Nested
    class DiTest1 {

        PlexusContainer container;

        @BeforeEach
        void setup() throws Exception {
            container = new DefaultPlexusContainer(
                    new DefaultContainerConfiguration(),
                    new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(ModelParser.class).to(TestModelParser.class);
                        }
                    },
                    new SisuDiBridgeModule(false));
        }

        @Test
        void testPlexus() throws Exception {
            List<ModelParser> parsers = container.lookupList(ModelParser.class);
            assertNotNull(parsers);
            assertEquals(1, parsers.size());
            Map<String, ModelParser> parsersMap = container.lookupMap(ModelParser.class);
            assertNotNull(parsersMap);
            assertEquals(1, parsersMap.size());
        }

        @Test
        void testGuice() throws Exception {
            List<Binding<ModelParser>> parsers =
                    container.lookup(Injector.class).findBindingsByType(TypeLiteral.get(ModelParser.class));
            assertNotNull(parsers);
            assertEquals(1, parsers.size());
        }

        @Test
        void testDI() throws Exception {
            DiInjected diInjected = new DiInjected();
            container.lookup(org.apache.maven.di.Injector.class).injectInstance(diInjected);
            assertNotNull(diInjected.parser);
            assertNotNull(diInjected.parsers);
            assertEquals(1, diInjected.parsers.size());
            assertNotNull(diInjected.parsersMap);
            assertEquals(1, diInjected.parsersMap.size());
        }

        static class DiInjected {
            @org.apache.maven.api.di.Inject
            ModelParser parser;

            @org.apache.maven.api.di.Inject
            List<ModelParser> parsers;

            @org.apache.maven.api.di.Inject
            Map<String, ModelParser> parsersMap;
        }

        @Named
        @Singleton
        static class TestModelParser implements ModelParser {
            @Override
            public Optional<Source> locate(Path dir) {
                return Optional.empty();
            }

            @Override
            public Model parse(Source source, Map<String, ?> options) throws ModelParserException {
                return null;
            }
        }
    }

    @Nested
    class DiTest2 {

        PlexusContainer container;

        @BeforeEach
        void setup() throws Exception {
            container = new DefaultPlexusContainer(new DefaultContainerConfiguration(), new SisuDiBridgeModule(false) {
                @Override
                protected void configure() {
                    super.configure();
                    injector.bindImplicit(TestModelParser.class);
                }
            });
        }

        @Test
        void testPlexus() throws Exception {
            List<ModelParser> parsers = container.lookupList(ModelParser.class);
            assertNotNull(parsers);
            assertEquals(1, parsers.size());
            Map<String, ModelParser> parsersMap = container.lookupMap(ModelParser.class);
            assertNotNull(parsersMap);
            assertEquals(1, parsersMap.size());
        }

        @Test
        void testGuice() throws Exception {
            List<Binding<ModelParser>> parsers2 =
                    container.lookup(Injector.class).findBindingsByType(TypeLiteral.get(ModelParser.class));
            assertNotNull(parsers2);
            assertEquals(1, parsers2.size());
        }

        @Test
        @EnabledIf("org.apache.maven.di.DiTest#testShouldNotHaveDuplicates")
        void testDI() throws Exception {
            DiInjected diInjected = new DiInjected();
            container.lookup(org.apache.maven.di.Injector.class).injectInstance(diInjected);
            assertNotNull(diInjected.parser);
            assertNotNull(diInjected.parsers);
            assertEquals(1, diInjected.parsers.size());
            assertNotNull(diInjected.parsersMap);
            assertEquals(1, diInjected.parsersMap.size());
        }

        static class DiInjected {
            @org.apache.maven.api.di.Inject
            ModelParser parser;

            @org.apache.maven.api.di.Inject
            List<ModelParser> parsers;

            @org.apache.maven.api.di.Inject
            Map<String, ModelParser> parsersMap;
        }

        @org.apache.maven.api.di.Named
        @org.apache.maven.api.di.Singleton
        static class TestModelParser implements ModelParser {
            @Override
            public Optional<Source> locate(Path dir) {
                return Optional.empty();
            }

            @Override
            public Model parse(Source source, Map<String, ?> options) throws ModelParserException {
                return null;
            }
        }
    }

    @Nested
    class DiTest3 {

        PlexusContainer container;

        @BeforeEach
        void setup() throws Exception {
            container = new DefaultPlexusContainer(new DefaultContainerConfiguration(), new SisuDiBridgeModule(false) {
                @Override
                protected void configure() {
                    super.configure();
                    injector.bindImplicit(TestModelParser.class);
                }
            });
        }

        @Test
        void testPlexus() throws Exception {
            List<ModelParser> parsers = container.lookupList(ModelParser.class);
            assertNotNull(parsers);
            assertEquals(1, parsers.size());
            Map<String, ModelParser> parsersMap = container.lookupMap(ModelParser.class);
            assertNotNull(parsersMap);
            assertEquals(1, parsersMap.size());
        }

        @Test
        void testGuice() throws Exception {
            List<Binding<ModelParser>> parsers =
                    container.lookup(Injector.class).findBindingsByType(TypeLiteral.get(ModelParser.class));
            assertNotNull(parsers);
            assertEquals(1, parsers.size());
        }

        @Test
        @EnabledIf("org.apache.maven.di.DiTest#testShouldNotHaveDuplicates")
        void testDI() throws Exception {
            DiInjected diInjected = new DiInjected();
            container.lookup(org.apache.maven.di.Injector.class).injectInstance(diInjected);
            assertNotNull(diInjected.parser);
            assertNotNull(diInjected.parsers);
            assertEquals(1, diInjected.parsers.size());
            assertNotNull(diInjected.parsersMap);
            assertEquals(1, diInjected.parsersMap.size());
        }

        static class DiInjected {
            @org.apache.maven.api.di.Inject
            ModelParser parser;

            @org.apache.maven.api.di.Inject
            List<ModelParser> parsers;

            @org.apache.maven.api.di.Inject
            Map<String, ModelParser> parsersMap;
        }

        @org.apache.maven.api.di.Named
        static class TestModelParser implements ModelParser {
            @Override
            public Optional<Source> locate(Path dir) {
                return Optional.empty();
            }

            @Override
            public Model parse(Source source, Map<String, ?> options) throws ModelParserException {
                return null;
            }
        }
    }
}
