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
package org.apache.maven.impl.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.api.spi.ModelParser.STRICT;

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
 * key. Since this is a default component; this will be a plain binding of ModelProcessor to
 * this implementation type; that is, no hint/name.
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
@Named
@Singleton
public class DefaultModelProcessor implements ModelProcessor {

    private final ModelXmlFactory modelXmlFactory;
    private final List<ModelParser> modelParsers;

    @Inject
    public DefaultModelProcessor(ModelXmlFactory modelXmlFactory, @Nullable List<ModelParser> modelParsers) {
        this.modelXmlFactory = modelXmlFactory;
        this.modelParsers = modelParsers;
    }

    /**
     * @implNote The ModelProcessor#locatePom never returns null while the ModelParser#locatePom needs to return an existing path!
     */
    @Override
    public Path locateExistingPom(Path projectDirectory) {
        return throwIfWrongProjectDirLocation(
                projectDirectory,
                modelParsers.stream()
                        .map(m -> m.locate(projectDirectory)
                                .map(org.apache.maven.api.services.Source::getPath)
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElseGet(() -> locateExistingPomWithFallback(projectDirectory)));
    }

    private static Path throwIfWrongProjectDirLocation(Path projectDirectory, Path pom) {
        if (pom != null && !pom.equals(projectDirectory) && !pom.getParent().equals(projectDirectory)) {
            throw new IllegalArgumentException("The POM found does not belong to the given directory: " + pom);
        }
        return pom;
    }

    private Path locateExistingPomWithFallback(Path project) {
        if (project != null) {
            if (Files.isDirectory(project)) {
                Path pom = project.resolve("pom.xml");
                return Files.isRegularFile(pom) ? pom : null;
            }
            return Files.isRegularFile(project) ? project : null;
        }
        return locateExistingPomWithFallback(Paths.get(System.getProperty("user.dir"))); // check in user dir
    }

    @Override
    public Model read(XmlReaderRequest request) throws IOException {
        Path pomFile = requireNonNull(request, "source cannot be null").getPath();
        if (pomFile != null) {
            List<ModelParserException> exceptions = new ArrayList<>();
            for (ModelParser parser : modelParsers) {
                try {
                    Optional<Model> parent = parent(request, parser, pomFile.getParent());
                    if (parent.isPresent()) {
                        return parent.get().withPomFile(pomFile);
                    }
                } catch (ModelParserException e) {
                    exceptions.add(e);
                }
            }
            throwErrorsSuppressed(exceptions);
        }
        return modelXmlFactory.read(request);
    }

    private static Optional<Model> parent(XmlReaderRequest request, ModelParser parser, Path projectDirectory) {
        return parser.locateAndParse(projectDirectory, Map.of(STRICT, request.isStrict()));
    }

    private static void throwErrorsSuppressed(List<ModelParserException> exceptions) throws IOException {
        if (!exceptions.isEmpty()) {
            IOException ex = new IOException();
            exceptions.forEach(ex::addSuppressed);
            throw ex;
        }
    }
}
