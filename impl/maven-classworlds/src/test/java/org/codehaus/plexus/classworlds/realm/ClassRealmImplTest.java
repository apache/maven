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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.classworlds.AbstractClassWorldsTestCase;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

class ClassRealmImplTest extends AbstractClassWorldsTestCase {
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
    void testNewRealm() throws Exception {
        ClassRealm realm = this.world.newRealm("foo");

        assertNotNull(realm);

        assertSame(this.world, realm.getWorld());

        assertEquals("foo", realm.getId());
    }

    @Test
    void testLocateSourceRealmNoImports() {
        ClassRealm realm = new ClassRealm(this.world, "foo", null);

        assertSame(null, realm.getImportClassLoader("com.werken.Stuff"));
    }

    @Test
    void testLocateSourceRealmSimpleImport() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        ClassRealm werkflowRealm = this.world.newRealm("werkflow");

        mainRealm.importFrom("werkflow", "com.werken.werkflow");

        assertSame(werkflowRealm, mainRealm.getImportClassLoader("com.werken.werkflow.WerkflowEngine"));

        assertSame(werkflowRealm, mainRealm.getImportClassLoader("com/werken/werkflow/some.properties"));

        assertSame(werkflowRealm, mainRealm.getImportClassLoader("com.werken.werkflow.process.ProcessManager"));

        assertSame(null, mainRealm.getImportClassLoader("com.werken.blissed.Process"));

        assertSame(null, mainRealm.getImportClassLoader("java.lang.Object"));

