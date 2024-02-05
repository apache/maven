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
import java.util.*;

import org.junit.jupiter.api.Test;

import static org.apache.maven.di.impl.TypeUtils.simplifyType;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeUtilsTest {

    @Test
    public void testSimplifyType() {
        {
            Type type = Integer.class;
            assertEquals(type, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<Integer>>() {}.getType();
            assertEquals(type, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(type, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<? extends Integer>>() {}.getType();
            Type expected = new TypeT<Set<Integer>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<? extends Set<? extends Set<? extends Integer>>>>() {}.getType();
            Type expected = new TypeT<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<Set<? extends Set<Integer>>>>() {}.getType();
            Type expected = new TypeT<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<? super Integer>>() {}.getType();
            Type expected = new TypeT<Set<Integer>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<? super Set<? super Set<? super Integer>>>>() {}.getType();
            Type expected = new TypeT<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<Set<? super Set<Integer>>>>() {}.getType();
            Type expected = new TypeT<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<? extends Set<? super Set<? extends Integer>>>>() {}.getType();
            Type expected = new TypeT<Set<Set<Set<Integer>>>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<? extends Integer>[]>() {}.getType();
            Type expected = new TypeT<Set<Integer>[]>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<Set<? super Integer>[]>() {}.getType();
            Type expected = new TypeT<Set<Integer>[]>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<TestInterface<? extends Integer, ? extends Integer>>() {}.getType();
            Type expected = new TypeT<TestInterface<Integer, Integer>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<TestInterface<Integer, Integer>>() {}.getType();
            Type expected = new TypeT<TestInterface<Integer, Integer>>() {}.getType();
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<TestClass<?, ?, ?, ?, ?, ?, ?, ?, ?>>() {}.getType();
            Type expected = TestClass.class;
            assertEquals(expected, simplifyType(type));
        }

        {
            Type type = new TypeT<TestClass<?, ?, ?, Object, ?, ?, ?, ?, ?>>() {}.getType();
            Type expected = TestClass.class;
            assertEquals(expected, simplifyType(type));
        }

        {
            //noinspection TypeParameterExplicitlyExtendsObject
            Type type = new TypeT<
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
            Type expected = new TypeT<
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
