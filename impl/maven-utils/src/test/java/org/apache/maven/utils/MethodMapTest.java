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
package org.apache.maven.utils;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MethodMapTest {
    private MethodMap methods;

    @BeforeEach
    void setUp() {
        methods = new MethodMap();
        for (Method method : Overloads.class.getMethods()) {
            methods.add(method);
        }
    }

    @Test
    void choosesMostSpecificReferenceType() throws Exception {
        assertEquals(String.class, methods.find("choose", "value").getParameterTypes()[0]);
        assertEquals(
                CharSequence.class, methods.find("choose", new StringBuilder()).getParameterTypes()[0]);
        assertEquals(Object.class, methods.find("choose", new Object()).getParameterTypes()[0]);
    }

    @Test
    void choosesMostSpecificTypeForNull() throws Exception {
        assertEquals(String.class, methods.find("choose", new Object[] {null}).getParameterTypes()[0]);
        assertThrows(MethodMap.AmbiguousException.class, () -> methods.find("ambiguous", new Object[] {null}));
    }

    @Test
    void supportsPrimitiveWidening() throws Exception {
        assertEquals(long.class, methods.find("numeric", Integer.valueOf(42)).getParameterTypes()[0]);
        assertEquals(double.class, methods.find("numeric", Double.valueOf(42)).getParameterTypes()[0]);
    }

    @Test
    void reportsAmbiguityBetweenPrimitiveAndBoxedParameters() {
        assertThrows(MethodMap.AmbiguousException.class, () -> methods.find("boxed", Integer.valueOf(42)));
    }

    @Test
    void returnsNullWhenNoOverloadApplies() throws Exception {
        assertNull(methods.find("missing", "value"));
        assertNull(methods.find("choose"));
        assertNull(methods.find("numeric", "value"));
    }

    public static class Overloads {
        public void choose(Object value) {}

        public void choose(CharSequence value) {}

        public void choose(String value) {}

        public void ambiguous(CharSequence value) {}

        public void ambiguous(Number value) {}

        public void numeric(long value) {}

        public void numeric(double value) {}

        public void boxed(int value) {}

        public void boxed(Integer value) {}
    }
}
