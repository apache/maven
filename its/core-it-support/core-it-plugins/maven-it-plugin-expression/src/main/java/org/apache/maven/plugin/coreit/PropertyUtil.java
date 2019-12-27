package org.apache.maven.plugin.coreit;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Assists in serializing primitives and beans into properties for later inspection/verification.
 *
 * @author Benjamin Bentmann
 *
 */
class PropertyUtil
{

    private static final Object[] NO_ARGS = {};

    private static final Class[] NO_PARAMS = {};

    /**
     * Serializes the specified object into the given properties, using the provided key. The object may be a scalar
     * value like a string or some array/collection/map or a bean.
     *
     * @param props The properties to serialize into, must not be <code>null</code>.
     * @param key   The key to use for serialization of the object data, must not be <code>null</code>.
     * @param obj   The object to serialize, may be <code>null</code>.
     */
    public static void store( Properties props, String key, Object obj )
    {
        store( props, key, obj, new HashSet() );
    }

    /**
     * Serializes the specified object into the given properties, using the provided key. The object may be a scalar
     * value like a string or some array/collection/map or a bean.
     *
     * @param props   The properties to serialize into, must not be <code>null</code>.
     * @param key     The key to use for serialization of the object data, must not be <code>null</code>.
     * @param obj     The object to serialize, may be <code>null</code>.
     * @param visited The set/stack of already visited objects, used to detect back references in the object graph, must
     *                not be <code>null</code>.
     */
    private static void store( Properties props, String key, Object obj, Collection visited )
    {
        if ( obj != null && !visited.contains( obj ) )
        {
            visited.add( obj );
            if ( ( obj instanceof String ) || ( obj instanceof Number ) || ( obj instanceof Boolean )
                || ( obj instanceof File ) )
            {
                props.put( key, obj.toString() );
            }
            else if ( obj instanceof Collection )
            {
                Collection coll = (Collection) obj;
                props.put( key, Integer.toString( coll.size() ) );
                int index = 0;
                for ( Iterator it = coll.iterator(); it.hasNext(); index++ )
                {
                    Object elem = it.next();
                    store( props, key + "." + index, elem, visited );
                }
            }
            else if ( obj instanceof Map )
            {
                Map map = (Map) obj;
                props.put( key, Integer.toString( map.size() ) );
                int index = 0;
                for ( Iterator it = map.entrySet().iterator(); it.hasNext(); index++ )
                {
                    Map.Entry entry = (Map.Entry) it.next();
                    store( props, key + "." + entry.getKey(), entry.getValue(), visited );
                }
            }
            else if ( obj.getClass().isArray() )
            {
                int length = Array.getLength( obj );
                props.put( key, Integer.toString( length ) );
                for ( int index = 0; index < length; index++ )
                {
                    Object elem = Array.get( obj, index );
                    store( props, key + "." + index, elem, visited );
                }
            }
            else if ( obj.getClass().getName().endsWith( "Xpp3Dom" ) )
            {
                Class type = obj.getClass();
                try
                {
                    Method getValue = type.getMethod( "getValue", NO_PARAMS );
                    String value = (String) getValue.invoke( obj, NO_ARGS );

                    if ( value != null )
                    {
                        props.put( key + ".value", value );
                    }

                    Method getName = type.getMethod( "getName", NO_PARAMS );

                    Method getChildren = type.getMethod( "getChildren", NO_PARAMS );
                    Object[] children = (Object[]) getChildren.invoke( obj, NO_ARGS );

                    props.put( key + ".children", Integer.toString( children.length ) );

                    Map indices = new HashMap();
                    for ( Object child : children )
                    {
                        String name = (String) getName.invoke( child, NO_ARGS );

                        Integer index = (Integer) indices.get( name );
                        if ( index == null )
                        {
                            index = 0;
                        }

                        store( props, key + ".children." + name + "." + index, child, visited );

                        indices.put( name, index + 1 );
                    }
                }
                catch ( Exception e )
                {
                    // can't happen
                }
            }
            else
            {
                Class type = obj.getClass();
                Method[] methods = type.getMethods();
                for ( Method method : methods )
                {
                    if ( Modifier.isStatic( method.getModifiers() ) || method.getParameterTypes().length > 0
                        || !method.getName().matches( "(get|is)\\p{Lu}.*" ) || method.getName().endsWith( "AsMap" )
                        || Class.class.isAssignableFrom( method.getReturnType() ) || Object.class.equals(
                        method.getReturnType() ) )
                    {
                        continue;
                    }

                    try
                    {
                        Object value = method.invoke( obj, NO_ARGS );
                        store( props, key + "." + getPropertyName( method.getName() ), value, visited );
                    }
                    catch ( Exception e )
                    {
                        // just ignore
                    }
                }
            }
            visited.remove( obj );
        }
    }

    /**
     * Derives the bean property name from the specified method for its getter.
     *
     * @param methodName The method name of the property's getter, must not be <code>null</code>.
     * @return The property name, never <code>null</code>.
     */
    static String getPropertyName( String methodName )
    {
        String propertyName = methodName;
        if ( methodName.startsWith( "get" ) && methodName.length() > 3 )
        {
            propertyName = Character.toLowerCase( methodName.charAt( 3 ) ) + methodName.substring( 4 );
        }
        else if ( methodName.startsWith( "is" ) && methodName.length() > 2 )
        {
            propertyName = Character.toLowerCase( methodName.charAt( 2 ) ) + methodName.substring( 3 );
        }
        return propertyName;
    }

    /**
     * Writes the specified properties to the given file.
     *
     * @param props The properties to write, must not be <code>null</code>.
     * @param file  The output file for the properties, must not be <code>null</code>.
     * @throws IOException If the properties could not be written to the file.
     */
    public static void write( Properties props, File file )
        throws IOException
    {
        OutputStream out = null;
        try
        {
            file.getParentFile().mkdirs();
            out = new FileOutputStream( file );
            props.store( out, "MAVEN-CORE-IT-LOG" );
        }
        finally
        {
            if ( out != null )
            {
                try
                {
                    out.close();
                }
                catch ( IOException e )
                {
                    // just ignore
                }
            }
        }
    }

}
