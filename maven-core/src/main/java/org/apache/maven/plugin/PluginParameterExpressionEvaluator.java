package org.apache.maven.plugin;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo belong in MavenSession, so it only gets created once?
 */
public class PluginParameterExpressionEvaluator
    implements ExpressionEvaluator
{
    private final PathTranslator pathTranslator;

    private final MavenSession context;

    private final Logger logger;

    public PluginParameterExpressionEvaluator( MavenSession context, PathTranslator pathTranslator, Logger logger )
    {
        this.context = context;
        this.pathTranslator = pathTranslator;
        this.logger = logger;
    }

    public Object evaluate( String expr )
        throws ExpressionEvaluationException
    {
        Object value = null;

        if ( expr == null )
        {
            return null;
        }

        String expression = stripTokens( expr );
        if ( expression.equals( expr ) )
        {
            int index = expr.indexOf( "${" );
            if ( index >= 0 )
            {
                int lastIndex = expr.indexOf( "}", index );
                if ( lastIndex >= 0 )
                {
                    String retVal = expr.substring( 0, index );
                    retVal += evaluate( expr.substring( index, lastIndex + 1 ) );
                    retVal += evaluate( expr.substring( lastIndex + 1 ) );
                    return retVal;
                }
            }

            // Was not an expression
            return expression;
        }

        if ( expression.equals( "localRepository" ) )
        {
            value = context.getLocalRepository();
        }
        else if ( expression.equals( "project" ) )
        {
            value = context.getProject();
        }
        else if ( expression.startsWith( "project" ) )
        {
            try
            {
                int pathSeparator = expression.indexOf( "/" );

                if ( pathSeparator > 0 )
                {
                    String pathExpression = expression.substring( 1, pathSeparator );
                    value = ReflectionValueExtractor.evaluate( pathExpression, context.getProject() );
                    value = value + expression.substring( pathSeparator );
                }
                else
                {
                    value = ReflectionValueExtractor.evaluate( expression.substring( 1 ), context.getProject() );
                }
            }
            catch ( Exception e )
            {
                // TODO: don't catch exception
                throw new ExpressionEvaluationException( "Error evaluating plugin parameter expression: " + expression,
                                                         e );
            }
        }
        else if ( "settings".equals( expression ) )
        {
            value = context.getSettings();
        }
        else if ( expression.equals( "basedir" ) )
        {
            value = context.getProject().getBasedir().getAbsolutePath();
        }
        else if ( expression.startsWith( "basedir" ) )
        {
            int pathSeparator = expression.indexOf( "/" );

            if ( pathSeparator > 0 )
            {
                value = context.getProject().getFile().getParentFile().getAbsolutePath() +
                    expression.substring( pathSeparator );
            }
            else
            {
                logger.error( "Got expression '" + expression + "' that was not recognised" );
            }
        }
        else
        {
            // Check properties that have been injected via profiles before we default over to 
            // system properties.
            
            if( context.getProject().getProfileConfiguration() != null )
            {
                value = context.getProject().getProfileConfiguration().getProperty( expression );
            }
            
            if( value == null )
            {
                // We will attempt to get nab a system property as a way to specify a
                // parameter to a plugins. My particular case here is allowing the surefire
                // plugin to run a single test so I want to specify that class on the cli
                // as a parameter.

                value = System.getProperty( expression );
            }
        }

        if ( value instanceof String )
        {
            // TODO: without #, this could just be an evaluate call...

            String val = (String) value;

            int exprStartDelimiter = val.indexOf( "${" );

            if ( exprStartDelimiter >= 0 )
            {
                if ( exprStartDelimiter > 0 )
                {
                    value = val.substring( 0, exprStartDelimiter ) + evaluate( val.substring( exprStartDelimiter ) );
                }
                else
                {
                    value = evaluate( val.substring( exprStartDelimiter ) );
                }
            }
        }

        return value;
    }

    private String stripTokens( String expr )
    {
        if ( expr.startsWith( "${" ) && expr.indexOf( "}" ) == expr.length() - 1 )
        {
            expr = expr.substring( 2, expr.length() - 1 );
        }
        return expr;
    }

    public File alignToBaseDirectory( File file )
    {
        File basedir = context.getProject().getFile().getParentFile();
        return new File( pathTranslator.alignToBaseDirectory( file.getPath(), basedir ) );
    }

}

