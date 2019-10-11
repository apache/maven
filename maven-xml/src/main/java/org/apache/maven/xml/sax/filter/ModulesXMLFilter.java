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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * Remove all modules, this is just buildtime information
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
class ModulesXMLFilter
    extends AbstractSAXFilter
{
    /**
     * Using 3 to also remove whitespace-block after closing tag
     * -1 none
     *  1 started
     *  2 ended
     */
    int modulesStatus = -1;
    
    ModulesXMLFilter()
    {
        super();
    }

    <T extends XMLReader & LexicalHandler> ModulesXMLFilter( T parent )
    {
        super( parent );
    }

    @Override
    public void startElement( String uri, String localName, String qName, Attributes atts )
        throws SAXException
    {
        if ( modulesStatus == -1 && "modules".equals( localName ) )
        {
            modulesStatus = 1;
        }
        else if ( modulesStatus == 2 )
        {
            modulesStatus = -1;
        }

        if ( modulesStatus != 1 )
        {
            super.startElement( uri, localName, qName, atts );
        }
    }

    @Override
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( modulesStatus == 1 && "modules".equals( localName ) )
        {
            modulesStatus = 2;
        }
        else if ( modulesStatus == 2 )
        {
            modulesStatus = -1;
        }
        
        if ( modulesStatus == -1 )
        {
            super.endElement( uri, localName, qName );
        }
    }

    @Override
    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        if ( modulesStatus == -1 )
        {
            super.characters( ch, start, length );
        }
    }
    
    @Override
    public void comment( char[] ch, int start, int length )
        throws SAXException
    {
        if ( modulesStatus != 1 )
        {
            super.comment( ch, start, length );
        }
    }
}
