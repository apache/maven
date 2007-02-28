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

import org.codehaus.plexus.PlexusTestCase;

public abstract class AbstractBuildContextManagerTest
    extends PlexusTestCase
{
    private BuildContextManager mgr;
    
    protected abstract String getRoleHintBeforeSetUp();
    
    protected abstract BuildContext createBuildContext();
    
    protected BuildContextManager getBuildContextManager()
    {
        return mgr;
    }
    
    public void setUp() throws Exception
    {
        super.setUp();
        
        mgr = (BuildContextManager) lookup( BuildContextManager.ROLE, getRoleHintBeforeSetUp() );
    }
    
    public void testNewUnstoredInstance_SuccessiveCallsShouldReturnDistinctContextInstances()
    {
        BuildContext context = createBuildContext();
        BuildContext context2 = createBuildContext();
        
        assertNotNull( context );
        assertNotNull( context2 );
        assertNotSame( context, context2 );
    }
    
    public void testStoreAndRead_ShouldRetrieveStoredValueAfterRead()
    {
        BuildContext ctx = createBuildContext();
        
        String key = "key";
        String value = "value";
        
        ctx.put( key, value );
        
        mgr.storeBuildContext( ctx );
        
        BuildContext ctx2 = mgr.readBuildContext( false );
        
        assertNotNull( ctx2 );
        assertEquals( value, ctx2.get( key ) );
    }

    public void testStoreAndClear_ShouldNotRetrieveStoredValueAfterClear()
    {
        BuildContext ctx = createBuildContext();
        
        String key = "key";
        String value = "value";
        
        ctx.put( key, value );
        
        mgr.storeBuildContext( ctx );
        
        // verify that we can get the value back out.
        BuildContext ctx2 = mgr.readBuildContext( false );
        
        assertNotNull( ctx2 );
        assertEquals( value, ctx2.get( key ) );
        
        mgr.clearBuildContext();
        
        BuildContext ctx3 = mgr.readBuildContext( false );
        
        assertNull( ctx3 );
    }

}
