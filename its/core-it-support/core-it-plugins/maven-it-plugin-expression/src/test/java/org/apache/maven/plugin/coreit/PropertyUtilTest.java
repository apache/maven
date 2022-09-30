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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Benjamin Bentmann
 *
 */
public class PropertyUtilTest
{

    @Test
    public void testStoreScalar()
    {
        Properties props = new Properties();
        PropertyUtil.store( props, "null", null );
        PropertyUtil.store( props, "string", "str" );
        PropertyUtil.store( props, "boolean", Boolean.TRUE );
        PropertyUtil.store( props, "int", 7 );
        PropertyUtil.store( props, "file", new File( "pom.xml" ) );

        assertNull( props.get( "null" ) );
        assertEquals( "str", props.get( "string" ) );
        assertEquals( "true", props.get( "boolean" ) );
        assertEquals( "7", props.get( "int" ) );
        assertEquals( "pom.xml", props.get( "file" ) );
        assertEquals( 4, props.size() );
    }

    @Test
    public void testStoreArray()
    {
        Properties props = new Properties();
        PropertyUtil.store( props, "arr", new String[]{ "one", "two" } );

        assertEquals( "2", props.get( "arr" ) );
        assertEquals( "one", props.get( "arr.0" ) );
        assertEquals( "two", props.get( "arr.1" ) );
        assertEquals( 3, props.size() );
    }

    @Test
    public void testStoreList()
    {
        Properties props = new Properties();
        PropertyUtil.store( props, "arr", Arrays.asList( new String[]{ "one", "two" } ) );

        assertEquals( "2", props.get( "arr" ) );
        assertEquals( "one", props.get( "arr.0" ) );
        assertEquals( "two", props.get( "arr.1" ) );
        assertEquals( 3, props.size() );
    }

    @Test
    public void testStoreMap()
    {
        Properties props = new Properties();
        PropertyUtil.store( props, "map", Collections.singletonMap( "key", "value" ) );

        assertEquals( "1", props.get( "map" ) );
        assertEquals( "value", props.get( "map.key" ) );
        assertEquals( 2, props.size() );
    }

    @Test
    public void testStoreBean()
    {
        Properties props = new Properties();
        PropertyUtil.store( props, "bean", new Bean() );

        assertEquals( "name", props.get( "bean.name" ) );
        assertEquals( "false", props.get( "bean.enabled" ) );
        assertEquals( 2, props.size() );
    }

    @Test
    public void testStoreCycle()
    {
        Object[] arr = { null };
        arr[0] = Collections.singleton( Collections.singletonMap( "key", arr ) );

        Properties props = new Properties();
        PropertyUtil.store( props, "cycle", arr );
        assertTrue( true, "Should not die because of stack overflow" );
    }

    @Test
    public void testGetPropertyName()
    {
        assertEquals( "name", PropertyUtil.getPropertyName( "getName" ) );
        assertEquals( "enabled", PropertyUtil.getPropertyName( "isEnabled" ) );
    }

    public static class Bean
    {
        public String getName()
        {
            return "name";
        }

        public boolean isEnabled()
        {
            return false;
        }

        public String toString()
        {
            return "excluded";
        }

        public Object getUntypedReturnValue()
        {
            return "excluded";
        }

        public Bean getCyclicReference()
        {
            return this;
        }
    }

}
