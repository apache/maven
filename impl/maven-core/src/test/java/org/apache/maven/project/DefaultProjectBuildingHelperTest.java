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
package org.apache.maven.project;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.MavenTestHelper;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
class DefaultProjectBuildingHelperTest {

    @Inject
    private DefaultProjectBuildingHelper helper;

    @Inject
    private MavenRepositorySystem repositorySystem;

    @Inject
    private PlexusContainer container;

    /**
     * A POM-declared repository and a request repository (e.g. the project's remote repositories, where Maven 4
     * puts {@code central} instead of the super POM) can end up mapped to the same mirror id by a single wildcard
     * mirror. Each side only carries its own original release/snapshot policy, so both copies must reach
     * {@link MavenRepositorySystem#getEffectiveRepositories} to be merged; dropping either one as a duplicate id
     * silently loses that side's policy (GitHub #14135).
     */
    @Test
    void mergesPoliciesWhenAPomRepositoryAndARequestRepositoryShareAMirrorId() throws Exception {
        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector()
                .add("all", "https://repo.example.com/mirror", "default", false, false, "*", null);

        DefaultRepositorySystemSession repoSession = MavenTestHelper.createSession(repositorySystem, container);
        repoSession.setMirrorSelector(mirrorSelector);

        // The project's remote repositories: central, releases enabled / snapshots disabled, already mirrored
        // the way it would be by the time DefaultProjectBuildingHelper sees it in the real pipeline.
        ArtifactRepository central = MavenRepositorySystem.createArtifactRepository(
                "central",
                "https://repo.maven.apache.org/maven2",
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(false, null, null),
                new ArtifactRepositoryPolicy(true, null, null));
        List<ArtifactRepository> externalRepositories = new ArrayList<>();
        externalRepositories.add(central);
        repositorySystem.injectMirror(repoSession, externalRepositories);

        // The POM declares a snapshots-only repository, matched by the same wildcard mirror.
        Repository snaps = new Repository();
        snaps.setId("snaps");
        snaps.setUrl("https://repository.apache.org/snapshots");
        RepositoryPolicy releasesDisabled = new RepositoryPolicy();
        releasesDisabled.setEnabled(false);
        snaps.setReleases(releasesDisabled);
        RepositoryPolicy snapshotsEnabled = new RepositoryPolicy();
        snapshotsEnabled.setEnabled(true);
        snaps.setSnapshots(snapshotsEnabled);

        DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
        request.setRepositorySession(repoSession);

        List<ArtifactRepository> effectiveRepositories =
                helper.createArtifactRepositories(Collections.singletonList(snaps), externalRepositories, request);

        assertEquals(1, effectiveRepositories.size(), "both sides map to the same mirror id and must merge into one");
        ArtifactRepository effective = effectiveRepositories.get(0);
        assertEquals("all", effective.getId());
        assertTrue(
                effective.getReleases().isEnabled(),
                "the mirrored central side's release policy must survive the merge");
        assertTrue(
                effective.getSnapshots().isEnabled(),
                "the mirrored POM repository's snapshot policy must survive the merge");
    }
}
