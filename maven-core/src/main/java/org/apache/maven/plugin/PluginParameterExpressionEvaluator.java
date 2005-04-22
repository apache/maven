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
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
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

    public PluginParameterExpressionEvaluator( MavenSession context, PathTranslator pathTranslator )
    {
        this.context = context;
        this.pathTranslator = pathTranslator;
    }

    public Object evaluate( String expr )
        throws ExpressionEvaluationException
    {
        Object value = null;

        if ( expr == null )
        {
            // TODO: this should not have happened - previously there was a note about a fix to plexus Trygve was going to make - investigate
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

        if ( expression.startsWith( "component" ) )
        {
            context.getLog().warn( "WARNING: plugin is using deprecated expression " + expression );

            // TODO: deprecated... and can remove the lookup method in context afterwards
            String role = expression.substring( 10 );

            try
            {
                value = context.lookup( role );
            }
            catch ( ComponentLookupException e )
            {
                throw new ExpressionEvaluationException( "Cannot lookup component: " + role + ".", e );
            }
        }
        else if ( expression.equals( "localRepository" ) )
        {
            value = context.getLocalRepository();
        }
        else if ( expression.equals( "maven.final.name" ) )
        {
            // TODO: remove this alias
            value = context.getProject().getBuild().getFinalName();
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
                context.getLog().error( "Got expression '" + expression + "' that was not recognised" );
            }
        }
        else
        {
            // We will attempt to get nab a system property as a way to specify a
            // parameter to a plugins. My particular case here is allowing the surefire
            // plugin to run a single test so I want to specify that class on the cli
            // as a parameter.

            value = System.getProperty( expression );
        }

        if ( value instanceof String )
        {
            // TODO: without #, this could just be an evaluate call...

            String val = (String) value;
            int sharpSeparator = val.indexOf( "#" );
            if ( sharpSeparator < 0 )
            {
                sharpSeparator = val.indexOf( "${" );
            }

            if ( sharpSeparator >= 0 )
            {
                if ( sharpSeparator > 0 )
                {
                    value = val.substring( 0, sharpSeparator ) + evaluate( val.substring( sharpSeparator ) );
                }
                else
                {
                    value = evaluate( val.substring( sharpSeparator ) );
                }
            }
        }

        return value;
    }

    private String stripTokens( String expr )
    {
        if ( expr.startsWith( "#" ) )
        {
            context.getLog().warn( "DEPRECATED: use ${} to delimit expressions instead of # for '" + expr + "'" );
            expr = expr.substring( 1 );
        }
        else if ( expr.startsWith( "${" ) && expr.endsWith( "}" ) )
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

