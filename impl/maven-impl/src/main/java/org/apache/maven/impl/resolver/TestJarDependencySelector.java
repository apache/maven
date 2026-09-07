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

import java.util.Objects;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Type;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

import static java.util.Objects.requireNonNull;

/**
 * Preserves Maven's normal non-transitive test-scope semantics while allowing the direct test-scoped dependencies of
 * a test JAR to participate in dependency collection. Those dependencies are part of the test artifact's classpath
 * contract, not the producer's main artifact contract.
 */
final class TestJarDependencySelector implements DependencySelector {
    private final DependencySelector delegate;
    private final boolean testJarParent;

    TestJarDependencySelector(DependencySelector delegate) {
        this(delegate, false);
    }

    private TestJarDependencySelector(DependencySelector delegate, boolean testJarParent) {
        this.delegate = requireNonNull(delegate, "delegate cannot be null");
        this.testJarParent = testJarParent;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        requireNonNull(dependency, "dependency cannot be null");
        if (testJarParent && DependencyScope.TEST.id().equals(dependency.getScope())) {
            return true;
        }
        return delegate.selectDependency(dependency);
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        requireNonNull(context, "context cannot be null");

        DependencySelector childDelegate = delegate.deriveChildSelector(context);
        Dependency parent = context.getDependency();
        boolean childOfTestJar = parent != null
                && Type.TEST_JAR.equals(parent.getArtifact().getProperty(ArtifactProperties.TYPE, ""));

        if (childDelegate == delegate && childOfTestJar == testJarParent) {
            return this;
        }
        return new TestJarDependencySelector(childDelegate, childOfTestJar);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TestJarDependencySelector that)) {
            return false;
        }
        return testJarParent == that.testJarParent && delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, testJarParent);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[delegate=" + delegate + ", testJarParent=" + testJarParent + ']';
    }
}
