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
package org.apache.maven.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.project.Graph.Vertex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphTest {

    @Test
    public void testGraph() throws CycleDetectedException {
        Graph graph = new Graph();
        graph.addVertex("a");
        assertEquals(1, graph.getVertices().size());
        assertEquals("a", graph.getVertex("a").getLabel());
        graph.addVertex("a");
        assertEquals(1, graph.getVertices().size());
        assertEquals("a", graph.getVertex("a").getLabel());

        graph.addVertex("b");
        assertEquals(2, graph.getVertices().size());
        assertFalse(hasEdge(graph, "a", "b"));
        assertFalse(hasEdge(graph, "b", "a"));

        Vertex a = graph.getVertex("a");
        Vertex b = graph.getVertex("b");
        assertEquals("a", a.getLabel());
        assertEquals("b", b.getLabel());

        addEdge(graph, "a", "b");
        assertTrue(a.getChildren().contains(b));
        assertTrue(b.getParents().contains(a));
        assertTrue(hasEdge(graph, "a", "b"));
        assertFalse(hasEdge(graph, "b", "a"));

        addEdge(graph, "c", "d");
        assertEquals(4, graph.getVertices().size());

        Vertex c = graph.getVertex("c");
        Vertex d = graph.getVertex("d");
        assertEquals("a", a.getLabel());
        assertEquals("b", b.getLabel());
        assertEquals("c", c.getLabel());
        assertEquals("d", d.getLabel());
        assertFalse(hasEdge(graph, "b", "a"));
        assertFalse(hasEdge(graph, "a", "c"));
        assertFalse(hasEdge(graph, "a", "d"));
        assertTrue(hasEdge(graph, "c", "d"));
        assertFalse(hasEdge(graph, "d", "c"));

        Set<String> labels = graph.getVertices().stream().map(Vertex::getLabel).collect(Collectors.toSet());
        assertEquals(4, labels.size());
        assertTrue(labels.contains("a"));
        assertTrue(labels.contains("b"));
        assertTrue(labels.contains("c"));
        assertTrue(labels.contains("d"));

        addEdge(graph, "a", "d");
        assertEquals(2, a.getChildren().size());
        assertTrue(a.getChildren().contains(b));
        assertTrue(a.getChildren().contains(d));
        assertEquals(2, d.getParents().size());
        assertTrue(d.getParents().contains(a));
        assertTrue(d.getParents().contains(c));
    }

    @Test
    public void testCycleDetection() throws Exception {
        Graph graph1 = new Graph();
        addEdge(graph1, "a", "b");
        addEdge(graph1, "b", "c");

        Graph graph2 = new Graph();
        addEdge(graph2, "a", "b");
        addEdge(graph2, "b", "c");
        CycleDetectedException cde = assertThrows(CycleDetectedException.class, () -> addEdge(graph2, "c", "a"));
        List<String> cycle = cde.getCycle();
        assertNotNull(cycle, "Cycle should be not null");
        assertTrue(cycle.contains("a"), "Cycle contains 'a'");
        assertTrue(cycle.contains("b"), "Cycle contains 'b'");
        assertTrue(cycle.contains("c"), "Cycle contains 'c'");

        Graph graph3 = new Graph();
        addEdge(graph3, "a", "b");
        addEdge(graph3, "b", "c");
        addEdge(graph3, "b", "d");
        addEdge(graph3, "a", "d");

        Graph graph4 = new Graph();
        addEdge(graph4, "a", "b");
        addEdge(graph4, "b", "c");
        addEdge(graph4, "b", "d");
        addEdge(graph4, "a", "d");
        cde = assertThrows(CycleDetectedException.class, () -> addEdge(graph4, "c", "a"));
        assertEquals(Arrays.asList("a", "b", "c", "a"), cde.getCycle());

        Graph graph5 = new Graph();
        addEdge(graph5, "a", "b");
        addEdge(graph5, "b", "c");
        addEdge(graph5, "b", "f");
        addEdge(graph5, "f", "g");
        addEdge(graph5, "g", "h");
        addEdge(graph5, "c", "d");
        addEdge(graph5, "d", "e");
        cde = assertThrows(CycleDetectedException.class, () -> addEdge(graph5, "e", "b"));
        assertEquals(Arrays.asList("b", "c", "d", "e", "b"), cde.getCycle());
        assertTrue(hasEdge(graph5, "a", "b"));
        assertTrue(hasEdge(graph5, "b", "c"));
        assertTrue(hasEdge(graph5, "b", "f"));
        assertTrue(hasEdge(graph5, "f", "g"));
        assertTrue(hasEdge(graph5, "g", "h"));
        assertTrue(hasEdge(graph5, "c", "d"));
        assertTrue(hasEdge(graph5, "d", "e"));
        assertFalse(hasEdge(graph5, "e", "b"));
    }

    @Test
    public void testDfs() throws CycleDetectedException {
        Graph graph1 = new Graph();
        addEdge(graph1, "a", "b");
        addEdge(graph1, "b", "c");
        List<String> expected1 = new ArrayList<>();
        expected1.add("c");
        expected1.add("b");
        expected1.add("a");
        List<String> actual1 = graph1.visitAll();
        assertEquals(expected1, actual1);

        Graph graph2 = new Graph();
        graph2.addVertex("a");
        graph2.addVertex("b");
        graph2.addVertex("c");
        addEdge(graph2, "b", "a");
        addEdge(graph2, "c", "b");
        List<String> expected2 = new ArrayList<>();
        expected2.add("a");
        expected2.add("b");
        expected2.add("c");
        List<String> actual2 = graph2.visitAll();
        assertEquals(expected2, actual2);

        Graph graph3 = new Graph();
        graph3.addVertex("a");
        graph3.addVertex("b");
        graph3.addVertex("c");
        graph3.addVertex("d");
        graph3.addVertex("e");
        graph3.addVertex("f");
        addEdge(graph3, "a", "b");
        addEdge(graph3, "b", "c");
        addEdge(graph3, "b", "d");
        addEdge(graph3, "c", "d");
        addEdge(graph3, "c", "e");
        addEdge(graph3, "f", "d");
        addEdge(graph3, "e", "f");
        addEdge(graph3, "f", "g");
        List<String> expected3 = new ArrayList<>();
        expected3.add("d");
        expected3.add("g");
        expected3.add("f");
        expected3.add("e");
        expected3.add("c");
        expected3.add("b");
        expected3.add("a");
        List<String> actual3 = graph3.visitAll();
        assertEquals(expected3, actual3);

        Graph graph4 = new Graph();
        graph4.addVertex("f");
        graph4.addVertex("e");
        graph4.addVertex("d");
        graph4.addVertex("c");
        graph4.addVertex("a");
        graph4.addVertex("b");
        addEdge(graph4, "a", "b");
        addEdge(graph4, "b", "c");
        addEdge(graph4, "b", "d");
        addEdge(graph4, "c", "d");
        addEdge(graph4, "c", "e");
        addEdge(graph4, "f", "d");
        addEdge(graph4, "e", "f");

        List<String> expected4 = new ArrayList<>();
        expected4.add("d");
        expected4.add("f");
        expected4.add("e");
        expected4.add("c");
        expected4.add("b");
        expected4.add("a");
        List<String> actual4 = graph4.visitAll();
        assertEquals(expected4, actual4);
    }

    static void addEdge(Graph graph, String v1, String v2) throws CycleDetectedException {
        Vertex vx1 = graph.addVertex(v1);
        Vertex vx2 = graph.addVertex(v2);
        graph.addEdge(vx1, vx2);
    }

    static boolean hasEdge(Graph graph, String v1, String v2) {
        Vertex vx1 = graph.getVertex(v1);
        Vertex vx2 = graph.getVertex(v2);
        return vx1 != null && vx2 != null && vx1.children.contains(vx2);
    }
}
