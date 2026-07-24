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
package org.apache.maven.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Repository;
import org.apache.maven.api.services.ArtifactResolverRequest;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultArtifactResolver}, specifically the {@code toResult()} conversion
 * that maps Aether's {@link ArtifactResult} to the Maven API's {@link ArtifactResolverResult}.
 */
class DefaultArtifactResolverTest {

    @SuppressWarnings("deprecation")
    private LocalRepository newLocalRepository(String basedir) {
        return new LocalRepository(basedir);
    }

    /**
     * Verifies that {@link ArtifactResult#NO_REPOSITORY} entries in the mapped exceptions
     * do not leak into the Maven API as {@code null} keys. This is the root cause of
     * <a href="https://github.com/apache/maven/issues/12531">#12531</a>: plugins that walk
     * the dependency tree threw {@code IllegalArgumentException} because
     * {@code AbstractSession.getRepository()} did not handle the {@code NoRepository} sentinel.
     */
    @Test
    void toResultFiltersNoRepositoryFromMappedExceptions() {
        // Set up mocks — InternalSession mock IS-A Session, so InternalSession.from() cast works
        InternalSession session = mock(InternalSession.class);
        ArtifactResolverRequest request = mock(ArtifactResolverRequest.class);
        when(request.getSession()).thenReturn(session);

        DefaultArtifact aetherArtifact = new DefaultArtifact("g:a:1.0");
        ArtifactCoordinates coordinates = mock(ArtifactCoordinates.class);
        org.apache.maven.api.Artifact mavenArtifact = mock(org.apache.maven.api.Artifact.class);
        when(mavenArtifact.toCoordinates()).thenReturn(coordinates);
        doReturn(mavenArtifact).when(session).getArtifact(any(org.eclipse.aether.artifact.Artifact.class));

        // NO_REPOSITORY should map to Optional.empty()
        doReturn(Optional.empty()).when(session).getRepository(eq(ArtifactResult.NO_REPOSITORY));

        // A real local repository should map to a proper Repository
        LocalRepository localRepo = newLocalRepository("/tmp/repo");
        Repository mavenLocalRepo = mock(Repository.class);
        doReturn(Optional.of(mavenLocalRepo)).when(session).getRepository(eq((ArtifactRepository) localRepo));

        // Create an ArtifactResult with exceptions under both NO_REPOSITORY and a real repository
        ArtifactRequest artRequest = new ArtifactRequest();
        artRequest.setArtifact(aetherArtifact);
        ArtifactResult aetherResult = new ArtifactResult(artRequest);
        aetherResult.addException(
                ArtifactResult.NO_REPOSITORY, new ArtifactNotFoundException(aetherArtifact, (String) null));
        aetherResult.addException(localRepo, new ArtifactNotFoundException(aetherArtifact, (String) null));

        // Convert
        DefaultArtifactResolver resolver = new DefaultArtifactResolver();
        DefaultArtifactResolver.ResolverResult resolverResult =
                new DefaultArtifactResolver.ResolverResult(null, aetherResult);
        ArtifactResolverResult result = resolver.toResult(request, Stream.of(resolverResult));

        // Verify the result
        ArtifactResolverResult.ResultItem item = result.getResult(coordinates);

        // The exceptions map should NOT contain a null key
        Map<Repository, List<Exception>> exceptions = item.getExceptions();
        assertFalse(exceptions.containsKey(null), "Exceptions map should not contain null key from NO_REPOSITORY");

        // The real repository's exceptions should still be present
        assertTrue(exceptions.containsKey(mavenLocalRepo), "Exceptions map should contain the real repository");
        assertEquals(1, exceptions.get(mavenLocalRepo).size());

        // isMissing() should still return true (it considers ALL exceptions, including NO_REPOSITORY ones)
        assertTrue(item.isMissing(), "isMissing() should consider exceptions from NO_REPOSITORY");
        assertFalse(item.isResolved());
        assertNull(item.getRepository());
    }

