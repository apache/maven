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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.api.Constants;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.internal.impl.DefaultLocalPathComposer;
import org.eclipse.aether.internal.impl.DefaultLocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.DefaultRepositoryKeyFunctionFactory;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.TrackingFileManagerSupplier;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class MavenLocalRepositoryManagerFactoryTest {
    private static final Artifact ARTIFACT = new DefaultArtifact("test:artifact:jar:1");
    private static final RemoteRepository ORIGIN =
            new RemoteRepository.Builder("old", "default", "https://old.invalid").build();
    private static final RemoteRepository OTHER =
            new RemoteRepository.Builder("new", "default", "https://new.invalid").build();
    private static final String IGNORE = Constants.MAVEN_REPO_LOCAL_IGNORE_AVAILABILITY;
    private static final String SPLIT = "aether.lrm.enhanced.split";
    private static final String LEGACY_SPLIT = "aether.enhancedLocalRepository.split";
    private static final String TAIL = Constants.MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY;
    private static final String RESOLVER_TAIL = ChainedLocalRepositoryManager.CONFIG_PROP_IGNORE_TAIL_AVAILABILITY;

    @TempDir
    Path directory;

    private final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
    private final EnhancedLocalRepositoryManagerFactory enhanced = enhancedFactory();
    private final MavenLocalRepositoryManagerFactory factory = new MavenLocalRepositoryManagerFactory(enhanced);

    private static EnhancedLocalRepositoryManagerFactory enhancedFactory() {
        final var keys = new DefaultRepositoryKeyFunctionFactory();
        return new EnhancedLocalRepositoryManagerFactory(
                new DefaultLocalPathComposer(),
                new TrackingFileManagerSupplier().get(),
                new DefaultLocalPathPrefixComposerFactory(keys),
                keys);
    }

    @Test
    void reusesCachedReleaseOfflineWithoutChangingProvenance() throws Exception {
        session.setOffline(true);
        final var manager = factory.newInstance(session, new LocalRepository(directory));
        final var path = cache(manager);
        final var tracking = path.resolveSibling("_remote.repositories");
        final var before = Files.readString(tracking);
        final var result = manager.find(session, request());
        assertTrue(result.isAvailable());
        assertEquals(path, result.getPath());
        assertNull(result.getRepository(), "Do not fabricate a remote origin");
        assertEquals(before, Files.readString(tracking));
        final var props = new Properties();
        try (var reader = Files.newBufferedReader(tracking)) {
            props.load(reader);
        }
        assertFalse(props.isEmpty(), "Remote registration must still record provenance");
        assertFalse(enhanced.newInstance(session, new LocalRepository(directory))
                .find(session, request())
                .isAvailable());
    }

    static Stream<Arguments> policies() {
        return Stream.of(
                Arguments.of(Map.of(), true),
                Arguments.of(Map.of(IGNORE, false), false),
                Arguments.of(Map.of(SPLIT, true), false),
                Arguments.of(Map.of(LEGACY_SPLIT, true), false),
                Arguments.of(Map.of(SPLIT, false, LEGACY_SPLIT, true), true),
                Arguments.of(Map.of(SPLIT, true, LEGACY_SPLIT, false), false),
                Arguments.of(Map.of(SPLIT, true, IGNORE, true), true),
                Arguments.of(Map.of(TAIL, false), false),
                Arguments.of(Map.of(RESOLVER_TAIL, false), false),
                Arguments.of(Map.of(TAIL, true, RESOLVER_TAIL, false), true),
                Arguments.of(Map.of(TAIL, false, RESOLVER_TAIL, true), false),
                Arguments.of(Map.of(TAIL, false, IGNORE, true), true));
    }

    @ParameterizedTest
    @MethodSource("policies")
    void honorsPolicyPrecedence(final Map<String, Object> properties, final boolean available) throws Exception {
        // Use a shared remote prefix so split mode can find the same path for both repository IDs.
        session.setConfigProperty("aether.lrm.enhanced.splitRemoteRepository", false);
        properties.forEach(session::setConfigProperty);
        final var manager = factory.newInstance(session, new LocalRepository(directory));
        cache(manager);
        assertEquals(available, manager.find(session, request()).isAvailable());
    }

    @Test
    void missingFilesAndDirectoriesRemainMisses() throws Exception {
        final var manager = factory.newInstance(session, new LocalRepository(directory));
        assertNull(manager.find(session, request()).getPath());
        final var path = manager.getAbsolutePathForRemoteArtifact(ARTIFACT, ORIGIN, "");
        Files.createDirectories(path);
        assertFalse(manager.find(session, request()).isAvailable());
        assertNull(manager.find(session, request()).getPath());
    }

    @Test
    void preservesRealPathValidation() throws Exception {
        session.setConfigProperty(EnhancedLocalRepositoryManagerFactory.CONFIG_PROP_VERIFY_REAL_PATH, true);
        final var manager = factory.newInstance(session, new LocalRepository(directory.resolve("cache")));
        final var path = manager.getAbsolutePathForRemoteArtifact(ARTIFACT, ORIGIN, "");
        Files.createDirectories(path.getParent());
        final var outside = Files.writeString(directory.resolve("outside.jar"), "outside");
        Files.createSymbolicLink(path, outside);
        manager.add(session, new LocalArtifactRegistration(ARTIFACT, ORIGIN, List.of("")));
        final var strict = enhanced.newInstance(
                        session, new LocalRepository(manager.getRepository().getBasePath()))
                .find(session, request());
        assertNull(strict.getPath());
        assertNull(manager.find(session, request()).getPath());
        assertFalse(manager.find(session, request()).isAvailable());
    }

    @Test
    void preservesInstalledArtifactsAndMetadata() throws Exception {
        final var manager = factory.newInstance(session, new LocalRepository(directory));
        final var path = manager.getAbsolutePathForLocalArtifact(ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "installed");
        manager.add(session, new LocalArtifactRegistration(ARTIFACT));
        assertTrue(manager.find(session, request()).isAvailable());
        final var metadata =
                new DefaultMetadata("test", "artifact", "1", "maven-metadata.xml", Metadata.Nature.RELEASE);
        final var metadataPath = manager.getAbsolutePathForRemoteMetadata(metadata, ORIGIN, "");
        Files.writeString(metadataPath, "metadata");
        manager.add(session, new LocalMetadataRegistration(metadata, ORIGIN, List.of("")));
        assertEquals(
                metadataPath,
                manager.find(session, new LocalMetadataRequest(metadata, ORIGIN, ""))
                        .getPath());
        assertNull(manager.find(session, new LocalMetadataRequest(metadata, OTHER, ""))
                .getPath());
        assertEquals(
                enhanced.newInstance(session, new LocalRepository(directory)).getAbsolutePathForLocalMetadata(metadata),
                manager.getAbsolutePathForLocalMetadata(metadata));
    }

    @Test
    void decoratesChainMembersPreservingPrecedenceAndWrites() throws Exception {
        final var head = factory.newInstance(session, new LocalRepository(directory.resolve("head")));
        final var tail = factory.newInstance(session, new LocalRepository(directory.resolve("tail")));
        final var tailPath = cache(tail);
        final var chain = new ChainedLocalRepositoryManager(head, List.of(tail), false);
        assertEquals(tailPath, chain.find(session, request()).getPath());
        final var headPath = cache(head);
        assertEquals(headPath, chain.find(session, request()).getPath());
        assertTrue(chain.find(session, request()).isAvailable());
        assertEquals(head.getAbsolutePathForLocalArtifact(ARTIFACT), chain.getAbsolutePathForLocalArtifact(ARTIFACT));
        final var tailTracking = Files.readString(tailPath.resolveSibling("_remote.repositories"));
        chain.add(session, new LocalArtifactRegistration(ARTIFACT));
        assertEquals(tailTracking, Files.readString(tailPath.resolveSibling("_remote.repositories")));
        assertTrue(enhanced.newInstance(
                        session, new LocalRepository(head.getRepository().getBasePath()))
                .find(session, request())
                .isAvailable());
    }

    @Test
    void strictOptOutDoesNotChangeExistingChainTailPolicy() throws Exception {
        session.setConfigProperty(IGNORE, false);
        final var head = factory.newInstance(session, new LocalRepository(directory.resolve("head")));
        final var tail = factory.newInstance(session, new LocalRepository(directory.resolve("tail")));
        final var tailPath = cache(tail);
        assertFalse(new ChainedLocalRepositoryManager(head, List.of(tail), false)
                .find(session, request())
                .isAvailable());
        assertEquals(
                tailPath,
                new ChainedLocalRepositoryManager(head, List.of(tail), true)
                        .find(session, request())
                        .getPath());
    }

    @Test
    void preservesExplicitSimpleAndHigherPriorityFactories() throws Exception {
        final var simple = new SimpleLocalRepositoryManagerFactory();
        final var provider =
                new DefaultLocalRepositoryProvider(Map.of("maven", factory, "enhanced", enhanced, "simple", simple));
        final var manager = provider.newLocalRepositoryManager(session, new LocalRepository(directory, "simple"));
        assertEquals(
                simple.newInstance(session, new LocalRepository(directory, "simple"))
                        .getClass(),
                manager.getClass());
        final var custom = new SimpleLocalRepositoryManagerFactory().setPriority(factory.getPriority() + 1);
        final var customProvider = new DefaultLocalRepositoryProvider(Map.of("maven", factory, "custom", custom));
        assertEquals(
                custom.newInstance(session, new LocalRepository(directory)).getClass(),
                customProvider
                        .newLocalRepositoryManager(session, new LocalRepository(directory))
                        .getClass());
    }

    private Path cache(final LocalRepositoryManager manager) throws Exception {
        final var path = manager.getAbsolutePathForRemoteArtifact(ARTIFACT, ORIGIN, "");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "cached");
        manager.add(session, new LocalArtifactRegistration(ARTIFACT, ORIGIN, List.of("")));
        return path;
    }

    private LocalArtifactRequest request() {
        return new LocalArtifactRequest(ARTIFACT, List.of(OTHER), "");
    }
}
