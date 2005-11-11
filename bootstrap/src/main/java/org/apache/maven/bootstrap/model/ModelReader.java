package org.apache.maven.bootstrap.model;

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

import org.apache.maven.bootstrap.download.DownloadFailedException;
import org.apache.maven.bootstrap.download.ArtifactResolver;
import org.apache.maven.bootstrap.util.AbstractReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

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

import com.sun.corba.se.impl.ior.ObjectAdapterIdArray;

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

    private Map managedDependencies = new HashMap();

    private Dependency currentDependency;

    private Resource currentResource;

    private boolean insideParent = false;

    private boolean insideDependency = false;

    private boolean insideResource = false;

    private boolean insideRepository = false;

    private StringBuffer bodyText = new StringBuffer();

    private final boolean resolveTransitiveDependencies;

    private Repository currentRepository;

    private final ArtifactResolver resolver;

    private static Set inProgress = new HashSet();

    private Map parentDependencies = new HashMap();

    private Map transitiveDependencies = new HashMap();

    private boolean insideDependencyManagement = false;

    private boolean insideReleases;

    private boolean insideSnapshots;

    private boolean insideExclusion;

    private Exclusion currentExclusion;

    private final Set excluded;

    private final List chain;

    private final String inheritedScope;

    private Map plugins = new HashMap();

    private boolean insideConfiguration;

    private boolean insideBuild;

    private Plugin currentPlugin;

    private boolean insidePlugin;

    private List modules = new ArrayList();

    public ModelReader( ArtifactResolver resolver, boolean resolveTransitiveDependencies )
    {
        this( resolver, null, resolveTransitiveDependencies, Collections.EMPTY_SET, Collections.EMPTY_LIST );
    }

    public ModelReader( ArtifactResolver resolver, String inheritedScope, boolean resolveTransitiveDependencies,
                        Set excluded, List chain )
    {
        this.resolver = resolver;

        this.resolveTransitiveDependencies = resolveTransitiveDependencies;

        this.excluded = excluded;

        this.inheritedScope = inheritedScope;

        this.chain = chain;
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

    public Collection getManagedDependencies()
    {
        Map m = new HashMap();
        m.putAll( managedDependencies );
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
            List chain =
                Collections.singletonList( new Dependency( groupId, artifactId, version, packaging, this.chain ) );
            currentDependency = new Dependency( chain );

            insideDependency = true;
        }
        else if ( rawName.equals( "build" ) && depth == 1 )
        {
            insideBuild = true;
        }
        else if ( rawName.equals( "plugin" ) )
        {
            currentPlugin = new Plugin();

            insidePlugin = true;
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
        else if ( rawName.equals( "snapshots" ) && insideRepository )
        {
            insideSnapshots = true;
        }
        else if ( rawName.equals( "releases" ) && insideRepository )
        {
            insideReleases = true;
        }
        else if ( rawName.equals( "exclusion" ) && insideDependency )
        {
            insideExclusion = true;

            currentExclusion = new Exclusion();
        }
        else if ( rawName.equals( "configuration" ) && insidePlugin )
        {
            insideConfiguration = true;
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

            // actually, these should be transtive (see MNG-77) - but some projects have circular deps that way
            ModelReader p = retrievePom( parentGroupId, parentArtifactId, parentVersion, "pom", inheritedScope, false,
                                         excluded, Collections.EMPTY_LIST );

            addDependencies( p.getDependencies(), parentDependencies, inheritedScope, excluded );

            addDependencies( p.getManagedDependencies(), managedDependencies, inheritedScope, Collections.EMPTY_SET );

            resources.addAll( p.getResources() );

            insideParent = false;
        }
        else if ( rawName.equals( "dependency" ) )
        {
            insideDependency = false;

            if ( insideDependencyManagement )
            {
                managedDependencies.put( currentDependency.getConflictId(), currentDependency );
            }
            else
            {
                dependencies.put( currentDependency.getConflictId(), currentDependency );
            }
        }
        else if ( rawName.equals( "exclusion" ) )
        {
            currentDependency.addExclusion( currentExclusion );
            insideExclusion = false;
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
        else if ( rawName.equals( "repository" ) )
        {
            repositories.add( currentRepository );

            insideRepository = false;
        }
        else if ( rawName.equals( "plugin" ) )
        {
            plugins.put( currentPlugin.getId(), currentPlugin );

            insidePlugin = false;
        }
        else if ( rawName.equals( "build" ) )
        {
            insideBuild = false;
        }
        else if ( rawName.equals( "module" ) )
        {
            modules.add( getBodyText() );
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
            if ( insideExclusion )
            {
                if ( rawName.equals( "groupId" ) )
                {
                    currentExclusion.setGroupId( getBodyText() );
                }
                else if ( rawName.equals( "artifactId" ) )
                {
                    currentExclusion.setArtifactId( getBodyText() );
                }
            }
            else if ( rawName.equals( "id" ) )
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
        else if ( insideBuild && insidePlugin )
        {
            if ( insideConfiguration )
            {
                if ( rawName.equals( "configuration" ) )
                {
                    insideConfiguration = false;
                }
                else
                {
                    currentPlugin.getConfiguration().put( rawName, getBodyText() );
                }
            }
            else if ( rawName.equals( "groupId" ) )
            {
                currentPlugin.setGroupId( getBodyText() );
            }
            else if ( rawName.equals( "artifactId" ) )
            {
                currentPlugin.setArtifactId( getBodyText() );
            }
            else if ( rawName.equals( "version" ) )
            {
                currentPlugin.setVersion( getBodyText() );
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
            else if ( rawName.equals( "enabled" ) )
            {
                if ( insideSnapshots )
                {
                    currentRepository.setSnapshots( Boolean.valueOf( getBodyText() ).booleanValue() );
                }
                else if ( insideReleases )
                {
                    currentRepository.setReleases( Boolean.valueOf( getBodyText() ).booleanValue() );
                }
            }
            else if ( rawName.equals( "snapshots" ) )
            {
                insideSnapshots = false;
            }
            else if ( rawName.equals( "releases" ) )
            {
                insideReleases = false;
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

        if ( depth == 1 ) // model / project
        {
            resolver.addBuiltArtifact( groupId, artifactId, "pom", pomFile );

            resolveDependencies();
        }

        bodyText = new StringBuffer();

        depth--;
    }

    private void resolveDependencies()
        throws SAXException
    {
        for ( Iterator it = dependencies.values().iterator(); it.hasNext(); )
        {
            Dependency dependency = (Dependency) it.next();

            if ( !excluded.contains( dependency.getConflictId() ) )
            {
                if ( !dependency.getScope().equals( Dependency.SCOPE_TEST ) || inheritedScope == null )
                {
                    if ( dependency.getVersion() == null )
                    {
                        Dependency managedDependency =
                            (Dependency) managedDependencies.get( dependency.getConflictId() );
                        if ( managedDependency == null )
                        {
                            throw new NullPointerException( "[" + groupId + ":" + artifactId + ":" + packaging + ":" +
                                version + "] " + "Dependency " + dependency.getConflictId() +
                                " is missing a version, and nothing is found in dependencyManagement. " );
                        }
                        dependency.setVersion( managedDependency.getVersion() );
                    }

                    if ( resolveTransitiveDependencies )
                    {
                        Set excluded = new HashSet( this.excluded );
                        excluded.addAll( dependency.getExclusions() );

                        ModelReader p = retrievePom( dependency.getGroupId(), dependency.getArtifactId(),
                                                     dependency.getVersion(), dependency.getType(),
                                                     dependency.getScope(), resolveTransitiveDependencies, excluded,
                                                     dependency.getChain() );

                        addDependencies( p.getDependencies(), transitiveDependencies, dependency.getScope(), excluded );
                    }
                }
            }
        }
    }

    private void addDependencies( Collection dependencies, Map target, String inheritedScope, Set excluded )
    {
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            // skip test deps
            if ( !Dependency.SCOPE_TEST.equals( d.getScope() ) )
            {
                // Do we care about runtime here?
                if ( Dependency.SCOPE_TEST.equals( inheritedScope ) )
                {
                    d.setScope( Dependency.SCOPE_TEST );
                }

                if ( !hasDependency( d, target ) && !excluded.contains( d.getConflictId() ) )
                {
                    if ( !"plexus".equals( d.getGroupId() ) || ( !"plexus-utils".equals( d.getArtifactId() ) &&
                        !"plexus-container-default".equals( d.getArtifactId() ) ) )
                    {
                        target.put( d.getConflictId(), d );
                    }
                }
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
                                     String inheritedScope, boolean resolveTransitiveDependencies, Set excluded,
                                     List chain )
        throws SAXException
    {
        String key = groupId + ":" + artifactId + ":" + version;

        if ( inProgress.contains( key ) )
        {
            throw new SAXException( "Circular dependency found, looking for " + key + "\nIn progress:" + inProgress );
        }

        inProgress.add( key );

        ModelReader p = new ModelReader( resolver, inheritedScope, resolveTransitiveDependencies, excluded, chain );

        try
        {
            Dependency pom = new Dependency( groupId, artifactId, version, "pom", chain );

            resolver.downloadDependencies( Collections.singletonList( pom ) );

            p.parse( resolver.getArtifactFile( pom ) );
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

    public Map getPlugins()
    {
        return plugins;
    }

    public List getModules()
    {
        return modules;
    }
}
