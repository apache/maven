package org.apache.maven.plugin;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.MavenMetadataSource;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;

import org.codehaus.plexus.ArtifactEnabledContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.dag.CycleDetectedException;

public class DefaultPluginManager
    extends AbstractLogEnabled
    implements PluginManager, ComponentDiscoveryListener, Initializable, Contextualizable
{
    static String MAVEN_PLUGIN = "maven-plugin";

    protected Map mojoDescriptors;

    protected Map pluginDescriptors;

    protected ArtifactResolver artifactResolver;

    protected ArtifactHandlerManager artifactHandlerManager;

    protected PlexusContainer container;

    protected PluginDescriptorBuilder pluginDescriptorBuilder;

    protected Set remotePluginRepositories;

    protected ArtifactRepository localRepository;

    protected ArtifactFilter artifactFilter;

    public DefaultPluginManager()
    {
        mojoDescriptors = new HashMap();

        pluginDescriptors = new HashMap();

        pluginDescriptorBuilder = new PluginDescriptorBuilder();
    }

    // ----------------------------------------------------------------------
    // Goal descriptors
    // ----------------------------------------------------------------------

    public Map getMojoDescriptors()
    {
        return mojoDescriptors;
    }

    public MojoDescriptor getMojoDescriptor( String name )
    {
        return (MojoDescriptor) mojoDescriptors.get( name );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Set pluginsInProcess = new HashSet();

    public void processPluginDescriptor( MavenPluginDescriptor mavenPluginDescriptor ) throws CycleDetectedException
    {
        if ( pluginsInProcess.contains( mavenPluginDescriptor.getPluginId() ) )
        {
            return;
        }

        pluginsInProcess.add( mavenPluginDescriptor.getPluginId() );

        PluginDescriptor pluginDescriptor = mavenPluginDescriptor.getPluginDescriptor();

        for ( Iterator it = mavenPluginDescriptor.getMavenMojoDescriptors().iterator(); it.hasNext(); )
        {
            MavenMojoDescriptor mavenMojoDescriptor = (MavenMojoDescriptor) it.next();

            MojoDescriptor mojoDescriptor = mavenMojoDescriptor.getMojoDescriptor();

            mojoDescriptors.put( mojoDescriptor.getId(), mojoDescriptor );

            pluginDescriptors.put( pluginDescriptor.getId(), pluginDescriptor );
        }
    }

    // ----------------------------------------------------------------------
    // Plugin discovery
    // ----------------------------------------------------------------------

    public void componentDiscovered( ComponentDiscoveryEvent event )
    {
        ComponentSetDescriptor componentSetDescriptor = event.getComponentSetDescriptor();

        if ( !( componentSetDescriptor instanceof MavenPluginDescriptor ) )
        {
            return;
        }

        MavenPluginDescriptor pluginDescriptor = (MavenPluginDescriptor) componentSetDescriptor;

        try
        {
            processPluginDescriptor( pluginDescriptor );
        }
        catch ( CycleDetectedException e )
        {
            getLogger().error( "A cycle was detected in the goal graph: ", e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public boolean isPluginInstalled( String pluginId )
    {
        return pluginDescriptors.containsKey( pluginId );
    }

    private String getPluginId( String goalName )
    {
        if ( goalName.indexOf( ":" ) > 0 )
        {
            return goalName.substring( 0, goalName.indexOf( ":" ) );
        }

        return goalName;
    }

    public void verifyPluginForGoal( String goalName ) throws Exception
    {
        String pluginId = getPluginId( goalName );

        if ( !isPluginInstalled( pluginId ) )
        {
            //!! This is entirely crappy. We need a better naming for plugin
            // artifact ids and
            //   we definitely need better version extraction support.

            String artifactId = "maven-" + pluginId + "-plugin";

            String version = "1.0-SNAPSHOT";

            Artifact pluginArtifact = new DefaultArtifact( "maven", artifactId, version, "plugin", "jar" );

            addPlugin( pluginArtifact );

            // Now, we need to resolve the plugins for this goal's prereqs.
            MojoDescriptor mojoDescriptor = getMojoDescriptor( goalName );

            if ( mojoDescriptor == null )
            {
                throw new Exception( "Could not find a mojo descriptor for goal: '" + goalName + "'." );
            }

            List prereqs = mojoDescriptor.getPrereqs();

            if ( prereqs != null )
            {
                for ( Iterator it = prereqs.iterator(); it.hasNext(); )
                {
                    String prereq = (String) it.next();

                    verifyPluginForGoal( prereq );
                }
            }
        }
    }

    public void addPlugin( Artifact pluginArtifact )
        throws Exception
    {
        artifactResolver = (ArtifactResolver) container.lookup( ArtifactResolver.ROLE );

        MavenMetadataSource metadataSource = new MavenMetadataSource( remotePluginRepositories, 
                                                                      localRepository,
                                                                      artifactResolver );

        ( (ArtifactEnabledContainer) container ).addComponent( pluginArtifact,
                                                               artifactResolver,
                                                               remotePluginRepositories,
                                                               localRepository,
                                                               metadataSource,
                                                               artifactFilter );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (ArtifactEnabledContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void initialize()
        throws Exception
    {
        artifactFilter = new ExclusionSetFilter( new String[]
        {
            "maven-core",
            "maven-artifact",
            "maven-model",
            "maven-plugin",
            "plexus",
            "xstream",
            "xpp3",
            "classworlds",
            "ognl"
        } );

        // TODO: move this to be configurable from the Maven component
        remotePluginRepositories = new HashSet();

        // TODO: needs to be configured from the POM element
        remotePluginRepositories.add( new ArtifactRepository( "plugin-repository", "http://repo1.maven.org" ) );
    }

    // TODO: is this needed or can it be found from the session?
    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    // TODO: is this needed or can it be found from the session? It is currently set from the session
    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }
}

