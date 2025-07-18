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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataStoreException;
import org.apache.maven.repository.Proxy;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * <strong>Warning:</strong> This is an internal utility class that is only public for technical reasons, it is not part
 * of the public API. In particular, this class can be changed or deleted without prior notice.
 *
 */
@Deprecated
public class LegacyLocalRepositoryManager implements LocalRepositoryManager {

    private final ArtifactRepository delegate;

    private final LocalRepository repo;

    private final boolean realLocalRepo;

    public static RepositorySystemSession overlay(
            ArtifactRepository repository, RepositorySystemSession session, RepositorySystem system) {
        return overlay(repository, session);
    }

    public static RepositorySystemSession overlay(ArtifactRepository repository, RepositorySystemSession session) {
        if (repository == null || repository.getBasedir() == null) {
            return session;
        }

        if (session != null) {
            LocalRepositoryManager lrm = session.getLocalRepositoryManager();
            if (lrm != null && lrm.getRepository().getBasedir().equals(new File(repository.getBasedir()))) {
                return session;
            }
        } else {
            session = new DefaultRepositorySystemSession();
        }

        final LocalRepositoryManager llrm = new LegacyLocalRepositoryManager(repository);

        return new DefaultRepositorySystemSession(session).setLocalRepositoryManager(llrm);
    }

    private LegacyLocalRepositoryManager(ArtifactRepository delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");

        ArtifactRepositoryLayout layout = delegate.getLayout();
        repo = new LocalRepository(
                new File(delegate.getBasedir()),
                (layout != null) ? layout.getClass().getSimpleName() : "legacy");

        /*
         * NOTE: "invoker:install" vs "appassembler:assemble": Both mojos use the artifact installer to put an artifact
         * into a repository. In the first case, the result needs to be a proper local repository that one can use for
         * local artifact resolution. In the second case, the result needs to precisely obey the path information of the
         * repository's layout to allow pointing at artifacts within the repository. Unfortunately,
         * DefaultRepositoryLayout does not correctly describe the layout of a local repository which unlike a remote
         * repository never uses timestamps in the filename of a snapshot artifact. The discrepancy gets notable when a
         * remotely resolved snapshot artifact gets passed into pathOf(). So producing a proper local artifact path
         * using DefaultRepositoryLayout requires us to enforce usage of the artifact's base version. This
         * transformation however contradicts the other use case of precisely obeying the repository's layout. The below
         * flag tries to detect which use case applies to make both plugins happy.
         */
        realLocalRepo = (layout instanceof DefaultRepositoryLayout) && "local".equals(delegate.getId());
    }

    @Override
    public LocalRepository getRepository() {
        return repo;
    }

    @Override
    public String getPathForLocalArtifact(Artifact artifact) {
        if (realLocalRepo) {
            return delegate.pathOf(RepositoryUtils.toArtifact(artifact.setVersion(artifact.getBaseVersion())));
        }
        return delegate.pathOf(RepositoryUtils.toArtifact(artifact));
    }

    @Override
    public String getPathForRemoteArtifact(Artifact artifact, RemoteRepository repository, String context) {
        return delegate.pathOf(RepositoryUtils.toArtifact(artifact));
    }

    @Override
    public String getPathForLocalMetadata(Metadata metadata) {
        return delegate.pathOfLocalRepositoryMetadata(new ArtifactMetadataAdapter(metadata), delegate);
    }

    @Override
    public String getPathForRemoteMetadata(Metadata metadata, RemoteRepository repository, String context) {
        return delegate.pathOfLocalRepositoryMetadata(
                new ArtifactMetadataAdapter(metadata), new ArtifactRepositoryAdapter(repository));
    }

