package org.apache.maven.lifecycle.phase;

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

import org.apache.maven.lifecycle.AbstractMavenLifecyclePhase;
import org.apache.maven.lifecycle.MavenLifecycleContext;
import org.apache.maven.plugin.OgnlProjectValueExtractor;
import org.apache.maven.plugin.Plugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class GoalAttainmentPhase
    extends AbstractMavenLifecyclePhase
{
    public void execute( MavenLifecycleContext context )
        throws Exception
    {
        PluginExecutionResponse response;

        PluginExecutionRequest request;

        for ( Iterator it = context.getResolvedGoals().iterator(); it.hasNext(); )
        {
            String goalName = (String) it.next();

            MojoDescriptor mojoDescriptor = context.getMojoDescriptor( goalName );

            getLogger().info( "[" + mojoDescriptor.getId() + "]" );

            request = new PluginExecutionRequest( createParameters( mojoDescriptor, context ) );

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
            finally
            {
                releaseComponents( mojoDescriptor, request, context );

                context.release( plugin );
            }
        }
    }

    private Map createParameters( MojoDescriptor goal, MavenLifecycleContext context )
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

                Object value = OgnlProjectValueExtractor.evaluate( expression, context );

                //@todo: mojo parameter validation
                // This is the place where parameter validation should be performed.
                //if ( value == null && parameter.isRequired() )
                //{
                //}

                map.put( key, value );
            }
        }

        return map;
    }

    private void releaseComponents( MojoDescriptor goal, PluginExecutionRequest request, MavenLifecycleContext context )
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
