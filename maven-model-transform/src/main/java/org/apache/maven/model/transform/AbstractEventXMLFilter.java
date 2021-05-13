package org.apache.maven.model.transform;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.apache.maven.model.transform.sax.SAXEvent;
import org.apache.maven.model.transform.sax.SAXEventFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Builds up a list of SAXEvents, which will be executed with {@link #executeEvents()}
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
abstract class AbstractEventXMLFilter extends AbstractSAXFilter
{
    private Queue<SAXEvent> saxEvents = new ArrayDeque<>();

    private SAXEventFactory eventFactory;

    // characters BEFORE startElement must get state of startingElement
    // this way removing based on state keeps correct formatting
    private List<SAXEvent> charactersSegments = new ArrayList<>();

    private boolean lockCharacters = false;

    protected abstract boolean isParsing();

    protected abstract String getState();

    protected boolean acceptEvent( String state )
    {
        return true;
    }

    AbstractEventXMLFilter()
    {
        super();
    }

    AbstractEventXMLFilter( AbstractSAXFilter parent )
    {
        super( parent );
    }

    private SAXEventFactory getEventFactory()
    {
        if ( eventFactory == null )
        {
            eventFactory = SAXEventFactory.newInstance( getContentHandler(), getLexicalHandler() );
        }
        return eventFactory;
    }

    private void processEvent( final SAXEvent event )
                    throws SAXException
    {
        if ( isParsing() )
        {
            final String eventState = getState();

            if ( !lockCharacters )
            {
                charactersSegments.stream().forEach( e ->
                {
                    saxEvents.add( () ->
                    {
                        if ( acceptEvent( eventState ) )
                        {
                            e.execute();
                        }
                    } );
                } );
                charactersSegments.clear();
            }

            saxEvents.add( () ->
            {
                if ( acceptEvent( eventState ) )
                {
                    event.execute();
                }
            } );
        }
        else
        {
            event.execute();
        }
    }

    /**
     * Should be used to include extra events before a closing element.
     * This is a lightweight solution to keep the correct indentation.
     */
    protected Includer include()
    {
        this.lockCharacters = true;

        return () -> lockCharacters = false;
    }

    protected final void executeEvents() throws SAXException
    {
        final String eventState = getState();
        charactersSegments.stream().forEach( e ->
        {
            saxEvents.add( () ->
            {
                if ( acceptEvent( eventState ) )
                {
                    e.execute();
                }
            } );
        } );
        charactersSegments.clear();

        // not with streams due to checked SAXException
        while ( !saxEvents.isEmpty() )
        {
            saxEvents.poll().execute();
        }
    }

    @Override
    public void setDocumentLocator( Locator locator )
    {
        try
        {
            processEvent( getEventFactory().setDocumentLocator( locator ) );
        }
        catch ( SAXException e )
        {
            // noop, setDocumentLocator can never throw a SAXException
        }
    }

    @Override
    public void startDocument() throws SAXException
    {
        processEvent( getEventFactory().startDocument() );
    }

    @Override
    public void endDocument() throws SAXException
    {
        processEvent( getEventFactory().endDocument() );
    }

    @Override
    public void startPrefixMapping( String prefix, String uri ) throws SAXException
    {
        processEvent( getEventFactory().startPrefixMapping( prefix, uri ) );
    }

    @Override
    public void endPrefixMapping( String prefix ) throws SAXException
    {
        processEvent( getEventFactory().endPrefixMapping( prefix ) );
    }

    @Override
    public void startElement( String uri, String localName, String qName, Attributes atts ) throws SAXException
    {
        processEvent( getEventFactory().startElement( uri, localName, qName, atts ) );
    }

    @Override
    public void endElement( String uri, String localName, String qName ) throws SAXException
    {
        processEvent( getEventFactory().endElement( uri, localName, qName ) );
    }

    @Override
    public void characters( char[] ch, int start, int length ) throws SAXException
    {
        if ( lockCharacters )
        {
            processEvent( getEventFactory().characters( ch, start, length ) );
        }
        else if ( isParsing() )
        {
            this.charactersSegments.add( getEventFactory().characters( ch, start, length ) );
        }
        else
        {
            super.characters( ch, start, length );
        }
    }

    @Override
    public void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException
    {
        processEvent( getEventFactory().ignorableWhitespace( ch, start, length ) );
    }

    @Override
    public void processingInstruction( String target, String data ) throws SAXException
    {
        processEvent( getEventFactory().processingInstruction( target, data ) );
    }

    @Override
    public void skippedEntity( String name ) throws SAXException
    {
        processEvent( getEventFactory().skippedEntity( name ) );
    }

    @Override
    public void startDTD( String name, String publicId, String systemId ) throws SAXException
    {
        processEvent( getEventFactory().startCDATA() );
    }

    @Override
    public void endDTD() throws SAXException
    {
        processEvent( getEventFactory().endDTD() );
    }

    @Override
    public void startEntity( String name ) throws SAXException
    {
        processEvent( getEventFactory().startEntity( name ) );
    }

    @Override
    public void endEntity( String name ) throws SAXException
    {
        processEvent( getEventFactory().endEntity( name ) );
    }

    @Override
    public void startCDATA()
        throws SAXException
    {
        processEvent( getEventFactory().startCDATA() );
    }

    @Override
    public void endCDATA()
        throws SAXException
    {
        processEvent( getEventFactory().endCDATA() );
    }

    @Override
    public void comment( char[] ch, int start, int length )
        throws SAXException
    {
        processEvent( getEventFactory().comment( ch, start, length ) );
    }

    /**
     * AutoCloseable with a close method that doesn't throw an exception
     *
     * @author Robert Scholte
     *
     */
    @FunctionalInterface
    protected interface Includer extends AutoCloseable
    {
        void close();
    }
}
