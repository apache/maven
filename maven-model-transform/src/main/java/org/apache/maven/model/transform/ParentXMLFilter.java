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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.model.transform.sax.SAXEventUtils;
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
 * @since 4.0.0
 */
class ParentXMLFilter
    extends AbstractEventXMLFilter
{
    private boolean parsingParent;

    // states
    private String state;

    // whiteSpace after <parent>, to be used to position <version>
    private String parentWhitespace = "";

    private String groupId;

    private String artifactId;

    private String relativePath;

    private boolean hasVersion;

    private boolean hasRelativePath;

    private Optional<RelativeProject> resolvedParent;

    private final Function<Path, Optional<RelativeProject>> relativePathMapper;

    private Path projectPath;

    /**
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

            // can be set to empty on purpose to enforce repository download
            hasRelativePath |= "relativePath".equals( localName );
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

            final String charSegment =  new String( ch, start, length );

            switch ( eventState )
            {
                case "parent":
                    parentWhitespace = nullSafeAppend( parentWhitespace, charSegment );
                    break;
                case "relativePath":
                    relativePath = nullSafeAppend( relativePath, charSegment );
                    break;
                case "groupId":
                    groupId = nullSafeAppend( groupId, charSegment );
                    break;
                case "artifactId":
                    artifactId = nullSafeAppend( artifactId, charSegment );
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
                    if ( !hasVersion && ( !hasRelativePath || relativePath != null ) )
                    {
                        resolvedParent =
                            resolveRelativePath( Paths.get( Objects.toString( relativePath, "../pom.xml" ) ) );
                    }
                    else
                    {
                        resolvedParent = Optional.empty();
                    }

                    if ( !hasVersion && resolvedParent.isPresent() )
                    {
                        try ( Includer i = super.include() )
                        {
                            super.characters( parentWhitespace.toCharArray(), 0,
                                              parentWhitespace.length() );

                            String versionQName = SAXEventUtils.renameQName( qName, "version" );

                            super.startElement( uri, "version", versionQName, null );

                            String resolvedParentVersion = resolvedParent.get().getVersion();

                            super.characters( resolvedParentVersion.toCharArray(), 0,
                                                          resolvedParentVersion.length() );

                            super.endElement( uri, "version", versionQName );
                        }
                    }
                    super.executeEvents();

                    parsingParent = false;
                    break;
                default:
                    // marker?
                    break;
            }
        }

        super.endElement( uri, localName, qName );
        state = "";
    }

    protected Optional<RelativeProject> resolveRelativePath( Path relativePath )
    {
        Path pomPath = projectPath.resolve( relativePath );
        if ( Files.isDirectory( pomPath ) )
        {
            pomPath = pomPath.resolve( "pom.xml" );
        }

        Optional<RelativeProject> mappedProject = relativePathMapper.apply( pomPath.normalize() );

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
