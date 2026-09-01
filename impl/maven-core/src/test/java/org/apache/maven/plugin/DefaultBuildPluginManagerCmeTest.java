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
package org.apache.maven.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the BND ConcurrentModificationException detection in {@link DefaultBuildPluginManager}.
 */
class DefaultBuildPluginManagerCmeTest {

    @Test
    void testDetectsBndComputeIfAbsentCme() throws Exception {
        ConcurrentModificationException cme = new ConcurrentModificationException();
        cme.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("java.util.TreeMap", "callMappingFunctionWithCheck", "TreeMap.java", 750),
            new StackTraceElement("java.util.TreeMap", "computeIfAbsent", "TreeMap.java", 604),
            new StackTraceElement("aQute.bnd.osgi.Jar", "putResource", "Jar.java", 259),
            new StackTraceElement("aQute.bnd.osgi.Jar$1", "visitFile", "Jar.java", 186),
        });

        assertTrue(invokeIsBndComputeIfAbsentCme(cme));
    }

    @Test
    void testDoesNotDetectUnrelatedCme() throws Exception {
        ConcurrentModificationException cme = new ConcurrentModificationException();
        cme.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("java.util.HashMap$HashIterator", "nextNode", "HashMap.java", 1605),
            new StackTraceElement("com.example.SomePlugin", "doWork", "SomePlugin.java", 42),
        });

        assertFalse(invokeIsBndComputeIfAbsentCme(cme));
    }

    @Test
    void testDoesNotDetectEmptyStackTrace() throws Exception {
        ConcurrentModificationException cme = new ConcurrentModificationException();
        cme.setStackTrace(new StackTraceElement[0]);

        assertFalse(invokeIsBndComputeIfAbsentCme(cme));
    }

    /**
     * Invokes the private static method via reflection for testing.
     */
    private static boolean invokeIsBndComputeIfAbsentCme(ConcurrentModificationException cme) throws Exception {
        Method method = DefaultBuildPluginManager.class.getDeclaredMethod(
                "isBndComputeIfAbsentCme", ConcurrentModificationException.class);
        method.setAccessible(true);
        try {
            return (boolean) method.invoke(null, cme);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}
