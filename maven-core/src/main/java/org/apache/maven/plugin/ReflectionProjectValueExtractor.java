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

import org.apache.maven.project.MavenProject;
import org.apache.maven.util.introspection.ClassMap;
import org.codehaus.plexus.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Using simple dotted expressions extract the values from a MavenProject
 * instance, For example we might want to extract a value like:
 *
 * project.build.sourceDirectory
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ReflectionProjectValueExtractor
{
    private static Class[] args = new Class[0];

    private static Object[] params = new Object[0];

    private static ClassMap classMap;
    
    private static Map classMaps = new HashMap();

    public static Object evaluate( String expression, MavenProject project )
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Remove the leading "project" token
        // ----------------------------------------------------------------------

        expression = expression.substring( expression.indexOf( '.' ) + 1 );

        Object value = project;

        // ----------------------------------------------------------------------
        // Walk the dots and retrieve the ultimate value desired from the
        // MavenProject instance.
        // ----------------------------------------------------------------------

        StringTokenizer parser = new StringTokenizer( expression, "." );

        while( parser.hasMoreTokens() )
        {
            classMap = getClassMap( value.getClass() );

            String token = parser.nextToken();

            String methodName = "get" + StringUtils.capitalizeFirstLetter( token );

            Method method = classMap.findMethod( methodName, args );

            value = method.invoke( value, params );
        }

        return value;
    }

    private static ClassMap getClassMap( Class clazz )
    {
        classMap = (ClassMap) classMaps.get( clazz );

        if ( classMap == null )
        {
            classMap = new ClassMap( clazz );
        }

        return classMap;
    }
}
