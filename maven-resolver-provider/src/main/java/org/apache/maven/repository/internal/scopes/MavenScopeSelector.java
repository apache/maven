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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ConflictContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ConflictItem;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeSelector;

/**
 * A scope selector for use with {@link ConflictResolver} that supports the scopes from {@link MavenDependencyScopes}.
 * In general, this selector picks the widest scope present among conflicting dependencies where e.g. "compile" is
 * wider than "runtime" which is wider than "test". If however a direct dependency is involved, its scope is selected.
 *
 * @since 4.0.0
 */
public final class MavenScopeSelector extends ScopeSelector {

    public MavenScopeSelector() {}

    @Override
    public void selectScope(ConflictContext context) throws RepositoryException {
        String scope = context.getWinner().getDependency().getScope();
        if (!MavenDependencyScopes.SYSTEM.equals(scope)) {
            scope = chooseEffectiveScope(context.getItems());
        }
        context.setScope(scope);
    }

    private String chooseEffectiveScope(Collection<ConflictItem> items) {
        Set<String> scopes = new HashSet<>();
        for (ConflictItem item : items) {
            if (item.getDepth() <= 1) {
                return item.getDependency().getScope();
            }
            scopes.addAll(item.getScopes());
        }
        return chooseEffectiveScope(scopes);
    }

    private String chooseEffectiveScope(Set<String> scopes) {
        if (scopes.size() > 1) {
            scopes.remove(MavenDependencyScopes.SYSTEM);
        }

        String effectiveScope = "";

        if (scopes.size() == 1) {
            effectiveScope = scopes.iterator().next();
        } else if (scopes.contains(MavenDependencyScopes.COMPILE)) {
            effectiveScope = MavenDependencyScopes.COMPILE;
        } else if (scopes.contains(MavenDependencyScopes.RUNTIME)) {
            effectiveScope = MavenDependencyScopes.RUNTIME;
        } else if (scopes.contains(MavenDependencyScopes.PROVIDED)) {
            effectiveScope = MavenDependencyScopes.PROVIDED;
        } else if (scopes.contains(MavenDependencyScopes.TEST)) {
            effectiveScope = MavenDependencyScopes.TEST;
        }

        return effectiveScope;
    }
}
