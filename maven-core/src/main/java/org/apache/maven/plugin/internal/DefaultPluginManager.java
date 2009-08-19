package org.apache.maven.plugin.internal;

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

import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Benjamin Bentmann
 */
@Component( role = PluginManager.class )
public class DefaultPluginManager
    implements PluginManager
{

    @Requirement
    private BuildPluginManager pluginManager;

    public void executeMojo( MavenProject project, MojoExecution execution, MavenSession session )
        throws MojoExecutionException, ArtifactResolutionException, MojoFailureException, ArtifactNotFoundException,
        InvalidDependencyVersionException, PluginManagerException, PluginConfigurationException
    {
        throw new UnsupportedOperationException();
    }

    public Object getPluginComponent( Plugin plugin, String role, String roleHint )
        throws PluginManagerException, ComponentLookupException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getPluginComponents( Plugin plugin, String role )
        throws ComponentLookupException, PluginManagerException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Plugin getPluginDefinitionForPrefix( String prefix, MavenSession session, MavenProject project )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public PluginDescriptor getPluginDescriptorForPrefix( String prefix )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public PluginDescriptor loadPluginDescriptor( Plugin plugin, MavenProject project, MavenSession session )
        throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
        InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException, PluginNotFoundException,
        PluginVersionNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public PluginDescriptor loadPluginFully( Plugin plugin, MavenProject project, MavenSession session )
        throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
        InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException, PluginNotFoundException,
        PluginVersionNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public PluginDescriptor verifyPlugin( Plugin plugin, MavenProject project, Settings settings,
                                          ArtifactRepository localRepository )
        throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
        InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException, PluginNotFoundException,
        PluginVersionNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
