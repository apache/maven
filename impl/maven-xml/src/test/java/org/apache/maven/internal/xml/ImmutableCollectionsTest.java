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
package org.apache.maven.internal.xml;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImmutableCollectionsTest {

    private static List<String> abc() {
        return ImmutableCollections.copy(Arrays.asList("a", "b", "c"));
    }

    @Test
    void subListRejectsIndexEqualToItsSize() {
        List<String> sub = abc().subList(0, 1);

        assertEquals(1, sub.size());
        assertEquals("a", sub.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> sub.get(1));
    }

    @Test
    void subListDoesNotReachPastItsEnd() {
        // A sub list that stops before the end of the backing list is the case that used to
        // read past its own bounds instead of failing: the parent list resolves the index
        // and hands back an element the sub list does not contain.
        List<String> sub = abc().subList(1, 2);

        assertEquals(List.of("b"), sub);
        assertThrows(IndexOutOfBoundsException.class, () -> sub.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> sub.get(-1));
    }

    @Test
    void subListIterationStaysWithinBounds() {
        List<String> sub = abc().subList(0, 2);

        assertEquals(List.of("a", "b"), sub);
        assertEquals(List.of("a", "b"), List.copyOf(sub));
    }

    @Test
    void listIteratorStillAcceptsIndexEqualToSize() {
        // List.listIterator(int) is specified to accept size() as a valid cursor position,
        // so that bound is deliberately not the same as the one for get(int).
        List<String> list = abc();

        assertEquals(3, list.size());
        assertEquals(false, list.listIterator(3).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(4));
    }
}