    @Override
    public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request) {
        String path = getPathForLocalArtifact(request.getArtifact());
        File file = new File(getRepository().getBasedir(), path);

        LocalArtifactResult result = new LocalArtifactResult(request);
        if (file.isFile()) {
            result.setFile(file);
            result.setAvailable(true);
        }

        return result;
    }

    @Override
    public LocalMetadataResult find(RepositorySystemSession session, LocalMetadataRequest request) {
        Metadata metadata = request.getMetadata();

        String path;
        if (request.getRepository() == null) {
            path = getPathForLocalMetadata(metadata);
        } else {
            path = getPathForRemoteMetadata(metadata, request.getRepository(), request.getContext());
        }

        File file = new File(getRepository().getBasedir(), path);

        LocalMetadataResult result = new LocalMetadataResult(request);
        if (file.isFile()) {
            result.setFile(file);
        }

        return result;
    }

    @Override
    public void add(RepositorySystemSession session, LocalArtifactRegistration request) {
        // noop
    }

    @Override
    public void add(RepositorySystemSession session, LocalMetadataRegistration request) {
        // noop
    }

    static class ArtifactMetadataAdapter implements ArtifactMetadata {

        private final Metadata metadata;

        ArtifactMetadataAdapter(Metadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public boolean storedInArtifactVersionDirectory() {
            return !metadata.getVersion().isEmpty();
        }

        @Override
        public boolean storedInGroupDirectory() {
            return metadata.getArtifactId().isEmpty();
        }

        @Override
        public String getGroupId() {
            return nullify(metadata.getGroupId());
        }

        @Override
        public String getArtifactId() {
            return nullify(metadata.getArtifactId());
        }

        @Override
        public String getBaseVersion() {
            return nullify(metadata.getVersion());
        }

        private String nullify(String str) {
            return (str == null || str.isEmpty()) ? null : str;
        }

        @Override
        public Object getKey() {
            return metadata.toString();
        }

        @Override
        public String getRemoteFilename() {
            return metadata.getType();
        }

        @Override
        public String getLocalFilename(ArtifactRepository repository) {
            return insertRepositoryKey(getRemoteFilename(), repository.getKey());
        }

        private String insertRepositoryKey(String filename, String repositoryKey) {
            String result;
            int idx = filename.indexOf('.');
            if (idx < 0) {
                result = filename + '-' + repositoryKey;
            } else {
                result = filename.substring(0, idx) + '-' + repositoryKey + filename.substring(idx);
            }
            return result;
        }

        @Override
        public void merge(org.apache.maven.repository.legacy.metadata.ArtifactMetadata metadata) {
            // not used
        }

        @Override
        public void merge(ArtifactMetadata metadata) {
            // not used
        }

        @Override
        public void storeInLocalRepository(ArtifactRepository localRepository, ArtifactRepository remoteRepository)
                throws RepositoryMetadataStoreException {
            // not used
        }

        @Override
        public String extendedToString() {
            return metadata.toString();
        }
    }

    static class ArtifactRepositoryAdapter implements ArtifactRepository {

        private final RemoteRepository repository;

        ArtifactRepositoryAdapter(RemoteRepository repository) {
            this.repository = repository;
        }

        @Override
        public String pathOf(org.apache.maven.artifact.Artifact artifact) {
            return null;
        }

        @Override
        public String pathOfRemoteRepositoryMetadata(ArtifactMetadata artifactMetadata) {
            return null;
        }

        @Override
        public String pathOfLocalRepositoryMetadata(ArtifactMetadata metadata, ArtifactRepository repository) {
            return null;
        }

        @Override
        public String getUrl() {
            return repository.getUrl();
        }

        @Override
        public void setUrl(String url) {}

        @Override
        public String getBasedir() {
            return null;
        }

        @Override
        public String getProtocol() {
            return repository.getProtocol();
        }

        @Override
        public String getId() {
            return repository.getId();
        }

        @Override
        public void setId(String id) {}

        @Override
        public ArtifactRepositoryPolicy getSnapshots() {
            return null;
        }

        @Override
        public void setSnapshotUpdatePolicy(ArtifactRepositoryPolicy policy) {}

        @Override
        public ArtifactRepositoryPolicy getReleases() {
            return null;
        }

        @Override
        public void setReleaseUpdatePolicy(ArtifactRepositoryPolicy policy) {}

        @Override
        public ArtifactRepositoryLayout getLayout() {
            return null;
        }

        @Override
        public void setLayout(ArtifactRepositoryLayout layout) {}

        @Override
        public String getKey() {
            return getId();
        }

        @Override
        public boolean isUniqueVersion() {
            return true;
        }

        @Override
        public boolean isBlacklisted() {
            return false;
        }

        @Override
        public void setBlacklisted(boolean blackListed) {}

        @Override
        public org.apache.maven.artifact.Artifact find(org.apache.maven.artifact.Artifact artifact) {
            return null;
        }

        @Override
        public List<String> findVersions(org.apache.maven.artifact.Artifact artifact) {
            return Collections.emptyList();
        }

        @Override
        public boolean isProjectAware() {
            return false;
        }

        @Override
        public void setAuthentication(Authentication authentication) {}

        @Override
        public Authentication getAuthentication() {
            return null;
        }

        @Override
        public void setProxy(Proxy proxy) {}

        @Override
        public Proxy getProxy() {
            return null;
        }

        @Override
        public List<ArtifactRepository> getMirroredRepositories() {
            return Collections.emptyList();
        }

        @Override
        public void setMirroredRepositories(List<ArtifactRepository> mirroredRepositories) {}

        @Override
        public boolean isBlocked() {
            return false;
        }

        @Override
        public void setBlocked(boolean blocked) {}
    }
}
