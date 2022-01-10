package org.apache.maven.session.scope.internal;

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

import javax.inject.Provider;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.model.locator.ModelLocator;
import org.apache.maven.plugin.DefaultPluginRealmCache;
import org.apache.maven.plugin.PluginRealmCache;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class SessionScopeTest {

    @Test
    public void testScope() throws Exception
    {
        SessionScope scope = new SessionScope();

        try
        {
            scope.seed( ModelLocator.class, new DefaultModelLocator() );
            fail( "Expected a " + OutOfScopeException.class.getName() + " exception to be thrown" );
        }
        catch ( OutOfScopeException e )
        {
            // expected
        }

        Provider<ModelLocator> pml = scope.scope( Key.get( ModelLocator.class), new DefaultModelLocatorProvider() );
        assertNotNull( pml );
        try
        {
            pml.get();
            fail( "Expected a " + OutOfScopeException.class.getName() + " exception to be thrown" );
        }
        catch ( OutOfScopeException e )
        {
            // expected
        }

        Provider<PluginRealmCache> pmst = scope.scope( Key.get( PluginRealmCache.class ), new DefaultPluginRealmCacheProvider() );
        assertNotNull( pmst );

        scope.enter();

        final DefaultModelLocator dml1 = new DefaultModelLocator();
        scope.seed( ModelLocator.class, dml1 );

        assertSame( dml1, pml.get() );

        PluginRealmCache mst1 = pmst.get();
        assertSame( mst1, pmst.get() );
        Provider<PluginRealmCache> pmst1 = scope.scope( Key.get( PluginRealmCache.class ), new DefaultPluginRealmCacheProvider() );
        assertNotNull( pmst1 );
        assertSame( mst1, pmst1.get() );

        scope.enter();

        pmst1 = scope.scope( Key.get( PluginRealmCache.class ), new DefaultPluginRealmCacheProvider() );
        assertNotNull( pmst1 );
        assertNotSame( mst1, pmst1.get() );

        scope.exit();

        assertSame( mst1, pmst.get() );

        scope.exit();

        try
        {
            pmst.get();
            fail( "Expected a " + OutOfScopeException.class.getName() + " exception to be thrown" );
        }
        catch ( OutOfScopeException e )
        {
            // expected
        }
        try
        {
            scope.seed( ModelLocator.class, new DefaultModelLocator() );
            fail( "Expected a " + OutOfScopeException.class.getName() + " exception to be thrown" );
        }
        catch ( OutOfScopeException e )
        {
            // expected
        }
    }

    private static class DefaultPluginRealmCacheProvider implements com.google.inject.Provider<PluginRealmCache>
    {
        @Override
        public PluginRealmCache get()
        {
            return new DefaultPluginRealmCache();
        }
    }

    private static class DefaultModelLocatorProvider implements com.google.inject.Provider<ModelLocator>
    {
        @Override
        public ModelLocator get()
        {
            return new DefaultModelLocator();
        }
    }

}
