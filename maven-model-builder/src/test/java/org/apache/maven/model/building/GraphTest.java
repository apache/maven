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
package org.apache.maven.model.building;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GraphTest {

    @Test
    void testCycle() throws Graph.CycleDetectedException {
        Graph graph = new Graph();
        graph.addEdge("a1", "a2");
        assertThrows(Graph.CycleDetectedException.class, () -> graph.addEdge("a2", "a1"));
    }

    @Test
    public void test() {
        Graph map = new Graph(16, 0.75f, 2);
        map.add("a", "b");
        map.add("a", "c");
        map.add("a", "d");
        assertEquals(Arrays.asList("b", "c", "d"), toList(map.get("a")));
        map.add("b", "c");
        assertEquals(Arrays.asList("b", "c", "d"), toList(map.get("a")));
        assertEquals(Arrays.asList("c"), toList(map.get("b")));
        map.add("a", "e");
        assertEquals(Arrays.asList("b", "c", "d", "e"), toList(map.get("a")));
        assertEquals(Arrays.asList("c"), toList(map.get("b")));
    }

    private static List<String> toList(Iterable<String> col) {
        List<String> l = new ArrayList<>();
        for (String s : col) {
            l.add(s);
        }
        return l;
    }
}
