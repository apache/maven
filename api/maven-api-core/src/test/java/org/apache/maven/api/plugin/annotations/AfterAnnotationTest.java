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
package org.apache.maven.api.plugin.annotations;

import java.lang.annotation.Repeatable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link After} annotation, including its {@link Repeatable} behavior.
 */
class AfterAnnotationTest {

    @After(phase = "compile", type = After.Type.PROJECT)
    static class SingleAfterMojo {}

    @After(phase = "sources")
    static class DefaultTypeMojo {}

    @After(phase = "compile", type = After.Type.PROJECT)
    @After(phase = "ready", type = After.Type.DEPENDENCIES, scope = "compile")
    @After(phase = "package", type = After.Type.CHILDREN)
    static class MultipleAfterMojo {}

    @Test
    void afterIsRepeatable() {
        assertTrue(After.class.isAnnotationPresent(Repeatable.class));
        assertEquals(Afters.class, After.class.getAnnotation(Repeatable.class).value());
    }

    @Test
    void singleAfterAnnotation() {
        After after = SingleAfterMojo.class.getAnnotation(After.class);
        assertNotNull(after);
        assertEquals("compile", after.phase());
        assertEquals(After.Type.PROJECT, after.type());
        assertEquals("", after.scope());
    }

    @Test
    void multipleAfterAnnotations() {
        After[] afters = MultipleAfterMojo.class.getAnnotationsByType(After.class);
        assertNotNull(afters);
        assertEquals(3, afters.length);

        assertEquals("compile", afters[0].phase());
        assertEquals(After.Type.PROJECT, afters[0].type());

        assertEquals("ready", afters[1].phase());
        assertEquals(After.Type.DEPENDENCIES, afters[1].type());
        assertEquals("compile", afters[1].scope());

        assertEquals("package", afters[2].phase());
        assertEquals(After.Type.CHILDREN, afters[2].type());
    }

    @Test
    void aftersContainerAnnotation() {
        Afters afters = MultipleAfterMojo.class.getAnnotation(Afters.class);
        assertNotNull(afters);
        assertEquals(3, afters.value().length);
    }

    @Test
    void typeDefaultsToProject() {
        After after = DefaultTypeMojo.class.getAnnotation(After.class);
        assertNotNull(after);
        assertEquals("sources", after.phase());
        assertEquals(After.Type.PROJECT, after.type());
    }

    @Test
    void scopeDefaultsToEmpty() {
        After after = SingleAfterMojo.class.getAnnotation(After.class);
        assertEquals("", after.scope());
    }
}
