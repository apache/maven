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

import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * An XML writer request.
 *
 * @since 4.0.0
 * @param <T> the object type to read
 */
@Experimental
public interface XmlWriterRequest<T> {

    @Nullable
    Path getPath();

    @Nullable
    OutputStream getOutputStream();

    @Nullable
    Writer getWriter();

    @Nonnull
    T getContent();

    @Nullable
    Function<Object, String> getInputLocationFormatter();

    static <T> XmlWriterRequestBuilder<T> builder() {
        return new XmlWriterRequestBuilder<>();
    }

    default void throwIfIncomplete() {
        if (getWriter() == null && getOutputStream() == null && getPath() == null) {
            throw new IllegalArgumentException("writer, outputStream, or path must be non null");
        }
    }

    class XmlWriterRequestBuilder<T> {
        Path path;
        OutputStream outputStream;
        Writer writer;
        T content;
        Function<Object, String> inputLocationFormatter;

        public XmlWriterRequestBuilder<T> path(Path path) {
            this.path = path;
            return this;
        }

        public XmlWriterRequestBuilder<T> outputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
            return this;
        }

        public XmlWriterRequestBuilder<T> writer(Writer writer) {
            this.writer = writer;
            return this;
        }

        public XmlWriterRequestBuilder<T> content(T content) {
            this.content = content;
            return this;
        }

        public XmlWriterRequestBuilder<T> inputLocationFormatter(Function<Object, String> inputLocationFormatter) {
            this.inputLocationFormatter = inputLocationFormatter;
            return this;
        }

        public XmlWriterRequest<T> build() {
            return new DefaultXmlWriterRequest<>(path, outputStream, writer, content, inputLocationFormatter);
        }

        private static class DefaultXmlWriterRequest<T> implements XmlWriterRequest<T> {
            final Path path;
            final OutputStream outputStream;
            final Writer writer;
            final T content;
            final Function<Object, String> inputLocationFormatter;

            DefaultXmlWriterRequest(
                    Path path,
                    OutputStream outputStream,
                    Writer writer,
                    T content,
                    Function<Object, String> inputLocationFormatter) {
                this.path = path;
                this.outputStream = outputStream;
                this.writer = writer;
                this.content = content;
                this.inputLocationFormatter = inputLocationFormatter;
            }

            @Override
            public Path getPath() {
                return path;
            }

            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }

            @Override
            public Writer getWriter() {
                return writer;
            }

            @Override
            public T getContent() {
                return content;
            }

            @Override
            public Function<Object, String> getInputLocationFormatter() {
                return inputLocationFormatter;
            }
        }
    }
}
