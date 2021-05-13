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

/**
 * During parsing the line separators are transformed to \n
 * Unlike characters(), comments don't use the systems line separator for serialization.
 * Hence use this class in the LexicalHandler chain to do so
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class CommentRenormalizer implements LexicalHandler
{
    private final LexicalHandler lexicalHandler;

    private final String lineSeparator;

    public CommentRenormalizer( LexicalHandler lexicalHandler )
    {
        this( lexicalHandler,  System.lineSeparator() );
    }

    // for testing purpose
    CommentRenormalizer( LexicalHandler lexicalHandler, String lineSeparator )
    {
        this.lexicalHandler = lexicalHandler;
        this.lineSeparator = lineSeparator;
    }

    @Override
    public void comment( char[] ch, int start, int length )
        throws SAXException
    {
        if ( "\n".equals( lineSeparator ) )
        {
            lexicalHandler.comment( ch, start, length );
        }
        else
        {
            char[] ca = new String( ch, start, length ).replaceAll( "\n", lineSeparator ).toCharArray();

            lexicalHandler.comment( ca, 0, ca.length );
        }
    }

    @Override
    public void startDTD( String name, String publicId, String systemId )
        throws SAXException
    {
        lexicalHandler.startDTD( name, publicId, systemId );
    }

    @Override
    public void endDTD()
        throws SAXException
    {
        lexicalHandler.endDTD();
    }

    @Override
    public void startEntity( String name )
        throws SAXException
    {
        lexicalHandler.startEntity( name );
    }

    @Override
    public void endEntity( String name )
        throws SAXException
    {
        lexicalHandler.endEntity( name );
    }

    @Override
    public void startCDATA()
        throws SAXException
    {
        lexicalHandler.startCDATA();
    }

    @Override
    public void endCDATA()
        throws SAXException
    {
        lexicalHandler.endCDATA();
    }
}
