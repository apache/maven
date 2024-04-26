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

import java.util.*;

class Graph {

    final Map<String, Set<String>> graph = new LinkedHashMap<>();

    synchronized void addEdge(String from, String to) throws CycleDetectedException {
        if (graph.computeIfAbsent(from, l -> new HashSet<>()).add(to)) {
            List<String> cycle = visitCycle(graph, Collections.singleton(to), new HashMap<>(), new LinkedList<>());
            if (cycle != null) {
                // remove edge which introduced cycle
                throw new CycleDetectedException(
                        "Edge between '" + from + "' and '" + to + "' introduces to cycle in the graph", cycle);
            }
        }
    }

    private enum DfsState {
        VISITING,
        VISITED
    }

    private static List<String> visitCycle(
            Map<String, Set<String>> graph,
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
