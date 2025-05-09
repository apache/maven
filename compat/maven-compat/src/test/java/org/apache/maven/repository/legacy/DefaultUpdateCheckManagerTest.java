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

import static org.assertj.core.api.Assertions.assertThat;

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
    void artifact() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createArtifact("a", "0.0.1-SNAPSHOT");
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        file.delete();
        a.setFile(file);

        File touchFile = updateCheckManager.getTouchfile(a);
        touchFile.delete();

        assertThat(updateCheckManager.isUpdateRequired(a, remoteRepository)).isTrue();

        file.getParentFile().mkdirs();
        file.createNewFile();
        updateCheckManager.touch(a, remoteRepository, null);

        assertThat(updateCheckManager.isUpdateRequired(a, remoteRepository)).isFalse();

        assertThat(updateCheckManager.readLastUpdated(touchFile, updateCheckManager.getRepositoryKey(remoteRepository))).isNull();

        assertThat(updateCheckManager.getTouchfile(a).exists()).isFalse();
    }

    @Test
    void missingArtifact() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createArtifact("a", "0.0.1-SNAPSHOT");
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        file.delete();
        a.setFile(file);

        File touchFile = updateCheckManager.getTouchfile(a);
        touchFile.delete();

        assertThat(updateCheckManager.isUpdateRequired(a, remoteRepository)).isTrue();

        updateCheckManager.touch(a, remoteRepository, null);

        assertThat(updateCheckManager.isUpdateRequired(a, remoteRepository)).isFalse();

        assertThat(file.exists()).isFalse();
        assertThat(updateCheckManager.readLastUpdated(touchFile, updateCheckManager.getRepositoryKey(remoteRepository))).isNotNull();
    }

    @Test
    void pom() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createArtifact("a", "0.0.1", "pom");
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        file.delete();
        a.setFile(file);

        File touchFile = updateCheckManager.getTouchfile(a);
        touchFile.delete();

        assertThat(updateCheckManager.isUpdateRequired(a, remoteRepository)).isTrue();

        file.getParentFile().mkdirs();
        file.createNewFile();
        updateCheckManager.touch(a, remoteRepository, null);

        assertThat(updateCheckManager.isUpdateRequired(a, remoteRepository)).isFalse();

        assertThat(updateCheckManager.readLastUpdated(touchFile, updateCheckManager.getRepositoryKey(remoteRepository))).isNull();

        assertThat(updateCheckManager.getTouchfile(a).exists()).isFalse();
    }

    @Test
    void missingPom() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createArtifact("a", "0.0.1", "pom");
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        file.delete();
        a.setFile(file);

        File touchFile = updateCheckManager.getTouchfile(a);
        touchFile.delete();

        assertThat(updateCheckManager.isUpdateRequired(a, remoteRepository)).isTrue();

        updateCheckManager.touch(a, remoteRepository, null);

        assertThat(updateCheckManager.isUpdateRequired(a, remoteRepository)).isFalse();

        assertThat(file.exists()).isFalse();
        assertThat(updateCheckManager.readLastUpdated(touchFile, updateCheckManager.getRepositoryKey(remoteRepository))).isNotNull();
    }

    @Test
    void metadata() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createRemoteArtifact("a", "0.0.1-SNAPSHOT");
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata(a);

        File file = new File(
                localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata(metadata, localRepository));
        file.delete();

        File touchFile = updateCheckManager.getTouchfile(metadata, file);
        touchFile.delete();

        assertThat(updateCheckManager.isUpdateRequired(metadata, remoteRepository, file)).isTrue();

        file.getParentFile().mkdirs();
        file.createNewFile();
        updateCheckManager.touch(metadata, remoteRepository, file);

        assertThat(updateCheckManager.isUpdateRequired(metadata, remoteRepository, file)).isFalse();

        assertThat(updateCheckManager.readLastUpdated(
                touchFile, updateCheckManager.getMetadataKey(remoteRepository, file))).isNotNull();
    }

    @Test
    void missingMetadata() throws Exception {
        ArtifactRepository remoteRepository = remoteRepository();

        ArtifactRepository localRepository = localRepository();

        Artifact a = createRemoteArtifact("a", "0.0.1-SNAPSHOT");
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata(a);

        File file = new File(
                localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata(metadata, localRepository));
        file.delete();

        File touchFile = updateCheckManager.getTouchfile(metadata, file);
        touchFile.delete();

        assertThat(updateCheckManager.isUpdateRequired(metadata, remoteRepository, file)).isTrue();

        updateCheckManager.touch(metadata, remoteRepository, file);

        assertThat(updateCheckManager.isUpdateRequired(metadata, remoteRepository, file)).isFalse();

        assertThat(updateCheckManager.readLastUpdated(
                touchFile, updateCheckManager.getMetadataKey(remoteRepository, file))).isNotNull();
    }

    @Test
    void artifactTouchFileName() throws Exception {
        ArtifactRepository localRepository = localRepository();

        Artifact a = artifactFactory.createArtifactWithClassifier("groupId", "a", "0.0.1-SNAPSHOT", "jar", null);
        File file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        a.setFile(file);

        assertThat(updateCheckManager.getTouchfile(a).getName()).isEqualTo("a-0.0.1-SNAPSHOT.jar.lastUpdated");

        a = artifactFactory.createArtifactWithClassifier("groupId", "a", "0.0.1-SNAPSHOT", "jar", "classifier");
        file = new File(localRepository.getBasedir(), localRepository.pathOf(a));
        a.setFile(file);

        assertThat(updateCheckManager.getTouchfile(a).getName()).isEqualTo("a-0.0.1-SNAPSHOT-classifier.jar.lastUpdated");
    }
}
