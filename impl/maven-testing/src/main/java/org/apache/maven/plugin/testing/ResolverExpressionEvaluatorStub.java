package org.apache.maven.plugin.testing;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

/**
 * Stub for {@link ExpressionEvaluator}
 *
 * @author jesse
 */
public class ResolverExpressionEvaluatorStub
    implements ExpressionEvaluator
{
    /** {@inheritDoc} */
    @Override
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

                    if ( index > 0 && expr.charAt( index - 1 ) == '$' )
                    {
                        retVal += expr.substring( index + 1, lastIndex + 1 );
                    }
                    else
                    {
                        retVal += evaluate( expr.substring( index, lastIndex + 1 ) );
                    }

                    retVal += evaluate( expr.substring( lastIndex + 1 ) );
                    return retVal;
                }
            }

            // Was not an expression
            if ( expression.indexOf( "$$" ) > -1 )
            {
                return expression.replaceAll( "\\$\\$", "\\$" );
            }
        }

        if ( "basedir".equals( expression ) || "project.basedir".equals( expression ) )
        {
            return PlexusTestCase.getBasedir();
        }
        else if ( expression.startsWith( "basedir" ) || expression.startsWith( "project.basedir" ) )
        {
            int pathSeparator = expression.indexOf( "/" );

            if ( pathSeparator > 0 )
            {
                value = PlexusTestCase.getBasedir() + expression.substring( pathSeparator );
            }
            else
            {
                System.out.println( "Got expression '" + expression + "' that was not recognised" );
            }
            return value;
        }
        else if ( "localRepository".equals( expression ) )
        {
            File localRepo = new File( PlexusTestCase.getBasedir(), "target/local-repo" );
            return new DefaultArtifactRepository( "localRepository", "file://" + localRepo.getAbsolutePath(),
                                                  new DefaultRepositoryLayout() );
        }
        else
        {
            return expr;
        }
    }

    private String stripTokens( String expr )
    {
        if ( expr.startsWith( "${" ) && expr.indexOf( "}" ) == expr.length() - 1 )
        {
            expr = expr.substring( 2, expr.length() - 1 );
        }

        return expr;
    }

    /** {@inheritDoc} */
    @Override
    public File alignToBaseDirectory( File file )
    {
        if ( file.getAbsolutePath().startsWith( PlexusTestCase.getBasedir() ) )
        {
            return file;
        }
        else if ( file.isAbsolute() )
        {
            return file;
        }
        else
        {
            return new File( PlexusTestCase.getBasedir(), file.getPath() );
        }
    }
}
