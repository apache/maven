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
package org.apache.maven.buildcache.hash;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * ReflectionUtils
 */
class ReflectionUtils
{

    static Method getMethod( String className, String methodName, Class<?>... parameterTypes )
    {
        try
        {
            final Method method = Class.forName( className ).getMethod( methodName, parameterTypes );
            method.setAccessible( true );
            return method;
        }
        catch ( Exception ignore )
        {
            return null;
        }
    }

    static Object getField( String className, String fieldName )
    {
        try
        {
            final Field field = Class.forName( className ).getDeclaredField( fieldName );
            field.setAccessible( true );
            return field.get( null );
        }
        catch ( Exception ignore )
        {
            return null;
        }
    }

    private ReflectionUtils()
    {
    }
}
