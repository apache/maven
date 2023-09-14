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

import java.util.*;

class Graph {
    private enum DfsState {
        VISITING,
        VISITED
    }

    final Map<String, Vertex> vertices = new LinkedHashMap<>();

    public Vertex getVertex(String id) {
        return vertices.get(id);
    }

    public Collection<Vertex> getVertices() {
        return vertices.values();
    }

    Vertex addVertex(String label) {
        return vertices.computeIfAbsent(label, Vertex::new);
    }

    void addEdge(Vertex from, Vertex to) throws CycleDetectedException {
        from.children.add(to);
        to.parents.add(from);
        List<String> cycle = findCycle(to);
        if (cycle != null) {
            // remove edge which introduced cycle
            removeEdge(from, to);
            throw new CycleDetectedException(
                    "Edge between '" + from.label + "' and '" + to.label + "' introduces to cycle in the graph", cycle);
        }
    }

    void removeEdge(Vertex from, Vertex to) {
        from.children.remove(to);
        to.parents.remove(from);
    }

    List<String> visitAll() {
        return visitAll(vertices.values(), new HashMap<>(), new ArrayList<>());
    }

    List<String> findCycle(Vertex vertex) {
        return visitCycle(Collections.singleton(vertex), new HashMap<>(), new LinkedList<>());
    }

    private static List<String> visitAll(
            Collection<Vertex> children, Map<Vertex, DfsState> stateMap, List<String> list) {
        for (Vertex v : children) {
            DfsState state = stateMap.putIfAbsent(v, DfsState.VISITING);
            if (state == null) {
                visitAll(v.children, stateMap, list);
                stateMap.put(v, DfsState.VISITED);
                list.add(v.label);
            }
        }
        return list;
    }

    private static List<String> visitCycle(
            Collection<Vertex> children, Map<Vertex, DfsState> stateMap, LinkedList<String> cycle) {
        for (Vertex v : children) {
            DfsState state = stateMap.putIfAbsent(v, DfsState.VISITING);
            if (state == null) {
                cycle.addLast(v.label);
                List<String> ret = visitCycle(v.children, stateMap, cycle);
                if (ret != null) {
                    return ret;
                }
                cycle.removeLast();
                stateMap.put(v, DfsState.VISITED);
            } else if (state == DfsState.VISITING) {
                // we are already visiting this vertex, this mean we have a cycle
                int pos = cycle.lastIndexOf(v.label);
                List<String> ret = cycle.subList(pos, cycle.size());
                ret.add(v.label);
                return ret;
            }
        }
        return null;
    }

    static class Vertex {
        final String label;
        final List<Vertex> children = new ArrayList<>();
        final List<Vertex> parents = new ArrayList<>();

        Vertex(String label) {
            this.label = label;
        }

        String getLabel() {
            return label;
        }

        List<Vertex> getChildren() {
            return children;
        }

        List<Vertex> getParents() {
            return parents;
        }
    }
}
