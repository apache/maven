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

import org.apache.maven.artifact.ArtifactScopeEnum;

/**
 * Resolves conflicts in the supplied dependency graph.
 * Different implementations will implement different conflict resolution policies.
 *
 * @author <a href="mailto:oleg@codehaus.org">Oleg Gusakov</a>
 */
public interface GraphConflictResolver {
    String ROLE = GraphConflictResolver.class.getName();

    /**
     * Cleanses the supplied graph by leaving only one directed versioned edge\
     * between any two nodes, if multiple exists. Uses scope relationships, defined
     * in <code>ArtifactScopeEnum</code>
     *
     * @param graph the "dirty" graph to be simplified via conflict resolution
     * @param scope scope for which the graph should be resolved
     *
     * @return resulting "clean" graph for the specified scope
     *
     * @since 3.0
     */
    MetadataGraph resolveConflicts(MetadataGraph graph, ArtifactScopeEnum scope)
            throws GraphConflictResolutionException;
}
