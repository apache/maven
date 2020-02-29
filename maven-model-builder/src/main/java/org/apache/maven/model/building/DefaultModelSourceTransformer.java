package org.apache.maven.model.building;

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

import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.xml.Factories;
import org.apache.maven.xml.sax.filter.BuildPomXMLFilterFactory;
import org.apache.maven.xml.sax.filter.BuildPomXMLFilterListener;
import org.eclipse.sisu.Nullable;
import org.xml.sax.SAXException;

/**
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
@Named
@Singleton
public class DefaultModelSourceTransformer implements ModelSourceTransformer
{
    @Inject
    @Nullable
    private BuildPomXMLFilterListener xmlFilterListener;
    
    @Override
    public final InputStream transform( Path pomFile, TransformerContext context )
        throws IOException, TransformerConfigurationException, SAXException, ParserConfigurationException
    {
        final BuildPomXMLFilterFactory buildPomXMLFilterFactory = new DefaultBuildPomXMLFilterFactory( context );
        final TransformerFactory transformerFactory = Factories.newTransformerFactory() ;
        
        final PipedOutputStream pipedOutputStream  = new PipedOutputStream();
        final PipedInputStream pipedInputStream  = new PipedInputStream( pipedOutputStream );

        final SAXSource transformSource =
            new SAXSource( buildPomXMLFilterFactory.get( pomFile ),
                           new org.xml.sax.InputSource( new FileInputStream( pomFile.toFile() ) ) );

        OutputStream out;
        if ( xmlFilterListener != null )
        {
            out = new FilterOutputStream( pipedOutputStream )
            {
                @Override
                public void write( byte[] b, int off, int len )
                    throws IOException
                {
                    super.write( b, off, len );
                    xmlFilterListener.write( pomFile, b, off, len );
                }  
            };
        }
        else
        {
            out = pipedOutputStream;
        }

        final StreamResult result = new StreamResult( out );
        
        final Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                try ( PipedOutputStream out = pipedOutputStream )
                {
                    transformerFactory.newTransformer().transform( transformSource, result );
                }
                catch ( TransformerException | IOException e )
                {
                    throw new RuntimeException( e );
                }
            }
        };

        new Thread( runnable ).start();

        return pipedInputStream;
    }
}