    /**
     * Verifies that {@code isMissing()} returns {@code false} when a NO_REPOSITORY exception
     * is NOT an {@link ArtifactNotFoundException}. Even though the NO_REPOSITORY exception
     * is filtered from the mapped exceptions map, it must still be checked by {@code isMissing()}.
     */
    @Test
    void isMissingReturnsFalseForNonNotFoundExceptionUnderNoRepository() {
        InternalSession session = mock(InternalSession.class);
        ArtifactResolverRequest request = mock(ArtifactResolverRequest.class);
        when(request.getSession()).thenReturn(session);

        DefaultArtifact aetherArtifact = new DefaultArtifact("g:a:1.0");
        ArtifactCoordinates coordinates = mock(ArtifactCoordinates.class);
        org.apache.maven.api.Artifact mavenArtifact = mock(org.apache.maven.api.Artifact.class);
        when(mavenArtifact.toCoordinates()).thenReturn(coordinates);
        doReturn(mavenArtifact).when(session).getArtifact(any(org.eclipse.aether.artifact.Artifact.class));
        doReturn(Optional.empty()).when(session).getRepository(eq(ArtifactResult.NO_REPOSITORY));

        // Create an ArtifactResult with a RuntimeException under NO_REPOSITORY
        ArtifactRequest artRequest = new ArtifactRequest();
        artRequest.setArtifact(aetherArtifact);
        ArtifactResult aetherResult = new ArtifactResult(artRequest);
        aetherResult.addException(ArtifactResult.NO_REPOSITORY, new RuntimeException("some error"));

        DefaultArtifactResolver resolver = new DefaultArtifactResolver();
        DefaultArtifactResolver.ResolverResult resolverResult =
                new DefaultArtifactResolver.ResolverResult(null, aetherResult);
        ArtifactResolverResult result = resolver.toResult(request, Stream.of(resolverResult));

        ArtifactResolverResult.ResultItem item = result.getResult(coordinates);

        // The exceptions map should be empty (NO_REPOSITORY filtered out)
        assertTrue(item.getExceptions().isEmpty(), "Exceptions map should be empty after filtering NO_REPOSITORY");

        // isMissing() should return false because the exception is NOT ArtifactNotFoundException
        assertFalse(item.isMissing(), "isMissing() should return false for non-ArtifactNotFoundException");
    }

    /**
     * Verifies that a resolved artifact with NO_REPOSITORY exceptions is properly handled.
     */
    @Test
    void resolvedArtifactWithNoRepositoryExceptions() {
        InternalSession session = mock(InternalSession.class);
        ArtifactResolverRequest request = mock(ArtifactResolverRequest.class);
        when(request.getSession()).thenReturn(session);

        Path artifactPath = Path.of("/tmp/artifact.jar");
        org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact("g:a:1.0").setPath(artifactPath);
        ArtifactCoordinates coordinates = mock(ArtifactCoordinates.class);
        org.apache.maven.api.Artifact mavenArtifact = mock(org.apache.maven.api.Artifact.class);
        DownloadedArtifact downloadedArtifact = mock(DownloadedArtifact.class);
        when(mavenArtifact.toCoordinates()).thenReturn(coordinates);
        doReturn(mavenArtifact).when(session).getArtifact(any(org.eclipse.aether.artifact.Artifact.class));
        doReturn(downloadedArtifact).when(session).getArtifact(any(Class.class), any());
        doReturn(Optional.empty()).when(session).getRepository(eq(ArtifactResult.NO_REPOSITORY));

        LocalRepository localRepo = newLocalRepository("/tmp/repo");
        Repository mavenLocalRepo = mock(Repository.class);
        doReturn(Optional.of(mavenLocalRepo)).when(session).getRepository(eq((ArtifactRepository) localRepo));

        ArtifactRequest artRequest = new ArtifactRequest();
        artRequest.setArtifact(new DefaultArtifact("g:a:1.0"));
        ArtifactResult aetherResult = new ArtifactResult(artRequest);
        aetherResult.setArtifact(aetherArtifact);
        aetherResult.setRepository(localRepo);
        // Add an exception under NO_REPOSITORY (can happen even on successful resolution)
        aetherResult.addException(
                ArtifactResult.NO_REPOSITORY, new ArtifactNotFoundException(aetherArtifact, (String) null));

        DefaultArtifactResolver resolver = new DefaultArtifactResolver();
        DefaultArtifactResolver.ResolverResult resolverResult =
                new DefaultArtifactResolver.ResolverResult(null, aetherResult);
        ArtifactResolverResult result = resolver.toResult(request, Stream.of(resolverResult));

        ArtifactResolverResult.ResultItem item = result.getResult(coordinates);

        // Should be resolved (has a path)
        assertTrue(item.isResolved());
        // Should NOT be missing (it's resolved)
        assertFalse(item.isMissing());
        // Exceptions map should not have null keys
        assertFalse(item.getExceptions().containsKey(null));
    }
}
