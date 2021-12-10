/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.buildcache;

/**
 * This utility class is used to work around the classloading problem described
 * in https://issues.apache.org/jira/browse/MNG-7160.
 * The simple workaround described in MNG-7160 is not possible because this
 * extension uses both the XPP3 classes and other utility classes. We thus rely on
 * reflection for all access to plugin configuration.
 */
public class Xpp3DomUtils
{

    public static Object[] getChildren( Object node )
    {
        try
        {
            return ( Object[] ) node.getClass().getMethod( "getChildren" ).invoke( node );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Error invoking Xpp3Dom.getChildren", e );
        }
    }

    public static String getName( Object node )
    {
        try
        {
            return ( String ) node.getClass().getMethod( "getName" ).invoke( node );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Error invoking Xpp3Dom.getName", e );
        }
    }

    public static String getValue( Object node )
    {
        try
        {
            return ( String ) node.getClass().getMethod( "getValue" ).invoke( node );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Error invoking Xpp3Dom.getValue", e );
        }
    }

    public static void removeChild( Object node, int index )
    {
        try
        {
            node.getClass().getMethod( "removeChild", int.class ).invoke( node, index );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Error invoking Xpp3Dom.removeChild", e );
        }
    }

    public static String getAttribute( Object node, String attribute )
    {
        try
        {
            return ( String ) node.getClass().getMethod( "getAttribute", String.class ).invoke( node, attribute );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Error invoking Xpp3Dom.getAttribute", e );
        }
    }
}
