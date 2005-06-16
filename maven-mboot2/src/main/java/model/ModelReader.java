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

import download.ArtifactDownloader;
import download.DownloadFailedException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import util.AbstractReader;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private Map dependencies = new HashMap();

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

    private final boolean resolveTransitiveDependencies;

    private Repository currentRepository;

    private final ArtifactDownloader downloader;

    private static Set inProgress = new HashSet();

    private Map parentDependencies = new HashMap();

    private Map transitiveDependencies = new HashMap();

    private boolean insideDependencyManagement = false;

    public ModelReader( ArtifactDownloader downloader, boolean resolveTransitiveDependencies )
    {
        this.downloader = downloader;

        this.resolveTransitiveDependencies = resolveTransitiveDependencies;
    }

    public List getRemoteRepositories()
    {
        return repositories;
    }

    public Collection getDependencies()
    {
        Map m = new HashMap();
        m.putAll( transitiveDependencies );
        m.putAll( parentDependencies );
        m.putAll( dependencies );
        return m.values();
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
            if ( !insideDependencyManagement )
            {
                currentDependency = new Dependency();

                insideDependency = true;
            }
        }
        else if ( rawName.equals( "dependencyManagement" ) )
        {
            insideDependencyManagement = true;
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

            // actually, these should be transtive (see MNG-77) - but some projects have circular deps that way (marmalade, and currently m2)
            ModelReader p = retrievePom( parentGroupId, parentArtifactId, parentVersion, "pom", false );

            addDependencies( p.getDependencies(), parentDependencies, null );

            resources.addAll( p.getResources() );

            insideParent = false;
        }
        else if ( rawName.equals( "dependency" ) )
        {
            insideDependency = false;

            if ( !hasDependency( currentDependency, dependencies ) )
            {
                if ( resolveTransitiveDependencies )
                {
                    ModelReader p = retrievePom( currentDependency.getGroupId(), currentDependency.getArtifactId(),
                                                 currentDependency.getVersion(), currentDependency.getType(),
                                                 resolveTransitiveDependencies );

                    addDependencies( p.getDependencies(), transitiveDependencies, currentDependency.getScope() );
                }
            }
            dependencies.put( currentDependency.getConflictId(), currentDependency );
        }
        else if ( rawName.equals( "dependencyManagement" ) )
        {
            insideDependencyManagement = false;
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
            if ( rawName.equals( "id" ) )
            {
                currentRepository.setId( getBodyText() );
            }
            else if ( rawName.equals( "url" ) )
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

    private void addDependencies( Collection dependencies, Map target, String inheritedScope )
    {
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            // Do we care about runtime here?
            if ( Dependency.SCOPE_TEST.equals( inheritedScope ) )
            {
                d.setScope( Dependency.SCOPE_TEST );
            }

            if ( !hasDependency( d, target ) )
            {
                target.put( d.getConflictId(), d );
            }
        }
    }

    private boolean hasDependency( Dependency d, Map dependencies )
    {
        String conflictId = d.getConflictId();
        if ( dependencies.containsKey( conflictId ) )
        {
            // We only care about pushing in compile scope dependencies I think
            // if not, we'll need to be able to get the original and pick the appropriate scope
            if ( d.getScope().equals( Dependency.SCOPE_COMPILE ) )
            {
                dependencies.remove( conflictId );
            }
            else
            {
                return true;
            }
        }
        return false;
    }

    private ModelReader retrievePom( String groupId, String artifactId, String version, String type,
                                     boolean resolveTransitiveDependencies )
        throws SAXException
    {
        String key = groupId + ":" + artifactId + ":" + version;

        if ( inProgress.contains( key ) )
        {
            throw new SAXException( "Circular dependency found, looking for " + key + "\nIn progress:" + inProgress );
        }

        inProgress.add( key );

        ModelReader p = new ModelReader( downloader, resolveTransitiveDependencies );

        try
        {
            Dependency pom = new Dependency( groupId, artifactId, version, type );
            downloader.downloadDependencies( Collections.singletonList( pom ) );

            Repository localRepository = downloader.getLocalRepository();
            p.parse(
                localRepository.getMetadataFile( groupId, artifactId, version, type,
                                                 artifactId + "-" + pom.getResolvedVersion() + ".pom" ) );
        }
        catch ( IOException e )
        {
            throw new SAXException( "Error getting parent POM", e );
        }
        catch ( ParserConfigurationException e )
        {
            throw new SAXException( "Error getting parent POM", e );
        }
        catch ( DownloadFailedException e )
        {
            throw new SAXException( "Error getting parent POM", e );
        }

        inProgress.remove( key );

        return p;
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
