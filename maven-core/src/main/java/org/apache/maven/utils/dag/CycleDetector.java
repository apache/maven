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
package org.apache.maven.utils.dag;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 *
 */
public class CycleDetector {

    private static final Integer NOT_VISITED = 0;

    private static final Integer VISITING = 1;

    private static final Integer VISITED = 2;

    /**
     * Return all edges that cause a cycle in the graph when an edge leading to given vertex was added.
     *
     * @param vertex the vertex
     * @param vertexStateMap the vertex Map
     * @return the found cycle
     */
    private static List<String> introducesCycle(final Vertex vertex, final Map<Vertex, Integer> vertexStateMap) {
        final LinkedList<String> cycleStack = new LinkedList<>();

        final boolean hasCycle = dfsVisit(vertex, cycleStack, vertexStateMap);

        if (hasCycle) {
            // we have a situation like: [b, a, c, d, b, f, g, h].
            // Label of Vertex which introduced the cycle is at the first position in the list
            // We have to find second occurrence of this label and use its position in the list
            // for getting the sublist of vertex labels of cycle participants
            //
            // So in our case we are searching for [b, a, c, d, b]
            final String label = cycleStack.getFirst();

            final int pos = cycleStack.lastIndexOf(label);

            final List<String> cycle = cycleStack.subList(0, pos + 1);

            Collections.reverse(cycle);

            return cycle;
        }

        return null;
    }

    public static List<String> introducesCycle(final Vertex vertex) {
        final Map<Vertex, Integer> vertexStateMap = new HashMap<>();

        return introducesCycle(vertex, vertexStateMap);
    }

    private static boolean isNotVisited(final Vertex vertex, final Map<Vertex, Integer> vertexStateMap) {
        final Integer state = vertexStateMap.get(vertex);

        return (state == null) || NOT_VISITED.equals(state);
    }

    private static boolean isVisiting(final Vertex vertex, final Map<Vertex, Integer> vertexStateMap) {
        final Integer state = vertexStateMap.get(vertex);

        return VISITING.equals(state);
    }

    private static boolean dfsVisit(
            final Vertex vertex, final LinkedList<String> cycle, final Map<Vertex, Integer> vertexStateMap) {
        cycle.addFirst(vertex.getLabel());

        vertexStateMap.put(vertex, VISITING);

        for (Vertex v : vertex.getChildren()) {
            if (isNotVisited(v, vertexStateMap)) {
                final boolean hasCycle = dfsVisit(v, cycle, vertexStateMap);

                if (hasCycle) {
                    return true;
                }
            } else if (isVisiting(v, vertexStateMap)) {
                cycle.addFirst(v.getLabel());

                return true;
            }
        }
        vertexStateMap.put(vertex, VISITED);

        cycle.removeFirst();

        return false;
    }
}
