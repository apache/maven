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
package org.apache.maven.artifact.repository;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystemSession;

/**
 * @author jdcasey
 */
@Component(role = ArtifactRepositoryFactory.class)
public class DefaultArtifactRepositoryFactory implements ArtifactRepositoryFactory {

    @Requirement
    private org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory factory;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private RepositorySystem repositorySystem;

    public ArtifactRepositoryLayout getLayout(String layoutId) throws UnknownRepositoryLayoutException {
        return factory.getLayout(layoutId);
    }

    public ArtifactRepository createDeploymentArtifactRepository(
            String id, String url, String layoutId, boolean uniqueVersion) throws UnknownRepositoryLayoutException {
        return injectSession(factory.createDeploymentArtifactRepository(id, url, layoutId, uniqueVersion), false);
    }

    public ArtifactRepository createDeploymentArtifactRepository(
            String id, String url, ArtifactRepositoryLayout repositoryLayout, boolean uniqueVersion) {
        return injectSession(
                factory.createDeploymentArtifactRepository(id, url, repositoryLayout, uniqueVersion), false);
    }

    public ArtifactRepository createArtifactRepository(
            String id,
            String url,
            String layoutId,
            ArtifactRepositoryPolicy snapshots,
            ArtifactRepositoryPolicy releases)
            throws UnknownRepositoryLayoutException {
        return injectSession(factory.createArtifactRepository(id, url, layoutId, snapshots, releases), true);
    }

    public ArtifactRepository createArtifactRepository(
            String id,
            String url,
            ArtifactRepositoryLayout repositoryLayout,
            ArtifactRepositoryPolicy snapshots,
            ArtifactRepositoryPolicy releases) {
        return injectSession(factory.createArtifactRepository(id, url, repositoryLayout, snapshots, releases), true);
    }

    public void setGlobalUpdatePolicy(String updatePolicy) {
        factory.setGlobalUpdatePolicy(updatePolicy);
    }

    public void setGlobalChecksumPolicy(String checksumPolicy) {
        factory.setGlobalChecksumPolicy(checksumPolicy);
    }

    private ArtifactRepository injectSession(ArtifactRepository repository, boolean mirrors) {
        RepositorySystemSession session = legacySupport.getRepositorySession();

        if (session != null && repository != null && !isLocalRepository(repository)) {
            List<ArtifactRepository> repositories = Arrays.asList(repository);

            if (mirrors) {
                repositorySystem.injectMirror(session, repositories);
            }

            repositorySystem.injectProxy(session, repositories);

            repositorySystem.injectAuthentication(session, repositories);
        }

        return repository;
    }

    private boolean isLocalRepository(ArtifactRepository repository) {
        // unfortunately, the API doesn't allow to tell a remote repo and the local repo apart...
        return "local".equals(repository.getId());
    }
}
