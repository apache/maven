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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.codehaus.plexus.classworlds.AbstractClassWorldsTestCase;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilteredClassRealmTest extends AbstractClassWorldsTestCase {
    private ClassWorld world;
    private ClassRealm realmA;

    @BeforeEach
    public void setUp() throws DuplicateRealmException {
        this.world = new ClassWorld();
        // only allow loading resources whose names start with "a."
        Set<String> allowedResourcePrefixes = new HashSet<>();
        allowedResourcePrefixes.add("a.");
        allowedResourcePrefixes.add("a/Aa");
        realmA = this.world.newRealm("realmA", getClass().getClassLoader(), s -> allowedResourcePrefixes.stream()
                .anyMatch(s::startsWith));
    }

    @Test
    void testLoadResources() throws Exception {
        realmA.addURL(getJarUrl("a.jar"));
        assertNull(realmA.getResource("common.properties"));
        assertFalse(realmA.getResources("common.properties").hasMoreElements());

        assertNotNull(realmA.getResource("a.properties"));
        assertTrue(realmA.getResources("a.properties").hasMoreElements());
    }

    @Test
    void testLoadClass() throws ClassNotFoundException {
        assertThrows(ClassNotFoundException.class, () -> realmA.loadClass("a.Aa"));
        realmA.addURL(getJarUrl("a.jar"));

        assertNotNull(realmA.loadClass("a.Aa"));
        assertThrows(ClassNotFoundException.class, () -> realmA.loadClass("a.A"));

        assertNotNull(realmA.loadClass("a.Aa"));
        assertThrows(ClassNotFoundException.class, () -> realmA.loadClass("a.A"));
    }

    @Test
    void testLoadClassWithModule() throws IOException {
        try (ExtendedFilteredClassRealm realmA = new ExtendedFilteredClassRealm(world, s -> s.startsWith("a/Aa"))) {
            realmA.addURL(getJarUrl("a.jar"));
            assertNotNull(realmA.simulateLoadClassFromModule("a.Aa"));
            assertNull(realmA.simulateLoadClassFromModule("a.A"));
        }
    }

    /**
     * Simulates new {@code java.lang.ClassLoader#findClass(String,String)} introduced with Java 9.
     * It is reversed in terms of inheritance but enables to simulate the same behavior in these tests.
     * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassLoader.html#findClass(java.lang.String,java.lang.String)">ClassLoader#findClass(String,String)</a>
     * @see ClassRealmImplTest.ExtendedClassRealm
     */
    static class ExtendedFilteredClassRealm extends FilteredClassRealm {

        ExtendedFilteredClassRealm(final ClassWorld world, Predicate<String> filter) {
            super(filter, world, "java9", Thread.currentThread().getContextClassLoader());
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
