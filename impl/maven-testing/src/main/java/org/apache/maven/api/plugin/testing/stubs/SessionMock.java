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
package org.apache.maven.api.plugin.testing.stubs;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactDeployerRequest;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactFactoryRequest;
import org.apache.maven.api.services.ArtifactInstaller;
import org.apache.maven.api.services.ArtifactInstallerRequest;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.LocalRepositoryManager;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.ProjectBuilderResult;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.internal.impl.DefaultModelVersionParser;
import org.apache.maven.internal.impl.DefaultModelXmlFactory;
import org.apache.maven.internal.impl.DefaultVersionParser;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.model.v4.MavenStaxReader;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.mockito.ArgumentMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 *
 */
public class SessionMock {

    public static InternalSession getMockSession(String localRepo) {
        LocalRepository localRepository = mock(LocalRepository.class);
        when(localRepository.getId()).thenReturn("local");
        when(localRepository.getPath()).thenReturn(Paths.get(localRepo));
        return getMockSession(localRepository);
    }

    @SuppressWarnings("checkstyle:MethodLength")
    public static InternalSession getMockSession(LocalRepository localRepository) {
        InternalSession session = mock(InternalSession.class);

        //
        // RepositoryFactory
        //
        RepositoryFactory repositoryFactory = mock(RepositoryFactory.class);
        when(session.createRemoteRepository(anyString(), anyString())).thenAnswer(iom -> {
            String id = iom.getArgument(0, String.class);
            String url = iom.getArgument(1, String.class);
            return session.getService(RepositoryFactory.class).createRemote(id, url);
        });
        when(session.createRemoteRepository(any()))
                .thenAnswer(iom -> repositoryFactory.createRemote(iom.getArgument(0, Repository.class)));
        when(repositoryFactory.createRemote(any(Repository.class))).thenAnswer(iom -> {
            Repository repository = iom.getArgument(0, Repository.class);
            return repositoryFactory.createRemote(repository.getId(), repository.getUrl());
        });
        when(repositoryFactory.createRemote(anyString(), anyString())).thenAnswer(iom -> {
            String id = iom.getArgument(0, String.class);
            String url = iom.getArgument(1, String.class);
            RemoteRepository remoteRepository =
                    mock(RemoteRepository.class, withSettings().lenient());
            when(remoteRepository.getId()).thenReturn(id);
            when(remoteRepository.getUrl()).thenReturn(url);
            when(remoteRepository.getProtocol()).thenReturn(URI.create(url).getScheme());
            return remoteRepository;
        });
        when(session.getService(RepositoryFactory.class)).thenReturn(repositoryFactory);

        //
        // VersionParser
        //
        VersionParser versionParser =
                new DefaultVersionParser(new DefaultModelVersionParser(new GenericVersionScheme()));
        when(session.parseVersion(any()))
                .thenAnswer(iom -> versionParser.parseVersion(iom.getArgument(0, String.class)));
        when(session.getService(VersionParser.class)).thenReturn(versionParser);

        //
        // LocalRepositoryManager
        //
        LocalRepositoryManager localRepositoryManager = mock(LocalRepositoryManager.class);
        when(session.getPathForLocalArtifact(any(Artifact.class)))
                .then(iom -> localRepositoryManager.getPathForLocalArtifact(
                        session, session.getLocalRepository(), iom.getArgument(0, Artifact.class)));
        when(session.getPathForRemoteArtifact(any(), any()))
                .thenAnswer(iom -> localRepositoryManager.getPathForRemoteArtifact(
                        session,
                        session.getLocalRepository(),
                        iom.getArgument(0, RemoteRepository.class),
                        iom.getArgument(1, Artifact.class)));
        when(localRepositoryManager.getPathForLocalArtifact(any(), any(), any()))
                .thenAnswer(iom -> {
                    LocalRepository localRepo = iom.getArgument(1, LocalRepository.class);
                    Artifact artifact = iom.getArgument(2, Artifact.class);
                    return localRepo.getPath().resolve(getPathForArtifact(artifact, true));
                });
        when(session.getService(LocalRepositoryManager.class)).thenReturn(localRepositoryManager);

        //
        // ArtifactInstaller
        //
        ArtifactInstaller artifactInstaller = mock(ArtifactInstaller.class);
        doAnswer(iom -> {
                    artifactInstaller.install(
                            ArtifactInstallerRequest.build(session, iom.getArgument(0, Collection.class)));
                    return null;
                })
                .when(session)
                .installArtifacts(any(Collection.class));
        doAnswer(iom -> {
                    artifactInstaller.install(ArtifactInstallerRequest.build(
                            session, Arrays.asList(iom.getArgument(0, Artifact[].class))));
                    return null;
                })
                .when(session)
                .installArtifacts(any(Artifact[].class));
        doAnswer(iom -> {
                    artifactInstaller.install(ArtifactInstallerRequest.build(
                            iom.getArgument(0, Session.class), iom.getArgument(1, Collection.class)));
                    return null;
                })
                .when(artifactInstaller)
                .install(any(Session.class), ArgumentMatchers.<Collection<Artifact>>any());
        when(session.getService(ArtifactInstaller.class)).thenReturn(artifactInstaller);

        //
        // ArtifactDeployer
        //
        ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);
        doAnswer(iom -> {
                    artifactDeployer.deploy(ArtifactDeployerRequest.build(
                            iom.getArgument(0, Session.class),
                            iom.getArgument(1, RemoteRepository.class),
                            Arrays.asList(iom.getArgument(2, Artifact[].class))));
                    return null;
                })
                .when(session)
                .deployArtifact(any(), any());
        doAnswer(iom -> {
                    artifactDeployer.deploy(ArtifactDeployerRequest.build(
                            iom.getArgument(0, Session.class),
                            iom.getArgument(1, RemoteRepository.class),
                            iom.getArgument(2, Collection.class)));
                    return null;
                })
                .when(artifactDeployer)
                .deploy(any(), any(), any());
        when(session.getService(ArtifactDeployer.class)).thenReturn(artifactDeployer);

