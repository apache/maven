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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.RepositoryPolicy;
import org.apache.maven.api.services.VersionRangeResolverException;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.apache.maven.impl.standalone.ApiRunner;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.transport.apache.ApacheTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                injector -> {
                    injector.bindInstance(DefaultModelResolverTest.class, this);
                },
                localRepoPath);
        RemoteRepository remoteRepository = s.createRemoteRepository(
                RemoteRepository.CENTRAL_ID, remoteRepoPath.toUri().toString());
        session = s.withRemoteRepositories(List.of(remoteRepository));
    }

    @Test
    void testResolveParentThrowsModelResolverExceptionWhenNotFound() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("0")
                .build();

        ModelResolverException e = assertThrows(
                ModelResolverException.class,
                () -> newModelResolver().resolveModel(session, null, parent, new AtomicReference<>()),
                "Expected 'ModelResolverException' not thrown.");
        assertNotNull(e.getMessage());
        String message = e.getMessage();
        assertTrue(
                message.contains("Could not find artifact org.apache:apache:pom:0 in central"),
                "Expected exception message to contain artifact not found text but was: " + message);
    }

    @Test
    void testResolveParentThrowsModelResolverExceptionWhenNoMatchingVersionFound() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("[2.0,2.1)")
                .build();

        ModelResolverException e = assertThrows(
                ModelResolverException.class,
                () -> newModelResolver().resolveModel(session, null, parent, new AtomicReference<>()),
                "Expected 'ModelResolverException' not thrown.");
        assertEquals("No versions matched the requested parent version range '[2.0,2.1)'", e.getMessage());
    }

    @Test
    void testResolveParentThrowsModelResolverExceptionWhenUsingRangesWithoutUpperBound() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("[1,)")
                .build();

        ModelResolverException e = assertThrows(
                ModelResolverException.class,
                () -> newModelResolver().resolveModel(session, null, parent, new AtomicReference<>()),
                "Expected 'ModelResolverException' not thrown.");
        assertEquals("The requested parent version range '[1,)' does not specify an upper bound", e.getMessage());
    }

    @Test
    void testResolveParentSuccessfullyResolvesExistingParentWithoutRange() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("1")
                .build();

        assertNotNull(this.newModelResolver().resolveModel(session, null, parent, new AtomicReference<>()));
        assertEquals("1", parent.getVersion());
    }

    @Test
    void testResolveParentSuccessfullyResolvesExistingParentUsingHighestVersion() throws Exception {
        final Parent parent = Parent.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("(,2.0)")
                .build();

        AtomicReference<org.apache.maven.api.model.Parent> modified = new AtomicReference<>();
        assertNotNull(this.newModelResolver().resolveModel(session, null, parent, modified));
        assertEquals("1", modified.get().getVersion());
    }

    @Test
    void testResolveDependencyThrowsModelResolverExceptionWhenNotFound() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("0")
                .build();

        ModelResolverException e = assertThrows(
                ModelResolverException.class,
                () -> newModelResolver().resolveModel(session, null, dependency, new AtomicReference<>()),
                "Expected 'ModelResolverException' not thrown.");
        assertNotNull(e.getMessage());
        String message = e.getMessage();
        assertTrue(
                message.contains("Could not find artifact org.apache:apache:pom:0 in central"),
                "Expected exception message to contain artifact not found text but was: " + message);
    }

    @Test
    void testResolveDependencyThrowsModelResolverExceptionWhenNoMatchingVersionFound() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("[2.0,2.1)")
                .build();

        ModelResolverException e = assertThrows(
                ModelResolverException.class,
                () -> newModelResolver().resolveModel(session, null, dependency, new AtomicReference<>()),
                "Expected 'ModelResolverException' not thrown.");
        assertEquals("No versions matched the requested dependency version range '[2.0,2.1)'", e.getMessage());
    }

    @Test
    void testResolveDependencyThrowsModelResolverExceptionWhenUsingRangesWithoutUpperBound() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("[1,)")
                .build();

        ModelResolverException e = assertThrows(
                ModelResolverException.class,
                () -> newModelResolver().resolveModel(session, null, dependency, new AtomicReference<>()),
                "Expected 'ModelResolverException' not thrown.");
        assertEquals("The requested dependency version range '[1,)' does not specify an upper bound", e.getMessage());
    }

    @Test
    void testResolveDependencySuccessfullyResolvesExistingDependencyWithoutRange() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("1")
                .build();

        assertNotNull(this.newModelResolver().resolveModel(session, null, dependency, new AtomicReference<>()));
        assertEquals("1", dependency.getVersion());
    }

    @Test
    void testResolveDependencySuccessfullyResolvesExistingDependencyUsingHighestVersion() throws Exception {
        final Dependency dependency = Dependency.newBuilder()
                .groupId("org.apache")
                .artifactId("apache")
                .version("(,2.0)")
                .build();

        AtomicReference<org.apache.maven.api.model.Dependency> modified = new AtomicReference<>();
        assertNotNull(this.newModelResolver().resolveModel(session, null, dependency, modified));
        assertEquals("1", modified.get().getVersion());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testParentRangeReportsMetadataChecksumFailure(boolean incorrectChecksum, @TempDir Path directory)
            throws Exception {
        Session resolvingSession = newIsolatedSession(directory);
        RemoteRepository remote = metadataRepository(
                resolvingSession,
                directory.resolve("remote"),
                incorrectChecksum ? MetadataChecksum.INCORRECT : MetadataChecksum.MISSING);
        resolvingSession = resolvingSession.withRemoteRepositories(List.of(remote));
        Session parentSession = resolvingSession;
        Parent parent = Parent.newBuilder()
                .groupId("org.example")
                .artifactId("range-parent")
                .version("[1.0,2.0)")
                .build();

        ModelResolverException error = assertThrows(
                ModelResolverException.class,
                () -> newModelResolver().resolveModel(parentSession, null, parent, new AtomicReference<>()));

        assertTrue(error.getMessage().contains("maven-metadata.xml"), error.getMessage());
        assertTrue(error.getMessage().contains("Checksum validation failed"), error.getMessage());
        assertFalse(error.getMessage().contains("No versions matched"), error.getMessage());
        assertInstanceOf(VersionRangeResolverException.class, error.getCause());
        assertNotNull(error.getCause().getCause());
    }

    @Test
    void testSessionRangePreservesMetadataFailures(@TempDir Path directory) throws Exception {
        Session isolated = newIsolatedSession(directory);
        RemoteRepository first = metadataRepository(isolated, directory.resolve("first"), MetadataChecksum.MISSING);
        RemoteRepository second = metadataRepository(isolated, directory.resolve("second"), MetadataChecksum.MISSING);
        Session resolvingSession = isolated.withRemoteRepositories(List.of(first, second));
        ArtifactCoordinates artifact =
                resolvingSession.createArtifactCoordinates("org.example:range-parent:pom:[1.0,2.0)");
        List<RemoteRepository> repositories = resolvingSession.getRemoteRepositories();
        List<Executable> resolutions = List.of(
                () -> resolvingSession.resolveVersionRange(artifact),
                () -> resolvingSession.resolveVersionRange(artifact, repositories),
                () -> resolvingSession.resolveHighestVersion(artifact, repositories));

        for (Executable resolution : resolutions) {
            VersionRangeResolverException error = assertThrows(VersionRangeResolverException.class, resolution);
            assertTrue(error.getMessage().contains(artifact.toString()), error.getMessage());
            assertTrue(error.getMessage().contains("metadata-first"), error.getMessage());
            assertTrue(error.getMessage().contains("metadata-second"), error.getMessage());
            assertNotNull(error.getCause());
            assertTrue(error.getCause().getMessage().contains("Checksum validation failed"));
            assertEquals(1, error.getSuppressed().length);
            assertTrue(error.getSuppressed()[0].getMessage().contains("Checksum validation failed"));
        }
    }

    @Test
    void testSessionRangeReturnsEmptyWhenNothingMatches(@TempDir Path directory) throws Exception {
        Session isolated = newIsolatedSession(directory);
        RemoteRepository remote = metadataRepository(isolated, directory.resolve("remote"), MetadataChecksum.VALID);
        Session resolvingSession = isolated.withRemoteRepositories(List.of(remote));
        ArtifactCoordinates artifact =
                resolvingSession.createArtifactCoordinates("org.example:range-parent:pom:[3.0,4.0)");

        assertTrue(resolvingSession.resolveVersionRange(artifact).isEmpty());
        assertTrue(
                resolvingSession.resolveVersionRange(artifact, List.of(remote)).isEmpty());
        assertTrue(resolvingSession
                .resolveHighestVersion(artifact, List.of(remote))
                .isEmpty());
    }

    @Test
    void testSessionRangeSucceedsDespiteAnotherRepositoryFailure(@TempDir Path directory) throws Exception {
        Session isolated = newIsolatedSession(directory);
        RemoteRepository failed = metadataRepository(isolated, directory.resolve("failed"), MetadataChecksum.INCORRECT);
        RemoteRepository valid = metadataRepository(isolated, directory.resolve("valid"), MetadataChecksum.VALID);
        List<RemoteRepository> repositories = List.of(failed, valid);
        Session resolvingSession = isolated.withRemoteRepositories(repositories);
        ArtifactCoordinates artifact =
                resolvingSession.createArtifactCoordinates("org.example:range-parent:pom:[1.0,2.0)");
        var expected = List.of(resolvingSession.parseVersion("1.0"), resolvingSession.parseVersion("1.5"));

        assertEquals(expected, resolvingSession.resolveVersionRange(artifact));
        assertEquals(expected, resolvingSession.resolveVersionRange(artifact, repositories));
        assertEquals(
                expected.get(1),
                resolvingSession.resolveHighestVersion(artifact, repositories).orElseThrow());
    }

    private Session newIsolatedSession(Path directory) {
        return ApiRunner.createSession(
                injector -> injector.bindInstance(DefaultModelResolverTest.class, this), directory.resolve("local"));
    }

    private RemoteRepository metadataRepository(Session isolated, Path directory, MetadataChecksum checksum)
            throws Exception {
        Path metadata = directory.resolve("org/example/range-parent/maven-metadata.xml");
        Files.createDirectories(metadata.getParent());
        Files.writeString(metadata, """
                <metadata>
                  <groupId>org.example</groupId>
                  <artifactId>range-parent</artifactId>
                  <versioning>
                    <versions>
                      <version>1.0</version>
                      <version>1.5</version>
                      <version>2.0</version>
                    </versions>
                  </versioning>
                </metadata>
                """);
        if (checksum != MetadataChecksum.MISSING) {
            String digest = checksum == MetadataChecksum.VALID
                    ? HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(metadata)))
                    : "0000000000000000000000000000000000000000";
            Files.writeString(metadata.resolveSibling("maven-metadata.xml.sha1"), digest);
        }
        return isolated.createRemoteRepository(Repository.newBuilder()
                .id("metadata-" + directory.getFileName())
                .url(directory.toUri().toString())
                .releases(RepositoryPolicy.newBuilder().checksumPolicy("fail").build())
                .build());
    }

    enum MetadataChecksum {
        MISSING,
        INCORRECT,
        VALID
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
