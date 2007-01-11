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

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.usability.plugin.Expression;
import org.apache.maven.usability.plugin.ExpressionDocumentationException;
import org.apache.maven.usability.plugin.ExpressionDocumenter;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginConfigurationException
    extends Exception
{
    private final PluginDescriptor pluginDescriptor;

    private String originalMessage;

    private static final List UNMODIFIABLE_EXPRESSIONS = Arrays.asList(
        new String[]{"localRepository", "reactorProjects", "settings", "project", "session", "plugin", "basedir"} );

    public PluginConfigurationException( PluginDescriptor pluginDescriptor, String message )
    {
        super( "Error configuring: " + pluginDescriptor.getPluginLookupKey() + ". Reason: " + message );
        this.pluginDescriptor = pluginDescriptor;
        this.originalMessage = message;
    }

    public PluginConfigurationException( PluginDescriptor pluginDescriptor, Throwable cause )
    {
        super( "Error configuring: " + pluginDescriptor.getPluginLookupKey() + ".", cause );
        this.pluginDescriptor = pluginDescriptor;
    }

    public PluginConfigurationException( PluginDescriptor pluginDescriptor, String message, Throwable cause )
    {
        super( "Error configuring: " + pluginDescriptor.getPluginLookupKey() + ". Reason: " + message, cause );
        this.pluginDescriptor = pluginDescriptor;
        this.originalMessage = message;
    }

    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }

    public String getOriginalMessage()
    {
        return originalMessage;
    }

    protected static void addParameterUsageInfo( String expression, StringBuffer messageBuffer )
    {
        StringBuffer expressionMessageBuffer = new StringBuffer();

        Matcher exprMatcher = Pattern.compile( "\\$\\{(.+)\\}" ).matcher( expression );

        boolean unmodifiableElementsFound = false;
        boolean activeElementsFound = false;

        int elementCount = 0;

        while ( exprMatcher.find() )
        {
            elementCount++;

            activeElementsFound = true;

            String subExpression = exprMatcher.group( 1 );

            StringTokenizer expressionParts = new StringTokenizer( subExpression, "." );

            String firstPart = expressionParts.nextToken();

            Map expressions = null;
            try
            {
                expressions = ExpressionDocumenter.load();
            }
            catch ( ExpressionDocumentationException e )
            {
                expressionMessageBuffer.append( "\n\nERROR!! Failed to load expression documentation!" );

                StringWriter sWriter = new StringWriter();
                PrintWriter pWriter = new PrintWriter( sWriter );

                e.printStackTrace( pWriter );

                expressionMessageBuffer.append( "\n\nException:\n\n" ).append( sWriter.toString() );
            }

            if ( expressions != null )
            {
                Expression expr = (Expression) expressions.get( subExpression );

                if ( expr != null )
                {
                    if ( !expr.isEditable() )
                    {
                        unmodifiableElementsFound = true;
                    }
                    else
                    {
                        addParameterConfigDocumentation( firstPart, exprMatcher.group( 0 ), subExpression,
                                                         expressionMessageBuffer, expressions );
                    }
                }
                else if ( UNMODIFIABLE_EXPRESSIONS.contains( subExpression ) )
                {
                    unmodifiableElementsFound = true;
                }
                else
                {
                    expressionMessageBuffer.append( "on the command line, specify: \'-D" ).append( subExpression )
                        .append( "=VALUE\'" );
                }
            }
        }

        if ( activeElementsFound )
        {
            messageBuffer.append( expressionMessageBuffer );
        }
        else
        {
            messageBuffer.append(
                "    (found static expression: \'" + expression + "\' which may act as a default value).\n" );
        }

        if ( unmodifiableElementsFound )
        {
            if ( elementCount > 1 )
            {
                messageBuffer.append( "    " );
            }

            messageBuffer
                .append( "NOTE: One or more purely derived expression elements were detected in \'" + expression +
                    "\'.\n    If you continue to get this error after any other expression elements are specified correctly," +
                    "\n    please report this issue to the Maven development team.\n" );
        }
    }

    private static void addParameterConfigDocumentation( String firstPart, String wholeExpression, String subExpression,
                                                         StringBuffer expressionMessageBuffer, Map expressionDoco )
    {
        Expression expr = (Expression) expressionDoco.get( subExpression );

        if ( expr != null )
        {
            expressionMessageBuffer.append( "check that the following section of " );
            if ( "project".equals( firstPart ) )
            {
                expressionMessageBuffer.append( "the pom.xml " );
            }
            else if ( "settings".equals( firstPart ) )
            {
                expressionMessageBuffer.append( "your ~/.m2/settings.xml file " );
            }

            expressionMessageBuffer.append( "is present and correct:\n\n" );

            String message = expr.getConfiguration();

            if ( message == null )
            {
                message = expr.getDescription();
            }

            expressionMessageBuffer.append( message );

            Properties cliConfig = expr.getCliOptions();

            if ( cliConfig != null && !cliConfig.isEmpty() )
            {
                expressionMessageBuffer.append( "\n\n-OR-\n\nUse the following command-line switches:\n" );

                prettyPrintCommandLineSwitches( cliConfig, '.', expressionMessageBuffer );
            }
        }
        else
        {
            expressionMessageBuffer.append( "ensure that the expression: \'" + wholeExpression + "\' is satisfied" );
        }
    }

    private static void prettyPrintCommandLineSwitches( Properties switches, char filler,
                                                        StringBuffer expressionMessageBuffer )
    {
        int maxKeyLen = 0;

        for ( Iterator it = switches.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            String key = (String) entry.getKey();

            int keyLen = key.length();
            if ( keyLen > maxKeyLen )
            {
                maxKeyLen = keyLen;
            }
        }

        final int minFillerCount = 4;

        for ( Iterator it = switches.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            String key = (String) entry.getKey();

            int keyLen = key.length();

            int fillerCount = maxKeyLen - keyLen + minFillerCount;

            expressionMessageBuffer.append( '\n' ).append( key ).append( ' ' );

            for ( int i = 0; i < fillerCount; i++ )
            {
                expressionMessageBuffer.append( filler );
            }

            expressionMessageBuffer.append( ' ' ).append( entry.getValue() );
        }

        expressionMessageBuffer.append( '\n' );
    }

    public String buildConfigurationDiagnosticMessage( ComponentConfigurationException cce )
    {
        StringBuffer message = new StringBuffer();

        PluginDescriptor descriptor = getPluginDescriptor();

        PlexusConfiguration failedConfiguration = cce.getFailedConfiguration();

        message.append( "Failed to configure plugin parameters for: " + descriptor.getId() + "\n\n" );

        if ( failedConfiguration != null )
        {
            String value = failedConfiguration.getValue( null );
            if ( value != null )
            {
                addParameterUsageInfo( value, message );
            }
        }

        message.append( "\n\nCause: " ).append( cce.getMessage() );

        return message.toString();
    }
}
