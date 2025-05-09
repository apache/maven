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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.apache.maven.impl.standalone.ApiRunner;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.transport.apache.ApacheTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Test cases for the project {@code ModelResolver} implementation.
 */
class DefaultModelResolverTest {

    Session session;

    @BeforeEach
    void setup() {
        Path basedir = Paths.get(System.getProperty("basedir", ""));
        Path localRepoPath = basedir.resolve("target/local-repo");
        Path remoteRepoPath = basedir.resolve("src/test/remote-repo");
        Session s = ApiRunner.createSession(
                injector -> injector.bindInstance(DefaultModelResolverTest.class, this), localRepoPath);
        RemoteRepository remoteRepository = s.createRemoteRepository(
                RemoteRepository.CENTRAL_ID, remoteRepoPath.toUri().toString());
        session = s.withRemoteRepositories(List.of(remoteRepository));
    }

    @Test
    void resolveParentThrowsModelResolverExceptionWhenNotFound() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("0")
                .build();

        ModelResolverException e = assertThatExceptionOfType(ModelResolverException.class)
                .as("Expected 'ModelResolverException' not thrown.")
                .isThrownBy(() -> newModelResolver().resolveModel(session, null, parent, new AtomicReference<>()))
                .actual();
        assertThat(e.getMessage()).isNotNull();
        assertThat(e.getMessage(), containsString("Could not find artifact org.apache:apache:pom:0 in central"));
    }

    @Test
    void resolveParentThrowsModelResolverExceptionWhenNoMatchingVersionFound() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("[2.0,2.1)")
                .build();

        ModelResolverException e = assertThatExceptionOfType(ModelResolverException.class)
                .as("Expected 'ModelResolverException' not thrown.")
                .isThrownBy(() -> newModelResolver().resolveModel(session, null, parent, new AtomicReference<>()))
                .actual();
        assertThat(e.getMessage()).isEqualTo("No versions matched the requested parent version range '[2.0,2.1)'");
    }

    @Test
    void resolveParentThrowsModelResolverExceptionWhenUsingRangesWithoutUpperBound() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("[1,)")
                .build();

        ModelResolverException e = assertThatExceptionOfType(ModelResolverException.class)
                .as("Expected 'ModelResolverException' not thrown.")
                .isThrownBy(() -> newModelResolver().resolveModel(session, null, parent, new AtomicReference<>()))
                .actual();
        assertThat(e.getMessage())
                .isEqualTo("The requested parent version range '[1,)' does not specify an upper bound");
    }

    @Test
    void resolveParentSuccessfullyResolvesExistingParentWithoutRange() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("1")
                .build();

        assertThat(this.newModelResolver().resolveModel(session, null, parent, new AtomicReference<>()))
                .isNotNull();
        assertThat(parent.getVersion()).isEqualTo("1");
    }

    @Test
    void resolveParentSuccessfullyResolvesExistingParentUsingHighestVersion() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("(,2.0)")
                .build();

        AtomicReference<org.apache.maven.api.model.Parent> modified = new AtomicReference<>();
        assertThat(this.newModelResolver().resolveModel(session, null, parent, modified))
                .isNotNull();
        assertThat(modified.get().getVersion()).isEqualTo("1");
    }

    @Test
    void resolveDependencyThrowsModelResolverExceptionWhenNotFound() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("0")
                .build();

        ModelResolverException e = assertThatExceptionOfType(ModelResolverException.class)
                .as("Expected 'ModelResolverException' not thrown.")
                .isThrownBy(() -> newModelResolver().resolveModel(session, null, dependency, new AtomicReference<>()))
                .actual();
        assertThat(e.getMessage()).isNotNull();
        assertThat(e.getMessage(), containsString("Could not find artifact org.apache:apache:pom:0 in central"));
    }

    @Test
    void resolveDependencyThrowsModelResolverExceptionWhenNoMatchingVersionFound() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("[2.0,2.1)")
                .build();

        ModelResolverException e = assertThatExceptionOfType(ModelResolverException.class)
                .as("Expected 'ModelResolverException' not thrown.")
                .isThrownBy(() -> newModelResolver().resolveModel(session, null, dependency, new AtomicReference<>()))
                .actual();
        assertThat(e.getMessage()).isEqualTo("No versions matched the requested dependency version range '[2.0,2.1)'");
    }

    @Test
    void resolveDependencyThrowsModelResolverExceptionWhenUsingRangesWithoutUpperBound() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("[1,)")
                .build();

        ModelResolverException e = assertThatExceptionOfType(ModelResolverException.class)
                .as("Expected 'ModelResolverException' not thrown.")
                .isThrownBy(() -> newModelResolver().resolveModel(session, null, dependency, new AtomicReference<>()))
                .actual();
        assertThat(e.getMessage())
                .isEqualTo("The requested dependency version range '[1,)' does not specify an upper bound");
    }

    @Test
    void resolveDependencySuccessfullyResolvesExistingDependencyWithoutRange() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("1")
                .build();

        assertThat(this.newModelResolver().resolveModel(session, null, dependency, new AtomicReference<>()))
                .isNotNull();
        assertThat(dependency.getVersion()).isEqualTo("1");
    }

    @Test
    void resolveDependencySuccessfullyResolvesExistingDependencyUsingHighestVersion() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("(,2.0)")
                .build();

        AtomicReference<org.apache.maven.api.model.Dependency> modified = new AtomicReference<>();
        assertThat(this.newModelResolver().resolveModel(session, null, dependency, modified))
                .isNotNull();
        assertThat(modified.get().getVersion()).isEqualTo("1");
    }

    private ModelResolver newModelResolver() throws Exception {
        return new DefaultModelResolver();
    }

    @Provides
    @Named(FileTransporterFactory.NAME)
    static FileTransporterFactory newFileTransporterFactory() {
        return new FileTransporterFactory();
    }

    @Provides
    @Named(ApacheTransporterFactory.NAME)
    static ApacheTransporterFactory newApacheTransporterFactory(
            ChecksumExtractor checksumExtractor, PathProcessor pathProcessor) {
        return new ApacheTransporterFactory(checksumExtractor, pathProcessor);
    }
}
