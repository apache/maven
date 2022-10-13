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
package org.apache.maven.lifecycle.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.api.xml.Dom;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.xml.Xpp3Dom;
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
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Resolves dependencies for the artifacts in context of the lifecycle build
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Jason van Zyl
 * @author jdcasey
 * @author Kristian Rosenvold (extracted class only)
 */
@Named
@Singleton
public class MojoDescriptorCreator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final PluginVersionResolver pluginVersionResolver;

    private final BuildPluginManager pluginManager;

    private final PluginPrefixResolver pluginPrefixResolver;

    private final LifecyclePluginResolver lifecyclePluginResolver;

    @Inject
    public MojoDescriptorCreator( PluginVersionResolver pluginVersionResolver, BuildPluginManager pluginManager,
                                  PluginPrefixResolver pluginPrefixResolver,
                                  LifecyclePluginResolver lifecyclePluginResolver )
    {
        this.pluginVersionResolver = pluginVersionResolver;
        this.pluginManager = pluginManager;
        this.pluginPrefixResolver = pluginPrefixResolver;
        this.lifecyclePluginResolver = lifecyclePluginResolver;
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

    public static org.codehaus.plexus.util.xml.Xpp3Dom convert( MojoDescriptor mojoDescriptor )
    {
        PlexusConfiguration c = mojoDescriptor.getMojoConfiguration();

        List<Dom> children = new ArrayList<>();
        PlexusConfiguration[] ces = c.getChildren();
        if ( ces != null )
        {
            for ( PlexusConfiguration ce : ces )
            {
                String value = ce.getValue( null );
                String defaultValue = ce.getAttribute( "default-value", null );
                if ( value != null || defaultValue != null )
                {
                    Xpp3Dom e = new Xpp3Dom( ce.getName(), value,
                                             defaultValue != null
                                                             ? Collections.singletonMap( "default-value", defaultValue )
                                                             : null,
                                             null, null );
                    children.add( e );
                }
            }
        }

        Xpp3Dom dom = new Xpp3Dom( "configuration", null, null, children, null );
        return new org.codehaus.plexus.util.xml.Xpp3Dom( dom );
    }

    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process@executionId

    public MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginVersionResolutionException
    {
        String goal = null;

        Plugin plugin = null;

        StringTokenizer tok = new StringTokenizer( task, ":" );

        int numTokens = tok.countTokens();

        if ( numTokens >= 4 )
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

            // This won't be valid, but it constructs something easy to read in the error message
            while ( tok.hasMoreTokens() )
            {
                goal += ":" + tok.nextToken();
            }
        }
        else if ( numTokens == 3 )
        {
            // groupId:artifactId:goal or pluginPrefix:version:goal (since Maven 3.9.0)

            String firstToken = tok.nextToken();
            // groupId or pluginPrefix? heuristics: groupId contains dot (.) but not pluginPrefix
            if ( firstToken.contains( "." ) )
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
                plugin.setGroupId( firstToken );
                plugin.setArtifactId( tok.nextToken() );
            }
            else
            {
                // pluginPrefix:version:goal, like remote-resources:3.5.0:process
                plugin = findPluginForPrefix( firstToken, session );
                plugin.setVersion( tok.nextToken() );
            }
            goal = tok.nextToken();
        }
        else
        {
            // We have a prefix and goal
            //
            // idea:idea
            //
            String prefix = tok.nextToken();

            if ( numTokens == 2 )
            {
                goal = tok.nextToken();
            }
            else
            {
                // goal was missing - pass through to MojoNotFoundException
                goal = "";
            }

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

        int executionIdx = goal.indexOf( '@' );
        if ( executionIdx > 0 )
        {
            goal = goal.substring( 0, executionIdx );
        }

        injectPluginDeclarationFromProject( plugin, project );

        // If there is no version to be found then we need to look in the repository metadata for
        // this plugin and see what's specified as the latest release.
        //
        if ( plugin.getVersion() == null )
        {
            resolvePluginVersion( plugin, session, project );
        }

        return pluginManager.getMojoDescriptor( plugin, goal.toString(), project.getRemotePluginRepositories(),
                                                session.getRepositorySession() );
    }

    // TODO take repo mans into account as one may be aggregating prefixes of many
    // TODO collect at the root of the repository, read the one at the root, and fetch remote if something is missing
    // or the user forces the issue

    public Plugin findPluginForPrefix( String prefix, MavenSession session )
        throws NoPluginFoundForPrefixException
    {
        // [prefix]:[goal]

        if ( session.getCurrentProject() != null )
        {
            try
            {
                lifecyclePluginResolver.resolveMissingPluginVersions( session.getCurrentProject(), session );
            }
            catch ( PluginVersionResolutionException e )
            {
                // not critical here
                logger.debug( e.getMessage(), e );
            }
        }

        PluginPrefixRequest prefixRequest = new DefaultPluginPrefixRequest( prefix, session );
        PluginPrefixResult prefixResult = pluginPrefixResolver.resolve( prefixRequest );

        Plugin plugin = new Plugin();
        plugin.setGroupId( prefixResult.getGroupId() );
        plugin.setArtifactId( prefixResult.getArtifactId() );

        return plugin;
    }

    private void resolvePluginVersion( Plugin plugin, MavenSession session, MavenProject project )
        throws PluginVersionResolutionException
    {
        PluginVersionRequest versionRequest = new DefaultPluginVersionRequest( plugin, session.getRepositorySession(),
                                                                               project.getRemotePluginRepositories() );
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

            plugin.setDependencies( new ArrayList<>( pluginInPom.getDependencies() ) );
        }
    }

    private Plugin findPlugin( Plugin plugin, Collection<Plugin> plugins )
    {
        return findPlugin( plugin.getGroupId(), plugin.getArtifactId(), plugins );
    }

}
