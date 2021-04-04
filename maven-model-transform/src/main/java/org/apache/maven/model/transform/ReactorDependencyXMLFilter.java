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

import java.util.function.BiFunction;

import org.apache.maven.model.transform.sax.SAXEventUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Will apply the version if the dependency is part of the reactor
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
public class ReactorDependencyXMLFilter extends AbstractEventXMLFilter
{
    private boolean parsingDependency;

    // states
    private String state;

    // whiteSpace after <dependency>, to be used to position <version>
    private String dependencyWhitespace;

    private boolean hasVersion;

    private String groupId;

    private String artifactId;

    private final BiFunction<String, String, String> reactorVersionMapper;

    public ReactorDependencyXMLFilter( BiFunction<String, String, String> reactorVersionMapper )
    {
        this.reactorVersionMapper = reactorVersionMapper;
    }

    @Override
    public void startElement( String uri, String localName, String qName, Attributes atts )
        throws SAXException
    {
        if ( !parsingDependency && "dependency".equals( localName ) )
        {
            parsingDependency = true;
        }

        if ( parsingDependency )
        {
            state = localName;

            hasVersion |= "version".equals( localName );
        }
        super.startElement( uri, localName, qName, atts );
    }

    @Override
    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        if ( parsingDependency )
        {
            final String eventState = state;
            String value = new String( ch, start, length );
            switch ( eventState )
            {
                case "dependency":
                    dependencyWhitespace = nullSafeAppend( dependencyWhitespace, value );
                    break;
                case "groupId":
                    groupId = nullSafeAppend( groupId, value );
                    break;
                case "artifactId":
                    artifactId = nullSafeAppend( artifactId, value );
                    break;
                default:
                    break;
            }
        }
        super.characters( ch, start, length );
    }

    @Override
    public void endElement( String uri, final String localName, String qName )
        throws SAXException
    {
        if ( parsingDependency )
        {
            switch ( localName )
            {
                case "dependency":
                    if ( !hasVersion )
                    {
                        String version = getVersion();

                        // dependency is not part of reactor, probably it is managed
                        if ( version != null )
                        {
                            try ( Includer i = super.include() )
                            {
                                if ( dependencyWhitespace != null )
                                {
                                    super.characters( dependencyWhitespace.toCharArray(), 0,
                                                      dependencyWhitespace.length() );

                                }
                                String versionQName = SAXEventUtils.renameQName( qName, "version" );

                                super.startElement( uri, "version", versionQName, null );
                                super.characters( version.toCharArray(), 0, version.length() );
                                super.endElement( uri, "version", versionQName );
                            }
                        }
                    }
                    super.executeEvents();

                    parsingDependency = false;

                    // reset
                    hasVersion = false;
                    dependencyWhitespace = null;
                    groupId = null;
                    artifactId = null;

                    break;
                default:
                    break;
            }
        }

        super.endElement( uri, localName, qName );

        state = "";
    }

    private String getVersion()
    {
        return reactorVersionMapper.apply( groupId, artifactId  );
    }

    @Override
    protected boolean isParsing()
    {
        return parsingDependency;
    }

    @Override
    protected String getState()
    {
        return state;
    }

}
