package org.apache.maven.plugin;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class PluginParameterExpressionEvaluator
{
    public static Object evaluate( String expression, MavenGoalExecutionContext context )
        throws PluginConfigurationException
    {
        Object value = null;

        if ( expression.startsWith( "#component" ) )
        {
            String role = expression.substring( 11 );

            try
            {
                value = context.lookup( role );
            }
            catch ( ComponentLookupException e )
            {
                throw new PluginConfigurationException( "Cannot lookup component: ", e );
            }
        }
        else if ( expression.equals( "#localRepository" ) )
        {
            value = context.getSession().getLocalRepository();
        }
        else if ( expression.equals( "#maven.repo.local" ) )
        {
            // TODO: remove this alias: but note that it is a string instead of an ArtifactRepository
            value = context.getSession().getLocalRepository().getUrl().substring( "file://".length() );
        }
        else if ( expression.equals( "#maven.final.name" ) )
        {
            // TODO: remove this alias
            value = context.getProject().getModel().getBuild().getFinalName();
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
                    value = getValue( expression.substring( 1, pathSeparator ), context.getProject() )
                        + expression.substring( pathSeparator );
                }
                else
                {
                    value = getValue( expression.substring( 1 ), context.getProject() );
                }
            }
            catch ( Exception e )
            {
                throw new PluginConfigurationException( "Error evaluating plugin parameter expression: " + expression, e );
            }
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
                value = context.getProject().getFile().getParentFile().getAbsolutePath()
                    + expression.substring( pathSeparator );
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

        // ----------------------------------------------------------------------              
        // If we strike and we are not dealing with an expression then we will
        // will let the value pass through unaltered so that users can hardcode
        // literal values. Expressions that evaluate to null will be passed
        // through as null so that the validator can see the null value and
        // act in accordance with the requirements laid out in the
        // mojo descriptor.
        // ----------------------------------------------------------------------

        if ( value == null && !expression.startsWith( "#" ) )
        {
            value = expression;
        }

        return value;
    }

    private static Object getValue( String expression, MavenProject project )
        throws Exception
    {
        return ReflectionProjectValueExtractor.evaluate( expression, project );
    }
}

