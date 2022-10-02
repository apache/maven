package org.apache.maven.internal.impl;

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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterException;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenXpp3ReaderEx;
import org.apache.maven.model.v4.MavenXpp3WriterEx;
import org.codehaus.plexus.util.ReaderFactory;

import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultModelXmlFactory
        implements ModelXmlFactory
{
    @Override
    public Model read( @Nonnull XmlReaderRequest request ) throws XmlReaderException
    {
        nonNull( request, "request can not be null" );
        Path path = request.getPath();
        URL url = request.getURL();
        Reader reader = request.getReader();
        InputStream inputStream = request.getInputStream();
        if ( path == null && url == null && reader == null && inputStream == null )
        {
            throw new IllegalArgumentException( "path, url, reader or inputStream must be non null" );
        }
        try
        {
            InputSource source = null;
            if ( request.getModelId() != null || request.getLocation() != null )
            {
                source = new InputSource( request.getModelId(), request.getLocation() );
            }
            MavenXpp3ReaderEx xml = new MavenXpp3ReaderEx();
            xml.setAddDefaultEntities( request.isAddDefaultEntities() );
            if ( path != null )
            {
                reader = ReaderFactory.newXmlReader( path.toFile() );
            }
            else if ( url != null )
            {
                reader = ReaderFactory.newXmlReader( url );
            }
            else if ( inputStream != null )
            {
                reader = ReaderFactory.newXmlReader( inputStream );
            }
            return xml.read( reader, request.isStrict(), source );
        }
        catch ( Exception e )
        {
            throw new XmlReaderException( "Unable to read model", e );
        }
    }

    @Override
    public void write( XmlWriterRequest<Model> request ) throws XmlWriterException
    {
        nonNull( request, "request can not be null" );
        Model content = nonNull( request.getContent(), "content can not be null" );
        Path path = request.getPath();
        OutputStream outputStream = request.getOutputStream();
        Writer writer = request.getWriter();
        if ( writer == null && outputStream == null && path == null )
        {
            throw new IllegalArgumentException( "writer, outputStream or path must be non null" );
        }
        try
        {
            if ( writer != null )
            {
                new MavenXpp3WriterEx().write( writer, content );
            }
            else if ( outputStream != null )
            {
                new MavenXpp3WriterEx().write( outputStream, content );
            }
            else
            {
                try ( OutputStream os = Files.newOutputStream( path ) )
                {
                    new MavenXpp3WriterEx().write( outputStream, content );
                }
            }
        }
        catch ( Exception e )
        {
            throw new XmlWriterException( "Unable to write model", e );
        }
    }
}