        assertSame(null, mainRealm.getImportClassLoader("NoviceProgrammerClass"));
    }

    @Test
    void testLocateSourceRealmMultipleImport() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        ClassRealm werkflowRealm = this.world.newRealm("werkflow");

        ClassRealm blissedRealm = this.world.newRealm("blissed");

        mainRealm.importFrom("werkflow", "com.werken.werkflow");

        mainRealm.importFrom("blissed", "com.werken.blissed");

        assertSame(werkflowRealm, mainRealm.getImportClassLoader("com.werken.werkflow.WerkflowEngine"));

        assertSame(werkflowRealm, mainRealm.getImportClassLoader("com.werken.werkflow.process.ProcessManager"));

        assertSame(blissedRealm, mainRealm.getImportClassLoader("com.werken.blissed.Process"));

        assertSame(blissedRealm, mainRealm.getImportClassLoader("com.werken.blissed.guard.BooleanGuard"));

        assertSame(null, mainRealm.getImportClassLoader("java.lang.Object"));

        assertSame(null, mainRealm.getImportClassLoader("NoviceProgrammerClass"));
    }

    @Test
    void testLocateSourceRealmHierachy() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        ClassRealm fooRealm = this.world.newRealm("foo");

        ClassRealm fooBarRealm = this.world.newRealm("fooBar");

        ClassRealm fooBarBazRealm = this.world.newRealm("fooBarBaz");

        mainRealm.importFrom("foo", "foo");

        mainRealm.importFrom("fooBar", "foo.bar");

        mainRealm.importFrom("fooBarBaz", "foo.bar.baz");

        assertSame(fooRealm, mainRealm.getImportClassLoader("foo.Goober"));

        assertSame(fooRealm, mainRealm.getImportClassLoader("foo.cheese.Goober"));

        assertSame(fooBarRealm, mainRealm.getImportClassLoader("foo.bar.Goober"));

        assertSame(fooBarRealm, mainRealm.getImportClassLoader("foo.bar.cheese.Goober"));

        assertSame(fooBarBazRealm, mainRealm.getImportClassLoader("foo.bar.baz.Goober"));

        assertSame(fooBarBazRealm, mainRealm.getImportClassLoader("foo.bar.baz.cheese.Goober"));

        assertSame(null, mainRealm.getImportClassLoader("java.lang.Object"));

        assertSame(null, mainRealm.getImportClassLoader("NoviceProgrammerClass"));
    }

    @Test
    void testLocateSourceRealmHierachyReverse() throws Exception {
        ClassRealm fooBarBazRealm = this.world.newRealm("fooBarBaz");

        ClassRealm fooBarRealm = this.world.newRealm("fooBar");

        ClassRealm fooRealm = this.world.newRealm("foo");

        ClassRealm mainRealm = this.world.newRealm("main");

        mainRealm.importFrom("fooBarBaz", "foo.bar.baz");

        mainRealm.importFrom("fooBar", "foo.bar");

        mainRealm.importFrom("foo", "foo");

        assertSame(fooRealm, mainRealm.getImportClassLoader("foo.Goober"));

        assertSame(fooRealm, mainRealm.getImportClassLoader("foo.cheese.Goober"));

        assertSame(fooBarRealm, mainRealm.getImportClassLoader("foo.bar.Goober"));

        assertSame(fooBarRealm, mainRealm.getImportClassLoader("foo.bar.cheese.Goober"));

        assertSame(fooBarBazRealm, mainRealm.getImportClassLoader("foo.bar.baz.Goober"));

        assertSame(fooBarBazRealm, mainRealm.getImportClassLoader("foo.bar.baz.cheese.Goober"));

        assertSame(null, mainRealm.getImportClassLoader("java.lang.Object"));

        assertSame(null, mainRealm.getImportClassLoader("NoviceProgrammerClass"));
    }

    @Test
    void testLoadClassSystemClass() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        Class<?> cls = mainRealm.loadClass("java.lang.Object");

        assertNotNull(cls);
    }

    @Test
    void testLoadClassNonSystemClass() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        try {
            Class<?> c = mainRealm.loadClass("com.werken.projectz.UberThing");

            System.out.println("c = " + c);

            fail("A ClassNotFoundException should be thrown!");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }
    }

    @Test
    void testLoadClassClassWorldsClass() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        Class<?> cls = mainRealm.loadClass("org.codehaus.plexus.classworlds.ClassWorld");

        assertNotNull(cls);

        assertSame(ClassWorld.class, cls);
    }

    @Test
    void testLoadClassLocal() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        try {
            mainRealm.loadClass("a.A");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }

        mainRealm.addURL(getJarUrl("a.jar"));

        Class<?> classA = mainRealm.loadClass("a.A");

        assertNotNull(classA);

        ClassRealm otherRealm = this.world.newRealm("other");

        try {
            otherRealm.loadClass("a.A");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }
    }

    @Test
    void testLoadClassImported() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        ClassRealm realmA = this.world.newRealm("realmA");

        try {
            realmA.loadClass("a.A");

            fail("realmA.loadClass(a.A) should have thrown a ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }

        realmA.addURL(getJarUrl("a.jar"));

        try {
            mainRealm.loadClass("a.A");

            fail("mainRealm.loadClass(a.A) should have thrown a ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }

        mainRealm.importFrom("realmA", "a");

        Class<?> classA = realmA.loadClass("a.A");

        assertNotNull(classA);

        assertEquals(realmA, classA.getClassLoader());

        Class<?> classMain = mainRealm.loadClass("a.A");

        assertNotNull(classMain);

        assertEquals(realmA, classMain.getClassLoader());

        assertSame(classA, classMain);
    }

    @Test
    void testLoadClassPackage() throws Exception {
        ClassRealm realmA = this.world.newRealm("realmA");
        realmA.addURL(getJarUrl("a.jar"));

        Class<?> clazz = realmA.loadClass("a.A");
        assertNotNull(clazz);
        assertEquals("a.A", clazz.getName());

        Package p = clazz.getPackage();
        assertNotNull(p);
        assertEquals("a", p.getName(), "p.getName()");
    }

    @Test
    void testLoadClassComplex() throws Exception {
        ClassRealm realmA = this.world.newRealm("realmA");
        ClassRealm realmB = this.world.newRealm("realmB");
        ClassRealm realmC = this.world.newRealm("realmC");

        realmA.addURL(getJarUrl("a.jar"));
        realmB.addURL(getJarUrl("b.jar"));
        realmC.addURL(getJarUrl("c.jar"));

        realmC.importFrom("realmA", "a");

        realmC.importFrom("realmB", "b");

        realmA.importFrom("realmC", "c");

        Class<?> classAA = realmA.loadClass("a.A");
        Class<?> classBB = realmB.loadClass("b.B");
        Class<?> classCC = realmC.loadClass("c.C");

        assertNotNull(classAA);
        assertNotNull(classBB);
        assertNotNull(classCC);

        assertEquals(realmA, classAA.getClassLoader());

        assertEquals(realmB, classBB.getClassLoader());

        assertEquals(realmC, classCC.getClassLoader());

        // load from C

        Class<?> classAC = realmC.loadClass("a.A");

        assertNotNull(classAC);

        assertSame(classAA, classAC);

        assertEquals(realmA, classAC.getClassLoader());

        Class<?> classBC = realmC.loadClass("b.B");

        assertNotNull(classBC);

        assertSame(classBB, classBC);

        assertEquals(realmB, classBC.getClassLoader());

        // load from A

        Class<?> classCA = realmA.loadClass("c.C");

        assertNotNull(classCA);

        assertSame(classCC, classCA);

        assertEquals(realmC, classCA.getClassLoader());

        try {
            realmA.loadClass("b.B");
            fail("throw ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }

        // load from B

        try {
            realmB.loadClass("a.A");
            fail("throw ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }

        try {
            realmB.loadClass("c.C");
            fail("throw ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected and correct
        }
    }

    @Test
    void testLoadClassClassWorldsClassRepeatedly() throws Exception {
        ClassRealm mainRealm = this.world.newRealm("main");

        for (int i = 0; i < 100; i++) {
            Class<?> cls = mainRealm.loadClass("org.codehaus.plexus.classworlds.ClassWorld");

            assertNotNull(cls);

            assertSame(ClassWorld.class, cls);
        }
    }

    @Test
    void testLoadClassWithModuleNameJava9() {
        final ExtendedClassRealm mainRealm = new ExtendedClassRealm(world);
        mainRealm.addURL(getJarUrl("a.jar"));
        assertNotNull(mainRealm.simulateLoadClassFromModule("a.A"));
    }

    @Test
    void testGetResourcesBaseBeforeSelf() throws Exception {
        String resource = "common.properties";

        ClassRealm base = this.world.newRealm("realmA");
        base.addURL(getJarUrl("a.jar"));

        URL baseUrl = base.getResource(resource);
        assertNotNull(baseUrl);

        ClassRealm sub = this.world.newRealm("realmB", base);
        sub.addURL(getJarUrl("b.jar"));

        URL subUrl = sub.getResource(resource);
        assertNotNull(subUrl);

        assertEquals(baseUrl, subUrl);

        List<String> urls = new ArrayList<>();
        for (URL url : Collections.list(sub.getResources(resource))) {
            String path = url.toString();
            path = path.substring(path.lastIndexOf('/', path.lastIndexOf(".jar!")));
            urls.add(path);
        }
        assertEquals(Arrays.asList("/a.jar!/common.properties", "/b.jar!/common.properties"), urls);
    }

    @Test
    void testGetResourcesSelfBeforeParent() throws Exception {
        String resource = "common.properties";

        ClassRealm parent = this.world.newRealm("realmA");
        parent.addURL(getJarUrl("a.jar"));

        URL parentUrl = parent.getResource(resource);
        assertNotNull(parentUrl);

        ClassRealm child = parent.createChildRealm("realmB");
        child.addURL(getJarUrl("b.jar"));

        URL childUrl = child.getResource(resource);
        assertNotNull(childUrl);

        List<URL> urls = Collections.list(child.getResources(resource));
        assertNotNull(urls);
        assertEquals(Arrays.asList(childUrl, parentUrl), urls);
    }

    /**
     * Simulates new {@code java.lang.ClassLoader#findClass(String,String)} introduced with Java 9.
     * It is reversed in terms of inheritance but enables to simulate the same behavior in these tests.
     * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassLoader.html#findClass(java.lang.String,java.lang.String)">ClassLoader#findClass(String,String)</a>
     */
    private static class ExtendedClassRealm extends ClassRealm {

        ExtendedClassRealm(final ClassWorld world) {
            super(world, "java9", Thread.currentThread().getContextClassLoader());
        }

        public Class<?> simulateLoadClassFromModule(final String name) {
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    c = findClass(null, name);
                }
                return c;
            }
        }
    }
}
