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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.Service;

@Experimental
public interface XmlFactory<T> extends Service
{

    @Nonnull
    default T read( @Nonnull InputStream input ) throws XmlReaderException
    {
        return read( input, true );
    }

    @Nonnull
    default T read( @Nonnull InputStream input, boolean strict ) throws XmlReaderException
    {
        return read( XmlReaderRequest.builder().inputStream( input ).strict( strict ).build() );
    }

    @Nonnull
    default T read( @Nonnull Reader reader ) throws XmlReaderException
    {
        return read( reader, true );
    }

    @Nonnull
    default T read( @Nonnull Reader reader, boolean strict ) throws XmlReaderException
    {
        return read( XmlReaderRequest.builder().reader( reader ).strict( strict ).build() );
    }

    @Nonnull
    T read( @Nonnull XmlReaderRequest request ) throws XmlReaderException;

    default void write( @Nonnull T content, @Nonnull OutputStream outputStream ) throws XmlWriterException
    {
        write( XmlWriterRequest.<T>builder().content( content ).outputStream( outputStream ).build() );
    }

    default void write( @Nonnull T content, @Nonnull Writer writer ) throws XmlWriterException
    {
        write( XmlWriterRequest.<T>builder().content( content ).writer( writer ).build() );
    }

    void write( @Nonnull XmlWriterRequest<T> request ) throws XmlWriterException;

}
