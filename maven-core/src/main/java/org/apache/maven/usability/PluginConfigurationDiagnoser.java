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
import org.apache.maven.usability.plugin.ExpressionDocumenter;
import org.apache.maven.usability.plugin.ExpressionDocumentationException;
import org.apache.maven.usability.plugin.ExpressionDocumentation;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
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
        exprs.add( "settings" );
        exprs.add( "project" );
        exprs.add( "session" );
        exprs.add( "plugin" );
        exprs.add( "basedir" );

        UNMODIFIABLE_EXPRESSIONS = exprs;
    }

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality(error, PluginConfigurationException.class );
    }

    public String diagnose( Throwable error )
    {
        PluginConfigurationException pce = (PluginConfigurationException) DiagnosisUtils.getFromCausality(error, PluginConfigurationException.class );
        
        if ( pce instanceof PluginParameterException )
        {
            PluginParameterException exception = (PluginParameterException) pce;

            return buildParameterDiagnosticMessage( exception );
        }
        else if( DiagnosisUtils.containsInCausality(pce, ComponentConfigurationException.class ) )
        {
            ComponentConfigurationException cce = (ComponentConfigurationException) DiagnosisUtils.getFromCausality( pce, ComponentConfigurationException.class );
            return buildInvalidPluginConfigurationDiagnosisMessage( cce );
        }
        else
        {
            return pce.getMessage();
        }
    }

    private String buildInvalidPluginConfigurationDiagnosisMessage( ComponentConfigurationException cce )
    {
        StringBuffer message = new StringBuffer();
        
        message.append( "Either your POM or one of its ancestors has declared an invalid plugin configuration.\n" )
               .append( "The error message is:\n\n" )
               .append( cce.getMessage() ).append( "\n" );
        
        return message.toString();
    }

    private String buildParameterDiagnosticMessage( PluginParameterException exception )
    {
        StringBuffer messageBuffer = new StringBuffer();

        List params = exception.getParameters();
        MojoDescriptor mojo = exception.getMojoDescriptor();

        messageBuffer.append( "One or more required plugin parameters are invalid/missing for \'" ).append(
            mojo.getPluginDescriptor().getGoalPrefix() ).append( ":" ).append( mojo.getGoal() ).append( "\'\n" );

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
            messageBuffer.append( "inside the definition for plugin: \'" + mojo.getPluginDescriptor().getArtifactId() + "\'specify the following:\n\n<configuration>\n  ...\n  <" + param.getName() + ">VALUE</" + param.getName() + ">\n</configuration>" );

            String alias = param.getAlias();
            if ( StringUtils.isNotEmpty( alias ) )
            {
                messageBuffer.append( "\n\n-OR-\n\n<configuration>\n  ...\n  <" + alias + ">VALUE</" + alias + ">\n</configuration>\n" );
            }
        }

        if ( StringUtils.isEmpty( expression ) )
        {
            messageBuffer.append( "." );
        }
        else
        {
            StringBuffer expressionMessageBuffer = new StringBuffer();

            if ( param.isEditable() )
            {
                expressionMessageBuffer.append( "\n\n-OR-\n\n" );
            }

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

                if ( expressionParts.hasMoreTokens() && ( "project".equals( firstPart ) || "settings".equals( firstPart ) ) )
                {
                    addParameterConfigDocumentation( firstPart, exprMatcher.group( 0 ), subExpression, expressionMessageBuffer );
                }
                else if ( UNMODIFIABLE_EXPRESSIONS.contains( subExpression ) )
                {
                    unmodifiableElementsFound = true;
                }
                else
                {
                    expressionMessageBuffer.append( "on the command line, specify: \'-D" ).append( subExpression ).append("=VALUE\'" );
                }
            }

            if ( activeElementsFound )
            {
                messageBuffer.append( expressionMessageBuffer );
            }
            else
            {
                messageBuffer.append( "    (found static expression: \'" + expression +
                                      "\' which may act as a default value).\n" );
            }

            if ( unmodifiableElementsFound )
            {
                if ( elementCount > 1 )
                {
                    messageBuffer.append( "    " );
                }

                messageBuffer.append( "NOTE: One or more purely derived expression elements were detected in \'" +
                                      expression +
                                      "\'.\n    If you continue to get this error after any other expression elements are specified correctly," +
                                      "\n    please report this issue to the Maven development team.\n" );
            }
        }
    }

    private void addParameterConfigDocumentation( String firstPart, String wholeExpression, String subExpression, StringBuffer expressionMessageBuffer )
    {
        try
        {
            Map expressionDoco = ExpressionDocumenter.load();

            ExpressionDocumentation info = (ExpressionDocumentation) expressionDoco.get( subExpression );

            if ( info != null )
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
                
                String message = info.getOrigin();
                
                if ( message == null )
                {
                    message = info.getUsage();
                }
                
                expressionMessageBuffer.append( message );
                
                String addendum = info.getAddendum();
                
                if ( addendum != null )
                {
                    expressionMessageBuffer.append("\n\n").append( addendum );
                }
            }
            else
            {
                expressionMessageBuffer.append( "ensure that the expression: \'"
                    + wholeExpression + "\' is satisfied" );
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

}
