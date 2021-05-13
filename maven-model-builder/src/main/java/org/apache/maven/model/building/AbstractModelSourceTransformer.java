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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.apache.maven.model.transform.sax.CommentRenormalizer;
import org.apache.maven.model.transform.sax.Factories;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Offers a transformation implementation based on PipelineStreams.
 * Subclasses are responsible for providing the right SAXFilter.
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public abstract class AbstractModelSourceTransformer
    implements ModelSourceTransformer
{
    private static final AtomicInteger TRANSFORM_THREAD_COUNTER = new AtomicInteger();

    private final TransformerFactory transformerFactory = Factories.newTransformerFactory();

    protected abstract AbstractSAXFilter getSAXFilter( Path pomFile,
                                                       TransformerContext context,
                                                       Consumer<LexicalHandler> lexicalHandlerConsumer )
        throws TransformerConfigurationException, SAXException, ParserConfigurationException;

    protected OutputStream filterOutputStream( OutputStream outputStream, Path pomFile )
    {
        return outputStream;
    }

    public SAXTransformerFactory getTransformerFactory()
    {
        return ( SAXTransformerFactory ) transformerFactory;
    }

    protected TransformerHandler getTransformerHandler( Path pomFile )
        throws IOException, org.apache.maven.model.building.TransformerException
    {
        return null;
    }

    @Override
    public final InputStream transform( Path pomFile, TransformerContext context )
        throws IOException, org.apache.maven.model.building.TransformerException
    {
        final TransformerHandler transformerHandler = getTransformerHandler( pomFile );

        final PipedOutputStream pout = new PipedOutputStream();
        OutputStream out = filterOutputStream( pout, pomFile );

        final javax.xml.transform.Result result;
        final Consumer<LexicalHandler> lexConsumer;
        if ( transformerHandler == null )
        {
            result = new StreamResult( out );
            lexConsumer = null;
        }
        else
        {
            result = new SAXResult( transformerHandler );
            lexConsumer = l -> ( (SAXResult) result ).setLexicalHandler( new CommentRenormalizer( l ) );
            transformerHandler.setResult( new StreamResult( out ) );
        }

        final AbstractSAXFilter filter;
        try
        {
            filter = getSAXFilter( pomFile, context, lexConsumer );
            filter.setLexicalHandler( transformerHandler );
            // By default errors are written to stderr.
            // Hence set custom errorHandler to reduce noice
            filter.setErrorHandler( new ErrorHandler()
            {
                @Override
                public void warning( SAXParseException exception )
                    throws SAXException
                {
                    throw exception;
                }

                @Override
                public void fatalError( SAXParseException exception )
                    throws SAXException
                {
                    throw exception;
                }

                @Override
                public void error( SAXParseException exception )
                    throws SAXException
                {
                    throw exception;
                }
            } );
        }
        catch ( TransformerConfigurationException | SAXException | ParserConfigurationException e )
        {
            throw new org.apache.maven.model.building.TransformerException( e );
        }

        final SAXSource transformSource =
            new SAXSource( filter, new org.xml.sax.InputSource( Files.newInputStream( pomFile ) ) );

        IOExceptionHandler eh = new IOExceptionHandler();

        // Ensure pipedStreams are connected before the transformThread starts!!
        final PipedInputStream pipedInputStream = new PipedInputStream( pout );

        Thread transformThread = new Thread( () ->
        {
            try ( PipedOutputStream pos = pout )
            {
                transformerFactory.newTransformer().transform( transformSource, result );
            }
            catch ( TransformerException | IOException e )
            {
                eh.uncaughtException( Thread.currentThread(), e );
            }
        }, "TransformThread-" + TRANSFORM_THREAD_COUNTER.incrementAndGet() );
        transformThread.setUncaughtExceptionHandler( eh );
        transformThread.setDaemon( true );
        transformThread.start();

        return new ThreadAwareInputStream( pipedInputStream, eh );
    }

    private static class IOExceptionHandler
        implements Thread.UncaughtExceptionHandler, AutoCloseable
    {
        private volatile Throwable cause;

        @Override
        public void uncaughtException( Thread t, Throwable e )
        {
            try
            {
                throw e;
            }
            catch ( TransformerException | IOException | RuntimeException | Error allGood )
            {
                // all good
                this.cause = e;
            }
            catch ( Throwable notGood )
            {
                throw new AssertionError( "Unexpected Exception", e );
            }
        }

        @Override
        public void close()
            throws IOException
        {
            if ( cause != null )
            {
                try
                {
                    throw cause;
                }
                catch ( IOException | RuntimeException | Error e )
                {
                    throw e;
                }
                catch ( Throwable t )
                {
                    // Any checked exception
                    throw new RuntimeException( "Failed to transform pom", t );
                }
            }
        }
    }

    private class ThreadAwareInputStream
        extends FilterInputStream
    {
        final IOExceptionHandler h;

        protected ThreadAwareInputStream( InputStream in, IOExceptionHandler h )
        {
            super( in );
            this.h = h;
        }

        @Override
        public void close()
            throws IOException
        {
            try ( IOExceptionHandler eh = h )
            {
                super.close();
            }
        }
    }
}
