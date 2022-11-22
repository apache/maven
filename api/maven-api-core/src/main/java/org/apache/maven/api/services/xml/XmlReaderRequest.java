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

/**
 * An XML reader request.
 *
 * @since 4.0
 */
@Experimental
@Immutable
public interface XmlReaderRequest {

    Path getPath();

    URL getURL();

    InputStream getInputStream();

    Reader getReader();

    Transformer getTransformer();

    boolean isStrict();

    String getModelId();

    String getLocation();

    boolean isAddDefaultEntities();

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

    @Nonnull
    static XmlReaderRequestBuilder builder() {
        return new XmlReaderRequestBuilder();
    }

    @NotThreadSafe
    class XmlReaderRequestBuilder {
        Path path;
        URL url;
        InputStream inputStream;
        Reader reader;
        Transformer transformer;
        boolean strict;
        String modelId;
        String location;
        boolean addDefaultEntities = true;

        public XmlReaderRequestBuilder path(Path path) {
            this.path = path;
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

        public XmlReaderRequest build() {
            return new DefaultXmlReaderRequest(
                    path, url, inputStream, reader, transformer, strict, modelId, location, addDefaultEntities);
        }

        private static class DefaultXmlReaderRequest implements XmlReaderRequest {
            final Path path;
            final URL url;
            final InputStream inputStream;
            final Reader reader;
            final Transformer transformer;
            final boolean strict;
            final String modelId;
            final String location;
            final boolean addDefaultEntities;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultXmlReaderRequest(
                    Path path,
                    URL url,
                    InputStream inputStream,
                    Reader reader,
                    Transformer transformer,
                    boolean strict,
                    String modelId,
                    String location,
                    boolean addDefaultEntities) {
                this.path = path;
                this.url = url;
                this.inputStream = inputStream;
                this.reader = reader;
                this.transformer = transformer;
                this.strict = strict;
                this.modelId = modelId;
                this.location = location;
                this.addDefaultEntities = addDefaultEntities;
            }

            @Override
            public Path getPath() {
                return path;
            }

            @Override
            public URL getURL() {
                return null;
            }

            @Override
            public InputStream getInputStream() {
                return inputStream;
            }

            public Reader getReader() {
                return reader;
            }

            @Override
            public Transformer getTransformer() {
                return transformer;
            }

            @Override
            public boolean isStrict() {
                return strict;
            }

            @Override
            public String getModelId() {
                return modelId;
            }

            @Override
            public String getLocation() {
                return location;
            }

            @Override
            public boolean isAddDefaultEntities() {
                return addDefaultEntities;
            }
        }
    }
}
