package org.apache.maven.api.services.xml;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.OutputStream;
import java.io.Writer;

public interface XmlWriterRequest<T>
{

    OutputStream getOutputStream();

    Writer getWriter();

    T getContent();

    static <T> XmlWriterRequestBuilder<T> builder()
    {
        return new XmlWriterRequestBuilder<>();
    }

    class XmlWriterRequestBuilder<T>
    {
        OutputStream outputStream;
        Writer writer;
        T content;

        public XmlWriterRequestBuilder<T> outputStream( OutputStream outputStream )
        {
            this.outputStream = outputStream;
            return this;
        }

        public XmlWriterRequestBuilder<T> writer( Writer writer )
        {
            this.writer = writer;
            return this;
        }

        public XmlWriterRequestBuilder<T> content( T content )
        {
            this.content = content;
            return this;
        }

        public XmlWriterRequest<T> build()
        {
            return new DefaultXmlWriterRequest<>( outputStream, writer, content );
        }

        private static class DefaultXmlWriterRequest<T> implements XmlWriterRequest<T>
        {
            final OutputStream outputStream;
            final Writer writer;
            final T content;

            DefaultXmlWriterRequest( OutputStream outputStream, Writer writer, T content )
            {
                this.outputStream = outputStream;
                this.writer = writer;
                this.content = content;
            }

            @Override
            public OutputStream getOutputStream()
            {
                return outputStream;
            }

            @Override
            public Writer getWriter()
            {
                return writer;
            }

            @Override
            public T getContent()
            {
                return content;
            }
        }
    }
}
