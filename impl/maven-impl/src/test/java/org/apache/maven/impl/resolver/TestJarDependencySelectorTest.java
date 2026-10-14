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
package org.apache.maven.impl.resolver;

import java.util.List;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Type;
import org.apache.maven.impl.resolver.type.DefaultTypeProvider;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.internal.impl.scope.OptionalDependencySelector;
import org.eclipse.aether.internal.impl.scope.ScopeDependencySelector;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestJarDependencySelectorTest {

    private final ArtifactType testJarType = new DefaultTypeProvider()
            .types().stream()
                    .filter(type -> Type.TEST_JAR.equals(type.id()))
                    .findFirst()
                    .orElseThrow()
                    .toArtifactType();

    @Test
    void allowsDirectTestDependenciesOfTestJar() {
        DependencySelector selector = childrenOf(dependency(testJar("producer"), "test"));

        assertTrue(selector.selectDependency(dependency(jar("support"), "test")));
        assertTrue(selector.selectDependency(dependency(jar("compile-support"), "compile")));
    }

    @Test
    void keepsNestedTestJarSemantics() {
        DependencySelector selector = childrenOf(dependency(testJar("outer"), "test"));

        Dependency nested = dependency(testJar("nested"), "test");
        assertTrue(selector.selectDependency(nested));
        selector = selector.deriveChildSelector(context(nested));

        assertTrue(selector.selectDependency(dependency(jar("nested-support"), "test")));
    }

    @Test
    void stillHonorsOptionalAndExclusionSelectors() {
        Dependency parent =
                new Dependency(testJar("producer"), "test", false, List.of(new Exclusion("g", "excluded", "*", "*")));
        DependencySelector selector = childrenOf(parent);

        assertFalse(selector.selectDependency(new Dependency(jar("optional"), "test", true)));
        assertFalse(selector.selectDependency(dependency(new DefaultArtifact("g:excluded:1"), "test")));
        assertTrue(selector.selectDependency(dependency(jar("support"), "test")));
    }

    private DependencySelector childrenOf(Dependency parent) {
        DependencySelector selector = newSelector();
        selector = selector.deriveChildSelector(context(null));
        return selector.deriveChildSelector(context(parent));
    }

    private DependencySelector newSelector() {
        return new AndDependencySelector(
                new TestJarDependencySelector(ScopeDependencySelector.legacy(
                        null, List.of(DependencyScope.TEST.id(), DependencyScope.PROVIDED.id()))),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector());
    }

    private DependencyCollectionContext context(Dependency dependency) {
        DependencyCollectionContext context = mock(DependencyCollectionContext.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getSession()).thenReturn(new DefaultRepositorySystemSession(h -> false));
        return context;
    }

    private Dependency dependency(Artifact artifact, String scope) {
        return new Dependency(artifact, scope);
    }

    private Artifact testJar(String artifactId) {
        return new DefaultArtifact("g:" + artifactId + ":1", testJarType);
    }

    private Artifact jar(String artifactId) {
        return new DefaultArtifact("g:" + artifactId + ":1");
    }
}
