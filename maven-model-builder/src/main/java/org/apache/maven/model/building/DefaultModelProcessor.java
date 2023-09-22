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
package org.apache.maven.model.building;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.apache.maven.building.Source;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.locator.ModelLocator;
import org.eclipse.sisu.Typed;

/**
 *
 * Note: uses @Typed to limit the types it is available for injection to just ModelProcessor.
 *
 * This is because the ModelProcessor interface extends ModelLocator and ModelReader. If we
 * made this component available under all its interfaces then it could end up being injected
 * into itself leading to a stack overflow.
 *
 * A side effect of using @Typed is that it translates to explicit bindings in the container.
 * So instead of binding the component under a 'wildcard' key it is now bound with an explicit
 * key. Since this is a default component this will be a plain binding of ModelProcessor to
 * this implementation type, ie. no hint/name.
 *
 * This leads to a second side effect in that any @Inject request for just ModelProcessor in
 * the same injector is immediately matched to this explicit binding, which means extensions
 * cannot override this binding. This is because the lookup is always short-circuited in this
 * specific situation (plain @Inject request, and plain explicit binding for the same type.)
 *
 * The simplest solution is to use a custom @Named here so it isn't bound under the plain key.
 * This is only necessary for default components using @Typed that want to support overriding.
 *
 * As a non-default component this now gets a negative priority relative to other implementations
 * of the same interface. Since we want to allow overriding this doesn't matter in this case.
 * (if it did we could add @Priority of 0 to match the priority given to default components.)
 */
@Named("core-default")
@Singleton
@Typed(ModelProcessor.class)
public class DefaultModelProcessor implements ModelProcessor {

    private final Collection<ModelParser> modelParsers;
    private final ModelLocator modelLocator;
    private final ModelReader modelReader;

    @Inject
    public DefaultModelProcessor(
            Collection<ModelParser> modelParsers, ModelLocator modelLocator, ModelReader modelReader) {
        this.modelParsers = modelParsers;
        this.modelLocator = modelLocator;
        this.modelReader = modelReader;
    }

    @Override
    public File locatePom(File projectDirectory) {
        return locatePom(projectDirectory.toPath()).toFile();
    }

    public Path locatePom(Path projectDirectory) {
        // Note that the ModelProcessor#locatePom never returns null
        // while the ModelParser#locatePom needs to return an existing path!
        Path pom = modelParsers.stream()
                .map(m -> m.locate(projectDirectory)
                        .map(org.apache.maven.api.services.Source::getPath)
                        .orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(
                        () -> modelLocator.locatePom(projectDirectory.toFile()).toPath());
        if (!pom.equals(projectDirectory) && !pom.getParent().equals(projectDirectory)) {
            throw new IllegalArgumentException("The POM found does not belong to the given directory: " + pom);
        }
        return pom;
    }

    public File locateExistingPom(File projectDirectory) {
        Path path = locateExistingPom(projectDirectory.toPath());
        return path != null ? path.toFile() : null;
    }

    public Path locateExistingPom(Path projectDirectory) {
        // Note that the ModelProcessor#locatePom never returns null
        // while the ModelParser#locatePom needs to return an existing path!
        Path pom = modelParsers.stream()
                .map(m -> m.locate(projectDirectory).map(s -> s.getPath()).orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> {
                    File f = modelLocator.locateExistingPom(projectDirectory.toFile());
                    return f != null ? f.toPath() : null;
                });
        if (pom != null && !pom.equals(projectDirectory) && !pom.getParent().equals(projectDirectory)) {
            throw new IllegalArgumentException("The POM found does not belong to the given directory: " + pom);
        }
        return pom;
    }

    protected org.apache.maven.api.model.Model read(
            Path pomFile, InputStream input, Reader reader, Map<String, ?> options) throws IOException {
        Source source = (Source) options.get(ModelProcessor.SOURCE);
        if (pomFile == null && source instanceof org.apache.maven.building.FileSource) {
            pomFile = ((org.apache.maven.building.FileSource) source).getFile().toPath();
        }
        if (pomFile != null) {
            Path projectDirectory = pomFile.getParent();
            List<ModelParserException> exceptions = new ArrayList<>();
            for (ModelParser parser : modelParsers) {
                try {
                    Optional<Model> model = parser.locateAndParse(projectDirectory, options);
                    if (model.isPresent()) {
                        return model.get().withPomFile(pomFile);
                    }
                } catch (ModelParserException e) {
                    exceptions.add(e);
                }
            }
            try {
                return readXmlModel(pomFile, null, null, options);
            } catch (IOException e) {
                exceptions.forEach(e::addSuppressed);
                throw e;
            }
        } else {
            return readXmlModel(pomFile, input, reader, options);
        }
    }

    private org.apache.maven.api.model.Model readXmlModel(
            Path pomFile, InputStream input, Reader reader, Map<String, ?> options) throws IOException {
        if (pomFile != null) {
            return modelReader.read(pomFile.toFile(), options).getDelegate();
        } else if (input != null) {
            return modelReader.read(input, options).getDelegate();
        } else {
            return modelReader.read(reader, options).getDelegate();
        }
    }

    @Override
    public org.apache.maven.model.Model read(File file, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        Path path = file.toPath();
        org.apache.maven.api.model.Model model = read(path, null, null, options);
        return new org.apache.maven.model.Model(model);
    }

    @Override
    public org.apache.maven.model.Model read(InputStream input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");
        try (InputStream in = input) {
            org.apache.maven.api.model.Model model = read(null, in, null, options);
            return new org.apache.maven.model.Model(model);
        } catch (ModelParserException e) {
            throw new ModelParseException("Unable to read model: " + e, e.getLineNumber(), e.getColumnNumber(), e);
        }
    }

    @Override
    public org.apache.maven.model.Model read(Reader reader, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(reader, "reader cannot be null");
        try (Reader r = reader) {
            org.apache.maven.api.model.Model model = read(null, null, r, options);
            return new org.apache.maven.model.Model(model);
        }
    }
}
