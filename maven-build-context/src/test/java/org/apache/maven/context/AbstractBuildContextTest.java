package org.apache.maven.context;

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

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBuildContextTest
    extends TestCase
{
    
    protected abstract BuildContext newContext();

    public void testPutAndGet_ShouldStoreAndRetrieveKeyValuePairOfStrings()
    {
        BuildContext ctx = newContext();
        
        String key = "key";
        String value = "value";
        
        ctx.put( key, value );
        
        assertEquals( value, ctx.get( key ) );
    }
    
    public void testPutAndGet_ShouldStoreAndRetrieveStringKeyWithMapValue()
    {
        BuildContext ctx = newContext();
        
        String key = "key";
        Map value = new HashMap();
        
        String key2 = "key2";
        String value2 = "value";
        
        value.put( key2, value2 );
        
        ctx.put( key, value );
        
        assertSame( value, ctx.get( key ) );
        
        assertEquals( value2, ((Map) ctx.get( key )).get( key2 ) );
    }
    
    public void testPutDeleteAndGet_ShouldStoreKeyValuePairDeleteThemAndRetrieveNull()
    {
        BuildContext ctx = newContext();
        
        String key = "key";
        String value = "value";
        
        ctx.put( key, value );
        
        assertEquals( value, ctx.get( key ) );
        
        ctx.delete( key );
        
        assertNull( ctx.get( key ) );
    }
    
}
