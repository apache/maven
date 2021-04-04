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
import java.util.Deque;

import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;

/**
 * This filter will skip all following filters and write directly to the output.
 * Should be used in case of a DOM that should not be effected by other filters, even though the elements match
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
class FastForwardFilter extends AbstractSAXFilter
{
    /**
     * DOM elements of pom
     *
     * <ul>
     *  <li>execution.configuration</li>
     *  <li>plugin.configuration</li>
     *  <li>plugin.goals</li>
     *  <li>profile.reports</li>
     *  <li>project.reports</li>
     *  <li>reportSet.configuration</li>
     * <ul>
     */
    private final Deque<String> state = new ArrayDeque<>();

    private int domDepth = 0;

    private ContentHandler originalHandler;

    FastForwardFilter()
    {
        super();
    }

    FastForwardFilter( AbstractSAXFilter parent )
    {
        super( parent );
    }

    @Override
    public void startElement( String uri, String localName, String qName, Attributes atts )
        throws SAXException
    {
        super.startElement( uri, localName, qName, atts );
        if ( domDepth > 0 )
        {
            domDepth++;
        }
        else
        {
            final String key = state.peek() + '.' + localName;
            switch ( key )
            {
                case "execution.configuration":
                case "plugin.configuration":
                case "plugin.goals":
                case "profile.reports":
                case "project.reports":
                case "reportSet.configuration":
                    domDepth++;

                    originalHandler = getContentHandler();

                    ContentHandler outputContentHandler = getContentHandler();
                    while ( outputContentHandler instanceof XMLFilter )
                    {
                        outputContentHandler = ( (XMLFilter) outputContentHandler ).getContentHandler();
                    }
                    setContentHandler( outputContentHandler );
                    break;
                default:
                    break;
            }
            state.push( localName );
        }
    }

    @Override
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( domDepth > 0 )
        {
            domDepth--;

            if ( domDepth == 0 )
            {
                setContentHandler( originalHandler );
            }
        }
        else
        {
            state.pop();
        }
        super.endElement( uri, localName, qName );
    }


}