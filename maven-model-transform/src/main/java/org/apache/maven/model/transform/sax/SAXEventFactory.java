package org.apache.maven.model.transform.sax;

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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.ext.LexicalHandler;

/**
 * Factory for SAXEvents
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public final class SAXEventFactory
{
    private final ContentHandler contentHandler;

    private final LexicalHandler lexicalHandler;

    protected SAXEventFactory( ContentHandler contentHandler, LexicalHandler lexicalHandler )
    {
        this.contentHandler = contentHandler;
        this.lexicalHandler = lexicalHandler;
    }

    public SAXEvent characters( final char[] ch, final int start, final int length )
    {
        final char[] txt = new char[length];
        System.arraycopy( ch, start, txt, 0, length );
        return () -> contentHandler.characters( txt, 0, length );
    }

    public SAXEvent endDocument()
    {
        return () -> contentHandler.endDocument();
    }

    public SAXEvent endElement( final String uri, final String localName, final String qName )
    {
        return () -> contentHandler.endElement( uri, localName, qName );
    }

    public SAXEvent endPrefixMapping( final String prefix )
    {
        return () ->  contentHandler.endPrefixMapping( prefix );
    }

    public SAXEvent ignorableWhitespace( final char[] ch, final int start, final int length )
    {
        return () ->  contentHandler.ignorableWhitespace( ch, start, length );
    }

    public SAXEvent processingInstruction( final String target, final String data )
    {
        return () -> contentHandler.processingInstruction( target, data );
    }

    public SAXEvent setDocumentLocator( final Locator locator )
    {
        return () -> contentHandler.setDocumentLocator( locator );
    }

    public SAXEvent skippedEntity( final String name )
    {
        return () -> contentHandler.skippedEntity( name );
    }

    public SAXEvent startDocument()
    {
        return () -> contentHandler.startDocument();
    }

    public SAXEvent startElement( final String uri, final String localName, final String qName, final Attributes atts )
    {
        return () -> contentHandler.startElement( uri, localName, qName, atts );
    }

    public SAXEvent startPrefixMapping( final String prefix, final String uri )
    {
        return () -> contentHandler.startPrefixMapping( prefix, uri );
    }

    public static SAXEventFactory newInstance( ContentHandler contentHandler, LexicalHandler lexicalHandler )
    {
        return new SAXEventFactory( contentHandler, lexicalHandler );
    }

    public SAXEvent startDTD( String name, String publicId, String systemId )
    {
        return () -> lexicalHandler.startDTD( name, publicId, systemId );
    }

    public SAXEvent endDTD()
    {
        return () -> lexicalHandler.endDTD();
    }

    public SAXEvent startEntity( String name )
    {
        return () -> lexicalHandler.startEntity( name );
    }

    public SAXEvent endEntity( String name )
    {
        return () -> lexicalHandler.endEntity( name );

    }

    public SAXEvent startCDATA()
    {
        return () -> lexicalHandler.startCDATA();
    }

    public SAXEvent endCDATA()
    {
        return () -> lexicalHandler.endCDATA();
    }

    public SAXEvent comment( char[] ch, int start, int length )
    {
        final char[] txt = new char[length];
        System.arraycopy( ch, start, txt, 0, length );
        return () -> lexicalHandler.comment(  txt, 0, length );
    }
}
