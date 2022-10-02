package org.apache.maven.internal.xml;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.api.xml.Dom;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;

public class XmlPlexusConfiguration extends DefaultPlexusConfiguration
{
    public static PlexusConfiguration toPlexusConfiguration( Dom node )
    {
        return new XmlPlexusConfiguration( node );
    }

    public XmlPlexusConfiguration( Dom node )
    {
        super( node.getName(), node.getValue() );
        node.getAttributes().forEach( this::setAttribute );
        node.getChildren().forEach( c -> this.addChild( new XmlPlexusConfiguration( c ) ) );
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder().append( '<' ).append( getName() );
        for ( final String a : getAttributeNames() )
        {
            buf.append( ' ' ).append( a ).append( "=\"" ).append( getAttribute( a ) ).append( '"' );
        }
        if ( getChildCount() > 0 )
        {
            buf.append( '>' );
            for ( int i = 0, size = getChildCount(); i < size; i++ )
            {
                buf.append( getChild( i ) );
            }
            buf.append( "</" ).append( getName() ).append( '>' );
        }
        else if ( null != getValue() )
        {
            buf.append( '>' ).append( getValue() ).append( "</" ).append( getName() ).append( '>' );
        }
        else
        {
            buf.append( "/>" );
        }
        return buf.append( '\n' ).toString();
    }

}
