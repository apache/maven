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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;

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
    private final Map<String, ModelParser> modelParsers;

    @Inject
    public DefaultModelProcessor(ModelXmlFactory modelXmlFactory, @Nullable Map<String, ModelParser> modelParsers) {
        this.modelXmlFactory = modelXmlFactory;
        this.modelParsers = modelParsers != null ? modelParsers : Map.of();
    }

    @Override
    public Path locateExistingPom(Path projectDirectory) {
        // Note that the ModelProcessor#locatePom never returns null
        // while the ModelParser#locatePom needs to return an existing path!
        Path pom = modelParsers.values().stream()
                .map(m -> m.locate(projectDirectory)
                        .map(org.apache.maven.api.services.Source::getPath)
                        .orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> doLocateExistingPom(projectDirectory));
        if (pom != null && !pom.equals(projectDirectory) && !pom.getParent().equals(projectDirectory)) {
            throw new IllegalArgumentException("The POM found does not belong to the given directory: " + pom);
        }
        return pom;
    }

    @Override
    public Model read(Source source, Map<String, ?> options) throws ModelParserException {
        Objects.requireNonNull(source, "source cannot be null");
        Path pomFile = source.getPath();
        if (pomFile != null) {
            // a parser may locate the project directory itself rather than a file within it
            Path projectDirectory = Files.isDirectory(pomFile) ? pomFile : pomFile.getParent();
            Map<String, ModelParserException> exceptions = new LinkedHashMap<>();
            for (Map.Entry<String, ModelParser> parser : modelParsers.entrySet()) {
                try {
                    Optional<Model> model = parser.getValue().locateAndParse(projectDirectory, options);
                    if (model.isPresent()) {
                        return model.get().withPomFile(pomFile);
                    }
                } catch (ModelParserException e) {
                    exceptions.put(parser.getKey(), e);
                }
            }
            try {
                return doRead(source, options);
            } catch (ModelParserException e) {
                if (!exceptions.isEmpty()) {
                    ModelParserException mpe =
                            new ModelParserException(buildDetailedErrorMessage(pomFile, exceptions, e), e);
                    exceptions.values().forEach(mpe::addSuppressed);
                    mpe.addSuppressed(e);
                    throw mpe;
                }
                throw e;
            }
        } else {
            return doRead(source, options);
        }
    }

    /**
     * Implements the deprecated bridge for backward compatibility.
     * Delegates to {@link #read(Source, Map)}, then re-throws {@link XmlReaderException}
     * or {@link IOException} from the cause when possible.
     *
     * @deprecated Use {@link #read(Source, Map)} instead.
     */
    @Deprecated(since = "4.0.0")
    @Override
    public Model read(XmlReaderRequest request) throws IOException, XmlReaderException {
        Objects.requireNonNull(request, "request cannot be null");
        Source source = toSource(request);
        try {
            return read(
                    source,
                    request.getTransformer() != null
                            ? Map.of(
                                    ModelParser.STRICT,
                                    request.isStrict(),
                                    ModelProcessor.TRANSFORMER,
                                    request.getTransformer())
                            : Map.of(ModelParser.STRICT, request.isStrict()));
        } catch (ModelParserException e) {
            // Unwrap one level: doRead wraps XmlReaderException/IOException as the direct cause
            if (e.getCause() instanceof XmlReaderException xre) {
                throw xre;
            }
            if (e.getCause() instanceof IOException ioe) {
                throw ioe;
            }
            throw new XmlReaderException(e.getMessage(), null, e);
        }
    }

    public static ModelSource urlModelSource(URL url, String modelId) {
        return new UrlModelSource(url, modelId);
    }

    record UrlModelSource(URL url, String modelId) implements ModelSource {
        UrlModelSource {
            Objects.requireNonNull(url, "url cannot be null");
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public InputStream openStream() throws IOException {
            return url.openStream();
        }

        @Override
        public String getLocation() {
            return url.toExternalForm();
        }

        @Override
        public Source resolve(String relative) {
            return null;
        }

        @Override
        public ModelSource resolve(ModelLocator modelLocator, String relative) {
            return null;
        }

        @Override
        public String getModelId() {
            return modelId;
        }
    }

    private static Source toSource(XmlReaderRequest request) {
        if (request.getInputStream() == null
                && request.getPath() == null
                && request.getReader() == null
                && request.getURL() != null) {
            return urlModelSource(request.getURL(), request.getModelId());
        }
        // Always return a ModelSource so modelId is preserved.
        return new ModelSource() {
            @Override
            public Path getPath() {
                return request.getPath();
            }

            @Override
            public InputStream openStream() throws IOException {
                if (request.getInputStream() != null) {
                    return request.getInputStream();
                } else if (request.getURL() != null) {
                    return request.getURL().openStream();
                } else if (request.getPath() != null) {
                    return Files.newInputStream(request.getPath());
                } else if (request.getReader() != null) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int n;
                    while ((n = request.getReader().read(buf)) != -1) {
                        sb.append(buf, 0, n);
                    }
                    return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
                }
                throw new IOException("No input stream available in request");
            }

            @Override
            public String getLocation() {
                if (request.getLocation() != null) {
                    return request.getLocation();
                }
                if (request.getPath() != null) {
                    return request.getPath().toString();
                }
                if (request.getURL() != null) {
                    return request.getURL().toExternalForm();
                }
                return "<unknown>";
            }

            @Override
            public Source resolve(String relative) {
                if (request.getPath() != null) {
                    return toSource(XmlReaderRequest.builder()
                            .path(request.getPath().resolve(relative))
                            .build());
                }
                return null;
            }

            @Override
            public ModelSource resolve(ModelLocator modelLocator, String relative) {
                return null;
            }

            @Override
            public String getModelId() {
                return request.getModelId();
            }
        };
    }

    private Path doLocateExistingPom(Path project) {
        if (project == null) {
            project = Paths.get(System.getProperty("user.dir"));
        }
        if (Files.isDirectory(project)) {
            Path pom = project.resolve("pom.xml");
            return Files.isRegularFile(pom) ? pom : null;
        } else if (Files.isRegularFile(project)) {
            return project;
        } else {
            return null;
        }
    }

    private Model doRead(Source source, Map<String, ?> options) throws ModelParserException {
        boolean strict = isStrict(options);
        XmlReaderRequest.Transformer transformer = getTransformer(options);
        try (InputStream is = source.openStream()) {
            XmlReaderRequest.XmlReaderRequestBuilder builder = XmlReaderRequest.builder()
                    .strict(strict)
                    .location(source.getLocation())
                    .path(source.getPath())
                    .inputStream(is)
                    .transformer(transformer);
            if (source instanceof ModelSource ms) {
                builder.modelId(ms.getModelId());
            }
            return modelXmlFactory.read(builder.build());
        } catch (XmlReaderException e) {
            if (e.getLocation() != null) {
                throw new ModelParserException(
                        e.getMessage(),
                        e.getLocation().getLineNumber(),
                        e.getLocation().getColumnNumber(),
                        e);
            }
            throw new ModelParserException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ModelParserException(e.getMessage(), e);
        }
    }

    private boolean isStrict(Map<String, ?> options) {
        Object value = (options != null) ? options.get(ModelParser.STRICT) : null;
        return value == null || Boolean.parseBoolean(value.toString());
    }

    private XmlReaderRequest.Transformer getTransformer(Map<String, ?> options) {
        Object value = (options != null) ? options.get(ModelProcessor.TRANSFORMER) : null;
        if (value instanceof XmlReaderRequest.Transformer t) {
            return t;
        }
        return new ModelInterningTransformer();
    }

    private String buildDetailedErrorMessage(
            Path pomFile, Map<String, ModelParserException> parserExceptions, Exception defaultReaderException) {
        StringBuilder message = new StringBuilder();
        message.append("Unable to parse POM ").append(pomFile).append(System.lineSeparator());

        if (!parserExceptions.isEmpty()) {
            message.append("        Tried ")
                    .append(parserExceptions.size())
                    .append(" parser")
                    .append(parserExceptions.size() > 1 ? "s" : "")
                    .append(":")
                    .append(System.lineSeparator());

            for (Map.Entry<String, ModelParserException> entry : parserExceptions.entrySet()) {
                ModelParserException e = entry.getValue();
                message.append("          ").append(entry.getKey()).append(") ");

                String parserMessage = e.getMessage();
                if (parserMessage != null && !parserMessage.isEmpty()) {
                    message.append(parserMessage);
                } else {
                    message.append(e.getClass().getSimpleName());
                }

                if (e.getLineNumber() > 0) {
                    message.append(" at line ").append(e.getLineNumber());
                    if (e.getColumnNumber() > 0) {
                        message.append(", column ").append(e.getColumnNumber());
                    }
                }

                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    String causeMessage = e.getCause().getMessage();
                    if (parserMessage == null || !parserMessage.contains(causeMessage)) {
                        message.append(": ").append(causeMessage);
                    }
                }

                message.append(System.lineSeparator());
            }
        }

        message.append("          default) XML reader also failed: ");
        String defaultMessage = defaultReaderException.getMessage();
        if (defaultMessage != null && !defaultMessage.isEmpty()) {
            message.append(defaultMessage);
        } else {
            message.append(defaultReaderException.getClass().getSimpleName());
        }

        return message.toString();
    }
}