        //
        // ArtifactManager
        //
        ArtifactManager artifactManager = mock(ArtifactManager.class);
        Map<Artifact, Path> paths = new HashMap<>();
        doAnswer(iom -> {
                    paths.put(iom.getArgument(0), iom.getArgument(1));
                    return null;
                })
                .when(artifactManager)
                .setPath(any(), any());
        doAnswer(iom -> Optional.ofNullable(paths.get(iom.getArgument(0, Artifact.class))))
                .when(artifactManager)
                .getPath(any());
        doAnswer(iom -> artifactManager.getPath(iom.getArgument(0, Artifact.class)))
                .when(session)
                .getArtifactPath(any());
        when(session.getService(ArtifactManager.class)).thenReturn(artifactManager);

        //
        // ProjectManager
        //
        ProjectManager projectManager = mock(ProjectManager.class);
        Map<Project, Collection<Artifact>> attachedArtifacts = new HashMap<>();
        doAnswer(iom -> {
                    Project project = iom.getArgument(1, Project.class);
                    String type = iom.getArgument(2, String.class);
                    Path path = iom.getArgument(3, Path.class);
                    ProducedArtifact artifact = session.createProducedArtifact(
                            project.getGroupId(), project.getArtifactId(), project.getVersion(), null, null, type);
                    artifactManager.setPath(artifact, path);
                    attachedArtifacts
                            .computeIfAbsent(project, p -> new ArrayList<>())
                            .add(artifact);
                    return null;
                })
                .when(projectManager)
                .attachArtifact(same(session), any(Project.class), any(), any());
        doAnswer(iom -> {
                    Project project = iom.getArgument(0, Project.class);
                    ProducedArtifact artifact = iom.getArgument(1, ProducedArtifact.class);
                    Path path = iom.getArgument(2, Path.class);
                    artifactManager.setPath(artifact, path);
                    attachedArtifacts
                            .computeIfAbsent(project, p -> new ArrayList<>())
                            .add(artifact);
                    return null;
                })
                .when(projectManager)
                .attachArtifact(any(Project.class), any(ProducedArtifact.class), any(Path.class));
        when(projectManager.getAttachedArtifacts(any()))
                .then(iom ->
                        attachedArtifacts.computeIfAbsent(iom.getArgument(0, Project.class), p -> new ArrayList<>()));
        when(projectManager.getAllArtifacts(any())).then(iom -> {
            Project project = iom.getArgument(0, Project.class);
            List<Artifact> result = new ArrayList<>();
            result.addAll(project.getArtifacts());
            result.addAll(attachedArtifacts.computeIfAbsent(project, p -> new ArrayList<>()));
            return result;
        });
        when(session.getService(ProjectManager.class)).thenReturn(projectManager);

