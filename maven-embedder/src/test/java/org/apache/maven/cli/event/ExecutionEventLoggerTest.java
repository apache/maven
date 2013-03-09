package org.apache.maven.cli.event;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;

public class ExecutionEventLoggerTest
    extends TestCase
{

    private static String getFormattedTime( long time )
        throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException
    {
        Method method = ExecutionEventLogger.class.getDeclaredMethod( "getFormattedTime", long.class );
        boolean accessible = method.isAccessible();
        try
        {
            method.setAccessible( true );
            return (String) method.invoke( null, time );
        }
        finally
        {
            method.setAccessible( accessible );
        }
    }

    public void testGetFormattedTime()
        throws Exception
    {
        assertEquals( "0.001s", getFormattedTime( 1 ) );
        assertEquals( "0.999s", getFormattedTime( 1000 - 1 ) );
        assertEquals( "1.000s", getFormattedTime( 1000 ) );
        assertEquals( "59.999s", getFormattedTime( 60 * 1000 - 1 ) );
        assertEquals( "1:00.000s", getFormattedTime( 60 * 1000 ) );
        assertEquals( "59:59.999s", getFormattedTime( 60 * 60 * 1000 - 1 ) );
        assertEquals( "1:00:00.000s", getFormattedTime( 60 * 60 * 1000 ) );
        assertEquals( "23:59:59.999s", getFormattedTime( 24 * 60 * 60 * 1000 - 1 ) );
        assertEquals( "24:00:00.000s", getFormattedTime( 24 * 60 * 60 * 1000 ) );
    }
}
