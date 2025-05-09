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
package org.apache.maven.artifact.resolver.filter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ScopeArtifactFilter}.
 *
 */
class ScopeArtifactFilterTest {

    private Artifact newArtifact(String scope) {
        return new DefaultArtifact("g", "a", "1.0", scope, "jar", "", null);
    }

    @Test
    void includeCompile() {
        ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE);

        assertThat(filter.include(newArtifact(Artifact.SCOPE_COMPILE))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_SYSTEM))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_PROVIDED))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_RUNTIME))).isFalse();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_TEST))).isFalse();
    }

    @Test
    void includeCompilePlusRuntime() {
        ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE_PLUS_RUNTIME);

        assertThat(filter.include(newArtifact(Artifact.SCOPE_COMPILE))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_SYSTEM))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_PROVIDED))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_RUNTIME))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_TEST))).isFalse();
    }

    @Test
    void includeRuntime() {
        ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

        assertThat(filter.include(newArtifact(Artifact.SCOPE_COMPILE))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_SYSTEM))).isFalse();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_PROVIDED))).isFalse();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_RUNTIME))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_TEST))).isFalse();
    }

    @Test
    void includeRuntimePlusSystem() {
        ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME_PLUS_SYSTEM);

        assertThat(filter.include(newArtifact(Artifact.SCOPE_COMPILE))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_SYSTEM))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_PROVIDED))).isFalse();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_RUNTIME))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_TEST))).isFalse();
    }

    @Test
    void includeTest() {
        ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_TEST);

        assertThat(filter.include(newArtifact(Artifact.SCOPE_COMPILE))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_SYSTEM))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_PROVIDED))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_RUNTIME))).isTrue();
        assertThat(filter.include(newArtifact(Artifact.SCOPE_TEST))).isTrue();
    }
}
