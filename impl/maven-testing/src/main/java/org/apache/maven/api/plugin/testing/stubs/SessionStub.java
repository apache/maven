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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
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
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.internal.impl.DefaultModelXmlFactory;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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
public class SessionStub {

    public static Session getMockSession(String localRepo) {
        LocalRepository localRepository = mock(LocalRepository.class);
        when(localRepository.getId()).thenReturn("local");
        when(localRepository.getPath()).thenReturn(Paths.get(localRepo));
        return getMockSession(localRepository);
    }

    public static Session getMockSession(LocalRepository localRepository) {
        Session session = mock(Session.class);

        RepositoryFactory repositoryFactory = mock(RepositoryFactory.class);
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

        LocalRepositoryManager localRepositoryManager = mock(LocalRepositoryManager.class);
        when(localRepositoryManager.getPathForLocalArtifact(any(), any(), any()))
                .thenAnswer(iom -> {
                    LocalRepository localRepo = iom.getArgument(1, LocalRepository.class);
                    Artifact artifact = iom.getArgument(2, Artifact.class);
                    return localRepo.getPath().resolve(getPathForArtifact(artifact, true));
                });

        ArtifactInstaller artifactInstaller = mock(ArtifactInstaller.class);
        doAnswer(iom -> {
                    artifactInstaller.install(ArtifactInstallerRequest.build(
                            iom.getArgument(0, Session.class), iom.getArgument(1, Collection.class)));
                    return null;
                })
                .when(artifactInstaller)
                .install(any(Session.class), ArgumentMatchers.<Collection<Artifact>>any());

        ArtifactDeployer artifactDeployer = mock(ArtifactDeployer.class);
        doAnswer(iom -> {
                    artifactDeployer.deploy(ArtifactDeployerRequest.build(
                            iom.getArgument(0, Session.class),
                            iom.getArgument(1, RemoteRepository.class),
                            iom.getArgument(2, Collection.class)));
                    return null;
                })
                .when(artifactDeployer)
                .deploy(any(), any(), any());

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

        ProjectManager projectManager = mock(ProjectManager.class);
        Map<Project, Collection<Artifact>> attachedArtifacts = new HashMap<>();
        doAnswer(iom -> {
                    Project project = iom.getArgument(1, Project.class);
                    String type = iom.getArgument(2, String.class);
                    Path path = iom.getArgument(3, Path.class);
                    Artifact artifact = session.createArtifact(
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
                    Artifact artifact = iom.getArgument(1, Artifact.class);
                    Path path = iom.getArgument(2, Path.class);
                    artifactManager.setPath(artifact, path);
                    attachedArtifacts
                            .computeIfAbsent(project, p -> new ArrayList<>())
                            .add(artifact);
                    return null;
                })
                .when(projectManager)
                .attachArtifact(any(Project.class), any(Artifact.class), any(Path.class));
        when(projectManager.getAttachedArtifacts(any()))
                .then(iom ->
                        attachedArtifacts.computeIfAbsent(iom.getArgument(0, Project.class), p -> new ArrayList<>()));

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

        ProjectBuilder projectBuilder = mock(ProjectBuilder.class);
        when(projectBuilder.build(any(ProjectBuilderRequest.class))).then(iom -> {
            ProjectBuilderRequest request = iom.getArgument(0, ProjectBuilderRequest.class);
            ProjectBuilderResult result = mock(ProjectBuilderResult.class);
            Model model = new MavenXpp3Reader()
                    .read(request.getSource().get().getInputStream())
                    .getDelegate();
            ProjectStub projectStub = new ProjectStub();
            projectStub.setModel(model);
            ArtifactStub artifactStub = new ArtifactStub(
                    model.getGroupId(), model.getArtifactId(), "", model.getVersion(), model.getPackaging());
            projectStub.setArtifact(artifactStub);
            when(result.getProject()).thenReturn(Optional.of(projectStub));
            return result;
        });

        Properties sysProps = new Properties();
        Properties usrProps = new Properties();
        doReturn(sysProps).when(session).getSystemProperties();
        doReturn(usrProps).when(session).getUserProperties();

        when(session.getLocalRepository()).thenReturn(localRepository);
        when(session.getService(RepositoryFactory.class)).thenReturn(repositoryFactory);
        when(session.getService(ProjectBuilder.class)).thenReturn(projectBuilder);
        when(session.getService(LocalRepositoryManager.class)).thenReturn(localRepositoryManager);
        when(session.getService(ProjectManager.class)).thenReturn(projectManager);
        when(session.getService(ArtifactManager.class)).thenReturn(artifactManager);
        when(session.getService(ArtifactInstaller.class)).thenReturn(artifactInstaller);
        when(session.getService(ArtifactDeployer.class)).thenReturn(artifactDeployer);
        when(session.getService(ArtifactFactory.class)).thenReturn(artifactFactory);
        when(session.getService(ModelXmlFactory.class)).thenReturn(new DefaultModelXmlFactory());

        when(session.getPathForLocalArtifact(any(Artifact.class)))
                .then(iom -> localRepositoryManager.getPathForLocalArtifact(
                        session, session.getLocalRepository(), iom.getArgument(0, Artifact.class)));
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
        when(session.createRemoteRepository(anyString(), anyString())).thenAnswer(iom -> {
            String id = iom.getArgument(0, String.class);
            String url = iom.getArgument(1, String.class);
            return session.getService(RepositoryFactory.class).createRemote(id, url);
        });
        doAnswer(iom -> artifactManager.getPath(iom.getArgument(0, Artifact.class)))
                .when(session)
                .getArtifactPath(any());

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
}
