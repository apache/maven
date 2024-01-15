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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void testPerf() throws IOException {
        List<String[]> data = new ArrayList<>();
        String k = null;
        for (String line : Files.readAllLines(Paths.get("src/test/resources/dag.txt"))) {
            if (line.startsWith("\t")) {
                data.add(new String[] {k, line.trim()});
            } else {
                k = line;
            }
        }
        Collections.shuffle(data);

        int nb = 100;
        for (int i = 0; i < nb; i++) {
            Graph g = new Graph();
            data.parallelStream().forEach(s -> {
                try {
                    g.addEdge(s[0], s[1]);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        for (int i = 0; i < nb; i++) {
            OldGraph g = new OldGraph();
            data.parallelStream().forEach(s -> {
                try {
                    g.addEdge(s[0], s[1]);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        List<Double> ratios = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            Collections.shuffle(data);
            int nbr = 1000;
            long t0 = System.nanoTime();
            for (int i = 0; i < nbr; i++) {
                Graph g = new Graph();
                data.parallelStream().forEach(s -> {
                    try {
                        g.addEdge(s[0], s[1]);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            long t1 = System.nanoTime();
            System.out.println("new: " + TimeUnit.NANOSECONDS.toMillis(t1 - t0) + " ms");

            long t2 = System.nanoTime();
            for (int i = 0; i < nbr; i++) {
                OldGraph g = new OldGraph();
                data.parallelStream().forEach(s -> {
                    try {
                        g.addEdge(s[0], s[1]);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            long t3 = System.nanoTime();

            System.out.println("old: " + TimeUnit.NANOSECONDS.toMillis(t3 - t2) + " ns");
            System.out.println("ratio: " + Math.round((t1 - t0) * 1000.0 / (t3 - t2)) / 10.0 + "%");
            ratios.add((t1 - t0) * 1.0 / (t3 - t2));
        }

        double average = 0.0;
        for (double p : ratios) {
            average += p;
        }
        average /= ratios.size();
        double variance = 0.0;
        for (double p : ratios) {
            variance += (p - average) * (p - average);
        }
        variance /= ratios.size();
        System.out.println("average: " + Math.round(average * 1000.0) / 10.0 + "%");
        System.out.println("variance: " + Math.round(variance * 1000.0) / 1000.0);
    }

    static class OldGraph {
        final Map<String, List<String>> graph = new LinkedHashMap<>();

        synchronized void addEdge(String from, String to) throws CycleDetectedException {
            graph.computeIfAbsent(from, l -> new ArrayList<>()).add(to);
            List<String> cycle = visitCycle(graph, Collections.singleton(to), new HashMap<>(), new LinkedList<>());
            if (cycle != null) {
                // remove edge which introduced cycle
                throw new CycleDetectedException(
                        "Edge between '" + from + "' and '" + to + "' introduces to cycle in the graph", cycle);
            }
        }

        private enum DfsState {
            VISITING,
            VISITED
        }

        private static List<String> visitCycle(
                Map<String, List<String>> graph,
                Collection<String> children,
                Map<String, DfsState> stateMap,
                LinkedList<String> cycle) {
            if (children != null) {
                for (String v : children) {
                    DfsState state = stateMap.putIfAbsent(v, DfsState.VISITING);
                    if (state == null) {
                        cycle.addLast(v);
                        List<String> ret = visitCycle(graph, graph.get(v), stateMap, cycle);
                        if (ret != null) {
                            return ret;
                        }
                        cycle.removeLast();
                        stateMap.put(v, DfsState.VISITED);
                    } else if (state == DfsState.VISITING) {
                        // we are already visiting this vertex, this mean we have a cycle
                        int pos = cycle.lastIndexOf(v);
                        List<String> ret = cycle.subList(pos, cycle.size());
                        ret.add(v);
                        return ret;
                    }
                }
            }
            return null;
        }

        public static class CycleDetectedException extends Exception {
            private final List<String> cycle;

            CycleDetectedException(String message, List<String> cycle) {
                super(message);
                this.cycle = cycle;
            }

            public List<String> getCycle() {
                return cycle;
            }

            @Override
            public String getMessage() {
                return super.getMessage() + " " + String.join(" --> ", cycle);
            }
        }
    }

    private static List<String> toList(Iterable<String> col) {
        List<String> l = new ArrayList<>();
        for (String s : col) {
            l.add(s);
        }
        return l;
    }
}
