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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.apache.maven.model.transform.sax.AbstractSAXFilter;

/**
 * Remove all modules, this is just buildtime information
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
class ModulesXMLFilter
    extends AbstractEventXMLFilter
{
    private boolean parsingModules;

    private String state;

    ModulesXMLFilter()
    {
        super();
    }

    ModulesXMLFilter( AbstractSAXFilter parent )
    {
        super( parent );
    }

    @Override
    public void startElement( String uri, String localName, String qName, Attributes atts )
        throws SAXException
    {
        if ( !parsingModules && "modules".equals( localName ) )
        {
            parsingModules = true;
        }

        if ( parsingModules )
        {
            state = localName;
        }

        super.startElement( uri, localName, qName, atts );
    }

    @Override
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( parsingModules )
        {
            switch ( localName )
            {
                case "modules":
                    executeEvents();

                    parsingModules = false;
                    break;
                default:
                    super.endElement( uri, localName, qName );
                    break;
            }
        }
        else
        {
            super.endElement( uri, localName, qName );
        }

        // for this simple structure resetting to modules it sufficient
        state = "modules";
    }

    @Override
    protected boolean isParsing()
    {
        return parsingModules;
    }

    @Override
    protected String getState()
    {
        return state;
    }

    @Override
    protected boolean acceptEvent( String state )
    {
        return false;
    }
}
