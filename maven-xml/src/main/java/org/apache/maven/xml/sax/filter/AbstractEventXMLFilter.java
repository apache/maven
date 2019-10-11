package org.apache.maven.xml.sax.filter;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.xml.sax.SAXEvent;
import org.apache.maven.xml.sax.SAXEventFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * Builds up a list of SAXEvents, which will be executed with {@link #executeEvents()}
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
abstract class AbstractEventXMLFilter extends AbstractSAXFilter
{
    private List<SAXEvent> saxEvents = new ArrayList<>();
    
    private SAXEventFactory eventFactory;
    
    // characters BEFORE startElement must get state of startingElement
    // this way removing based on state keeps formatting
    private SAXEvent characters;
    
    // map of characters AFTER starting element, will be cleaned up after closeElement
    private Map<String, SAXEvent> charactersMap = new HashMap<>();
    
    protected abstract boolean isParsing();
    
    protected abstract String getState();
    
    protected boolean acceptEvent( String state )
    {
        return true;
    }
    
//    protected final void applyCharacters() throws SAXException
//    {
//        if ( characters != null )
//        {
//            processEvent( characters );
//        }
//    }
    
    AbstractEventXMLFilter()
    {
        super();
    }

    <T extends XMLReader & LexicalHandler> AbstractEventXMLFilter( T parent )
    {
        setParent( parent );
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
            final SAXEvent charactersEvent = characters;
            
            if ( charactersEvent != null )
            {
                saxEvents.add( () -> 
                {
                    if ( acceptEvent( eventState ) )
                    {
                        charactersEvent.execute();
                    }
                } );
                characters = null;
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
    
    protected final void executeEvents() throws SAXException
    {
        final String eventState = getState();
        final SAXEvent charactersEvent = characters;
        if ( charactersEvent != null )
        {
            saxEvents.add( () -> 
            {
                if ( acceptEvent( eventState ) )
                {
                    charactersEvent.execute();
                }
            } );
            characters = null;
        }
        
        // not with streams due to checked SAXException
        for ( SAXEvent saxEvent : saxEvents )
        {
            saxEvent.execute();
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
        if ( isParsing() )
        {
            this.characters = getEventFactory().characters( ch, start, length );
            this.charactersMap.put( getState(), characters );
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
}
