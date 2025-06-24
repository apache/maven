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
package org.apache.maven.impl;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultNodeTest {

    @Test
    void testAsString() {
        InternalSession session = Mockito.mock(InternalSession.class);

        // Create a basic dependency node
        DefaultArtifact artifact = new DefaultArtifact("org.example:myapp:1.0");
        Dependency dependency = new Dependency(artifact, "compile");
        DefaultDependencyNode node = new DefaultDependencyNode(dependency);

        // Test non-verbose mode
        DefaultNode defaultNode = new DefaultNode(session, node, false);
        assertEquals("org.example:myapp:jar:1.0:compile", defaultNode.asString());

        // Test verbose mode with managed version
        node.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_VERSION, "0.9");
        node.setManagedBits(DependencyNode.MANAGED_VERSION);
        defaultNode = new DefaultNode(session, node, true);
        assertEquals("org.example:myapp:jar:1.0:compile (version managed from 0.9)", defaultNode.asString());

        // Test verbose mode with managed scope
        node.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_SCOPE, "runtime");
        node.setManagedBits(DependencyNode.MANAGED_VERSION | DependencyNode.MANAGED_SCOPE);
        defaultNode = new DefaultNode(session, node, true);
        assertEquals(
                "org.example:myapp:jar:1.0:compile (version managed from 0.9; scope managed from runtime)",
                defaultNode.asString());

        // Test verbose mode with conflict resolution
        DefaultDependencyNode winner =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("org.example:myapp:2.0"), "compile"));
        node.setData(ConflictResolver.NODE_DATA_WINNER, winner);
        node.setManagedBits(0);
        defaultNode = new DefaultNode(session, node, true);
        assertEquals("(org.example:myapp:jar:1.0:compile - omitted for conflict with 2.0)", defaultNode.asString());
    }
}
