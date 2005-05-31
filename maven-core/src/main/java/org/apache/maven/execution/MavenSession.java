package org.apache.maven.execution;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.settings.Settings;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenSession
{
    private PlexusContainer container;

    private MavenProject project;

    private ArtifactRepository localRepository;

    private List goals;

    private EventDispatcher eventDispatcher;

    // TODO: make this the central one, get rid of build settings...
    private final Settings settings;

    public MavenSession( MavenProject project, PlexusContainer container, Settings settings,
                         ArtifactRepository localRepository, EventDispatcher eventDispatcher, List goals )
    {
        this.project = project;

        this.container = container;

        this.settings = settings;

        this.localRepository = localRepository;

        this.eventDispatcher = eventDispatcher;

        this.goals = goals;
        
        // TODO: Go back to this when we get the container ready to configure mojos...
        // NOTE: [jc] This is a possible way to add project, etc. to the container context to allow container-injected 
        // mojo configuration.
//        initializeContainerContext();
    }

    private void initializeContainerContext()
    {
        Context context = container.getContext();

        context.put( "project", project );
        context.put( "settings", settings );
        context.put( "basedir", project.getBasedir().getAbsolutePath() );
        context.put( "localRepository", localRepository );
        
        // TODO: remove this alias...change to ${project.build.finalName}
        context.put( "maven.final.name", project.getBuild().getFinalName() );
    }

    public PlexusContainer getContainer()
    {
        return container;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public List getRemoteRepositories()
    {
        return project.getRemoteArtifactRepositories();
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

    public List lookupList( String role )
        throws ComponentLookupException
    {
        return container.lookupList( role );
    }

    public Map lookupMap( String role )
        throws ComponentLookupException
    {
        return container.lookupMap( role );
    }

    public EventDispatcher getEventDispatcher()
    {
        return eventDispatcher;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public List getPluginRepositories()
    {
        return project.getPluginArtifactRepositories();
    }

}