        //
        // ArtifactFactory
        //
        ArtifactFactory artifactFactory = mock(ArtifactFactory.class);
        when(artifactFactory.create(any())).then(iom -> {
            ArtifactFactoryRequest request = iom.getArgument(0, ArtifactFactoryRequest.class);
            String classifier = request.getClassifier();
            String extension = request.getExtension();
            String type = request.getType();
            if (classifier == null) {
                classifier = "";
            }
            if (extension == null) {
                extension = type != null ? type : "";
            }
            return new ArtifactStub(
                    request.getGroupId(), request.getArtifactId(), classifier, request.getVersion(), extension);
        });
        when(artifactFactory.createProduced(any())).then(iom -> {
            ArtifactFactoryRequest request = iom.getArgument(0, ArtifactFactoryRequest.class);
            String classifier = request.getClassifier();
            String extension = request.getExtension();
            String type = request.getType();
            if (classifier == null) {
                classifier = "";
            }
            if (extension == null) {
                extension = type != null ? type : "";
            }
            return new ProducedArtifactStub(
                    request.getGroupId(), request.getArtifactId(), classifier, request.getVersion(), extension);
        });
        when(session.createArtifact(any(), any(), any(), any(), any(), any())).thenAnswer(iom -> {
            String groupId = iom.getArgument(0, String.class);
            String artifactId = iom.getArgument(1, String.class);
            String version = iom.getArgument(2, String.class);
            String classifier = iom.getArgument(3, String.class);
            String extension = iom.getArgument(4, String.class);
            String type = iom.getArgument(5, String.class);
            return session.getService(ArtifactFactory.class)
                    .create(ArtifactFactoryRequest.builder()
                            .session(session)
                            .groupId(groupId)
                            .artifactId(artifactId)
                            .version(version)
                            .classifier(classifier)
                            .extension(extension)
                            .type(type)
                            .build());
        });
        when(session.createArtifact(any(), any(), any(), any())).thenAnswer(iom -> {
            String groupId = iom.getArgument(0, String.class);
            String artifactId = iom.getArgument(1, String.class);
            String version = iom.getArgument(2, String.class);
            String extension = iom.getArgument(3, String.class);
            return session.getService(ArtifactFactory.class)
                    .create(ArtifactFactoryRequest.builder()
                            .session(session)
                            .groupId(groupId)
                            .artifactId(artifactId)
                            .version(version)
                            .extension(extension)
                            .build());
        });
        when(session.createProducedArtifact(any(), any(), any(), any(), any(), any()))
                .thenAnswer(iom -> {
                    String groupId = iom.getArgument(0, String.class);
                    String artifactId = iom.getArgument(1, String.class);
                    String version = iom.getArgument(2, String.class);
                    String classifier = iom.getArgument(3, String.class);
                    String extension = iom.getArgument(4, String.class);
                    String type = iom.getArgument(5, String.class);
                    return session.getService(ArtifactFactory.class)
                            .createProduced(ArtifactFactoryRequest.builder()
                                    .session(session)
                                    .groupId(groupId)
                                    .artifactId(artifactId)
                                    .version(version)
                                    .classifier(classifier)
                                    .extension(extension)
                                    .type(type)
                                    .build());
                });
        when(session.createProducedArtifact(any(), any(), any(), any())).thenAnswer(iom -> {
            String groupId = iom.getArgument(0, String.class);
            String artifactId = iom.getArgument(1, String.class);
            String version = iom.getArgument(2, String.class);
            String extension = iom.getArgument(3, String.class);
            return session.getService(ArtifactFactory.class)
                    .createProduced(ArtifactFactoryRequest.builder()
                            .session(session)
                            .groupId(groupId)
                            .artifactId(artifactId)
                            .version(version)
                            .extension(extension)
                            .build());
        });
        when(session.getService(ArtifactFactory.class)).thenReturn(artifactFactory);

