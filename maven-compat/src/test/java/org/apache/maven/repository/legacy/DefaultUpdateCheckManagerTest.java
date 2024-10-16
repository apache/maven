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
package org.apache.maven.repository.legacy;

import javax.inject.Inject;

import java.io.File;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deprecated
class DefaultUpdateCheckManagerTest extends AbstractArtifactComponentTestCase {

    @Inject
    private ArtifactFactory artifactFactory;

    DefaultUpdateCheckManager updateCheckManager;

    @Override
    protected String component() {
        return "updateCheckManager";
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        updateCheckManager = new DefaultUpdateCheckManager(new ConsoleLogger(Logger.LEVEL_DEBUG, "test"));
    }

    @Test
    void testArtifact() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createArtifact("a", "0.0.1-SNAPSHOT");
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        file.delete();
        a.setFile(file);

        File touchFile = updateCheckManager.getTouchfile(a);
        touchFile.delete();

        assertTrue(updateCheckManager.isUpdateRequired(a, remoteRepository));

        file.getParentFile().mkdirs();
        file.createNewFile();
        updateCheckManager.touch(a, remoteRepository, null);

        assertFalse(updateCheckManager.isUpdateRequired(a, remoteRepository));

        assertNull(
                updateCheckManager.readLastUpdated(touchFile, updateCheckManager.getRepositoryKey(remoteRepository)));

        assertFalse(updateCheckManager.getTouchfile(a).exists());
    }

    @Test
    void testMissingArtifact() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createArtifact("a", "0.0.1-SNAPSHOT");
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        file.delete();
        a.setFile(file);

        File touchFile = updateCheckManager.getTouchfile(a);
        touchFile.delete();

        assertTrue(updateCheckManager.isUpdateRequired(a, remoteRepository));

        updateCheckManager.touch(a, remoteRepository, null);

        assertFalse(updateCheckManager.isUpdateRequired(a, remoteRepository));

        assertFalse(file.exists());
        assertNotNull(
                updateCheckManager.readLastUpdated(touchFile, updateCheckManager.getRepositoryKey(remoteRepository)));
    }

    @Test
    void testPom() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createArtifact("a", "0.0.1", "pom");
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        file.delete();
        a.setFile(file);

        File touchFile = updateCheckManager.getTouchfile(a);
        touchFile.delete();

        assertTrue(updateCheckManager.isUpdateRequired(a, remoteRepository));

        file.getParentFile().mkdirs();
        file.createNewFile();
        updateCheckManager.touch(a, remoteRepository, null);

        assertFalse(updateCheckManager.isUpdateRequired(a, remoteRepository));

        assertNull(
                updateCheckManager.readLastUpdated(touchFile, updateCheckManager.getRepositoryKey(remoteRepository)));

        assertFalse(updateCheckManager.getTouchfile(a).exists());
    }

    @Test
    void testMissingPom() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createArtifact("a", "0.0.1", "pom");
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        file.delete();
        a.setFile(file);

        File touchFile = updateCheckManager.getTouchfile(a);
        touchFile.delete();

        assertTrue(updateCheckManager.isUpdateRequired(a, remoteRepository));

        updateCheckManager.touch(a, remoteRepository, null);

        assertFalse(updateCheckManager.isUpdateRequired(a, remoteRepository));

        assertFalse(file.exists());
        assertNotNull(
                updateCheckManager.readLastUpdated(touchFile, updateCheckManager.getRepositoryKey(remoteRepository)));
    }

    @Test
    void testMetadata() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createRemoteArtifact("a", "0.0.1-SNAPSHOT");
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata(a);

        File file = new File(
                localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata(metadata, localRepository));
        file.delete();

        File touchFile = updateCheckManager.getTouchfile(metadata, file);
        touchFile.delete();

        assertTrue(updateCheckManager.isUpdateRequired(metadata, remoteRepository, file));

        file.getParentFile().mkdirs();
        file.createNewFile();
        updateCheckManager.touch(metadata, remoteRepository, file);

        assertFalse(updateCheckManager.isUpdateRequired(metadata, remoteRepository, file));

        assertNotNull(updateCheckManager.readLastUpdated(
                touchFile, updateCheckManager.getMetadataKey(remoteRepository, file)));
    }

    @Test
    void testMissingMetadata() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createRemoteArtifact("a", "0.0.1-SNAPSHOT");
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata(a);

        File file = new File(
                localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata(metadata, localRepository));
        file.delete();

        File touchFile = updateCheckManager.getTouchfile(metadata, file);
        touchFile.delete();

        assertTrue(updateCheckManager.isUpdateRequired(metadata, remoteRepository, file));

        updateCheckManager.touch(metadata, remoteRepository, file);

        assertFalse(updateCheckManager.isUpdateRequired(metadata, remoteRepository, file));

        assertNotNull(updateCheckManager.readLastUpdated(
                touchFile, updateCheckManager.getMetadataKey(remoteRepository, file)));
    }

    @Test
    void testArtifactTouchFileName() throws Exception {
        ArtifactRepository localRepository = localRepository();

        Artifact a = artifactFactory.createArtifactWithClassifier("groupId", "a", "0.0.1-SNAPSHOT", "jar", null);
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        a.setFile(file);

        assertEquals(
                "a-0.0.1-SNAPSHOT.jar.lastUpdated",
                updateCheckManager.getTouchfile(a).getName());

        a = artifactFactory.createArtifactWithClassifier("groupId", "a", "0.0.1-SNAPSHOT", "jar", "classifier");
        file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        a.setFile(file);

        assertEquals(
                "a-0.0.1-SNAPSHOT-classifier.jar.lastUpdated",
                updateCheckManager.getTouchfile(a).getName());
    }
}
