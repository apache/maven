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
package org.apache.maven.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.model.Model;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.project.artifact.ArtifactWithDependencies;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.sisu.Priority;

/**
 */
@Named
@Singleton
@Priority(10)
@Deprecated
public class TestRepositorySystem implements RepositorySystem {

    private final ModelReader modelReader;

    private final ArtifactFactory artifactFactory;

    public TestRepositorySystem() {
        this(null, null);
    }

    @Inject
    public TestRepositorySystem(ModelReader modelReader, ArtifactFactory artifactFactory) {
        this.modelReader = modelReader;
        this.artifactFactory = artifactFactory;
    }

    @Override
    public ArtifactRepository buildArtifactRepository(Repository repository) throws InvalidRepositoryException {
        return new MavenArtifactRepository(
                repository.getId(),
                repository.getUrl(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy());
    }

    @Override
    public Artifact createArtifact(String groupId, String artifactId, String version, String packaging) {
        return createArtifact(groupId, artifactId, version, null, packaging);
    }

    @Override
    public Artifact createArtifact(String groupId, String artifactId, String version, String scope, String type) {
        return new DefaultArtifact(groupId, artifactId, version, scope, type, null, new TestArtifactHandler(type));
    }

    @Override
    public ArtifactRepository createArtifactRepository(
            String id,
            String url,
            ArtifactRepositoryLayout repositoryLayout,
            ArtifactRepositoryPolicy snapshots,
            ArtifactRepositoryPolicy releases) {
        return new MavenArtifactRepository(id, url, repositoryLayout, snapshots, releases);
    }

    @Override
    public Artifact createArtifactWithClassifier(
            String groupId, String artifactId, String version, String type, String classifier) {
        return new DefaultArtifact(groupId, artifactId, version, null, type, classifier, new TestArtifactHandler(type));
    }

    @Override
    public ArtifactRepository createDefaultLocalRepository() throws InvalidRepositoryException {
        return createLocalRepository(
                new File(System.getProperty("basedir", "."), "target/local-repo").getAbsoluteFile());
    }

    @Override
    public ArtifactRepository createDefaultRemoteRepository() throws InvalidRepositoryException {
        return new MavenArtifactRepository(
                DEFAULT_REMOTE_REPO_ID,
                "file://"
                        + new File(System.getProperty("basedir", "."), "src/test/remote-repo")
                                .getAbsoluteFile()
                                .toURI()
                                .getPath(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy());
    }

    @Override
    public Artifact createDependencyArtifact(Dependency dependency) {
        Artifact artifact = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getScope(),
                dependency.getType(),
                dependency.getClassifier(),
                new TestArtifactHandler(dependency.getType()));

        if (Artifact.SCOPE_SYSTEM.equals(dependency.getScope())) {
            artifact.setFile(new File(dependency.getSystemPath()));
            artifact.setResolved(true);
        }

        return artifact;
    }

    @Override
    public ArtifactRepository createLocalRepository(File localRepository) throws InvalidRepositoryException {
        return new MavenArtifactRepository(
                MavenRepositorySystem.DEFAULT_LOCAL_REPO_ID,
                "file://" + localRepository.toURI().getPath(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy());
    }

    @Override
    public Artifact createPluginArtifact(Plugin plugin) {
        VersionRange versionRange;
        try {
            String version = plugin.getVersion();
            if (version == null || version.isEmpty()) {
                version = "RELEASE";
            }
            versionRange = VersionRange.createFromVersionSpec(version);
        } catch (InvalidVersionSpecificationException e) {
            return null;
        }

        return artifactFactory.createPluginArtifact(plugin.getGroupId(), plugin.getArtifactId(), versionRange);
    }

    @Override
    public Artifact createProjectArtifact(String groupId, String artifactId, String version) {
        return createArtifact(groupId, artifactId, version, "pom");
    }

    @Override
    public List<ArtifactRepository> getEffectiveRepositories(List<ArtifactRepository> repositories) {
        return repositories;
    }

    @Override
    public Mirror getMirror(ArtifactRepository repository, List<Mirror> mirrors) {
        return null;
    }

    @Override
    public void injectAuthentication(List<ArtifactRepository> repositories, List<Server> servers) {}

    @Override
    public void injectMirror(List<ArtifactRepository> repositories, List<Mirror> mirrors) {}

    @Override
    public void injectProxy(List<ArtifactRepository> repositories, List<Proxy> proxies) {}

    @Override
    public void publish(
            ArtifactRepository repository, File source, String remotePath, ArtifactTransferListener transferListener)
            throws ArtifactTransferFailedException {
        // TODO Auto-generated method stub

    }

    @Override
    public ArtifactResolutionResult resolve(ArtifactResolutionRequest request) {
        ArtifactResolutionResult result = new ArtifactResolutionResult();

        if (request.isResolveRoot()) {
            try {
                resolve(request.getArtifact(), request);
                result.addArtifact(request.getArtifact());
            } catch (IOException e) {
                result.addMissingArtifact(request.getArtifact());
            }
        }

        if (request.isResolveTransitively()) {
            Map<String, Artifact> artifacts = new LinkedHashMap<>();

            if (request.getArtifactDependencies() != null) {
                for (Artifact artifact : request.getArtifactDependencies()) {
                    artifacts.put(artifact.getDependencyConflictId(), artifact);
                }
            }

            List<Dependency> dependencies = new ArrayList<>();
            if (request.getArtifact() instanceof ArtifactWithDependencies) {
                dependencies = ((ArtifactWithDependencies) request.getArtifact()).getDependencies();
            } else {
                Artifact pomArtifact = createProjectArtifact(
                        request.getArtifact().getGroupId(),
                        request.getArtifact().getArtifactId(),
                        request.getArtifact().getVersion());
                File pomFile = new File(
                        request.getLocalRepository().getBasedir(),
                        request.getLocalRepository().pathOf(pomArtifact));

                try {
                    Model model = modelReader.read(pomFile, null).getDelegate();

                    dependencies = Dependency.dependencyToApiV3(model.getDependencies());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (Dependency dependency : dependencies) {
                Artifact artifact = createDependencyArtifact(dependency);
                if (!artifacts.containsKey(artifact.getDependencyConflictId())) {
                    artifacts.put(artifact.getDependencyConflictId(), artifact);
                }
            }

            for (Artifact artifact : artifacts.values()) {
                try {
                    resolve(artifact, request);
                    result.addArtifact(artifact);
                } catch (IOException e) {
                    result.addMissingArtifact(artifact);
                }
            }
        }

        return result;
    }

    private void resolve(Artifact artifact, ArtifactResolutionRequest request) throws IOException {
        if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
            return;
        }

        ArtifactRepository localRepo = request.getLocalRepository();

        File localFile = new File(localRepo.getBasedir(), localRepo.pathOf(artifact));

        artifact.setFile(localFile);

        if (!localFile.exists()) {
            if (request.getRemoteRepositories().isEmpty()) {
                throw new IOException(localFile + " does not exist and no remote repositories are configured");
            }

            ArtifactRepository remoteRepo = request.getRemoteRepositories().get(0);

            File remoteFile = new File(remoteRepo.getBasedir(), remoteRepo.pathOf(artifact));

            Files.createDirectories(localFile.toPath().getParent());
            Files.copy(remoteFile.toPath(), localFile.toPath());
        }

        artifact.setResolved(true);
    }

    @Override
    public void retrieve(
            ArtifactRepository repository,
            File destination,
            String remotePath,
            ArtifactTransferListener transferListener)
            throws ArtifactTransferFailedException, ArtifactDoesNotExistException {
        // TODO Auto-generated method stub

    }

    @Override
    public void injectMirror(RepositorySystemSession session, List<ArtifactRepository> repositories) {}

    @Override
    public void injectProxy(RepositorySystemSession session, List<ArtifactRepository> repositories) {}

    @Override
    public void injectAuthentication(RepositorySystemSession session, List<ArtifactRepository> repositories) {}
}
