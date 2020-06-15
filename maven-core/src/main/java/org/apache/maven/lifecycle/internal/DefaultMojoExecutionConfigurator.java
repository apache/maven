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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collection;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.MojoExecutionConfigurator;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @since 3.3.1, MNG-5753
 */
@Named
@Singleton
public class DefaultMojoExecutionConfigurator
    implements MojoExecutionConfigurator
{

    @Override
    public void configure( MavenProject project, MojoExecution mojoExecution, boolean allowPluginLevelConfig )
    {
        String g = mojoExecution.getGroupId();

        String a = mojoExecution.getArtifactId();

        Plugin plugin = findPlugin( g, a, project.getBuildPlugins() );

        if ( plugin == null && project.getPluginManagement() != null )
        {
            plugin = findPlugin( g, a, project.getPluginManagement().getPlugins() );
        }

        if ( plugin != null )
        {
            PluginExecution pluginExecution =
                findPluginExecution( mojoExecution.getExecutionId(), plugin.getExecutions() );

            Xpp3Dom pomConfiguration = null;

            if ( pluginExecution != null )
            {
                pomConfiguration = (Xpp3Dom) pluginExecution.getConfiguration();
            }
            else if ( allowPluginLevelConfig )
            {
                pomConfiguration = (Xpp3Dom) plugin.getConfiguration();
            }

            Xpp3Dom mojoConfiguration = ( pomConfiguration != null ) ? new Xpp3Dom( pomConfiguration ) : null;

            mojoConfiguration = Xpp3Dom.mergeXpp3Dom( mojoExecution.getConfiguration(), mojoConfiguration );

            mojoExecution.setConfiguration( mojoConfiguration );
        }
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

    private PluginExecution findPluginExecution( String executionId, Collection<PluginExecution> executions )
    {
        if ( StringUtils.isNotEmpty( executionId ) )
        {
            for ( PluginExecution execution : executions )
            {
                if ( executionId.equals( execution.getId() ) )
                {
                    return execution;
                }
            }
        }

        return null;
    }

}
