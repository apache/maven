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
package org.apache.maven.model.transform;

import java.util.List;
import java.util.function.Function;

import org.apache.maven.model.transform.pull.NodeBufferingParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;

/**
 * Resolves all ci-friendly properties occurrences between version-tags
 *
 * @author Robert Scholte
 * @author Guillaume Nodet
 * @since 4.0.0
 */
class CiFriendlyXMLFilter
    extends NodeBufferingParser
{
    private final boolean replace;

    private Function<String, String> replaceChain = Function.identity();

    CiFriendlyXMLFilter( XmlPullParser xmlPullParser, boolean replace )
    {
        super( xmlPullParser, "version" );
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
    protected void process( List<Event> buffer )
    {
        for ( Event event : buffer )
        {
            if ( event.event == TEXT && replace && event.text.contains( "${" ) )
            {
                event.text = replaceChain.apply( event.text );
            }
            pushEvent( event );
        }
    }

}
