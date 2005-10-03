package org.apache.maven.usability;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginParameterException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.usability.plugin.Expression;
import org.apache.maven.usability.plugin.ExpressionDocumentationException;
import org.apache.maven.usability.plugin.ExpressionDocumenter;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginConfigurationDiagnoser
    implements ErrorDiagnoser
{

    private static final List UNMODIFIABLE_EXPRESSIONS;

    static
    {
        List exprs = new ArrayList();

        exprs.add( "localRepository" );
        exprs.add( "reactorProjects" );
        exprs.add( "settings" );
        exprs.add( "project" );
        exprs.add( "session" );
        exprs.add( "plugin" );
        exprs.add( "basedir" );

        UNMODIFIABLE_EXPRESSIONS = exprs;
    }

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, PluginConfigurationException.class );
    }

    public String diagnose( Throwable error )
    {
        PluginConfigurationException pce = (PluginConfigurationException) DiagnosisUtils
            .getFromCausality( error, PluginConfigurationException.class );

        if ( pce instanceof PluginParameterException )
        {
            PluginParameterException exception = (PluginParameterException) pce;

            return buildParameterDiagnosticMessage( exception );
        }
        else if ( DiagnosisUtils.containsInCausality( pce, ComponentConfigurationException.class ) )
        {
            ComponentConfigurationException cce = (ComponentConfigurationException) DiagnosisUtils
                .getFromCausality( pce, ComponentConfigurationException.class );
            
            return buildConfigurationDiagnosticMessage( pce, cce );
        }
        else
        {
            return pce.getMessage();
        }
    }

    private String buildConfigurationDiagnosticMessage( PluginConfigurationException pce, ComponentConfigurationException cce )
    {
        StringBuffer message = new StringBuffer();
        
        PluginDescriptor descriptor = pce.getPluginDescriptor();

        PlexusConfiguration failedConfiguration = cce.getFailedConfiguration();
        
        message.append( "Failed to configure plugin parameters for: " + descriptor.getId() + "\n\n" );
        
        if ( failedConfiguration != null )
        {
            String value = failedConfiguration.getValue( null );
            addParameterUsageInfo( value, message );
        }
        
        message.append( "Reason: " ).append( cce.getMessage() ).append( "\n" );
        
        Throwable root = DiagnosisUtils.getRootCause( cce );
        
        message.append( "Root Cause: " ).append( root.getMessage() ).append( "\n\n" );

        return message.toString();
    }

    private String buildParameterDiagnosticMessage( PluginParameterException exception )
    {
        StringBuffer messageBuffer = new StringBuffer();

        List params = exception.getParameters();
        MojoDescriptor mojo = exception.getMojoDescriptor();

        messageBuffer.append( "One or more required plugin parameters are invalid/missing for \'" )
            .append( mojo.getPluginDescriptor().getGoalPrefix() ).append( ":" ).append( mojo.getGoal() )
            .append( "\'\n" );

        int idx = 0;
        for ( Iterator it = params.iterator(); it.hasNext(); )
        {
            Parameter param = (Parameter) it.next();

            messageBuffer.append( "\n[" ).append( idx++ ).append( "] " );

            decomposeParameterIntoUserInstructions( mojo, param, messageBuffer );

            messageBuffer.append( "\n" );
        }

        return messageBuffer.toString();
    }

    private void decomposeParameterIntoUserInstructions( MojoDescriptor mojo, Parameter param,
                                                        StringBuffer messageBuffer )
    {
        String expression = param.getExpression();

        if ( param.isEditable() )
        {
            messageBuffer.append( "inside the definition for plugin: \'" + mojo.getPluginDescriptor().getArtifactId()
                + "\'specify the following:\n\n<configuration>\n  ...\n  <" + param.getName() + ">VALUE</"
                + param.getName() + ">\n</configuration>" );

            String alias = param.getAlias();
            if ( StringUtils.isNotEmpty( alias ) )
            {
                messageBuffer.append( "\n\n-OR-\n\n<configuration>\n  ...\n  <" + alias + ">VALUE</" + alias
                    + ">\n</configuration>\n" );
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
            
            addParameterUsageInfo( expression, messageBuffer );
        }
    }

    private void addParameterUsageInfo( String expression, StringBuffer messageBuffer )
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

            try
            {
                Map expressions = ExpressionDocumenter.load();
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
                                                         expressionMessageBuffer );
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
            catch ( ExpressionDocumentationException e )
            {
                expressionMessageBuffer.append( "\n\nERROR!! Failed to load expression documentation!" );

                StringWriter sWriter = new StringWriter();
                PrintWriter pWriter = new PrintWriter( sWriter );

                e.printStackTrace( pWriter );

                expressionMessageBuffer.append( "\n\nException:\n\n" ).append( sWriter.toString() );
            }
        }

        if ( activeElementsFound )
        {
            messageBuffer.append( expressionMessageBuffer );
        }
        else
        {
            messageBuffer.append( "    (found static expression: \'" + expression
                + "\' which may act as a default value).\n" );
        }

        if ( unmodifiableElementsFound )
        {
            if ( elementCount > 1 )
            {
                messageBuffer.append( "    " );
            }

            messageBuffer
                .append( "NOTE: One or more purely derived expression elements were detected in \'"
                    + expression
                    + "\'.\n    If you continue to get this error after any other expression elements are specified correctly,"
                    + "\n    please report this issue to the Maven development team.\n" );
        }
    }

    private void addParameterConfigDocumentation( String firstPart, String wholeExpression, String subExpression,
                                                 StringBuffer expressionMessageBuffer )
        throws ExpressionDocumentationException
    {
        Map expressionDoco = ExpressionDocumenter.load();

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

    private void prettyPrintCommandLineSwitches( Properties switches, char filler, StringBuffer expressionMessageBuffer )
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

}
