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
package org.codehaus.plexus.classworlds;

/*
 * Copyright 2001-2006 Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ClassWorldTest extends AbstractClassWorldsTestCase {
    private ClassWorld world;

    @BeforeEach
    public void setUp() {
        this.world = new ClassWorld();
    }

    @AfterEach
    public void tearDown() {
        this.world = null;
    }

    @Test
    void testEmpty() {
        assertTrue(this.world.getRealms().isEmpty());
    }

    @Test
    void testNewRealm() throws Exception {
        ClassRealm realm = this.world.newRealm("foo");

        assertNotNull(realm);
    }

    @Test
    void testGetRealm() throws Exception {
        ClassRealm realm = this.world.newRealm("foo");

        assertSame(realm, this.world.getRealm("foo"));
    }

    @Test
    void testNewRealmDuplicate() {
        try {
            this.world.newRealm("foo");
            this.world.newRealm("foo");

            fail("throw DuplicateRealmException");
        } catch (DuplicateRealmException e) {
            // expected and correct

            assertSame(this.world, e.getWorld());

            assertEquals("foo", e.getId());
        }
    }

    @Test
    void testGetRealmNoSuch() {
        try {
            this.world.getRealm("foo");
            fail("throw NoSuchRealmException");
        } catch (NoSuchRealmException e) {
            // expected and correct

            assertSame(this.world, e.getWorld());

            assertEquals("foo", e.getId());
        }
    }

    @Test
    void testGetRealms() throws Exception {
        assertTrue(this.world.getRealms().isEmpty());

        ClassRealm foo = this.world.newRealm("foo");

        assertEquals(1, this.world.getRealms().size());

        assertTrue(this.world.getRealms().contains(foo));

        ClassRealm bar = this.world.newRealm("bar");

        assertEquals(2, this.world.getRealms().size());

        assertTrue(this.world.getRealms().contains(bar));
    }

    @Test
    void testPLX334() throws Exception {
        ClassLoader loader = new URLClassLoader(new URL[] {getJarUrl("component1-1.0.jar")});
        world.newRealm("netbeans", loader);
        ClassRealm plexus = world.newRealm("plexus");
        plexus.importFrom("netbeans", "META-INF/plexus");
        plexus.importFrom("netbeans", "org.codehaus.plexus");
        Enumeration<URL> e = plexus.getResources("META-INF/plexus/components.xml");
        assertNotNull(e);
        int resourceCount = 0;
        for (Enumeration<URL> resources = e; resources.hasMoreElements(); ) {
            URL obj = resources.nextElement();
            assertTrue(obj.getPath().contains("/component1-1.0.jar!/META-INF/plexus/components.xml"));
            resourceCount++;
        }
        //        assertEquals( 2, resourceCount );
        // for some reason surefire-plugin 2.3 returned 2 items there:
        //        for example:
        // jar:file:/home/mkleint/.m2/repository/org/codehaus/plexus/plexus-archiver/1.0-alpha-7/plexus-archiver-1.0-alpha-7.jar!/META-INF/plexus/components.xml
        // jar:file:/home/mkleint/src/plexus-trunk/plexus-classworlds/src/test-jars/component1-1.0.jar!/META-INF/plexus/components.xml
        //    However only 1 is correct, which is actually returned by the 2.4 surefire-plugin

        assertEquals(1, resourceCount);
        Class<?> c = plexus.loadClass("org.codehaus.plexus.Component1");
        assertNotNull(c);
    }
}
