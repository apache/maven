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

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.xml.sax.SAXEventUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * <p>
 * Transforms relativePath to version.
 * We could decide to simply allow {@code <parent/>}, but let's require the GA for now for checking
 * This filter does NOT remove the relativePath (which is done by {@link RelativePathXMLFilter}, it will only 
 * optionally include the version based on the path 
 * </p>
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
class ParentXMLFilter
    extends AbstractEventXMLFilter
{
    private boolean parsingParent;

    // states
    private String state;

    private String groupId;

    private String artifactId;
    
    private String relativePath;

    private boolean hasVersion;
    
    private Optional<RelativeProject> resolvedParent;

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
    
    @Override
    protected boolean isParsing()
    {
        return parsingParent;
    }

    @Override
    protected String getState()
    {
        return state;
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
        
        super.startElement( uri, localName, qName, atts );
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
        
        super.characters( ch, start, length );
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
                    
                    if ( !hasVersion && resolvedParent.isPresent() )
                    {
                        String versionQName = SAXEventUtils.renameQName( qName, "version" );
                        
                        super.startElement( uri, "version", versionQName, null );
                        
                        String resolvedParentVersion = resolvedParent.get().getVersion();
                        
                        super.characters( resolvedParentVersion.toCharArray(), 0,
                                                      resolvedParentVersion.length() );
                        
                        super.endElement( uri, "version", versionQName );
                    }
                    executeEvents();
                    
                    parsingParent = false;
                    break;
                default:
                    break;
            }
        }
        
        super.endElement( uri, localName, qName );

        // for this simple structure resetting to parent it sufficient
        state = "parent";
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
