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
package org.apache.maven.repository.internal.scopes;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

import static java.util.Objects.requireNonNull;

/**
 * A dependency graph transformer that refines the request context for nodes that belong to the "project" context by
 * appending the buildpath type to which the node belongs. For instance, a compile-time project dependency will be
 * assigned the request context "project/compile".
 *
 * @see DependencyNode#getRequestContext()
 *
 * @since 4.0.0
 */
public final class MavenDependencyContextRefiner implements DependencyGraphTransformer {

    public MavenDependencyContextRefiner() {}

    @Override
    public DependencyNode transformGraph(DependencyNode node, DependencyGraphTransformationContext context)
            throws RepositoryException {
        requireNonNull(node, "node cannot be null");
        requireNonNull(context, "context cannot be null");
        String ctx = node.getRequestContext();

        if ("project".equals(ctx)) {
            String scope = getBuildpathScope(node);
            if (scope != null) {
                ctx += '/' + scope;
                node.setRequestContext(ctx);
            }
        }

        for (DependencyNode child : node.getChildren()) {
            transformGraph(child, context);
        }

        return node;
    }

    private String getBuildpathScope(DependencyNode node) {
        Dependency dependency = node.getDependency();
        if (dependency == null) {
            return null;
        }

        String scope = dependency.getScope();

        if (MavenDependencyScopes.COMPILE.equals(scope)
                || MavenDependencyScopes.SYSTEM.equals(scope)
                || MavenDependencyScopes.PROVIDED.equals(scope)) {
            return MavenDependencyScopes.COMPILE;
        } else if (MavenDependencyScopes.RUNTIME.equals(scope)) {
            return MavenDependencyScopes.RUNTIME;
        } else if (MavenDependencyScopes.TEST.equals(scope)) {
            return MavenDependencyScopes.TEST;
        }

        return null;
    }
}
