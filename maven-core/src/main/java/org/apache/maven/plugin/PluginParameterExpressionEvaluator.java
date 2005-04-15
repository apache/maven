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
import org.apache.maven.util.introspection.ReflectionValueExtractor;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo belong in MavenSession, so it only gets created once?
 */
public class PluginParameterExpressionEvaluator
    implements ExpressionEvaluator
{
    private final MavenSession context;

    public PluginParameterExpressionEvaluator( MavenSession context )
    {
        this.context = context;
    }

    public Object evaluate( String expression )
        throws ExpressionEvaluationException
    {
        Object value = null;

        if ( expression == null )
        {
            // todo : verify if it's fixed with trygvis modification in Plexus
            return null;
        }
        if ( expression.startsWith( "#component" ) )
        {
            context.getLog().warn( "WARNING: plugin is using deprecated expression " + expression );

            // TODO: deprecated... and can remove the lookup method in context afterwards
            String role = expression.substring( 11 );

            try
            {
                value = context.lookup( role );
            }
            catch ( ComponentLookupException e )
            {
                throw new ExpressionEvaluationException( "Cannot lookup component: " + role + ".", e );
            }
        }
        else if ( expression.equals( "#localRepository" ) )
        {
            value = context.getLocalRepository();
        }
        else if ( expression.equals( "#maven.final.name" ) )
        {
            // TODO: remove this alias
            value = context.getProject().getBuild().getFinalName();
        }
        else if ( expression.equals( "#project" ) )
        {
            value = context.getProject();
        }
        else if ( expression.startsWith( "#project" ) )
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
        else if ( "#settings".equals( expression ) )
        {
            value = context.getSettings();
        }
        else if ( expression.equals( "#basedir" ) )
        {
            value = context.getProject().getFile().getParentFile().getAbsolutePath();
        }
        else if ( expression.startsWith( "#basedir" ) )
        {
            int pathSeparator = expression.indexOf( "/" );

            if ( pathSeparator > 0 )
            {
                value = context.getProject().getFile().getParentFile().getAbsolutePath() +
                    expression.substring( pathSeparator );
            }
            else
            {
                new Exception( "Got expression '" + expression + "' that was not recognised" ).printStackTrace();
            }
        }
        else if ( expression.startsWith( "#" ) )
        {
            // We will attempt to get nab a system property as a way to specify a
            // parameter to a plugins. My particular case here is allowing the surefire
            // plugin to run a single test so I want to specify that class on the cli
            // as a parameter.

            value = System.getProperty( expression.substring( 1 ) );
        }

        if ( value instanceof String )
        {
            String val = (String) value;
            int sharpSeparator = val.indexOf( "#" );

            if ( sharpSeparator > 0 )
            {
                val = val.substring( 0, sharpSeparator ) + evaluate( val.substring( sharpSeparator ) );
                value = val;
            }
            else if ( sharpSeparator > 0 )
            {
                value = evaluate( val.substring( sharpSeparator ) );
            }
        }

        // ----------------------------------------------------------------------
        // If we strike and we are not dealing with an expression then we will
        // will let the value pass through unaltered so that users can hardcode
        // literal values. Expressions that evaluate to null will be passed
        // through as null so that the validator can see the null value and
        // act in accordance with the requirements laid out in the
        // mojo descriptor.
        // ----------------------------------------------------------------------

        if ( value == null && expression.length() > 0 && !expression.startsWith( "#" ) )
        {
            value = expression;
        }

        return value;
    }

}

