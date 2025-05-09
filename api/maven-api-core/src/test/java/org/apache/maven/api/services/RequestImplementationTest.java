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
package org.apache.maven.api.services;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequestImplementationTest {

    @Test
    void artifactResolverRequestEquality() {
        Session session = mock(Session.class);
        ArtifactCoordinates coords1 = mock(ArtifactCoordinates.class);
        ArtifactCoordinates coords2 = mock(ArtifactCoordinates.class);
        RemoteRepository repo1 = mock(RemoteRepository.class);
        RemoteRepository repo2 = mock(RemoteRepository.class);

        List<RemoteRepository> repositories1 = Arrays.asList(repo1, repo2);
        List<RemoteRepository> repositories2 = Arrays.asList(repo1, repo2);

        ArtifactResolverRequest.ArtifactResolverRequestBuilder builder = ArtifactResolverRequest.builder();

        ArtifactResolverRequest request1 = builder.session(session)
                .coordinates(Arrays.asList(coords1, coords2))
                .repositories(repositories1)
                .build();

        ArtifactResolverRequest request2 = builder.session(session)
                .coordinates(Arrays.asList(coords1, coords2))
                .repositories(repositories2)
                .build();

        ArtifactResolverRequest request3 = builder.session(session)
                .coordinates(Arrays.asList(coords2, coords1)) // Different order
                .repositories(repositories1)
                .build();

        // Test equals and hashCode
        assertThat(request2).isEqualTo(request1);
        assertThat(request2.hashCode()).isEqualTo(request1.hashCode());
        assertThat(request3).isNotEqualTo(request1);

        // Test toString
        String toString = request1.toString();
        assertThat(toString.contains("coordinates=")).isTrue();
        assertThat(toString.contains("repositories=")).isTrue();
    }

    @Test
    void requestTraceIntegration() {
        Session session = mock(Session.class);
        RequestTrace trace = new RequestTrace("test-context", null, "test-data");

        ArtifactInstallerRequest request =
                ArtifactInstallerRequest.builder().session(session).trace(trace).build();

        assertThat(request.getTrace()).isEqualTo(trace);
        assertThat(request.getSession()).isEqualTo(session);
    }

    @Test
    void dependencyResolverRequestEquality() {
        Session session = mock(Session.class);

        DependencyResolverRequest.DependencyResolverRequestBuilder builder = DependencyResolverRequest.builder();
        DependencyResolverRequest request1 = builder.session(session)
                .requestType(DependencyResolverRequest.RequestType.COLLECT)
                .pathScope(PathScope.MAIN_COMPILE)
                .build();

        DependencyResolverRequest request2 = builder.session(session)
                .requestType(DependencyResolverRequest.RequestType.COLLECT)
                .pathScope(PathScope.MAIN_COMPILE)
                .build();

        DependencyResolverRequest request3 = builder.session(session)
                .requestType(DependencyResolverRequest.RequestType.RESOLVE)
                .pathScope(PathScope.MAIN_COMPILE)
                .build();

        assertThat(request2).isEqualTo(request1);
        assertThat(request2.hashCode()).isEqualTo(request1.hashCode());
        assertThat(request3).isNotEqualTo(request1);

        String toString = request1.toString();
        assertThat(toString.contains("requestType=")).isTrue();
        assertThat(toString.contains("pathScope=")).isTrue();
    }
}
