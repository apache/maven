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
package org.apache.maven.api.classworlds;

import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test to demonstrate the Maven 4 ClassWorlds API.
 */
class ApiTest {

    @Test
    void testApiUsage() throws Exception {
        // Create a ClassWorld using the implementation
        ClassWorld world = new org.codehaus.plexus.classworlds.ClassWorld();

        // Test basic API methods
        assertNotNull(world);

        // Create a realm
        ClassRealm realm = world.newRealm("test-realm");
        assertNotNull(realm);
        assertEquals("test-realm", realm.getId());
        assertSame(world, realm.getWorld());

        // Test getClassLoader() method
        ClassLoader classLoader = realm.getClassLoader();
        assertNotNull(classLoader);
        assertSame(realm, classLoader); // Should return the realm itself

        // Test URL operations
        URL url = new URL("file:///tmp/test.jar");
        realm.addURL(url);
        URL[] urls = realm.getURLs();
        assertTrue(urls.length > 0);

        // Test import operations
        ClassRealm importRealm = world.newRealm("import-realm");
        realm.importFrom(importRealm.getId(), "org.example");

        // Test parent operations
        realm.setParentClassLoader(ClassLoader.getSystemClassLoader());
        assertNotNull(realm.getParentClassLoader());

        // Test strategy
        Strategy strategy = realm.getStrategy();
        assertNotNull(strategy);
        assertSame(realm, strategy.getRealm());

        // Test listener
        TestListener listener = new TestListener();
        world.addListener(listener);

        ClassRealm newRealm = world.newRealm("listener-test");
        assertTrue(listener.realmCreated);
        assertEquals(newRealm, listener.createdRealm);

        world.disposeRealm("listener-test");
        assertTrue(listener.realmDisposed);
        assertEquals(newRealm, listener.disposedRealm);

        // Test exception handling
        try {
            world.newRealm("test-realm"); // Duplicate
            fail("Should have thrown DuplicateRealmException");
        } catch (DuplicateRealmException e) {
            assertEquals("test-realm", e.getId());
            assertSame(world, e.getWorld());
        }

        try {
            world.getRealm("non-existent");
            fail("Should have thrown NoSuchRealmException");
        } catch (NoSuchRealmException e) {
            assertEquals("non-existent", e.getId());
            assertSame(world, e.getWorld());
        }

        // Test null-safe operations
        assertNull(world.getClassRealm("non-existent"));

        world.close();
    }

    private static class TestListener implements ClassWorldListener {
        boolean realmCreated = false;
        boolean realmDisposed = false;
        ClassRealm createdRealm;
        ClassRealm disposedRealm;

        @Override
        public void realmCreated(ClassRealm realm) {
            this.realmCreated = true;
            this.createdRealm = realm;
        }

        @Override
        public void realmDisposed(ClassRealm realm) {
            this.realmDisposed = true;
            this.disposedRealm = realm;
        }
    }
}
