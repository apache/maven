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
package org.apache.maven.impl.di;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.di.Typed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for SessionScope#getInterfaces behaviour with @Typed and session-scoped proxies.
 */
class SessionScopeTest {

    interface A {}

    interface B extends A {}

    interface C {}

    static class Base implements B {}

    @Typed // no explicit interfaces: should collect all from hierarchy (B, A, C)
    static class Impl extends Base implements C {}

    @Typed({C.class}) // explicit interface list
    static class ImplExplicit extends Base implements C {}

    static class NoTyped extends Base implements C {}

    static class ExposedSessionScope extends SessionScope {
        Class<?>[] interfacesOf(Class<?> type) {
            return getInterfaces(type);
        }
    }

    @Test
    void typedWithoutValuesIncludesOnlyDirectInterfaces() {
        ExposedSessionScope scope = new ExposedSessionScope();
        Class<?>[] itfs = scope.interfacesOf(Impl.class);
        Set<Class<?>> set = Arrays.stream(itfs).collect(Collectors.toSet());
        assertTrue(set.contains(C.class), "Should include only direct interfaces implemented by the class");
        assertFalse(set.contains(B.class), "Should NOT include interfaces from superclass");
        assertFalse(set.contains(A.class), "Should NOT include super-interfaces");
        // Proxy should not include concrete classes
        assertFalse(set.contains(Base.class));
        assertFalse(set.contains(Impl.class));
    }

    @Test
    void typedWithExplicitValuesRespectsExplicitInterfacesOnly() {
        ExposedSessionScope scope = new ExposedSessionScope();
        Class<?>[] itfs = scope.interfacesOf(ImplExplicit.class);
        assertArrayEquals(new Class<?>[] {C.class}, itfs, "Only explicitly listed interfaces should be used");
    }

    @Test
    void missingTypedAnnotationThrows() {
        ExposedSessionScope scope = new ExposedSessionScope();
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> scope.interfacesOf(NoTyped.class));
        assertTrue(ex.getMessage().contains("Typed"));
    }
}
