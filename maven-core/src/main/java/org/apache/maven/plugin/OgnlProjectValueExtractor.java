package org.apache.maven.plugin;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import ognl.Ognl;
import ognl.OgnlException;
import org.apache.maven.lifecycle.MavenLifecycleContext;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class OgnlProjectValueExtractor
{
    public static Object evaluate( String expression, MavenLifecycleContext context )
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
                // do nothing
            }
        }
        else if ( expression.equals( "#localRepository") )
        {
            value = context.getLocalRepository();
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
                    value = Ognl.getValue( expression.substring( 9, pathSeparator ), context.getProject() )
                        + expression.substring( pathSeparator );
                }
                else
                {
                    value = Ognl.getValue( expression.substring( 9 ), context.getProject() );
                }
            }
            catch ( OgnlException e )
            {
                // do nothing
            }
        }
        else if ( expression.equals( "#basedir" ) )
        {
            value = context.getProject().getFile().getParentFile().getAbsolutePath();
        }
        else if ( expression.startsWith( "#" ) )
        {
            expression = expression.substring( 1 );

            int pathSeparator = expression.indexOf( "/" );

            if ( pathSeparator > 0 )
            {
                value = context.getProject().getProperty( expression.substring( 0, pathSeparator ) )
                    + expression.substring( pathSeparator );
            }
            else
            {
                value = context.getProject().getProperty( expression );
            }

        }


        // If we strike out we'll just use the expression which allows
        // people to use hardcoded values if they wish.

        if ( value == null )
        {
            value = expression;
        }

        return value;
    }
}
