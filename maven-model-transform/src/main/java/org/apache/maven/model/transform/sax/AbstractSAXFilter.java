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

import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * XMLFilter with LexicalHandler.
 * Since some filters collect events before processing them, the LexicalHandler events must be collected too.
 * Otherwise the LexicalHandler events might end up before all collected XMLReader events.
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class AbstractSAXFilter extends XMLFilterImpl implements LexicalHandler
{
    private LexicalHandler lexicalHandler;

    public AbstractSAXFilter()
    {
        super();
    }

    public AbstractSAXFilter( AbstractSAXFilter parent )
    {
        super( parent );
        parent.setLexicalHandler( this );
    }

    public LexicalHandler getLexicalHandler()
    {
        return lexicalHandler;
    }

    public void setLexicalHandler( LexicalHandler lexicalHandler )
    {
        this.lexicalHandler = lexicalHandler;
    }

    @Override
    public void startDTD( String name, String publicId, String systemId )
        throws SAXException
    {
        if ( lexicalHandler != null )
        {
            lexicalHandler.startDTD( name, publicId, systemId );
        }
    }

    @Override
    public void endDTD()
        throws SAXException
    {
        if ( lexicalHandler != null )
        {
            lexicalHandler.endDTD();
        }
    }

    @Override
    public void startEntity( String name )
        throws SAXException
    {
        if ( lexicalHandler != null )
        {
            lexicalHandler.startEntity( name );
        }
    }

    @Override
    public void endEntity( String name )
        throws SAXException
    {
        if ( lexicalHandler != null )
        {
            lexicalHandler.endEntity( name );
        }
    }

    @Override
    public void startCDATA()
        throws SAXException
    {
        if ( lexicalHandler != null )
        {
            lexicalHandler.startCDATA();
        }
    }

    @Override
    public void endCDATA()
        throws SAXException
    {
        if ( lexicalHandler != null )
        {
            lexicalHandler.endCDATA();
        }
    }

    @Override
    public void comment( char[] ch, int start, int length )
        throws SAXException
    {
        if ( lexicalHandler != null )
        {
            lexicalHandler.comment( ch, start, length );
        }
    }


    protected static String nullSafeAppend( String originalValue, String charSegment )
    {
        if ( originalValue == null )
        {
            return charSegment;
        }
        else
        {
            return originalValue + charSegment;
        }
    }


}
