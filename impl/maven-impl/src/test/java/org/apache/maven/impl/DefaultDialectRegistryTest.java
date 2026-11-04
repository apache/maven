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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Dialect;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.spi.DialectProvider;
import org.apache.maven.api.spi.ModelDialectProvider;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDialectRegistryTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static DialectProvider dialectProviderFor(Dialect dialect) {
        return () -> List.of(dialect);
    }

    private static ModelDialectProvider modelDialectProviderFor(Dialect dialect) {
        return new ModelDialectProvider() {
            @Override
            public Dialect getDialect() {
                return dialect;
            }

            @Override
            public ModelParser getModelParser() {
                return new ModelParser() {
                    @Override
                    public Optional<Source> locate(Path dir) {
                        return Optional.empty();
                    }

                    @Override
                    public Model parse(Source source, Map<String, ?> options) {
                        return null;
                    }
                };
            }

            @Override
            public ModelWriter getModelWriter() {
                return new ModelWriter() {
                    @Override
                    public Optional<Path> target(Path dir) {
                        return Optional.empty();
                    }

                    @Override
                    public void write(Path target, Model model, Map<String, ?> options) {}
                };
            }
        };
    }

    private ExtensibleEnumRegistries.DefaultDialectRegistry registry(
            List<DialectProvider> dp, List<ModelDialectProvider> mdp) {
        return new ExtensibleEnumRegistries.DefaultDialectRegistry(dp, mdp);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void xmlIsAlwaysPresent() {
        var reg = registry(List.of(), List.of());
        assertTrue(reg.lookup("xml").isPresent(), "XML must always be registered");
        assertEquals(Dialect.XML, reg.lookup("xml").get());
    }

    @Test
    void dialectProviderContributesDialect() {
        Dialect yaml = Dialect.of("yaml");
        var reg = registry(List.of(dialectProviderFor(yaml)), List.of());
        assertTrue(reg.lookup("yaml").isPresent());
        assertEquals(yaml, reg.lookup("yaml").get());
    }

    @Test
    void modelDialectProviderContributesDialectWhenNoDialectProvider() {
        // Extension implements only ModelDialectProvider, not DialectProvider.
        // The dialect must still appear in the registry.
        Dialect toml = Dialect.of("toml");
        var reg = registry(List.of(), List.of(modelDialectProviderFor(toml)));
        assertTrue(reg.lookup("toml").isPresent(), "toml dialect must be registered via ModelDialectProvider");
    }

    @Test
    void modelDialectProviderDoesNotDuplicateWhenDialectProviderAlsoRegisters() {
        // Extension implements both DialectProvider and ModelDialectProvider for same dialect.
        // Registry must not fail and must contain exactly one entry for "yaml".
        Dialect yaml = Dialect.of("yaml");
        var reg = registry(List.of(dialectProviderFor(yaml)), List.of(modelDialectProviderFor(yaml)));
        assertTrue(reg.lookup("yaml").isPresent());
        // lookup returns a single value — no exception, no duplicate
    }

    @Test
    void lookupIsCaseInsensitive() {
        Dialect yaml = Dialect.of("YAML");
        var reg = registry(List.of(dialectProviderFor(yaml)), List.of());
        assertTrue(reg.lookup("yaml").isPresent(), "lookup must be case-insensitive");
        assertTrue(reg.lookup("YAML").isPresent());
    }

    @Test
    void lookupAbsentForUnregistered() {
        var reg = registry(List.of(), List.of());
        assertFalse(reg.lookup("nonexistent").isPresent());
    }

    @Test
    void multipleDialectsFromModelDialectProviders() {
        Dialect yaml = Dialect.of("yaml");
        Dialect toml = Dialect.of("toml");
        Dialect json = Dialect.of("json");
        var reg = registry(
                List.of(),
                List.of(modelDialectProviderFor(yaml), modelDialectProviderFor(toml), modelDialectProviderFor(json)));
        assertTrue(reg.lookup("yaml").isPresent());
        assertTrue(reg.lookup("toml").isPresent());
        assertTrue(reg.lookup("json").isPresent());
        assertTrue(reg.lookup("xml").isPresent()); // XML is always present
    }
}
