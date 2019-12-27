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

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Benjamin Bentmann
 *
 */
public class ExpressionUtilTest
    extends TestCase
{

    public void testEvaluate()
    {
        Object array = new String[]{ "one", "two", "three" };
        Object list = Arrays.asList( new String[]{ "0", "-1", "-2" } );
        Object map = Collections.singletonMap( "some.key", "value" );
        Object bean = new BeanTwo();

        Map contexts = new HashMap();
        contexts.put( "array", array );
        contexts.put( "list", list );
        contexts.put( "map", map );
        contexts.put( "bean", bean );

        assertSame( array, ExpressionUtil.evaluate( "array", contexts ) );
        assertSame( array, ExpressionUtil.evaluate( "array/", contexts ) );
        assertSame( list, ExpressionUtil.evaluate( "list", contexts ) );
        assertSame( map, ExpressionUtil.evaluate( "map", contexts ) );
        assertSame( bean, ExpressionUtil.evaluate( "bean", contexts ) );
        assertNull( ExpressionUtil.evaluate( "no-root", contexts ) );

        assertEquals( 3, ExpressionUtil.evaluate( "array/length", contexts ) );
        assertEquals( "three", ExpressionUtil.evaluate( "array/2", contexts ) );
        assertEquals( 5, ExpressionUtil.evaluate( "array/2/length", contexts ) );
        assertNull( ExpressionUtil.evaluate( "array/invalid", contexts ) );
        assertNull( ExpressionUtil.evaluate( "array/-1", contexts ) );
        assertNull( ExpressionUtil.evaluate( "array/999", contexts ) );

        assertEquals( 3, ExpressionUtil.evaluate( "list/size", contexts ) );
        assertEquals( "-2", ExpressionUtil.evaluate( "list/2", contexts ) );
        assertNull( ExpressionUtil.evaluate( "list/invalid", contexts ) );
        assertNull( ExpressionUtil.evaluate( "list/-1", contexts ) );
        assertNull( ExpressionUtil.evaluate( "list/999", contexts ) );

        assertEquals( 1, ExpressionUtil.evaluate( "map/size", contexts ) );
        assertEquals( "value", ExpressionUtil.evaluate( "map/some.key", contexts ) );
        assertNull( ExpressionUtil.evaluate( "map/invalid", contexts ) );

        assertEquals( "field", ExpressionUtil.evaluate( "bean/field", contexts ) );
        assertNull( ExpressionUtil.evaluate( "bean/invalid", contexts ) );
        assertEquals( "prop", ExpressionUtil.evaluate( "bean/bean/prop", contexts ) );
        assertEquals( "flag", ExpressionUtil.evaluate( "bean/bean/flag", contexts ) );
        assertEquals( "arg", ExpressionUtil.evaluate( "bean/bean/arg", contexts ) );
    }

    public void testGetProperty()
    {
        BeanOne bean1 = new BeanOne();
        BeanTwo bean2 = new BeanTwo();

        assertEquals( bean1.isFlag(), ExpressionUtil.getProperty( bean1, "flag" ) );
        assertEquals( bean1.getProp(), ExpressionUtil.getProperty( bean1, "prop" ) );
        assertEquals( bean1.get( "get" ), ExpressionUtil.getProperty( bean1, "get" ) );

        assertNull( ExpressionUtil.getProperty( bean2, "invalid" ) );
        assertEquals( bean2.field, ExpressionUtil.getProperty( bean2, "field" ) );
        assertSame( bean2.bean, ExpressionUtil.getProperty( bean2, "bean" ) );

        assertEquals( 0, ExpressionUtil.getProperty( new String[0], "length" ) );
    }

    public static class BeanOne
    {
        public String isFlag()
        {
            return "flag";
        }

        public String getProp()
        {
            return "prop";
        }

        public String get( String arg )
        {
            return arg;
        }
    }

    public static class BeanTwo
    {
        public String field = "field";

        public BeanOne bean = new BeanOne();

    }
}
