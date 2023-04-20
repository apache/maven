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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Benjamin Bentmann
 *
 */
public class ExpressionUtilTest
{

    @Test
    public void testEvaluate()
    {
        Object array = new String[]{ "one", "two", "three" };
        Object list = Arrays.asList( "0", "-1", "-2" );
        Object map = Collections.singletonMap( "some.key", "value" );
        Object bean = new BeanTwo();

        Map<String, Object> contexts = new HashMap<>();
        contexts.put( "array", array );
        contexts.put( "list", list );
        contexts.put( "map", map );
        contexts.put( "bean", bean );

        assertSame( array, evaluate( "array", contexts ) );
        assertSame( array, ExpressionUtil.evaluate( "array/", contexts ).get( "array" ) );
        assertSame( list, evaluate( "list", contexts ) );
        assertSame( map, evaluate( "map", contexts ) );
        assertSame( bean, evaluate( "bean", contexts ) );
        assertNull( evaluate( "no-root", contexts ) );

        assertEquals( 3, evaluate( "array/length", contexts ) );
        assertEquals( "three", evaluate( "array/2", contexts ) );
        assertEquals( 5, evaluate( "array/2/length", contexts ) );
        assertNull( evaluate( "array/invalid", contexts ) );
        assertNull( evaluate( "array/-1", contexts ) );
        assertNull( evaluate( "array/999", contexts ) );
        assertEquals( 3, ExpressionUtil.evaluate( "array/*", contexts ).size() );
        assertEquals( "one", ExpressionUtil.evaluate( "array/*", contexts ).get( "array/0" ) );
        assertEquals( "two", ExpressionUtil.evaluate( "array/*", contexts ).get( "array/1" ) );
        assertEquals( "three", ExpressionUtil.evaluate( "array/*", contexts ).get( "array/2" ) );

        assertEquals( 3, evaluate( "list/size", contexts ) );
        assertEquals( "-2", evaluate( "list/2", contexts ) );
        assertNull( evaluate( "list/invalid", contexts ) );
        assertNull( evaluate( "list/-1", contexts ) );
        assertNull( evaluate( "list/999", contexts ) );
        assertEquals( 3, ExpressionUtil.evaluate( "list/*", contexts ).size() );
        assertEquals( "0", ExpressionUtil.evaluate( "list/*", contexts ).get( "list/0" ) );
        assertEquals( "-1", ExpressionUtil.evaluate( "list/*", contexts ).get( "list/1" ) );
        assertEquals( "-2", ExpressionUtil.evaluate( "list/*", contexts ).get( "list/2" ) );

        assertEquals( 1, evaluate( "map/size", contexts ) );
        assertEquals( "value", evaluate( "map/some.key", contexts ) );
        assertNull( evaluate( "map/invalid", contexts ) );

        assertEquals( "field", evaluate( "bean/field", contexts ) );
        assertNull( evaluate( "bean/invalid", contexts ) );
        assertEquals( "prop", evaluate( "bean/bean/prop", contexts ) );
        assertEquals( "flag", evaluate( "bean/bean/flag", contexts ) );
        assertEquals( "arg", evaluate( "bean/bean/arg", contexts ) );
    }

    private static Object evaluate( String expression, Object context )
    {
        return ExpressionUtil.evaluate( expression, context ).get( expression );
    }

    @Test
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