        //
        // ProjectBuilder
        //
        ProjectBuilder projectBuilder = mock(ProjectBuilder.class);
        when(projectBuilder.build(any(ProjectBuilderRequest.class))).then(iom -> {
            ProjectBuilderRequest request = iom.getArgument(0, ProjectBuilderRequest.class);
            ProjectBuilderResult result = mock(ProjectBuilderResult.class);
            Model model = new MavenStaxReader().read(request.getSource().get().openStream());
            ProjectStub projectStub = new ProjectStub();
            projectStub.setModel(model);
            ProducedArtifactStub artifactStub = new ProducedArtifactStub(
                    model.getGroupId(), model.getArtifactId(), "", model.getVersion(), model.getPackaging());
            if (!"pom".equals(model.getPackaging())) {
                projectStub.setMainArtifact(artifactStub);
            }
            when(result.getProject()).thenReturn(Optional.of(projectStub));
            return result;
        });
        when(session.getService(ProjectBuilder.class)).thenReturn(projectBuilder);

        //
        // ModelXmlFactory
        //
        when(session.getService(ModelXmlFactory.class)).thenReturn(new DefaultModelXmlFactory());

        //
        // Other
        //
        Properties sysProps = new Properties();
        Properties usrProps = new Properties();
        doReturn(sysProps).when(session).getSystemProperties();
        doReturn(usrProps).when(session).getUserProperties();
        when(session.getLocalRepository()).thenReturn(localRepository);
        when(session.getData()).thenReturn(new TestSessionData());
        when(session.withLocalRepository(any()))
                .thenAnswer(iom -> getMockSession(iom.getArgument(0, LocalRepository.class)));

        return session;
    }

    static String getPathForArtifact(Artifact artifact, boolean local) {
        StringBuilder path = new StringBuilder(128);
        path.append(artifact.getGroupId().replace('.', '/')).append('/');
        path.append(artifact.getArtifactId()).append('/');
        path.append(artifact.getVersion()).append('/');
        path.append(artifact.getArtifactId()).append('-');
        path.append(artifact.getVersion());
        if (artifact.getClassifier().length() > 0) {
            path.append('-').append(artifact.getClassifier());
        }
        if (artifact.getExtension().length() > 0) {
            path.append('.').append(artifact.getExtension());
        }
        return path.toString();
    }

    static class TestSessionData implements SessionData {
        private final Map<Key<?>, Object> map = new ConcurrentHashMap<>();

        @Override
        public <T> void set(Key<T> key, T value) {
            map.put(key, value);
        }

        @Override
        public <T> boolean replace(Key<T> key, T oldValue, T newValue) {
            return map.replace(key, oldValue, newValue);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Key<T> key) {
            return (T) map.get(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T computeIfAbsent(Key<T> key, Supplier<T> supplier) {
            return (T) map.computeIfAbsent(key, k -> supplier.get());
        }
    }
}
