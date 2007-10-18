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

import org.apache.maven.bootstrap.download.ArtifactResolver;
import org.apache.maven.bootstrap.util.AbstractReader;
import org.apache.maven.bootstrap.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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

    private Model model;

    private Dependency currentDependency;

    private Resource currentResource;

    private boolean insideProfiles;

    private boolean insideParent;

    private boolean insideDependency;

    private boolean insideResource;

    private boolean insideRepository;

    private StringBuffer bodyText = new StringBuffer();

    private final boolean resolveTransitiveDependencies;

    private Repository currentRepository;

    private final ArtifactResolver resolver;

    private boolean insideDependencyManagement;

    private boolean insideDistributionManagement;

    private boolean insideReleases;

    private boolean insideSnapshots;

    private boolean insideExclusion;

    private Exclusion currentExclusion;

    private final Set excluded;

    private final String inheritedScope;

    private boolean insideConfiguration;

    private boolean insideBuild;

    private Plugin currentPlugin;

    private boolean insidePlugin;

    public ModelReader( ArtifactResolver resolver, boolean resolveTransitiveDependencies )
    {
        this( resolver, null, resolveTransitiveDependencies, Collections.EMPTY_SET );
    }

    public ModelReader( ArtifactResolver resolver, String inheritedScope, boolean resolveTransitiveDependencies,
                        Set excluded )
    {
        this.resolver = resolver;

        this.resolveTransitiveDependencies = resolveTransitiveDependencies;

        this.excluded = excluded;

        this.inheritedScope = inheritedScope;
    }

    public Model parseModel( File file, List chain )
        throws ParserConfigurationException, SAXException, IOException
    {
        this.model = new Model( chain );
        model.setPomFile( file );

        super.parse( file );

        return model;
    }

    public void startElement( String uri, String localName, String rawName, Attributes attributes )
    {
        // skip profile contents
        if ( insideProfiles )
        {
            return;
        }

        if ( rawName.equals( "parent" ) )
        {
            insideParent = true;
        }
        else if ( rawName.equals( "profiles" ) )
        {
            insideProfiles = true;
        }
        else if ( rawName.equals( "repository" ) )
        {
            currentRepository = new Repository();

            insideRepository = true;
        }
        else if ( rawName.equals( "dependency" ) )
        {
            currentDependency = new Dependency( model.getChain() );

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
        else if ( rawName.equals( "distributionManagement" ) )
        {
            insideDistributionManagement = true;
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
        // skip profile contents
        if ( insideProfiles )
        {
            return;
        }

        bodyText.append( buffer, start, length );
    }

    private String getBodyText()
    {
        return bodyText.toString().trim();
    }

    public void endElement( String uri, String localName, String rawName )
        throws SAXException
    {
        if ( rawName.equals( "profiles" ) )
        {
            insideProfiles = false;
        }

        if ( insideProfiles )
        {
            return;
        }

        // support both v3 <extend> and v4 <parent>
        if ( rawName.equals( "parent" ) )
        {
            if ( model.getParentArtifactId() == null || model.getParentArtifactId().trim().length() == 0 )
            {
                throw new SAXException( "Missing required element in <parent>: artifactId." );
            }

            if ( model.getParentGroupId() == null || model.getParentGroupId().trim().length() == 0 )
            {
                throw new SAXException( "Missing required element in <parent>: groupId." );
            }

            if ( model.getParentVersion() == null || model.getParentVersion().trim().length() == 0 )
            {
                throw new SAXException( "Missing required element in <parent>: version." );
            }

            if ( model.getGroupId() == null )
            {
                model.setGroupId( model.getParentGroupId() );
            }

            if ( model.getVersion() == null )
            {
                model.setVersion( model.getParentVersion() );
            }

            Model p = ProjectResolver.retrievePom( resolver, model.getParentGroupId(), model.getParentArtifactId(),
                                                   model.getParentVersion(), inheritedScope, false, excluded, model.getChain() );

            ProjectResolver.addDependencies( p.getAllDependencies(), model.getParentDependencies(), inheritedScope, excluded );

            ProjectResolver.addDependencies( p.getManagedDependenciesCollection(), model.getManagedDependencies(), inheritedScope, Collections.EMPTY_SET );

            model.getRepositories().addAll( p.getRepositories() );

            model.getResources().addAll( p.getResources() );

            insideParent = false;
        }
        else if ( rawName.equals( "dependency" ) )
        {
            insideDependency = false;

            if ( insideDependencyManagement )
            {
                model.getManagedDependencies().put( currentDependency.getConflictId(), currentDependency );
            }
            else
            {
                model.getDependencies().put( currentDependency.getConflictId(), currentDependency );
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
        else if ( rawName.equals( "distributionManagement" ) )
        {
            insideDistributionManagement = false;
        }
        else if ( rawName.equals( "resource" ) )
        {
            model.getResources().add( currentResource );

            insideResource = false;
        }
        else if ( rawName.equals( "repository" ) )
        {
            if ( !insideDistributionManagement )
            {
                model.getRepositories().add( currentRepository );
            }

            insideRepository = false;
        }
        else if ( rawName.equals( "plugin" ) )
        {
            model.getPlugins().put( currentPlugin.getId(), currentPlugin );

            insidePlugin = false;
        }
        else if ( rawName.equals( "build" ) )
        {
            insideBuild = false;
        }
        else if ( rawName.equals( "module" ) )
        {
            model.getModules().add( getBodyText() );
        }
        else if ( insideParent )
        {
            if ( rawName.equals( "groupId" ) )
            {
                model.setParentGroupId( getBodyText() );
            }
            else if ( rawName.equals( "artifactId" ) )
            {
                model.setParentArtifactId( getBodyText() );
            }
            else if ( rawName.equals( "version" ) )
            {
                model.setParentVersion( getBodyText() );
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
                currentDependency.setVersion( interpolate( getBodyText() ) );
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
                currentDependency.setGroupId( interpolate( getBodyText() ) );
            }
            else if ( rawName.equals( "artifactId" ) )
            {
                currentDependency.setArtifactId( getBodyText() );
            }
            else if ( rawName.equals( "scope" ) )
            {
                currentDependency.setScope( getBodyText() );
            }
            else if ( rawName.equals( "optional" ) )
            {
                currentDependency.setOptional( Boolean.valueOf( getBodyText() ).booleanValue() );
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
                model.setArtifactId( getBodyText() );
            }
            else if ( rawName.equals( "version" ) )
            {
                model.setVersion( getBodyText() );
            }
            else if ( rawName.equals( "groupId" ) )
            {
                model.setGroupId( getBodyText() );
            }
            else if ( rawName.equals( "packaging" ) )
            {
                model.setPackaging( getBodyText() );
            }
        }

        if ( depth == 1 ) // model / project
        {
            resolver.addBuiltArtifact( model.getGroupId(), model.getArtifactId(), "pom", model.getProjectFile() );

            ProjectResolver.resolveDependencies( resolver, model, resolveTransitiveDependencies, inheritedScope, excluded );
        }

        bodyText = new StringBuffer();

        depth--;
    }

    private String interpolate( String text )
    {
        Map map = new HashMap();
        map.put( "pom.groupId", model.getGroupId() );

        return StringUtils.interpolate( text, map );
    }
}
