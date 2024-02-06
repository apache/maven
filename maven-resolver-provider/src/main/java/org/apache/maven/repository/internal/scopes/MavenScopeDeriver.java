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
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeDeriver;

/**
 * A scope deriver for use with {@link ConflictResolver} that supports the scopes from {@link DefaultDependencyScopeManager}.
 *
 * @since 4.0.0
 */
public final class MavenScopeDeriver extends ScopeDeriver {

    /**
     * Creates a new instance of this scope deriver.
     */
    public MavenScopeDeriver() {}

    @Override
    public void deriveScope(ScopeContext context) throws RepositoryException {
        context.setDerivedScope(getDerivedScope(context.getParentScope(), context.getChildScope()));
    }

    private String getDerivedScope(String parentScope, String childScope) {
        String derivedScope;

        if (DefaultDependencyScopeManager.SYSTEM.is(childScope) || DefaultDependencyScopeManager.TEST.is(childScope)) {
            derivedScope = childScope;
        } else if (parentScope == null
                || parentScope.isEmpty()
                || DefaultDependencyScopeManager.COMPILE.is(parentScope)) {
            derivedScope = childScope;
        } else if (DefaultDependencyScopeManager.TEST.is(parentScope)
                || DefaultDependencyScopeManager.RUNTIME.is(parentScope)) {
            derivedScope = parentScope;
        } else if (DefaultDependencyScopeManager.SYSTEM.is(parentScope)
                || DefaultDependencyScopeManager.PROVIDED.is(parentScope)) {
            derivedScope = DefaultDependencyScopeManager.PROVIDED.id();
        } else {
            derivedScope = DefaultDependencyScopeManager.RUNTIME.id();
        }

        return derivedScope;
    }
}
