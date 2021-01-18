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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * PluginParameterException
 */
public class PluginParameterException
    extends PluginConfigurationException
{

    private static final String LS = System.lineSeparator();

    private final List<Parameter> parameters;

    private final MojoDescriptor mojo;

    public PluginParameterException( MojoDescriptor mojo, List<Parameter> parameters )
    {
        super( mojo.getPluginDescriptor(), "The parameters " + format( parameters ) + " for goal "
            + mojo.getRoleHint() + " are missing or invalid" );

        this.mojo = mojo;

        this.parameters = parameters;
    }

    private static String format( List<Parameter> parameters )
    {
        StringBuilder buffer = new StringBuilder( 128 );
        if ( parameters != null )
        {
            for ( Parameter parameter : parameters )
            {
                if ( buffer.length() > 0 )
                {
                    buffer.append( ", " );
                }
                buffer.append( '\'' ).append( parameter.getName() ).append( '\'' );
            }
        }
        return buffer.toString();
    }

    public MojoDescriptor getMojoDescriptor()
    {
        return mojo;
    }

    public List<Parameter> getParameters()
    {
        return parameters;
    }

    private static void decomposeParameterIntoUserInstructions( MojoDescriptor mojo, Parameter param,
                                                                StringBuilder messageBuffer )
    {
        String expression = param.getExpression();

        if ( param.isEditable() )
        {
            boolean isArray = param.getType().endsWith( "[]" );
            boolean isCollection = false;
            boolean isMap = false;
            boolean isProperties = false;
            if ( !isArray )
            {
                try
                {
                    //assuming Type is available in current ClassLoader
                    isCollection = Collection.class.isAssignableFrom( Class.forName( param.getType() ) );
                    isMap = Map.class.isAssignableFrom( Class.forName( param.getType() ) );
                    isProperties = Properties.class.isAssignableFrom( Class.forName( param.getType() ) );
                }
                catch ( ClassNotFoundException e )
                {
                    // assume it is not assignable from Collection or Map
                }
            }

            messageBuffer.append( "Inside the definition for plugin \'" );
            messageBuffer.append( mojo.getPluginDescriptor().getArtifactId() );
            messageBuffer.append( "\', specify the following:" ).append( LS ).append( LS );
            messageBuffer.append( "<configuration>" ).append( LS ).append( "  ..." ).append( LS );
            messageBuffer.append( "  <" ).append( param.getName() ).append( '>' );
            if ( isArray || isCollection )
            {
                messageBuffer.append( LS );
                messageBuffer.append( "    <item>" );
            }
            else if ( isProperties )
            {
                messageBuffer.append( LS );
                messageBuffer.append( "    <property>" ).append( LS );
                messageBuffer.append( "      <name>KEY</name>" ).append( LS );
                messageBuffer.append( "      <value>" );
            }
            else if ( isMap )
            {
                messageBuffer.append( LS );
                messageBuffer.append( "    <KEY>" );
            }
            messageBuffer.append( "VALUE" );
            if ( isArray || isCollection )
            {
                messageBuffer.append( "</item>" ).append( LS );
                messageBuffer.append( "  " );
            }
            else if ( isProperties )
            {
                messageBuffer.append( "</value>" ).append( LS );
                messageBuffer.append( "    </property>" ).append( LS );
                messageBuffer.append( "  " );
            }
            else if ( isMap )
            {
                messageBuffer.append( "</KEY>" ).append( LS );
                messageBuffer.append( "  " );
            }
            messageBuffer.append( "</" ).append( param.getName() ).append( ">" ).append( LS );
            messageBuffer.append( "</configuration>" );

            String alias = param.getAlias();
            if ( StringUtils.isNotEmpty( alias ) && !alias.equals( param.getName() ) )
            {
                messageBuffer.append( LS ).append( LS ).append( "-OR-" ).append( LS ).append( LS );
                messageBuffer.append( "<configuration>" ).append( LS ).append( "  ..." ).append( LS );
                messageBuffer.append( "  <" ).append( alias ).append(
                    ">VALUE</" ).append( alias ).append( ">" ).append( LS ).append( "</configuration>" ).append( LS );
            }
        }

        if ( StringUtils.isEmpty( expression ) )
        {
            messageBuffer.append( '.' );
        }
        else
        {
            if ( param.isEditable() )
            {
                messageBuffer.append( LS ).append( LS ).append( "-OR-" ).append( LS ).append( LS );
            }

            //addParameterUsageInfo( expression, messageBuffer );
        }
    }

    public String buildDiagnosticMessage()
    {
        StringBuilder messageBuffer = new StringBuilder( 256 );

        List<Parameter> params = getParameters();
        MojoDescriptor mojo = getMojoDescriptor();

        messageBuffer.append( "One or more required plugin parameters are invalid/missing for \'" )
            .append( mojo.getPluginDescriptor().getGoalPrefix() ).append( ':' ).append( mojo.getGoal() )
            .append( "\'" ).append( LS );

        int idx = 0;
        for ( Iterator<Parameter> it = params.iterator(); it.hasNext(); idx++ )
        {
            Parameter param = it.next();

            messageBuffer.append( LS ).append( "[" ).append( idx ).append( "] " );

            decomposeParameterIntoUserInstructions( mojo, param, messageBuffer );

            messageBuffer.append( LS );
        }

        return messageBuffer.toString();
    }
}
