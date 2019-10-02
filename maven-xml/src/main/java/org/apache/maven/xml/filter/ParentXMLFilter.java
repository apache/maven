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

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.xml.SAXEvent;
import org.apache.maven.xml.SAXEventFactory;
import org.apache.maven.xml.SAXEventUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * <p>
 * Transforms relativePath to version.
 * We could decide to simply allow {@code <parent/>}, but let's require the GA for now for checking
 * </p>
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
class ParentXMLFilter
    extends XMLFilterImpl
{
    private boolean parsingParent;

    // states
    private String state;

    private String groupId;

    private String artifactId;
    
    private String relativePath;

    private boolean hasVersion;
    
    private Optional<RelativeProject> resolvedParent;

    private char[] linebreak;
    
    private List<SAXEvent> saxEvents = new ArrayList<>();

    private SAXEventFactory eventFactory;
    
    private final Function<Path, Optional<RelativeProject>> relativePathMapper;
    
    private Path projectPath;

    /**
     * 
     * 
     * @param relativePathMapper
     */
    ParentXMLFilter( Function<Path, Optional<RelativeProject>> relativePathMapper )
    {
        this.relativePathMapper = relativePathMapper;
    }

    public void setProjectPath( Path projectPath )
    {
        this.projectPath = projectPath;
    }
    
    private SAXEventFactory getEventFactory()
    {
        if ( eventFactory == null )
        {
            eventFactory = SAXEventFactory.newInstance( getContentHandler() );
        }
        return eventFactory;
    }

    private void processEvent( final SAXEvent event )
        throws SAXException
    {
        if ( parsingParent )
        {
            final String eventState = state;

            saxEvents.add( () -> 
            {
                if ( !( "relativePath".equals( eventState ) && resolvedParent.isPresent() ) )
                {
                    event.execute();
                }
            } );
        }
        else
        {
            event.execute();
        }
    }

    @Override
    public void startElement( String uri, final String localName, String qName, Attributes atts )
        throws SAXException
    {
        if ( !parsingParent && "parent".equals( localName ) )
        {
            parsingParent = true;
        }

        if ( parsingParent )
        {
            state = localName;
            
            hasVersion |= "version".equals( localName );
        }
        
        processEvent( getEventFactory().startElement( uri, localName, qName, atts ) );
    }

    @Override
    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        if ( parsingParent )
        {
            final String eventState = state;
            
            switch ( eventState )
            {
                case "parent":
                    int l;
                    for ( l = length ; l >= 0; l-- )
                    {
                        int i = start + l - 1; 
                        if ( ch[i] == '\n' || ch[i] == '\r' )
                        {
                            break;
                        }
                    }
                    
                    linebreak = new char[l];
                    System.arraycopy( ch, start, linebreak, 0, l );
                    break;
                case "relativePath":
                    relativePath = new String( ch, start, length );
                    break;
                case "groupId":
                    groupId = new String( ch, start, length );
                    break;
                case "artifactId":
                    artifactId = new String( ch, start, length );
                    break;
                default:
                    break;
            }
        }
        
        processEvent( getEventFactory().characters( ch, start, length ) );
    }

    @Override
    public void endDocument()
        throws SAXException
    {
        processEvent( getEventFactory().endDocument() );
    }

    @Override
    public void endElement( String uri, final String localName, String qName )
        throws SAXException
    {
        if ( parsingParent )
        {
            switch ( localName )
            {
                case "parent":
                    if ( !hasVersion || relativePath != null )
                    {
                        resolvedParent =
                            resolveRelativePath( Paths.get( Objects.toString( relativePath, "../pom.xml" ) ) );
                    }
                    
                    // not with streams due to checked SAXException
                    for ( SAXEvent saxEvent : saxEvents )
                    {
                        saxEvent.execute();
                    }
                    
                    if ( !hasVersion && resolvedParent.isPresent() )
                    {
                        String versionQName = SAXEventUtils.renameQName( qName, "version" );
                        
                        getEventFactory().startElement( uri, "version", versionQName, null ).execute();
                        
                        String resolvedParentVersion = resolvedParent.get().getVersion();
                        
                        getEventFactory().characters( resolvedParentVersion.toCharArray(), 0,
                                                      resolvedParentVersion.length() ).execute();
                        
                        getEventFactory().endElement( uri, "version", versionQName ).execute();
                        
                        if ( linebreak != null )
                        {
                            getEventFactory().characters( linebreak, 0, linebreak.length ).execute();
                        }
                    }
                    
                    parsingParent = false;
                    break;
                default:
                    break;
            }
        }
        
        processEvent( getEventFactory().endElement( uri, localName, qName ) );

        // for this simple structure resetting to parent it sufficient
        state = "parent";
    }

    @Override
    public void endPrefixMapping( String prefix )
        throws SAXException
    {
        processEvent( getEventFactory().endPrefixMapping( prefix ) );
    }

    @Override
    public void ignorableWhitespace( char[] ch, int start, int length )
        throws SAXException
    {
        processEvent( getEventFactory().ignorableWhitespace( ch, start, length ) );
    }

    @Override
    public void processingInstruction( String target, String data )
        throws SAXException
    {
        processEvent( getEventFactory().processingInstruction( target, data ) );

    }

    @Override
    public void setDocumentLocator( Locator locator )
    {
        try
        {
            processEvent( getEventFactory().setDocumentLocator( locator ) );
        }
        catch ( SAXException e )
        {
            // noop
        }
    }

    @Override
    public void skippedEntity( String name )
        throws SAXException
    {
        processEvent( getEventFactory().skippedEntity( name ) );
    }

    @Override
    public void startDocument()
        throws SAXException
    {
        processEvent( getEventFactory().startDocument() );
    }

    @Override
    public void startPrefixMapping( String prefix, String uri )
        throws SAXException
    {
        processEvent( getEventFactory().startPrefixMapping( prefix, uri ) );
    }

    protected Optional<RelativeProject> resolveRelativePath( Path relativePath )
    {
        Optional<RelativeProject> mappedProject =
            relativePathMapper.apply( projectPath.resolve( relativePath ).normalize() );
        
        if ( mappedProject.isPresent() )
        {
            RelativeProject project = mappedProject.get();
            
            if ( Objects.equals( groupId, project.getGroupId() )
                && Objects.equals( artifactId, project.getArtifactId() ) )
            {
                return mappedProject;
            }
        }
        return Optional.empty();
    }
}
