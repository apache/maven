package org.apache.maven.plugin;

import org.apache.maven.util.introspection.ClassMap;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;

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

            String methodName = "get" + StringUtils.capitalise( token );

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
