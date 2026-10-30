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
import org.codehaus.plexus.classworlds.AbstractClassWorldsTestCase;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="bwalding@jakarta.org">Ben Walding</a>
 */
class EntryTest extends AbstractClassWorldsTestCase {

    @Test
    void testCompareTo() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry1 = new Entry(r, "org.test");
        Entry entry2 = new Entry(r, "org.test.impl");

        assertTrue(entry1.compareTo(entry2) > 0, "org.test > org.test.impl");
    }

    /**
     * Tests the equality is realm independant
     */
    @Test
    void testEquals() throws DuplicateRealmException {
        ClassWorld cw = new ClassWorld();
        ClassRealm r1 = cw.newRealm("test1");
        ClassRealm r2 = cw.newRealm("test2");

        Entry entry1 = new Entry(r1, "org.test");
        Entry entry2 = new Entry(r2, "org.test");

        assertEquals(entry1, entry2, "entry1 == entry2");
        assertEquals(entry1.hashCode(), entry2.hashCode(), "entry1.hashCode() == entry2.hashCode()");
    }

    @Test
    void testMatchesClassByPackageImport() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry = new Entry(r, "org.test");

        assertTrue(entry.matches("org.test.MyClass"));
        assertTrue(entry.matches("org.test.MyClass$NestedClass"));
        assertTrue(entry.matches("org.test.MyClassUtils"));
        assertTrue(entry.matches("org.test.impl.MyClass"));
        assertFalse(entry.matches("org.tests.AnotherClass"));
    }

    @Test
    void testMatchesClassByClassImport() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry = new Entry(r, "org.test.MyClass");

        assertTrue(entry.matches("org.test.MyClass"));
        assertTrue(entry.matches("org.test.MyClass$NestedClass"));
        assertFalse(entry.matches("org.test.MyClassUtils"));
        assertFalse(entry.matches("org.test.AnotherClass"));
    }

    @Test
    void testMatchesResourceByPackageImport() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry = new Entry(r, "org.test");

        assertTrue(entry.matches("org/test/MyClass.class"));
        assertTrue(entry.matches("org/test/MyClass$NestedClass.class"));
        assertTrue(entry.matches("org/test/MyClasses.properties"));
        assertTrue(entry.matches("org/test/impl/MyClass.class"));
        assertFalse(entry.matches("org/tests/AnotherClass.class"));
    }

    @Test
    void testMatchesResourceByClassImport() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry = new Entry(r, "org.test.MyClass");

        assertTrue(entry.matches("org/test/MyClass.class"));
        assertTrue(entry.matches("org/test/MyClass$NestedClass.class"));
        assertFalse(entry.matches("org/test/MyClass.properties"));
        assertFalse(entry.matches("org/test/AnotherClass"));
    }

    @Test
    void testMatchesAllImport() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry = new Entry(r, "");

        assertTrue(entry.matches("org.test.MyClass"));
        assertTrue(entry.matches("org.test.MyClass$NestedClass"));
        assertTrue(entry.matches("org/test/MyClass.class"));
        assertTrue(entry.matches("org/test/MyClass.properties"));
    }

    @Test
    void testMatchesResourceByResourceImport() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry1 = new Entry(r, "some.properties");

        assertTrue(entry1.matches("some.properties"));
        assertFalse(entry1.matches("other.properties"));

        Entry entry2 = new Entry(r, "org/test/some.properties");

        assertTrue(entry2.matches("org/test/some.properties"));
        assertFalse(entry2.matches("org/test/other.properties"));
    }

    @Test
    void testMatchesClassByExactPackageImport() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry = new Entry(r, "org.test.*");

        assertTrue(entry.matches("org.test.MyClass"));
        assertTrue(entry.matches("org.test.MyClass$NestedClass"));
        assertTrue(entry.matches("org.test.MyClassUtils"));
        assertFalse(entry.matches("org.test.impl.MyClass"));
        assertFalse(entry.matches("org.tests.AnotherClass"));
    }

    @Test
    void testMatchesResourceByExactPackageImport() throws Exception {
        ClassWorld cw = new ClassWorld();
        ClassRealm r = cw.newRealm("test1");

        Entry entry = new Entry(r, "org.test.*");

        assertTrue(entry.matches("org/test/MyClass.class"));
        assertTrue(entry.matches("org/test/MyClass$NestedClass.class"));
        assertTrue(entry.matches("org/test/MyClasses.properties"));
        assertFalse(entry.matches("org/test/impl/MyClass.class"));
        assertFalse(entry.matches("org/tests/AnotherClass.class"));
    }
}
