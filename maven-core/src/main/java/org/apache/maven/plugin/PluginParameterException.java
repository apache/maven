package org.apache.maven.plugin;

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

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;

public class PluginParameterException
    extends PluginConfigurationException
{

    private final List parameters;

    private final MojoDescriptor mojo;

    public PluginParameterException( MojoDescriptor mojo, List parameters )
    {
        super( mojo.getPluginDescriptor(),
               "Invalid or missing parameters: " + parameters + " for mojo: " + mojo.getRoleHint() );

        this.mojo = mojo;

        this.parameters = parameters;
    }

    public MojoDescriptor getMojoDescriptor()
    {
        return mojo;
    }

    public List getParameters()
    {
        return parameters;
    }

    private static void decomposeParameterIntoUserInstructions( MojoDescriptor mojo, Parameter param,
                                                                StringBuffer messageBuffer )
    {
        String expression = param.getExpression();

        if ( param.isEditable() )
        {
            messageBuffer.append( "Inside the definition for plugin \'" + mojo.getPluginDescriptor().getArtifactId() +
                "\' specify the following:\n\n<configuration>\n  ...\n  <" + param.getName() + ">VALUE</" +
                param.getName() + ">\n</configuration>" );

            String alias = param.getAlias();
            if ( StringUtils.isNotEmpty( alias ) && !alias.equals( param.getName() ) )
            {
                messageBuffer.append(
                    "\n\n-OR-\n\n<configuration>\n  ...\n  <" + alias + ">VALUE</" + alias + ">\n</configuration>\n" );
            }
        }

        if ( StringUtils.isEmpty( expression ) )
        {
            messageBuffer.append( "." );
        }
        else
        {
            if ( param.isEditable() )
            {
                messageBuffer.append( "\n\n-OR-\n\n" );
            }

            //addParameterUsageInfo( expression, messageBuffer );
        }
    }

    public String buildDiagnosticMessage()
    {
        StringBuffer messageBuffer = new StringBuffer();

        List params = getParameters();
        MojoDescriptor mojo = getMojoDescriptor();

        messageBuffer.append( "One or more required plugin parameters are invalid/missing for \'" )
            .append( mojo.getPluginDescriptor().getGoalPrefix() ).append( ":" ).append( mojo.getGoal() )
            .append( "\'\n" );

        int idx = 0;
        for ( Iterator it = params.iterator(); it.hasNext(); idx++ )
        {
            Parameter param = (Parameter) it.next();

            messageBuffer.append( "\n[" ).append( idx ).append( "] " );

            decomposeParameterIntoUserInstructions( mojo, param, messageBuffer );

            messageBuffer.append( "\n" );
        }

        return messageBuffer.toString();
    }
}
