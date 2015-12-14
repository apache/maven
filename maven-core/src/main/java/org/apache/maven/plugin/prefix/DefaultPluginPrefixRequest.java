package org.apache.maven.plugin.prefix;

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

import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Collects settings required to resolve a plugin prefix.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
public class DefaultPluginPrefixRequest
    implements PluginPrefixRequest
{

    private String prefix;

    private List<String> pluginGroups = Collections.emptyList();

    private Model pom;

    private List<RemoteRepository> repositories = Collections.emptyList();

    private RepositorySystemSession session;

    /**
     * Creates an empty request.
     */
    public DefaultPluginPrefixRequest()
    {
    }

    /**
     * Creates a request for the specified plugin prefix and build session. The provided build session will be used to
     * configure repository settings. If the session has a current project, its plugin repositories and model will be
     * used as well.
     *
     * @param prefix The plugin prefix to resolve, must not be {@code null}.
     * @param session The build session from which to derive further settings, must not be {@code null}.
     */
    public DefaultPluginPrefixRequest( String prefix, MavenSession session )
    {
        setPrefix( prefix );

        setRepositorySession( session.getRepositorySession() );

        MavenProject project = session.getCurrentProject();
        if ( project != null )
        {
            setRepositories( project.getRemotePluginRepositories() );
            setPom( project.getModel() );
        }

        setPluginGroups( session.getPluginGroups() );
    }

    public String getPrefix()
    {
        return prefix;
    }

    public DefaultPluginPrefixRequest setPrefix( String prefix )
    {
        this.prefix = prefix;

        return this;
    }

    public List<String> getPluginGroups()
    {
        return pluginGroups;
    }

    public DefaultPluginPrefixRequest setPluginGroups( List<String> pluginGroups )
    {
        if ( pluginGroups != null )
        {
            this.pluginGroups = Collections.unmodifiableList( pluginGroups );
        }
        else
        {
            this.pluginGroups = Collections.emptyList();
        }

        return this;
    }

    public Model getPom()
    {
        return pom;
    }

    public DefaultPluginPrefixRequest setPom( Model pom )
    {
        this.pom = pom;

        return this;
    }

    public List<RemoteRepository> getRepositories()
    {
        return repositories;
    }

    public DefaultPluginPrefixRequest setRepositories( List<RemoteRepository> repositories )
    {
        if ( repositories != null )
        {
            this.repositories = Collections.unmodifiableList( repositories );
        }
        else
        {
            this.repositories = Collections.emptyList();
        }

        return this;
    }

    public RepositorySystemSession getRepositorySession()
    {
        return session;
    }

    public DefaultPluginPrefixRequest setRepositorySession( RepositorySystemSession session )
    {
        this.session = session;

        return this;
    }

}
