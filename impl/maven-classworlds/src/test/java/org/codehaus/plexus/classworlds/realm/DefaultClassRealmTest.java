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
package org.codehaus.plexus.classworlds.realm;

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
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.codehaus.plexus.classworlds.AbstractClassWorldsTestCase;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultClassRealmTest extends AbstractClassWorldsTestCase {
    // ----------------------------------------------------------------------
    // Class testing
    // ----------------------------------------------------------------------

    @Test
    void testLoadClassFromRealm() throws Exception {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(getJarUrl("component0-1.0.jar"));

        loadClass(mainRealm, "org.codehaus.plexus.Component0");
    }

    @Test
    void testLoadClassFromChildRealmWhereClassIsLocatedInParentRealm() throws Exception {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(getJarUrl("component0-1.0.jar"));

        ClassRealm childRealm = mainRealm.createChildRealm("child");

        loadClass(childRealm, "org.codehaus.plexus.Component0");
    }

    @Test
    void testLoadClassFromChildRealmWhereClassIsLocatedInGrantParentRealm() throws Exception {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(getJarUrl("component0-1.0.jar"));

        ClassRealm childRealm = mainRealm.createChildRealm("child");

        ClassRealm grandchildRealm = childRealm.createChildRealm("grandchild");

        loadClass(grandchildRealm, "org.codehaus.plexus.Component0");
    }

    @Test
    void testLoadClassFromChildRealmWhereClassIsLocatedInBothChildRealmAndParentRealm() throws Exception {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "parent", null);

        mainRealm.addURL(getJarUrl("component5-1.0.jar"));

        ClassRealm childRealm = mainRealm.createChildRealm("child");

        childRealm.addURL(getJarUrl("component5-2.0.jar"));

        Class<?> cls = loadClass(childRealm, "test.Component5");

        assertSame(childRealm, cls.getClassLoader());
        assertEquals(1, cls.getMethods().length);
        assertEquals("printNew", cls.getMethods()[0].getName());
    }

    @Test
    void testLoadNonExistentClass() {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(getJarUrl("component0-1.0.jar"));

        try {
            mainRealm.loadClass("org.foo.bar.NonExistentClass");

            fail("A ClassNotFoundException should have been thrown!");
        } catch (ClassNotFoundException e) {
            // expected
        }
    }

    @Test
    void testImport() throws Exception {
        ClassWorld world = new ClassWorld();

        ClassRealm r0 = world.newRealm("r0");

        ClassRealm r1 = world.newRealm("r1");

        r0.addURL(getJarUrl("component0-1.0.jar"));

        r1.importFrom("r0", "org.codehaus.plexus");

        loadClass(r1, "org.codehaus.plexus.Component0");
    }

    @Test
    void testParentImport() throws Exception {
        ClassWorld world = new ClassWorld();

        ClassRealm parent = world.newRealm("parent");

        ClassRealm child = world.newRealm("child");

        parent.addURL(getJarUrl("component0-1.0.jar"));

        child.setParentRealm(parent);

        Class<?> type = loadClass(child, "org.codehaus.plexus.Component0");

        child.importFromParent("non-existing");

        assertSame(null, loadClassOrNull(child, "org.codehaus.plexus.Component0"));

        child.importFromParent("org.codehaus.plexus");

        assertSame(type, loadClass(child, "org.codehaus.plexus.Component0"));
    }

    @Test
    void testLoadClassFromBaseClassLoaderBeforeSelf() throws Exception {
        ClassWorld world = new ClassWorld();

        ClassRealm base = world.newRealm("base");

        base.addURL(getJarUrl("a.jar"));

        ClassRealm child = world.newRealm("child", base);

        child.addURL(getJarUrl("a.jar"));

        Class<?> baseClass = loadClass(base, "a.A");
        Class<?> childClass = loadClass(child, "a.A");

        assertSame(base, baseClass.getClassLoader());
        assertSame(base, childClass.getClassLoader());
        assertSame(baseClass, childClass);
    }

    @Test
    void testLoadClassFromRealmWithCircularClassReferences() throws Exception {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(getJarUrl("circular-0.1.jar"));

        /*
         * This was reported to fail with a ClassCircularityError in IBM JDK 1.5.0-SR2, 1.5.0-SR7 and 1.6.0-SR2. It
         * works in IBM JDK 1.5.0-SR10 and 1.6.0-SR6.
         */
        loadClass(mainRealm, "A$C");
    }

    // ----------------------------------------------------------------------
    // Resource testing
    // ----------------------------------------------------------------------

    @Test
    void testResource() throws Exception {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(getJarUrl("component0-1.0.jar"));

        getResource(mainRealm, "META-INF/plexus/components.xml");
    }

    @Test
    void testMalformedResource() throws Exception {
        URL jarUrl = getJarUrl("component0-1.0.jar");

        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(jarUrl);

        ClassLoader officialClassLoader = new URLClassLoader(new URL[] {jarUrl});

        String resource = "META-INF/plexus/components.xml";

        assertNotNull(mainRealm.getResource(resource));
        assertNotNull(officialClassLoader.getResource(resource));

        /*
         * NOTE: Resource names with a leading slash are invalid when passed to a class loader and must not be found!
         * One can use a leading slash in Class.getResource() but not in ClassLoader.getResource().
         */

        assertSame(null, mainRealm.getResource("/" + resource));
        assertSame(null, officialClassLoader.getResource("/" + resource));
    }

    @Test
    void testFindResourceOnlyScansSelf() throws Exception {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(getJarUrl("a.jar"));

        ClassRealm childRealm = mainRealm.createChildRealm("child");

        childRealm.addURL(getJarUrl("b.jar"));

        assertNotNull(childRealm.getResource("a.properties"));
        assertNotNull(childRealm.getResource("b.properties"));

        assertNull(childRealm.findResource("a.properties"));

        assertNotNull(childRealm.findResource("b.properties"));
    }

    @Test
    void testFindResourcesOnlyScansSelf() throws Exception {
        ClassRealm mainRealm = new ClassRealm(new ClassWorld(), "main", null);

        mainRealm.addURL(getJarUrl("a.jar"));

        ClassRealm childRealm = mainRealm.createChildRealm("child");

        childRealm.addURL(getJarUrl("b.jar"));

        assertTrue(childRealm.getResources("a.properties").hasMoreElements());
        assertTrue(childRealm.getResources("b.properties").hasMoreElements());

        assertFalse(childRealm.findResources("a.properties").hasMoreElements());

        assertTrue(childRealm.findResources("b.properties").hasMoreElements());
    }

    /** Should never deadlock. Ever */
    @Test
    void testParallelDeadlockClassRealm() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            doOneDeadlockAttempt();
        }
    }

    private void doOneDeadlockAttempt() throws InterruptedException {
        // Deadlock sample graciously ripped from http://docs.oracle.com/javase/7/docs/technotes/guides/lang/cl-mt.html
        final ClassRealm cl1 = new ClassRealm(new ClassWorld(), "cl1", null);
        final ClassRealm cl2 = new ClassRealm(new ClassWorld(), "cl2", cl1);
        cl1.setParentRealm(cl2);
        cl1.addURL(getJarUrl("deadlock.jar"));
        cl2.addURL(getJarUrl("deadlock.jar"));
        final CountDownLatch latch = new CountDownLatch(1);

        Runnable r1 = () -> {
            try {
                latch.await();
                cl1.loadClass("deadlock.A");
            } catch (ClassNotFoundException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        Runnable r2 = () -> {
            try {
                latch.await();
                cl1.loadClass("deadlock.C");
            } catch (ClassNotFoundException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        Thread thread = new Thread(r1);
        thread.start();
        Thread thread1 = new Thread(r2);
        thread1.start();
        latch.countDown();
        thread.join();
        thread1.join();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Class<?> loadClassOrNull(ClassRealm realm, String name) {
        try {
            return loadClass(realm, name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Class<?> loadClass(ClassRealm realm, String name) throws ClassNotFoundException {
        Class<?> cls = realm.loadClass(name);

        /*
         * NOTE: Load the class both directly from the realm and indirectly from an (ordinary) child class loader which
         * uses the specified class realm for parent delegation. The child class loader itself has no additional class
         * path entries but relies entirely on the provided class realm. Hence, the created child class loader should in
         * theory be able to load exactly the same classes/resources as the underlying class realm. In practice, it will
         * test that class realms properly integrate into the standard Java class loader hierarchy.
         */
        ClassLoader childLoader = new URLClassLoader(new URL[0], realm);
        assertEquals(cls, childLoader.loadClass(name));

        return cls;
    }

    private void getResource(ClassRealm realm, String name) throws Exception {
        ClassLoader childLoader = new URLClassLoader(new URL[0], realm);
        assertNotNull(realm.getResource(name));
        assertEquals(realm.getResource(name), childLoader.getResource(name));
        assertEquals(Collections.list(realm.getResources(name)), Collections.list(childLoader.getResources(name)));
    }
}
