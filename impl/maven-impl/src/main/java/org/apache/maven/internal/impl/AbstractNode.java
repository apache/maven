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
package org.apache.maven.internal.impl;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.api.Node;
import org.apache.maven.api.NodeVisitor;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

public abstract class AbstractNode implements Node {

    abstract org.eclipse.aether.graph.DependencyNode getDependencyNode();

    @Override
    public boolean accept(NodeVisitor visitor) {
        if (visitor.enter(this)) {
            for (Node child : getChildren()) {
                if (!child.accept(visitor)) {
                    break;
                }
            }
        }
        return visitor.leave(this);
    }

    @Override
    public Node filter(Predicate<Node> filter) {
        List<Node> children =
                getChildren().stream().filter(filter).map(n -> n.filter(filter)).collect(Collectors.toList());
        return new WrapperNode(this, Collections.unmodifiableList(children));
    }

    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder();

        DependencyNode node = getDependencyNode();
        Artifact artifact = node.getArtifact();
        sb.append(artifact);

        Dependency dependency = node.getDependency();
        if (dependency != null) {
            sb.append(":").append(dependency.getScope());
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getDependencyNode().toString();
    }
}
