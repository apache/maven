package org.apache.maven.plugin;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.realm.RealmManagementException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

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

/**
 * Exception in the plugin manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class PluginManagerException
    extends Exception
{

    private final String pluginGroupId;

    private final String pluginArtifactId;

    private final String pluginVersion;

    private String goal;

    private MavenProject project;

    protected PluginManagerException( Plugin plugin,
                                      String message,
                                      MavenProject project,
                                      Throwable cause )
    {
        super( message, cause );

        this.project = project;
        pluginGroupId = plugin.getGroupId();
        pluginArtifactId = plugin.getArtifactId();
        pluginVersion = plugin.getVersion();
    }

    protected PluginManagerException( Plugin plugin,
                                      String message,
                                      Throwable cause )
    {
        super( message, cause );

        pluginGroupId = plugin.getGroupId();
        pluginArtifactId = plugin.getArtifactId();
        pluginVersion = plugin.getVersion();
    }

    protected PluginManagerException( MojoDescriptor mojoDescriptor,
                                      String message,
                                      Throwable cause )
    {
        super( message, cause );
        pluginGroupId = mojoDescriptor.getPluginDescriptor().getGroupId();
        pluginArtifactId = mojoDescriptor.getPluginDescriptor().getArtifactId();
        pluginVersion = mojoDescriptor.getPluginDescriptor().getVersion();
        goal = mojoDescriptor.getGoal();
    }

    protected PluginManagerException( MojoDescriptor mojoDescriptor,
                                      MavenProject project,
                                      String message )
    {
        super( message );
        this.project = project;
        pluginGroupId = mojoDescriptor.getPluginDescriptor().getGroupId();
        pluginArtifactId = mojoDescriptor.getPluginDescriptor().getArtifactId();
        pluginVersion = mojoDescriptor.getPluginDescriptor().getVersion();
        goal = mojoDescriptor.getGoal();
    }

    protected PluginManagerException( MojoDescriptor mojoDescriptor,
                                      MavenProject project,
                                      String message,
                                      Throwable cause )
    {
        super( message, cause );
        this.project = project;
        pluginGroupId = mojoDescriptor.getPluginDescriptor().getGroupId();
        pluginArtifactId = mojoDescriptor.getPluginDescriptor().getArtifactId();
        pluginVersion = mojoDescriptor.getPluginDescriptor().getVersion();
        goal = mojoDescriptor.getGoal();
    }

    public PluginManagerException( Plugin plugin,
                                   InvalidVersionSpecificationException cause )
    {
        super( cause );

        pluginGroupId = plugin.getGroupId();
        pluginArtifactId = plugin.getArtifactId();
        pluginVersion = plugin.getVersion();
    }

    public PluginManagerException( Plugin plugin,
                                   String message,
                                   PlexusConfigurationException cause )
    {
        super( message, cause );

        pluginGroupId = plugin.getGroupId();
        pluginArtifactId = plugin.getArtifactId();
        pluginVersion = plugin.getVersion();
    }

    public PluginManagerException( Plugin plugin,
                                   String message,
                                   ComponentRepositoryException cause )
    {
        super( message, cause );

        pluginGroupId = plugin.getGroupId();
        pluginArtifactId = plugin.getArtifactId();
        pluginVersion = plugin.getVersion();
    }

    public PluginManagerException( MojoDescriptor mojoDescriptor,
                                   MavenProject project,
                                   String message,
                                   NoSuchRealmException cause )
    {
        super( message, cause );

        this.project = project;
        pluginGroupId = mojoDescriptor.getPluginDescriptor().getGroupId();
        pluginArtifactId = mojoDescriptor.getPluginDescriptor().getArtifactId();
        pluginVersion = mojoDescriptor.getPluginDescriptor().getVersion();
        goal = mojoDescriptor.getGoal();
    }

    public PluginManagerException( MojoDescriptor mojoDescriptor,
                                   String message,
                                   MavenProject project,
                                   PlexusContainerException cause )
    {
        super( message, cause );

        this.project = project;

        PluginDescriptor pd = mojoDescriptor.getPluginDescriptor();
        pluginGroupId = pd.getGroupId();
        pluginArtifactId = pd.getArtifactId();
        pluginVersion = pd.getVersion();

        goal = mojoDescriptor.getGoal();
    }

    public PluginManagerException( Plugin plugin,
                                   String message,
                                   PlexusContainerException cause )
    {
        super( message, cause );

        pluginGroupId = plugin.getGroupId();
        pluginArtifactId = plugin.getArtifactId();
        pluginVersion = plugin.getVersion();
    }

    public PluginManagerException( Plugin plugin,
                                   String message,
                                   RealmManagementException cause )
    {
        super( message, cause );

        pluginGroupId = plugin.getGroupId();
        pluginArtifactId = plugin.getArtifactId();
        pluginVersion = plugin.getVersion();
    }

    public PluginManagerException( Plugin plugin,
                                   String message,
                                   MavenProject project )
    {
        super( message );

        pluginGroupId = plugin.getGroupId();
        pluginArtifactId = plugin.getArtifactId();
        pluginVersion = plugin.getVersion();
        this.project = project;
    }

    public String getPluginGroupId()
    {
        return pluginGroupId;
    }

    public String getPluginArtifactId()
    {
        return pluginArtifactId;
    }

    public String getPluginVersion()
    {
        return pluginVersion;
    }

    public String getGoal()
    {
        return goal;
    }

    public MavenProject getProject()
    {
        return project;
    }
}
