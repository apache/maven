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
package org.apache.maven.api.services.xml;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

/**
 * An XML reader request.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface XmlReaderRequest {

    /**
     * Returns the path to the XML file to read, or {@code null} if another input source is used.
     *
     * @return the file path, or {@code null}
     */
    @Nullable
    Path path();

    /** @deprecated Use {@link #path()} instead. */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default Path getPath() {
        return path();
    }

    /**
     * Returns the root directory for resolving relative paths, or {@code null} if not set.
     *
     * @return the root directory, or {@code null}
     */
    @Nullable
    Path rootDirectory();

    /**
     * @deprecated Use {@link #rootDirectory()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default Path getRootDirectory() {
        return rootDirectory();
    }

    /**
     * Returns the URL to read the XML from, or {@code null} if another input source is used.
     *
     * @return the URL, or {@code null}
     */
    @Nullable
    URL url();

    /**
     * @deprecated Use {@link #url()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default URL getURL() {
        return url();
    }

    /**
     * Returns the input stream to read the XML from, or {@code null} if another input source is used.
     *
     * @return the input stream, or {@code null}
     */
    @Nullable
    InputStream inputStream();

    /**
     * @deprecated Use {@link #inputStream()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default InputStream getInputStream() {
        return inputStream();
    }

    /**
     * Returns the reader to read the XML from, or {@code null} if another input source is used.
     *
     * @return the reader, or {@code null}
     */
    @Nullable
    Reader reader();

    /**
     * @deprecated Use {@link #reader()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default Reader getReader() {
        return reader();
    }

    /**
     * Returns the transformer to apply during parsing, or {@code null} if none is set.
     *
     * @return the transformer, or {@code null}
     */
    @Nullable
    Transformer transformer();

    /**
     * @deprecated Use {@link #transformer()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default Transformer getTransformer() {
        return transformer();
    }

    /**
     * Returns whether strict parsing should be enforced.
     *
     * @return {@code true} if strict parsing is enabled
     */
    boolean strict();

    /**
     * @deprecated Use {@link #strict()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    default boolean isStrict() {
        return strict();
    }

    /**
     * Returns the identifier of the model being parsed, used for error reporting.
     *
     * @return the model identifier, or {@code null}
     */
    @Nullable
    String modelId();

    /**
     * @deprecated Use {@link #modelId()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default String getModelId() {
        return modelId();
    }

    /**
     * Returns the location description used for error reporting.
     *
     * @return the location string, or {@code null}
     */
    @Nullable
    String location();

    /**
     * @deprecated Use {@link #location()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    @Nullable
    default String getLocation() {
        return location();
    }

    /**
     * Returns whether default XML entities should be added during parsing.
     *
     * @return {@code true} if default entities should be added
     */
    boolean addDefaultEntities();

    /**
     * @deprecated Use {@link #addDefaultEntities()} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    default boolean isAddDefaultEntities() {
        return addDefaultEntities();
    }

    /**
     * Indicates whether location information (line/column tracking) should be
     * recorded during parsing. Defaults to {@code true}. Setting this to
     * {@code false} for imported dependency management POMs avoids allocating
     * location maps that are never read, significantly reducing memory churn
     * in large reactors.
     *
     * @return {@code true} if location information should be tracked
     * @since 4.0.0
     */
    default boolean isAddLocationInformation() {
        return true;
    }

    interface Transformer {
        /**
         * Interpolate the value read from the xml document
         *
         * @param source    The source value
         * @param fieldName A description of the field being interpolated. The implementation may use this to
         *                  log stuff.
         * @return the interpolated value
         */
        String transform(String source, String fieldName);
    }

    /**
     * Creates a new builder for constructing {@code XmlReaderRequest} instances.
     *
     * @return a new builder
     */
    @Nonnull
    static XmlReaderRequestBuilder builder() {
        return new XmlReaderRequestBuilder();
    }

    @NotThreadSafe
    class XmlReaderRequestBuilder {
        Path path;
        Path rootDirectory;
        URL url;
        InputStream inputStream;
        Reader reader;
        Transformer transformer;
        boolean strict;
        String modelId;
        String location;
        boolean addDefaultEntities = true;
        boolean addLocationInformation = true;

        public XmlReaderRequestBuilder path(Path path) {
            this.path = path;
            return this;
        }

        public XmlReaderRequestBuilder rootDirectory(Path rootDirectory) {
            this.rootDirectory = rootDirectory;
            return this;
        }

        public XmlReaderRequestBuilder url(URL url) {
            this.url = url;
            return this;
        }

        public XmlReaderRequestBuilder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public XmlReaderRequestBuilder reader(Reader reader) {
            this.reader = reader;
            return this;
        }

        public XmlReaderRequestBuilder transformer(Transformer transformer) {
            this.transformer = transformer;
            return this;
        }

        public XmlReaderRequestBuilder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        public XmlReaderRequestBuilder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public XmlReaderRequestBuilder location(String location) {
            this.location = location;
            return this;
        }

        public XmlReaderRequestBuilder addDefaultEntities(boolean addDefaultEntities) {
            this.addDefaultEntities = addDefaultEntities;
            return this;
        }

        public XmlReaderRequestBuilder addLocationInformation(boolean addLocationInformation) {
            this.addLocationInformation = addLocationInformation;
            return this;
        }

        public XmlReaderRequest build() {
            return new DefaultXmlReaderRequest(
                    path,
                    rootDirectory,
                    url,
                    inputStream,
                    reader,
                    transformer,
                    strict,
                    modelId,
                    location,
                    addDefaultEntities,
                    addLocationInformation);
        }

        private static class DefaultXmlReaderRequest implements XmlReaderRequest {
            final Path path;
            final Path rootDirectory;
            final URL url;
            final InputStream inputStream;
            final Reader reader;
            final Transformer transformer;
            final boolean strict;
            final String modelId;
            final String location;
            final boolean addDefaultEntities;
            final boolean addLocationInformation;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultXmlReaderRequest(
                    Path path,
                    Path rootDirectory,
                    URL url,
                    InputStream inputStream,
                    Reader reader,
                    Transformer transformer,
                    boolean strict,
                    String modelId,
                    String location,
                    boolean addDefaultEntities,
                    boolean addLocationInformation) {
                this.path = path;
                this.rootDirectory = rootDirectory;
                this.url = url;
                this.inputStream = inputStream;
                this.reader = reader;
                this.transformer = transformer;
                this.strict = strict;
                this.modelId = modelId;
                this.location = location;
                this.addDefaultEntities = addDefaultEntities;
                this.addLocationInformation = addLocationInformation;
            }

            @Override
            public Path path() {
                return path;
            }

            @Override
            public Path rootDirectory() {
                return rootDirectory;
            }

            @Override
            public URL url() {
                return url;
            }

            @Override
            public InputStream inputStream() {
                return inputStream;
            }

            @Override
            public Reader reader() {
                return reader;
            }

            @Override
            public Transformer transformer() {
                return transformer;
            }

            @Override
            public boolean strict() {
                return strict;
            }

            @Override
            public String modelId() {
                return modelId;
            }

            @Override
            public String location() {
                return location;
            }

            @Override
            public boolean addDefaultEntities() {
                return addDefaultEntities;
            }

            @Override
            public boolean isAddLocationInformation() {
                return addLocationInformation;
            }
        }
    }
}
