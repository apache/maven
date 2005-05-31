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
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
        exprs.add( "basedir" );

        UNMODIFIABLE_EXPRESSIONS = exprs;
    }

    public boolean canDiagnose( Throwable error )
    {
        return error instanceof PluginConfigurationException;
    }

    public String diagnose( Throwable error )
    {
        if ( error instanceof PluginParameterException )
        {
            PluginParameterException exception = (PluginParameterException) error;

            return buildParameterDiagnosticMessage( exception );
        }
        else if( DiagnosisUtils.containsInCausality(error, ComponentConfigurationException.class ) )
        {
            ComponentConfigurationException cce = (ComponentConfigurationException) DiagnosisUtils.getFromCausality( error, ComponentConfigurationException.class );
            return buildInvalidPluginConfigurationDiagnosisMessage( cce );
        }
        else
        {
            return error.getMessage();
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
            messageBuffer.append( "specify configuration for <" + param.getName() + ">VALUE</" + param.getName() + ">" );

            String alias = param.getAlias();
            if ( StringUtils.isNotEmpty( alias ) )
            {
                messageBuffer.append( " (aliased as: <" + alias + ">VALUE</" + alias + ">)" );
            }

            messageBuffer.append( "\n    inside the <configuration/> section for " +
                                  mojo.getPluginDescriptor().getArtifactId() );
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
                expressionMessageBuffer.append( ", or\n    " );
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

                if ( "project".equals( firstPart ) && expressionParts.hasMoreTokens() )
                {
                    appendProjectSection( expressionParts, expressionMessageBuffer );
                }
                else if ( "reports".equals( firstPart ) )
                {
                    expressionMessageBuffer.append(
                        "make sure the <reports/> section of the pom.xml contains valid report names\n" );
                }
                else if ( UNMODIFIABLE_EXPRESSIONS.contains( subExpression ) )
                {
                    unmodifiableElementsFound = true;
                }
                else
                {
                    expressionMessageBuffer.append( "Please provide the system property: " ).append( subExpression ).append(
                        "\n    (specified as \'-D" + subExpression + "=VALUE\' on the command line)\n" );
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

    private void appendProjectSection( StringTokenizer expressionParts, StringBuffer messageBuffer )
    {
        messageBuffer.append( "check that the following section of the pom.xml is present and correct:\n\n" );

        Stack nestedParts = new Stack();

        String indentation = "      ";

        messageBuffer.append( indentation ).append( "<project>\n" );

        nestedParts.push( "project" );

        indentation += "  ";

        while ( expressionParts.hasMoreTokens() )
        {
            String nextPart = expressionParts.nextToken();

            messageBuffer.append( indentation ).append( "<" ).append( nextPart );

            if ( expressionParts.hasMoreTokens() )
            {
                messageBuffer.append( ">\n" );

                indentation += "  ";

                nestedParts.push( nextPart );
            }
            else
            {
                messageBuffer.append( "/>\n" );

                indentation = indentation.substring( 2 );
            }
        }

        if ( !nestedParts.isEmpty() )
        {
            while ( nestedParts.size() > 0 )
            {
                String prevPart = (String) nestedParts.pop();

                messageBuffer.append( indentation ).append( "</" ).append( prevPart ).append( ">\n" );

                indentation = indentation.substring( 2 );
            }
        }
    }

}
