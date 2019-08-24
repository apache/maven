package org.apache.maven.xml.filter;

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

import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Resolves all ci-friendly properties occurrences
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
class CiFriendlyXMLFilter
    extends XMLFilterImpl
{
    private Function<String, String> replaceChain = Function.identity();
    
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
        String text = new String( ch, start, length );

        // assuming this has the best performance
        if ( text.contains( "${" ) )
        {
            String newText = replaceChain.apply( text );
            
            super.characters( newText.toCharArray(), 0, newText.length() );
        }
        else
        {
            super.characters( ch, start, length );
        }
    }
    
    
}