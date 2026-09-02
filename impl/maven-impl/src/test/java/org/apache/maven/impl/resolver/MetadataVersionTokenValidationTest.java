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

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.Version;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.impl.standalone.ApiRunner;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.transport.apache.ApacheTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that version tokens adopted from downloaded {@code maven-metadata.xml} are checked before they are
 * spliced into a resolved coordinate, exercising the real {@link DefaultVersionResolver} and
 * {@link DefaultVersionRangeResolver} used by the Maven 4 resolver stack (as opposed to the legacy compat
 * path under {@code maven-resolver-provider}, which has its own equivalent tests).
 */
class MetadataVersionTokenValidationTest {

    Session session;

    @BeforeEach
    void setup() {
        Path basedir = Paths.get(System.getProperty("basedir", ""));
        Path localRepoPath = basedir.resolve("target/local-repo");
        Path remoteRepoPath = basedir.resolve("src/test/remote-repo");
        Session s = ApiRunner.createSession(
                injector -> injector.bindInstance(MetadataVersionTokenValidationTest.class, this), localRepoPath);
        RemoteRepository remoteRepository = s.createRemoteRepository(
                RemoteRepository.CENTRAL_ID, remoteRepoPath.toUri().toString());
        session = s.withRemoteRepositories(List.of(remoteRepository));
    }

    @Test
    void testSnapshotVersionFromMetadataWithInvalidTokenIsRejected() {
        ArtifactCoordinates coordinates =
                session.createArtifactCoordinates("org.apache.maven.its:dep-invalid-sv:1.0-SNAPSHOT");

        // The metadata carries a snapshotVersion value that is not a valid coordinate component, so the
        // metadata is treated as invalid and resolution falls back to the requested base version.
        Version version = session.resolveVersion(coordinates);
        assertEquals("1.0-SNAPSHOT", version.toString());
    }

    @Test
    void testSnapshotTimestampFromMetadataWithInvalidTokenIsRejected() {
        ArtifactCoordinates coordinates =
                session.createArtifactCoordinates("org.apache.maven.its:dep-invalid-ts:1.0-SNAPSHOT");

        // The metadata carries a snapshot timestamp that is not a valid coordinate component, so the metadata
        // is treated as invalid and resolution falls back to the requested base version.
        Version version = session.resolveVersion(coordinates);
        assertEquals("1.0-SNAPSHOT", version.toString());
    }

    @Test
    void testRangeResolutionWithInvalidTokenInMetadataIsRejected() {
        ArtifactCoordinates coordinates =
                session.createArtifactCoordinates("org.apache.maven.its:dep-invalid-range:jar:[1.0,2.0]");

        // The metadata carries a versions[] entry that is not a valid coordinate component, so the whole
        // document is treated as invalid and none of its versions (including the otherwise-valid 1.0 and 2.0)
        // are offered as candidates for the range.
        List<Version> versions = session.resolveVersionRange(coordinates);
        assertTrue(versions.isEmpty(), "expected no versions, got " + versions);
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
