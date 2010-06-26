package org.apache.maven.lifecycle.internal;

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

import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.prefix.DefaultPluginPrefixRequest;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.prefix.PluginPrefixRequest;
import org.apache.maven.plugin.prefix.PluginPrefixResolver;
import org.apache.maven.plugin.prefix.PluginPrefixResult;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * Resolves dependencies for the artifacts in context of the lifecycle build
 *
 * @author Benjamin Bentmann
 * @author Jason van Zyl
 * @author jdcasey
 * @author Kristian Rosenvold (extracted class only)
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */

@Component( role = MojoDescriptorCreator.class )
public class MojoDescriptorCreator
{
    @Requirement
    private PluginVersionResolver pluginVersionResolver;

    @Requirement
    private BuildPluginManager pluginManager;

    @Requirement
    private PluginPrefixResolver pluginPrefixResolver;

    @SuppressWarnings( { "UnusedDeclaration" } )
    public MojoDescriptorCreator()
    {
    }

    public MojoDescriptorCreator( PluginVersionResolver pluginVersionResolver, BuildPluginManager pluginManager,
                                  PluginPrefixResolver pluginPrefixResolver )
    {
        this.pluginVersionResolver = pluginVersionResolver;
        this.pluginManager = pluginManager;
        this.pluginPrefixResolver = pluginPrefixResolver;
    }

    private Plugin findPlugin( String groupId, String artifactId, Collection<Plugin> plugins )
    {
        for ( Plugin plugin : plugins )
        {
            if ( artifactId.equals( plugin.getArtifactId() ) && groupId.equals( plugin.getGroupId() ) )
            {
                return plugin;
            }
        }

        return null;
    }

    public static Xpp3Dom convert( MojoDescriptor mojoDescriptor )
    {
        Xpp3Dom dom = new Xpp3Dom( "configuration" );

        PlexusConfiguration c = mojoDescriptor.getMojoConfiguration();

        PlexusConfiguration[] ces = c.getChildren();

        if ( ces != null )
        {
            for ( PlexusConfiguration ce : ces )
            {
                String value = ce.getValue( null );
                String defaultValue = ce.getAttribute( "default-value", null );
                if ( value != null || defaultValue != null )
                {
                    Xpp3Dom e = new Xpp3Dom( ce.getName() );
                    e.setValue( value );
                    if ( defaultValue != null )
                    {
                        e.setAttribute( "default-value", defaultValue );
                    }
                    dom.addChild( e );
                }
            }
        }

        return dom;
    }

    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process

    public MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginVersionResolutionException
    {
        String goal = null;

        Plugin plugin = null;

        StringTokenizer tok = new StringTokenizer( task, ":" );

        int numTokens = tok.countTokens();

        if ( numTokens == 4 )
        {
            // We have everything that we need
            //
            // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
            //
            // groupId
            // artifactId
            // version
            // goal
            //
            plugin = new Plugin();
            plugin.setGroupId( tok.nextToken() );
            plugin.setArtifactId( tok.nextToken() );
            plugin.setVersion( tok.nextToken() );
            goal = tok.nextToken();

        }
        else if ( numTokens == 3 )
        {
            // We have everything that we need except the version
            //
            // org.apache.maven.plugins:maven-remote-resources-plugin:???:process
            //
            // groupId
            // artifactId
            // ???
            // goal
            //
            plugin = new Plugin();
            plugin.setGroupId( tok.nextToken() );
            plugin.setArtifactId( tok.nextToken() );
            goal = tok.nextToken();
        }
        else if ( numTokens == 2 )
        {
            // We have a prefix and goal
            //
            // idea:idea
            //
            String prefix = tok.nextToken();
            goal = tok.nextToken();

            // This is the case where someone has executed a single goal from the command line
            // of the form:
            //
            // mvn remote-resources:process
            //
            // From the metadata stored on the server which has been created as part of a standard
            // Maven plugin deployment we will find the right PluginDescriptor from the remote
            // repository.

            plugin = findPluginForPrefix( prefix, session );
        }

        injectPluginDeclarationFromProject( plugin, project );

        RepositoryRequest repositoryRequest = DefaultRepositoryRequest.getRepositoryRequest( session, project );

        // If there is no version to be found then we need to look in the repository metadata for
        // this plugin and see what's specified as the latest release.
        //
        if ( plugin.getVersion() == null )
        {
            resolvePluginVersion( plugin, repositoryRequest );
        }

        return pluginManager.getMojoDescriptor( plugin, goal, repositoryRequest );
    }

    // TODO: take repo mans into account as one may be aggregating prefixes of many
    // TODO: collect at the root of the repository, read the one at the root, and fetch remote if something is missing
    // or the user forces the issue

    public Plugin findPluginForPrefix( String prefix, MavenSession session )
        throws NoPluginFoundForPrefixException
    {
        // [prefix]:[goal]

        PluginPrefixRequest prefixRequest = new DefaultPluginPrefixRequest( prefix, session );
        PluginPrefixResult prefixResult = pluginPrefixResolver.resolve( prefixRequest );

        Plugin plugin = new Plugin();
        plugin.setGroupId( prefixResult.getGroupId() );
        plugin.setArtifactId( prefixResult.getArtifactId() );

        return plugin;
    }

    private void resolvePluginVersion( Plugin plugin, RepositoryRequest repositoryRequest )
        throws PluginVersionResolutionException
    {
        PluginVersionRequest versionRequest = new DefaultPluginVersionRequest( plugin, repositoryRequest );
        plugin.setVersion( pluginVersionResolver.resolve( versionRequest ).getVersion() );
    }

    private void injectPluginDeclarationFromProject( Plugin plugin, MavenProject project )
    {
        Plugin pluginInPom = findPlugin( plugin, project.getBuildPlugins() );

        if ( pluginInPom == null && project.getPluginManagement() != null )
        {
            pluginInPom = findPlugin( plugin, project.getPluginManagement().getPlugins() );
        }

        if ( pluginInPom != null )
        {
            if ( plugin.getVersion() == null )
            {
                plugin.setVersion( pluginInPom.getVersion() );
            }

            plugin.setDependencies( new ArrayList<Dependency>( pluginInPom.getDependencies() ) );
        }
    }

    private Plugin findPlugin( Plugin plugin, Collection<Plugin> plugins )
    {
        return findPlugin( plugin.getGroupId(), plugin.getArtifactId(), plugins );
    }

}
