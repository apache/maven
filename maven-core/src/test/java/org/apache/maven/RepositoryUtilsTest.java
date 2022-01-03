package org.apache.maven;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.test.util.DependencyGraphParser;
import org.junit.jupiter.api.Test;

/**
 * Test RepositoryUtils
 *
 * @author Gabriel Belingueres
 */
public class RepositoryUtilsTest
{

    @Test
    public void testToArtifactsCollectionOrderedByNodeDepth()
    {
        Collection<org.apache.maven.artifact.Artifact> artifacts = new ArrayList<>();

        DependencyNode root = createDependencyTree();

        List<String> trail = new ArrayList<>();
        DependencyFilter filter = null;

        RepositoryUtils.toArtifacts( artifacts, root.getChildren(), trail, filter );

        String expected =
            "[gid:zlevel1:jar:1:, gid:ylevel1:jar:1:, gid:xlevel1:jar:1:, gid:alevel2:jar:1:, gid:blevel2:jar:1:, gid:clevel2:jar:1:, gid:alevel3:jar:1:]";
        assertEquals( expected, artifacts.toString() );
    }

    @Test
    public void testToArtifactsCollectionOrderedByNodeDepthWithFilter()
    {
        Collection<org.apache.maven.artifact.Artifact> artifacts = new ArrayList<>();

        DependencyNode root = createDependencyTree();

        List<String> trail = new ArrayList<>();
        DependencyFilter filter = new DependencyFilter()
        {
            @Override
            public boolean accept( DependencyNode node, List<DependencyNode> parents )
            {
                // accept node if artifactId does NOT contain "level2"
                return !node.getArtifact().getArtifactId().contains( "level2" );
            }
        };

        RepositoryUtils.toArtifacts( artifacts, root.getChildren(), trail, filter );

        String expected =
            "[gid:zlevel1:jar:1:, gid:ylevel1:jar:1:, gid:xlevel1:jar:1:, gid:alevel3:jar:1:]";
        assertEquals( expected, artifacts.toString() );
    }

    /**
     * Create a dependency tree.
     *
     * @return the root node or the tree.
     */
    private DependencyNode createDependencyTree()
    {
        String LS = System.lineSeparator();
        DependencyGraphParser parser = new DependencyGraphParser();
        String dependencyGraph =
                        "gid:root:1" + LS +
                        "+- gid:zlevel1:1" + LS +
                        "|  \\- gid:alevel2:1" + LS +
                        "\\- gid:ylevel1:1" + LS +
                        "|  \\- gid:blevel2:1" + LS +
                        "|      \\- gid:alevel3:1" + LS +
                        "\\- gid:xlevel1:1" + LS +
                        "   \\- gid:clevel2:1" + LS;

        try
        {
            return parser.parseLiteral( dependencyGraph );
        }
        catch ( IOException e )
        {
            fail( "Failed the parsing of the dependency node graph" );
        }
        return null;
    }

}
