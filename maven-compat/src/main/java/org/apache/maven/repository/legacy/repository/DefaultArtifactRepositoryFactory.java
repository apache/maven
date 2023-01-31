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
package org.apache.maven.repository.legacy.repository;

import java.util.Map;

import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout2;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author jdcasey
 */
@Component(role = ArtifactRepositoryFactory.class)
public class DefaultArtifactRepositoryFactory implements ArtifactRepositoryFactory {
    // TODO use settings?
    private String globalUpdatePolicy;

    private String globalChecksumPolicy;

    @Requirement(role = ArtifactRepositoryLayout.class)
    private Map<String, ArtifactRepositoryLayout> repositoryLayouts;

    public ArtifactRepositoryLayout getLayout(String layoutId) throws UnknownRepositoryLayoutException {
        return repositoryLayouts.get(layoutId);
    }

    public ArtifactRepository createDeploymentArtifactRepository(
            String id, String url, String layoutId, boolean uniqueVersion) throws UnknownRepositoryLayoutException {
        ArtifactRepositoryLayout layout = repositoryLayouts.get(layoutId);

        checkLayout(id, layoutId, layout);

        return createDeploymentArtifactRepository(id, url, layout, uniqueVersion);
    }

    private void checkLayout(String repositoryId, String layoutId, ArtifactRepositoryLayout layout)
            throws UnknownRepositoryLayoutException {
        if (layout == null) {
            throw new UnknownRepositoryLayoutException(repositoryId, layoutId);
        }
    }

    public ArtifactRepository createDeploymentArtifactRepository(
            String id, String url, ArtifactRepositoryLayout repositoryLayout, boolean uniqueVersion) {
        return createArtifactRepository(id, url, repositoryLayout, null, null);
    }

    public ArtifactRepository createArtifactRepository(
            String id,
            String url,
            String layoutId,
            ArtifactRepositoryPolicy snapshots,
            ArtifactRepositoryPolicy releases)
            throws UnknownRepositoryLayoutException {
        ArtifactRepositoryLayout layout = repositoryLayouts.get(layoutId);

        checkLayout(id, layoutId, layout);

        return createArtifactRepository(id, url, layout, snapshots, releases);
    }

    public ArtifactRepository createArtifactRepository(
            String id,
            String url,
            ArtifactRepositoryLayout repositoryLayout,
            ArtifactRepositoryPolicy snapshots,
            ArtifactRepositoryPolicy releases) {
        if (snapshots == null) {
            snapshots = new ArtifactRepositoryPolicy();
        }

        if (releases == null) {
            releases = new ArtifactRepositoryPolicy();
        }

        if (globalUpdatePolicy != null) {
            snapshots.setUpdatePolicy(globalUpdatePolicy);
            releases.setUpdatePolicy(globalUpdatePolicy);
        }

        if (globalChecksumPolicy != null) {
            snapshots.setChecksumPolicy(globalChecksumPolicy);
            releases.setChecksumPolicy(globalChecksumPolicy);
        }

        ArtifactRepository repository;
        if (repositoryLayout instanceof ArtifactRepositoryLayout2) {
            repository = ((ArtifactRepositoryLayout2) repositoryLayout)
                    .newMavenArtifactRepository(id, url, snapshots, releases);
        } else {
            repository = new MavenArtifactRepository(id, url, repositoryLayout, snapshots, releases);
        }

        return repository;
    }

    public void setGlobalUpdatePolicy(String updatePolicy) {
        globalUpdatePolicy = updatePolicy;
    }

    public void setGlobalChecksumPolicy(String checksumPolicy) {
        globalChecksumPolicy = checksumPolicy;
    }
}
