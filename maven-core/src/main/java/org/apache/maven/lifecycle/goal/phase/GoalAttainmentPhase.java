package org.apache.maven.lifecycle.goal.phase;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.lifecycle.goal.AbstractMavenGoalPhase;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.plugin.Plugin;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.CollectionUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class GoalAttainmentPhase
    extends AbstractMavenGoalPhase
{
    public void execute( MavenGoalExecutionContext context )
        throws GoalExecutionException
    {
        PluginExecutionResponse response;

        PluginExecutionRequest request;

        for ( Iterator it = context.getResolvedGoals().iterator(); it.hasNext(); )
        {
            String goalName = (String) it.next();

            MojoDescriptor mojoDescriptor = context.getMojoDescriptor( goalName );

            getLogger().info( "[" + mojoDescriptor.getId() + "]" );

            try
            {
                request = new PluginExecutionRequest( createParameters( mojoDescriptor, context ) );
            }
            catch ( PluginConfigurationException e )
            {
                throw new GoalExecutionException( "Error configuring plugin for execution.", e );
            }

            response = new PluginExecutionResponse();

            Plugin plugin = null;

            try
            {
                String roleHint = context.getPluginId( mojoDescriptor );

                plugin = (Plugin) context.lookup( Plugin.ROLE, roleHint );

                plugin.execute( request, response );

                if ( response.isExecutionFailure() )
                {
                    context.setExecutionFailure( mojoDescriptor.getId(), response.getFailureResponse() );

                    break;
                }
            }
            catch ( ComponentLookupException e )
            {
                throw new GoalExecutionException( "Error looking up plugin: ", e );
            }
            catch ( Exception e )
            {
                throw new GoalExecutionException( "Error executing plugin: ", e );
            }
            finally
            {
                releaseComponents( mojoDescriptor, request, context );

                context.release( plugin );
            }
        }
    }

    private Map createParameters( MojoDescriptor goal, MavenGoalExecutionContext context )
        throws PluginConfigurationException
    {
        Map map = null;

        List parameters = goal.getParameters();

        if ( parameters != null )
        {
            map = new HashMap();

            for ( int i = 0; i < parameters.size(); i++ )
            {
                Parameter parameter = (Parameter) parameters.get( i );

                String key = parameter.getName();

                String expression = parameter.getExpression();

                Object value = PluginParameterExpressionEvaluator.evaluate( expression, context );

                map.put( key, value );
            }

            map = mergeProjectDefinedPluginConfiguration( context.getProject(), goal.getId(), map );
        }

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = (Parameter) parameters.get( i );

            String key = parameter.getName();

            Object value = map.get( key );

            // ----------------------------------------------------------------------
            // We will perform a basic check here for parameters values that are
            // required. Required parameters can't be null so we throw an
            // Exception in the case where they are. We probably want some pluggable
            // mechanism here but this will catch the most obvious of
            // misconfigurations.
            // ----------------------------------------------------------------------

            if ( value == null && parameter.isRequired() )
            {
                throw new PluginConfigurationException( createPluginParameterRequiredMessage( goal, parameter ) );
            }
        }

        return map;
    }

    private Map mergeProjectDefinedPluginConfiguration( MavenProject project, String goalId, Map map )
    {
        // ----------------------------------------------------------------------
        // I would like to be able to lookup the Plugin object using a key but
        // we have a limitation in modello that will be remedied shortly. So
        // for now I have to iterate through and see what we have.
        // ----------------------------------------------------------------------

        if ( project.getPlugins() != null )
        {
            String pluginId = goalId.substring( 0, goalId.indexOf( ":" ) );

            for ( Iterator iterator = project.getPlugins().iterator(); iterator.hasNext(); )
            {
                org.apache.maven.model.Plugin plugin = (org.apache.maven.model.Plugin) iterator.next();

                if ( pluginId.equals( plugin.getId() ) )
                {
                    return CollectionUtils.mergeMaps( plugin.getConfiguration(), map );
                }
            }
        }

        return map;
    }

    private String createPluginParameterRequiredMessage( MojoDescriptor mojo, Parameter parameter )
    {
        StringBuffer message = new StringBuffer();

        message.append( "The '" + parameter.getName() ).
            append( "' parameter is required for the execution of the " ).
            append( mojo.getId() ).
            append( " mojo and cannot be null." );

        return message.toString();
    }

    private void releaseComponents( MojoDescriptor goal, PluginExecutionRequest request, MavenGoalExecutionContext context )
    {
        if ( request != null && request.getParameters() != null )
        {
            for ( Iterator iterator = goal.getParameters().iterator(); iterator.hasNext(); )
            {
                Parameter parameter = (Parameter) iterator.next();

                String key = parameter.getName();

                String expression = parameter.getExpression();

                if ( expression != null & expression.startsWith( "#component" ) )
                {
                    Object component = request.getParameter( key );

                    context.release( component );
                }
            }
        }
    }
}
