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
package org.apache.maven.internal.impl.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.api.Dialect;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelDialectManager;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.spi.ModelDialectProvider;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.apache.maven.api.spi.ModelWriter;
import org.apache.maven.api.spi.ModelWriterException;

@Named
@Singleton
public class DefaultModelDialectManager implements ModelDialectManager {
    private final Map<Dialect, ModelDialectProvider> dialectProviders;

    @Inject
    public DefaultModelDialectManager(
            ModelXmlFactory modelXmlFactory, List<ModelDialectProvider> modelDialectProviders) {
        this.dialectProviders = new HashMap<>();
        // XML
        dialectProviders.put(Dialect.XML, getXmlDialectProvider(modelXmlFactory));
        // SPI TODO: (they can override XML dialect as well?)
        for (ModelDialectProvider modelDialectProvider : modelDialectProviders) {
            dialectProviders.put(modelDialectProvider.getDialect(), modelDialectProvider);
        }
    }

    private ModelDialectProvider getXmlDialectProvider(ModelXmlFactory modelXmlFactory) {
        ModelParser xmlModelParser = new ModelParser() {
            @Override
            public Optional<Source> locate(Path dir) {
                if (Files.isDirectory(dir)) {
                    Path pom = dir.resolve("pom.xml");
                    if (Files.isRegularFile(pom)) {
                        return Optional.of(Source.fromPath(pom));
                    }
                }
                return Optional.empty();
            }

            @Override
            public Model parse(Source source, Map<String, ?> options) throws ModelParserException {
                return modelXmlFactory.read(source.getPath(), false);
            }
        };
        ModelWriter xmlModelWriter = new ModelWriter() {
            @Override
            public Optional<Path> target(Path dir) {
                if (Files.isDirectory(dir)) {
                    Path pom = dir.resolve("pom.xml");
                    if (!Files.isDirectory(pom)) {
                        return Optional.of(pom);
                    }
                }
                return Optional.empty();
            }

            @Override
            public void write(Path target, Model model, Map<String, ?> options) throws ModelWriterException {
                modelXmlFactory.write(model, target);
            }
        };
        return new ModelDialectProvider() {
            @Override
            public Dialect getDialect() {
                return Dialect.XML;
            }

            @Override
            public ModelParser getModelParser() {
                return xmlModelParser;
            }

            @Override
            public ModelWriter getModelWriter() {
                return xmlModelWriter;
            }
        };
    }

    @Override
    public Set<Dialect> getAvailableDialects() {
        return Set.copyOf(dialectProviders.keySet());
    }

    @Override
    public Optional<Model> readModel(Path dir, Dialect dialect, Map<String, ?> options) {
        return requireModelDialectProvider(dialect).getModelParser().locateAndParse(dir, options);
    }

    @Override
    public Optional<Path> writeModel(Path dir, Dialect dialect, Model model, Map<String, ?> options) {
        return requireModelDialectProvider(dialect).getModelWriter().targetAndWrite(dir, model, options);
    }

    @Nonnull
    private ModelDialectProvider requireModelDialectProvider(Dialect dialect) {
        ModelDialectProvider dialectProvider = dialectProviders.get(dialect);
        if (dialectProvider == null) {
            throw new IllegalArgumentException("Unknown model dialect " + dialect);
        }
        return dialectProvider;
    }
}
