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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.MojoExecutionConfigurator;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.stream;

/**
 * @since 3.3.1, MNG-5753
 */
@Named
@Singleton
public class DefaultMojoExecutionConfigurator
    implements MojoExecutionConfigurator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void configure( MavenProject project, MojoExecution mojoExecution, boolean allowPluginLevelConfig )
    {
        String g = mojoExecution.getPlugin().getGroupId();

        String a = mojoExecution.getPlugin().getArtifactId();

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

            checkUnknownMojoConfigurationParameters( mojoExecution );
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

    private void checkUnknownMojoConfigurationParameters( MojoExecution mojoExecution )
    {
        if ( mojoExecution.getConfiguration() == null || mojoExecution.getConfiguration().getChildCount() == 0 )
        {
            return;
        }

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        // in first step get parameter names of current goal
        Set<String> parametersNamesGoal = mojoDescriptor.getParameters().stream()
            .flatMap( this::getParameterNames )
            .collect( Collectors.toSet() );

        Set<String> unknownParameters = getUnknownParameters( mojoExecution, parametersNamesGoal );

        if ( unknownParameters.isEmpty() )
        {
            return;
        }

        // second step get parameter names of all plugin goals
        Set<String> parametersNamesAll = mojoDescriptor.getPluginDescriptor().getMojos().stream()
            .flatMap( m -> m.getParameters().stream() )
            .flatMap( this::getParameterNames )
            .collect( Collectors.toSet() );

        unknownParameters = getUnknownParameters( mojoExecution, parametersNamesAll );

        unknownParameters.stream()
            .filter( parameterName -> isNotReportPluginsForMavenSite( parameterName, mojoExecution ) )
            .forEach(
                name ->
                {
                    MessageBuilder messageBuilder = MessageUtils.buffer()
                        .warning( "Parameter '" )
                        .warning( name )
                        .warning( "' is unknown for plugin '" )
                        .warning( mojoExecution.getArtifactId() ).warning( ":" )
                        .warning( mojoExecution.getVersion() ).warning( ":" )
                        .warning( mojoExecution.getGoal() );

                    if ( mojoExecution.getExecutionId() != null )
                    {
                        messageBuilder.warning( " (" );
                        messageBuilder.warning( mojoExecution.getExecutionId() );
                        messageBuilder.warning( ")" );
                    }

                    messageBuilder.warning( "'" );

                    logger.warn( messageBuilder.toString() );
                } );
    }

    /**
     * Method can be removed after {@link org.apache.maven.model.plugin.ReportingConverter} will be removed or disabled
     *
     * @return true if parameter is not reportPlugins for m-site-p
     */
    private boolean isNotReportPluginsForMavenSite( String parameterName,
                                                    MojoExecution mojoExecution )
    {
        return !( "reportPlugins".equals( parameterName )
            && "maven-site-plugin".equals( mojoExecution.getArtifactId() ) );
    }

    private Stream<String> getParameterNames( Parameter parameter )
    {
        if ( parameter.getAlias() != null )
        {
            return Stream.of( parameter.getName(), parameter.getAlias() );
        }
        else
        {
            return Stream.of( parameter.getName() );
        }
    }

    private Set<String> getUnknownParameters( MojoExecution mojoExecution, Set<String> parameters )
    {
        return stream( mojoExecution.getConfiguration().getChildren() )
            .map( Xpp3Dom::getName )
            .filter( name -> !parameters.contains( name ) )
            .collect( Collectors.toSet() );
    }

}
