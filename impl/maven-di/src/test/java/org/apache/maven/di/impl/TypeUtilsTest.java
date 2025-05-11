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
package org.apache.maven.di.impl;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.di.Key;
import org.junit.jupiter.api.Test;

import static org.apache.maven.di.impl.Types.simplifyType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TypeUtilsTest {

    TreeSet<String> aField;

    @Test
    void getSuperTypes() {
        Type type = new Key<TreeSet<String>>() {}.getType();
        Set<Type> types = Types.getAllSuperTypes(type);
        assertNotNull(types);
        List<String> typesStr = types.stream().map(Type::toString).sorted().collect(Collectors.toList());
        typesStr.remove("java.util.SequencedSet<java.lang.String>");
        typesStr.remove("java.util.SequencedCollection<java.lang.String>");
        assertEquals(
                Arrays.asList(
                        "class java.lang.Object",
                        "interface java.io.Serializable",
                        "interface java.lang.Cloneable",
                        "java.lang.Iterable<java.lang.String>",
                        "java.util.AbstractCollection<java.lang.String>",
                        "java.util.AbstractSet<java.lang.String>",
                        "java.util.Collection<java.lang.String>",
                        "java.util.NavigableSet<java.lang.String>",
                        "java.util.Set<java.lang.String>",
                        "java.util.SortedSet<java.lang.String>",
                        "java.util.TreeSet<java.lang.String>"),
                typesStr);
    }

    @Test
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    void testSimplifyType() {
        {
            Type type = Integer.class;
            assertEquals(type, simplifyType(type));
        }

        {
            Type type = new Key<Set<Integer>>() {}.getType();
            assertEquals(type, simplifyType(type));
        }

        {
            Type type = new Key<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(type, simplifyType(type));
        }

        {
            Type type = new Key<Set<? extends Integer>>() {}.getType();
            Type expected = new Key<Set<Integer>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<Set<? extends Set<? extends Set<? extends Integer>>>>() {}.getType();
            Type expected = new Key<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<Set<Set<? extends Set<Integer>>>>() {}.getType();
            Type expected = new Key<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<Set<? super Integer>>() {}.getType();
            Type expected = new Key<Set<Integer>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<Set<? super Set<? super Set<? super Integer>>>>() {}.getType();
            Type expected = new Key<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<Set<Set<? super Set<Integer>>>>() {}.getType();
            Type expected = new Key<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<Set<? extends Set<? super Set<? extends Integer>>>>() {}.getType();
            Type expected = new Key<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<Set<? extends Integer>[]>() {}.getType();
            Type expected = new Key<Set<Integer>[]>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<Set<? super Integer>[]>() {}.getType();
            Type expected = new Key<Set<Integer>[]>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<TestInterface<? extends Integer, ? extends Integer>>() {}.getType();
            Type expected = new Key<TestInterface<Integer, Integer>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<TestInterface<Integer, Integer>>() {}.getType();
            Type expected = new Key<TestInterface<Integer, Integer>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<TestClass<?, ?, ?, ?, ?, ?, ?, ?, ?>>() {}.getType();
            Type expected = TestClass.class;
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new Key<TestClass<?, ?, ?, Object, ?, ?, ?, ?, ?>>() {}.getType();
            Type expected = TestClass.class;
            assertEquals(expected, simplifyType(type));
        }

        {
            //noinspection TypeParameterExplicitlyExtendsObject
            Type type = new Key<
                    TestClass<
                            Integer,
                            ? extends Integer,
                            ? super Integer,
                            Object,
                            ? extends Object,
                            ? super Object,
                            ?,
                            Set<? extends TestInterface<Integer, ? extends Integer>>,
                            Set<? super TestInterface<Integer, ? super Integer>>>>() {}.getType();
            Type expected = new Key<
                    TestClass<
                            Integer,
                            Integer,
                            Integer,
                            Object,
                            Object,
                            Object,
                            Object,
                            Set<TestInterface<Integer, Integer>>,
                            Set<TestInterface<Integer, Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }
    }

    public static final class TestClass<A, B, C, D, E, F, G, H, I> {}

    interface TestInterface<A, B extends Integer> {}
}
