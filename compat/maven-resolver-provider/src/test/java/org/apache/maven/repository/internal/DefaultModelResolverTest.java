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
package org.apache.maven.repository.internal;

import java.net.MalformedURLException;
import java.util.Arrays;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * Test cases for the default {@code ModelResolver} implementation.
 *
 * @since 3.5.0
 */
final class DefaultModelResolverTest extends AbstractRepositoryTestCase {

    /**
     * Creates a new {@code DefaultModelResolverTest} instance.
     */
    DefaultModelResolverTest() {
        super();
    }

    @Test
    void resolveParentThrowsUnresolvableModelExceptionWhenNotFound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("0");

        UnresolvableModelException e = assertThatExceptionOfType(UnresolvableModelException.class).as("Expected 'UnresolvableModelException' not thrown.").isThrownBy(() -> newModelResolver().resolveModel(parent)).actual();
        assertThat(e.getMessage()).isNotNull();
        assertThat(e.getMessage().contains("Could not find artifact ut.simple:artifact:pom:0 in repo")).isTrue();
    }

    @Test
    void resolveParentThrowsUnresolvableModelExceptionWhenNoMatchingVersionFound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("[2.0,2.1)");

        UnresolvableModelException e = assertThatExceptionOfType(UnresolvableModelException.class).as("Expected 'UnresolvableModelException' not thrown.").isThrownBy(() -> newModelResolver().resolveModel(parent)).actual();
        assertThat(e.getMessage()).isNotNull();
        assertThat(e.getMessage()).isEqualTo("No versions matched the requested parent version range '[2.0,2.1)'");
    }

    @Test
    void resolveParentThrowsUnresolvableModelExceptionWhenUsingRangesWithoutUpperBound() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("[1.0,)");

        UnresolvableModelException e = assertThatExceptionOfType(UnresolvableModelException.class).as("Expected 'UnresolvableModelException' not thrown.").isThrownBy(() -> newModelResolver().resolveModel(parent)).actual();
        assertThat(e.getMessage()).isEqualTo("The requested parent version range '[1.0,)' does not specify an upper bound");
    }

    @Test
    void resolveParentSuccessfullyResolvesExistingParentWithoutRange() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("1.0");

        assertThat(this.newModelResolver().resolveModel(parent)).isNotNull();
        assertThat(parent.getVersion()).isEqualTo("1.0");
    }

    @Test
    void resolveParentSuccessfullyResolvesExistingParentUsingHighestVersion() throws Exception {
        final Parent parent = new Parent();
        parent.setGroupId("ut.simple");
        parent.setArtifactId("artifact");
        parent.setVersion("(,2.0)");

        assertThat(this.newModelResolver().resolveModel(parent)).isNotNull();
        assertThat(parent.getVersion()).isEqualTo("1.0");
    }

    @Test
    void resolveDependencyThrowsUnresolvableModelExceptionWhenNotFound() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("0");

        UnresolvableModelException e = assertThatExceptionOfType(UnresolvableModelException.class).as("Expected 'UnresolvableModelException' not thrown.").isThrownBy(() -> newModelResolver().resolveModel(dependency)).actual();
        assertThat(e.getMessage()).isNotNull();
        assertThat(e.getMessage().contains("Could not find artifact ut.simple:artifact:pom:0 in repo")).isTrue();
    }

    @Test
    void resolveDependencyThrowsUnresolvableModelExceptionWhenNoMatchingVersionFound() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("[2.0,2.1)");

        UnresolvableModelException e = assertThatExceptionOfType(UnresolvableModelException.class).as("Expected 'UnresolvableModelException' not thrown.").isThrownBy(() -> newModelResolver().resolveModel(dependency)).actual();
        assertThat(e.getMessage()).isEqualTo("No versions matched the requested dependency version range '[2.0,2.1)'");
    }

    @Test
    void resolveDependencyThrowsUnresolvableModelExceptionWhenUsingRangesWithoutUpperBound() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("[1.0,)");

        UnresolvableModelException e = assertThatExceptionOfType(UnresolvableModelException.class).as("Expected 'UnresolvableModelException' not thrown.").isThrownBy(() -> newModelResolver().resolveModel(dependency)).actual();
        assertThat(e.getMessage()).isEqualTo("The requested dependency version range '[1.0,)' does not specify an upper bound");
    }

    @Test
    void resolveDependencySuccessfullyResolvesExistingDependencyWithoutRange() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("1.0");

        assertThat(this.newModelResolver().resolveModel(dependency)).isNotNull();
        assertThat(dependency.getVersion()).isEqualTo("1.0");
    }

    @Test
    void resolveDependencySuccessfullyResolvesExistingDependencyUsingHighestVersion() throws Exception {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("ut.simple");
        dependency.setArtifactId("artifact");
        dependency.setVersion("(,2.0)");

        assertThat(this.newModelResolver().resolveModel(dependency)).isNotNull();
        assertThat(dependency.getVersion()).isEqualTo("1.0");
    }

    private ModelResolver newModelResolver() throws ComponentLookupException, MalformedURLException {
        return new DefaultModelResolver(
                this.session,
                null,
                this.getClass().getName(),
                getContainer().lookup(ArtifactResolver.class),
                getContainer().lookup(VersionRangeResolver.class),
                getContainer().lookup(RemoteRepositoryManager.class),
                Arrays.asList(newTestRepository()));
    }
}
