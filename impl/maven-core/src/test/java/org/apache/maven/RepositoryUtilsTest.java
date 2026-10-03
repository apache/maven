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
package org.apache.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RepositoryUtilsTest {

    @Test
    void testToArtifactMethodsReturnNullWhenInputParameterIsNull() {
        assertNull(RepositoryUtils.toArtifact((Dependency) null));
        assertNull(RepositoryUtils.toArtifact((Artifact) null));
        assertNull(RepositoryUtils.toArtifact((org.apache.maven.artifact.Artifact) null));
    }

    public void testToArtifactsCollectionOrderedByNodeDepth(RepositoryUtils.ArtifactOrdering ordering) {
        List<org.apache.maven.artifact.Artifact> artifacts = new ArrayList<>();

        DependencyNode root = createDependencyTree();

        List<String> trail = new ArrayList<>();
        DependencyFilter filter = null;

        RepositoryUtils.toArtifacts(artifacts, root.getChildren(), trail, filter);

        String expected =
                "[gid:zlevel1:jar:1:, gid:ylevel1:jar:1:, gid:xlevel1:jar:1:, gid:alevel2:jar:1:, gid:blevel2:jar:1:, gid:clevel2:jar:1:, gid:alevel3:jar:1:]";
        assertEquals(expected, artifacts.toString());

        String[][] expectedTrails = {
            {"gid:zlevel1:jar:1"},
            {"gid:ylevel1:jar:1"},
            {"gid:xlevel1:jar:1"},
            {"gid:zlevel1:jar:1", "gid:alevel2:jar:1"},
            {"gid:ylevel1:jar:1", "gid:blevel2:jar:1"},
            {"gid:xlevel1:jar:1", "gid:clevel2:jar:1"},
            {"gid:ylevel1:jar:1", "gid:blevel2:jar:1", "gid:alevel3:jar:1"}
        };

        assertDependencyTrails(artifacts, expectedTrails);
    }

    public void testToArtifactsCollectionOrderedByNodeDepthWithFilter(RepositoryUtils.ArtifactOrdering ordering) {
        List<org.apache.maven.artifact.Artifact> artifacts = new ArrayList<>();

        DependencyNode root = createDependencyTree();

        List<String> trail = new ArrayList<>();
        DependencyFilter filter = new DependencyFilter() {
            @Override
            public boolean accept(DependencyNode node, List<DependencyNode> parents) {
                // accept node if artifactId does NOT contain "level2"
                return !node.getArtifact().getArtifactId().contains("level2");
            }
        };

        RepositoryUtils.toArtifacts(artifacts, root.getChildren(), trail, filter);

        String expected = "[gid:zlevel1:jar:1:, gid:ylevel1:jar:1:, gid:xlevel1:jar:1:, gid:alevel3:jar:1:]";
        assertEquals(expected, artifacts.toString());

        String[][] expectedTrails = {
            {"gid:zlevel1:jar:1"},
            {"gid:ylevel1:jar:1"},
            {"gid:xlevel1:jar:1"},
            {"gid:ylevel1:jar:1", "gid:blevel2:jar:1", "gid:alevel3:jar:1"}
        };

        assertDependencyTrails(artifacts, expectedTrails);
    }

    private void assertDependencyTrails(List<Artifact> artifacts, String[][] expectedTrails) {
        assertEquals(
                expectedTrails.length,
                artifacts.size(),
                "Expected " + expectedTrails.length + " artifacts but got " + artifacts.size());

        for (int i = 0; i < artifacts.size(); i++) {
            Artifact artifact = artifacts.get(i);
            assertIterableEquals(
                    List.of(expectedTrails[i]),
                    artifact.getDependencyTrail(),
                    "Wrong dependency trail for artifact at index " + i + " (" + artifact.getId() + ")");
        }
    }

    /**
     * Create a dependency tree.
     *
     * gid:root:1
     * +- gid:zlevel1:1
     * |  \\- gid:alevel2:1
     * \\- gid:ylevel1:1
     * |  \\- gid:blevel2:1
     * |      \\- gid:alevel3:1
     * \\- gid:xlevel1:1
     *    \\- gid:clevel2:1
     *
     * @return the root node or the tree.
     */
    private DependencyNode createDependencyTree() {
        return TreeNode.of(
                        "gid:root:1",
                        TreeNode.of("gid:zlevel1:1", TreeNode.of("gid:alevel2:1")),
                        TreeNode.of("gid:ylevel1:1", TreeNode.of("gid:blevel2:1", TreeNode.of("gid:alevel3:1"))),
                        TreeNode.of("gid:xlevel1:1", TreeNode.of("gid:clevel2:1")))
                .build();
    }

    /**
     * DependencyNode tree builder.
     */
    public static class TreeNode {
        private final String coords;
        private final List<TreeNode> children = new ArrayList<>();

        private TreeNode(String coords) {
            this.coords = coords;
        }

        public static TreeNode of(String coords, TreeNode... children) {
            TreeNode node = new TreeNode(coords);
            node.children.addAll(Arrays.asList(children));
            return node;
        }

        /**
         * Recursively converts this spec into a real DependencyNode tree.
         */
        public DependencyNode build() {
            Dependency dependency = new Dependency(new DefaultArtifact(coords), null);
            DefaultDependencyNode node = new DefaultDependencyNode(dependency);

            if (!children.isEmpty()) {
                List<DependencyNode> built = new ArrayList<>(children.size());
                for (TreeNode child : children) {
                    built.add(child.build()); // recursion happens here
                }
                node.setChildren(built);
            }
            return node;
        }
    }
}
