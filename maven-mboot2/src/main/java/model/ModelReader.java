package model;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import util.AbstractReader;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse a POM.
 *
 * @version $Id$
 */
public class ModelReader
    extends AbstractReader
{
    private int depth = 0;

    private String artifactId;

    private String version;

    private String groupId;

    private String packaging = "jar";

    private String parentGroupId;

    private String parentArtifactId;

    private String parentVersion;

    private List dependencies = new ArrayList();

    private List repositories = new ArrayList();

    private List resources = new ArrayList();

    private List testResources = new ArrayList();

    private Dependency currentDependency;

    private Resource currentResource;

    private boolean insideParent = false;

    private boolean insideDependency = false;

    private boolean insideResource = false;

    private boolean insideRepository = false;

    private StringBuffer bodyText = new StringBuffer();

    private final Repository localRepository;

    private Repository currentRepository;

    public ModelReader( Repository downloader )
    {
        this.localRepository = downloader;
    }

    public List getRemoteRepositories()
    {
        return repositories;
    }

    public List getDependencies()
    {
        return dependencies;
    }

    public List getResources()
    {
        return resources;
    }

    public void startElement( String uri, String localName, String rawName, Attributes attributes )
    {
        if ( rawName.equals( "parent" ) )
        {
            insideParent = true;
        }
        else if ( rawName.equals( "repository" ) )
        {
            currentRepository = new Repository();

            insideRepository = true;
        }
        else if ( rawName.equals( "dependency" ) )
        {
            currentDependency = new Dependency();

            insideDependency = true;
        }
        else if ( rawName.equals( "resource" ) )
        {
            currentResource = new Resource();

            insideResource = true;
        }
        else if ( rawName.equals( "testResource" ) )
        {
            currentResource = new Resource();

            insideResource = true;
        }
        depth++;
    }

    public void characters( char buffer[], int start, int length )
    {
        bodyText.append( buffer, start, length );
    }

    private String getBodyText()
    {
        return bodyText.toString().trim();
    }

    public void endElement( String uri, String localName, String rawName )
        throws SAXException
    {
        // support both v3 <extend> and v4 <parent>
        if ( rawName.equals( "parent" ) )
        {
            if ( parentArtifactId == null || parentArtifactId.trim().length() == 0 )
            {
                throw new SAXException( "Missing required element in <parent>: artifactId." );
            }

            if ( parentGroupId == null || parentGroupId.trim().length() == 0 )
            {
                throw new SAXException( "Missing required element in <parent>: groupId." );
            }

            if ( parentVersion == null || parentVersion.trim().length() == 0 )
            {
                throw new SAXException( "Missing required element in <parent>: version." );
            }

            if ( groupId == null )
            {
                groupId = parentGroupId;
            }

            if ( version == null )
            {
                version = parentVersion;
            }

            ModelReader p = new ModelReader( localRepository );

            try
            {
                p.parse( localRepository.getArtifactFile( parentGroupId, parentArtifactId, parentVersion, "pom" ) );
            }
            catch ( ParserConfigurationException e )
            {
                throw new SAXException( "Error getting parent POM", e );
            }
            catch ( IOException e )
            {
                throw new SAXException( "Error getting parent POM", e );
            }

            dependencies.addAll( p.getDependencies() );

            resources.addAll( p.getResources() );

            insideParent = false;
        }
        else if ( rawName.equals( "dependency" ) )
        {
            dependencies.add( currentDependency );

            insideDependency = false;
        }
        else if ( rawName.equals( "resource" ) )
        {
            resources.add( currentResource );

            insideResource = false;
        }
        else if ( rawName.equals( "testResource" ) )
        {
            testResources.add( currentResource );

            insideResource = false;
        }
        else if ( rawName.equals( "repository" ) )
        {
            repositories.add( currentRepository );

            insideRepository = false;
        }
        else if ( insideParent )
        {
            if ( rawName.equals( "groupId" ) )
            {
                parentGroupId = getBodyText();
            }
            else if ( rawName.equals( "artifactId" ) )
            {
                parentArtifactId = getBodyText();
            }
            else if ( rawName.equals( "version" ) )
            {
                parentVersion = getBodyText();
            }
        }
        else if ( insideDependency )
        {
            if ( rawName.equals( "id" ) )
            {
                currentDependency.setId( getBodyText() );
            }
            else if ( rawName.equals( "version" ) )
            {
                currentDependency.setVersion( getBodyText() );
            }
            else if ( rawName.equals( "jar" ) )
            {
                currentDependency.setJar( getBodyText() );
            }
            else if ( rawName.equals( "type" ) )
            {
                currentDependency.setType( getBodyText() );
            }
            else if ( rawName.equals( "groupId" ) )
            {
                currentDependency.setGroupId( getBodyText() );
            }
            else if ( rawName.equals( "artifactId" ) )
            {
                currentDependency.setArtifactId( getBodyText() );
            }
            else if ( rawName.equals( "scope" ) )
            {
                currentDependency.setScope( getBodyText() );
            }
        }
        else if ( insideResource )
        {
            if ( rawName.equals( "directory" ) )
            {
                currentResource.setDirectory( getBodyText() );
            }
            else if ( rawName.equals( "include" ) )
            {
                currentResource.addInclude( getBodyText() );
            }
            else if ( rawName.equals( "exclude" ) )
            {
                currentResource.addExclude( getBodyText() );
            }
        }
        else if ( insideRepository )
        {
            if ( rawName.equals( "url" ) )
            {
                currentRepository.setBasedir( getBodyText() );
            }
            else if ( rawName.equals( "layout" ) )
            {
                currentRepository.setLayout( getBodyText() );
            }
        }
        else if ( depth == 2 )
        {
            if ( rawName.equals( "artifactId" ) )
            {
                artifactId = getBodyText();
            }
            else if ( rawName.equals( "version" ) )
            {
                version = getBodyText();
            }
            else if ( rawName.equals( "groupId" ) )
            {
                groupId = getBodyText();
            }
            else if ( rawName.equals( "packaging" ) )
            {
                packaging = getBodyText();
            }
        }

        bodyText = new StringBuffer();

        depth--;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getPackaging()
    {
        return packaging;
    }
}
