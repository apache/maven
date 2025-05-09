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
package org.apache.maven.repository.metadata;

import javax.inject.Inject;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 *
 */
@PlexusTest
@Deprecated
class DefaultGraphConflictResolverTest {
    @Inject
    GraphConflictResolver resolver;

    MetadataGraph graph;

    MetadataGraphVertex v1;
    MetadataGraphVertex v2;
    MetadataGraphVertex v3;
    MetadataGraphVertex v4;

    // ------------------------------------------------------------------------------------------
    @BeforeEach
    void setUp() throws Exception {
        /*
         *       v2
         *   v1<
         *      v3-v4
         *
         */
        graph = new MetadataGraph(4, 3);
        v1 = graph.addVertex(new ArtifactMetadata("g", "a1", "1.0"));
        graph.setEntry(v1);
        v2 = graph.addVertex(new ArtifactMetadata("g", "a2", "1.0"));
        v3 = graph.addVertex(new ArtifactMetadata("g", "a3", "1.0"));
        v4 = graph.addVertex(new ArtifactMetadata("g", "a4", "1.0"));

        // v1-->v2
        graph.addEdge(v1, v2, new MetadataGraphEdge("1.1", true, null, null, 2, 1));
        graph.addEdge(v1, v2, new MetadataGraphEdge("1.2", true, null, null, 2, 2));

        // v1-->v3
        graph.addEdge(v1, v3, new MetadataGraphEdge("1.1", true, null, null, 2, 1));
        graph.addEdge(v1, v3, new MetadataGraphEdge("1.2", true, null, null, 4, 2));

        // v3-->v4
        graph.addEdge(v3, v4, new MetadataGraphEdge("1.1", true, ArtifactScopeEnum.runtime, null, 2, 1));
        graph.addEdge(v3, v4, new MetadataGraphEdge("1.2", true, ArtifactScopeEnum.provided, null, 2, 2));
    }

    // ------------------------------------------------------------------------------------------
    @Test
    void compileResolution() throws Exception {
        MetadataGraph res;

        res = resolver.resolveConflicts(graph, ArtifactScopeEnum.compile);

        assertThat(res).as("null graph after resolver").isNotNull();
        assertThat(res.getVertices()).as("no vertices in the resulting graph after resolver").isNotNull();

        assertThat(res.getExcidentEdges(v1)).as("no edges in the resulting graph after resolver").isNotNull();

        assertThat(res.getVertices().size()).as("wrong # of vertices in the resulting graph after resolver").isEqualTo(4);
        assertThat(res.getExcidentEdges(v1).size()).as("wrong # of excident edges in the resulting graph entry after resolver").isEqualTo(2);

        assertThat(res.getIncidentEdges(v2).size()).as("wrong # of v2 incident edges in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v2).get(0).getVersion()).as("wrong edge v1-v2 in the resulting graph after resolver").isEqualTo("1.2");

        assertThat(res.getIncidentEdges(v3).size()).as("wrong # of edges v1-v3 in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v3).get(0).getVersion()).as("wrong edge v1-v3 in the resulting graph after resolver").isEqualTo("1.1");

        assertThat(res.getIncidentEdges(v4).size()).as("wrong # of edges v3-v4 in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v4).get(0).getVersion()).as("wrong edge v3-v4 in the resulting graph after resolver").isEqualTo("1.2");
    }

    // ------------------------------------------------------------------------------------------
    @Test
    void runtimeResolution() throws Exception {
        MetadataGraph res;

        res = resolver.resolveConflicts(graph, ArtifactScopeEnum.runtime);

        assertThat(res).as("null graph after resolver").isNotNull();
        assertThat(res.getVertices()).as("no vertices in the resulting graph after resolver").isNotNull();
        assertThat(res.getExcidentEdges(v1)).as("no edges in the resulting graph after resolver").isNotNull();

        assertThat(res.getVertices().size()).as("wrong # of vertices in the resulting graph after resolver").isEqualTo(4);
        assertThat(res.getExcidentEdges(v1).size()).as("wrong # of excident edges in the resulting graph entry after resolver").isEqualTo(2);

        assertThat(res.getIncidentEdges(v2).size()).as("wrong # of v2 incident edges in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v2).get(0).getVersion()).as("wrong edge v1-v2 in the resulting graph after resolver").isEqualTo("1.2");

        assertThat(res.getIncidentEdges(v3).size()).as("wrong # of edges v1-v3 in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v3).get(0).getVersion()).as("wrong edge v1-v3 in the resulting graph after resolver").isEqualTo("1.1");

        assertThat(res.getIncidentEdges(v4).size()).as("wrong # of edges v3-v4 in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v4).get(0).getVersion()).as("wrong edge v3-v4 in the resulting graph after resolver").isEqualTo("1.1");
    }

    // ------------------------------------------------------------------------------------------
    @Test
    void testResolution() throws Exception {
        MetadataGraph res;

        res = resolver.resolveConflicts(graph, ArtifactScopeEnum.test);

        assertThat(res).as("null graph after resolver").isNotNull();
        assertThat(res.getVertices()).as("no vertices in the resulting graph after resolver").isNotNull();
        assertThat(res.getExcidentEdges(v1)).as("no edges in the resulting graph after resolver").isNotNull();

        assertThat(res.getVertices().size()).as("wrong # of vertices in the resulting graph after resolver").isEqualTo(4);
        assertThat(res.getExcidentEdges(v1).size()).as("wrong # of excident edges in the resulting graph entry after resolver").isEqualTo(2);

        assertThat(res.getIncidentEdges(v2).size()).as("wrong # of v2 incident edges in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v2).get(0).getVersion()).as("wrong edge v1-v2 in the resulting graph after resolver").isEqualTo("1.2");

        assertThat(res.getIncidentEdges(v3).size()).as("wrong # of edges v1-v3 in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v3).get(0).getVersion()).as("wrong edge v1-v3 in the resulting graph after resolver").isEqualTo("1.1");

        assertThat(res.getIncidentEdges(v4).size()).as("wrong # of edges v3-v4 in the resulting graph after resolver").isEqualTo(1);
        assertThat(res.getIncidentEdges(v4).get(0).getVersion()).as("wrong edge v3-v4 in the resulting graph after resolver").isEqualTo("1.2");
    }
    // ------------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------------
}
