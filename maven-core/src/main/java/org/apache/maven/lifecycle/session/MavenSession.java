package org.apache.maven.lifecycle.session;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositoryUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.Set;
import java.util.List;

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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenSession
{
    private PlexusContainer container;

    private MavenProject project;

    private ArtifactRepository localRepository;

    private PluginManager pluginManager;

    private Set remoteRepositories;

    private List goals;

    public MavenSession( PlexusContainer container,
                         PluginManager pluginManager,
                         MavenProject project,
                         ArtifactRepository localRepository,
                         List goals )
    {
        this.container = container;

        this.pluginManager = pluginManager;

        this.project = project;

        this.localRepository = localRepository;

        this.goals = goals;
    }

    public PlexusContainer getContainer()
    {
        return container;
    }

    public PluginManager getPluginManager()
    {
        return pluginManager;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public Set getRemoteRepositories()
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = RepositoryUtils.mavenToWagon( project.getRepositories() );
        }

        return remoteRepositories;
    }

    public List getGoals()
    {
        return goals;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public Object lookup( String role )
        throws ComponentLookupException
    {
        return container.lookup( role );
    }

    public Object lookup( String role, String roleHint )
        throws ComponentLookupException
    {
        return container.lookup( role, roleHint );
    }

    public void release( Object component )
    {
        if ( component != null )
        {
            try
            {
                container.release( component );
            }
            catch ( Exception e )
            {
                //@todo what to do here?
            }
        }
    }
}
