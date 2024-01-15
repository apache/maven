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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GraphTest {

    @Test
    void testCycle() throws Graph.CycleDetectedException {
        Graph graph = new Graph();
        graph.addEdge("a1", "a2");
        assertThrows(Graph.CycleDetectedException.class, () -> graph.addEdge("a2", "a1"));
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

        long t0 = System.nanoTime();
        Graph g = new Graph();
        data.parallelStream().forEach(s -> {
            try {
                g.addEdge(s[0], s[1]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        long t1 = System.nanoTime();
    }
}
