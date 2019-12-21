package org.apache.maven.model.io;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Handles deserialization of a model from some kind of textual format like XML.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultModelReader
    implements ModelReader
{

    @Override
    public Model read( File input, Map<String, ?> options )
        throws IOException
    {
        Objects.requireNonNull( input, "input cannot be null" );

        Model model = read( new FileInputStream( input ), options );

        model.setPomFile( input );

        return model;
    }

    @Override
    public Model read( Reader input, Map<String, ?> options )
        throws IOException
    {
        Objects.requireNonNull( input, "input cannot be null" );

        try ( final Reader in = input )
        {
            return read( in, isStrict( options ), getSource( options ) );
        }
    }

    @Override
    public Model read( InputStream input, Map<String, ?> options )
        throws IOException
    {
        Objects.requireNonNull( input, "input cannot be null" );

        try ( final XmlStreamReader in = ReaderFactory.newXmlReader( input ) )
        {
            return read( in, isStrict( options ), getSource( options ) );
        }
    }

    private boolean isStrict( Map<String, ?> options )
    {
        Object value = ( options != null ) ? options.get( IS_STRICT ) : null;
        return value == null || Boolean.parseBoolean( value.toString() );
    }

    private InputSource getSource( Map<String, ?> options )
    {
        Object value = ( options != null ) ? options.get( INPUT_SOURCE ) : null;
        return (InputSource) value;
    }

    private Model read( Reader reader, boolean strict, InputSource source )
        throws IOException
    {
        try
        {
            if ( source != null )
            {
                return new MavenXpp3ReaderEx().read( reader, strict, source );
            }
            else
            {
                return new MavenXpp3Reader().read( reader, strict );
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new ModelParseException( e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e );
        }
    }

}
