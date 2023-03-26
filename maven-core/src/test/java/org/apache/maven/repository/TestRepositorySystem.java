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
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.eclipse.aether.RepositorySystemSession;

/**
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class TestRepositorySystem implements RepositorySystem {

    @Inject
    public TestRepositorySystem() {}

    public ArtifactRepository buildArtifactRepository(Repository repository) throws InvalidRepositoryException {
        return new MavenArtifactRepository(
                repository.getId(),
                repository.getUrl(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy());
    }

    public Artifact createArtifact(String groupId, String artifactId, String version, String packaging) {
        return createArtifact(groupId, artifactId, version, null, packaging);
    }

    public Artifact createArtifact(String groupId, String artifactId, String version, String scope, String type) {
        return new DefaultArtifact(groupId, artifactId, version, scope, type, null, new TestArtifactHandler(type));
    }

    public ArtifactRepository createArtifactRepository(
            String id,
            String url,
            ArtifactRepositoryLayout repositoryLayout,
            ArtifactRepositoryPolicy snapshots,
            ArtifactRepositoryPolicy releases) {
        return new MavenArtifactRepository(id, url, repositoryLayout, snapshots, releases);
    }

    public Artifact createArtifactWithClassifier(
            String groupId, String artifactId, String version, String type, String classifier) {
        return new DefaultArtifact(groupId, artifactId, version, null, type, classifier, new TestArtifactHandler(type));
    }

    public ArtifactRepository createDefaultLocalRepository() throws InvalidRepositoryException {
        return createLocalRepository(
                new File(System.getProperty("basedir", "."), "target/local-repo").getAbsoluteFile());
    }

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

    public ArtifactRepository createLocalRepository(File localRepository) throws InvalidRepositoryException {
        return new MavenArtifactRepository(
                DEFAULT_LOCAL_REPO_ID,
                "file://" + localRepository.toURI().getPath(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy());
    }

    public Artifact createPluginArtifact(Plugin plugin) {
        throw new UnsupportedOperationException();
    }

    public Artifact createProjectArtifact(String groupId, String artifactId, String version) {
        return createArtifact(groupId, artifactId, version, "pom");
    }

    public List<ArtifactRepository> getEffectiveRepositories(List<ArtifactRepository> repositories) {
        return repositories;
    }

    public Mirror getMirror(ArtifactRepository repository, List<Mirror> mirrors) {
        return null;
    }

    public void injectAuthentication(List<ArtifactRepository> repositories, List<Server> servers) {}

    public void injectMirror(List<ArtifactRepository> repositories, List<Mirror> mirrors) {}

    public void injectProxy(List<ArtifactRepository> repositories, List<Proxy> proxies) {}

    public void publish(
            ArtifactRepository repository, File source, String remotePath, ArtifactTransferListener transferListener) {}

    public ArtifactResolutionResult resolve(ArtifactResolutionRequest request) {
        throw new UnsupportedOperationException();
    }

    public void retrieve(
            ArtifactRepository repository,
            File destination,
            String remotePath,
            ArtifactTransferListener transferListener) {}

    public void injectMirror(RepositorySystemSession session, List<ArtifactRepository> repositories) {}

    public void injectProxy(RepositorySystemSession session, List<ArtifactRepository> repositories) {}

    public void injectAuthentication(RepositorySystemSession session, List<ArtifactRepository> repositories) {}
}
