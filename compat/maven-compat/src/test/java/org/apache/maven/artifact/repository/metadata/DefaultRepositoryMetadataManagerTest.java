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
package org.apache.maven.artifact.repository.metadata;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.util.Collections;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link DefaultRepositoryMetadataManager}.
 */
@Deprecated
class DefaultRepositoryMetadataManagerTest extends AbstractArtifactComponentTestCase {

    @Inject
    private RepositoryMetadataManager repositoryMetadataManager;

    @Inject
    @Named("default")
    private ArtifactRepositoryLayout layout;

    @Override
    protected String component() {
        return "repositoryMetadataManager";
    }

    @Test
    void testResolveHonorsConfiguredFailChecksumPolicy() throws Exception {
        RepositoryMetadata metadata = new GroupRepositoryMetadata("checksum-policy-test-group");

        ArtifactRepositoryPolicy failPolicy = new ArtifactRepositoryPolicy(
                true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL);

        File remoteBase = new File(getBasedir(), "target/test-repositories/" + component() + "/remote-repository");
        FileUtils.deleteDirectory(remoteBase);

        ArtifactRepository remoteRepo = artifactRepositoryFactory.createArtifactRepository(
                "test", "file://" + remoteBase.getPath(), layout, failPolicy, failPolicy);

        String remotePath = remoteRepo.pathOfRemoteRepositoryMetadata(metadata);
        File remoteFile = new File(remoteBase, remotePath);
        remoteFile.getParentFile().mkdirs();
        FileUtils.fileWrite(remoteFile.getAbsolutePath(), "<metadata/>");
        FileUtils.fileWrite(remoteFile.getAbsolutePath() + ".sha1", "0000000000000000000000000000000000000000");

        ArtifactRepository localRepo = localRepository();
        FileUtils.deleteDirectory(new File(localRepo.getBasedir()));

        assertThrows(
                RepositoryMetadataResolutionException.class,
                () -> repositoryMetadataManager.resolve(metadata, Collections.singletonList(remoteRepo), localRepo));
    }
}
