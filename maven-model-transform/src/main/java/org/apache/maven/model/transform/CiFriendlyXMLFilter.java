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

import java.util.function.Function;

import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Resolves all ci-friendly properties occurrences between version-tags
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
class CiFriendlyXMLFilter
    extends AbstractSAXFilter
{
    private final boolean replace;

    private Function<String, String> replaceChain = Function.identity();

    private String characters;

    private boolean parseVersion;

    CiFriendlyXMLFilter( boolean replace )
    {
        this.replace = replace;
    }

    CiFriendlyXMLFilter( AbstractSAXFilter parent, boolean replace )
    {
        super( parent );
        this.replace = replace;
    }

    public CiFriendlyXMLFilter setChangelist( String changelist )
    {
        replaceChain = replaceChain.andThen( t -> t.replace( "${changelist}", changelist ) );
        return this;
    }

    public CiFriendlyXMLFilter setRevision( String revision )
    {
        replaceChain = replaceChain.andThen( t -> t.replace( "${revision}", revision ) );
        return this;
    }

    public CiFriendlyXMLFilter setSha1( String sha1 )
    {
        replaceChain = replaceChain.andThen( t -> t.replace( "${sha1}", sha1 ) );
        return this;
    }

    /**
     * @return {@code true} is any of the ci properties is set, otherwise {@code false}
     */
    public boolean isSet()
    {
        return !replaceChain.equals( Function.identity() );
    }

    @Override
    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        if ( parseVersion )
        {
            this.characters = nullSafeAppend( characters, new String( ch, start, length ) );
        }
        else
        {
            super.characters( ch, start, length );
        }
    }

    @Override
    public void startElement( String uri, String localName, String qName, Attributes atts )
        throws SAXException
    {
        if ( !parseVersion && "version".equals( localName ) )
        {
            parseVersion = true;
        }

        super.startElement( uri, localName, qName, atts );
    }

    @Override
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( parseVersion )
        {
            // assuming this has the best performance
            if ( replace && characters != null && characters.contains( "${" ) )
            {
                char[] ch = replaceChain.apply( characters ).toCharArray();
                super.characters( ch, 0, ch.length );
            }
            else
            {
                char[] ch = characters.toCharArray();
                super.characters( ch, 0, ch.length );
            }
            characters = null;
            parseVersion = false;
        }

        super.endElement( uri, localName, qName );
    }
}
