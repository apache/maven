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
package org.apache.maven.impl;

import java.util.List;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.services.ArtifactDeployerRequest;
import org.apache.maven.api.services.ArtifactInstallerRequest;
import org.apache.maven.api.services.RequestTrace;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepositoryRequestTraceTest {

    @Test
    void installerPropagatesAndRestoresRequestTrace() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession resolverSession = mock(RepositorySystemSession.class);
        InternalSession session = mock(InternalSession.class);
        ArtifactInstallerRequest request = mock(ArtifactInstallerRequest.class);
        RequestTrace context = new RequestTrace("context");
        RequestTrace explicitTrace = new RequestTrace("install", context, "install-request");
        when(request.getSession()).thenReturn(session);
        when(request.getTrace()).thenReturn(explicitTrace);
        when(request.getArtifacts()).thenReturn(List.of());
        when(session.getCurrentTrace()).thenReturn(context);
        when(session.getSession()).thenReturn(resolverSession);
        when(session.toArtifacts(List.of())).thenReturn(List.of());

        new DefaultArtifactInstaller(repositorySystem).install(request);

        ArgumentCaptor<InstallRequest> captor = ArgumentCaptor.forClass(InstallRequest.class);
        verify(repositorySystem).install(eq(resolverSession), captor.capture());
        assertEquals("install-request", captor.getValue().getTrace().getData());
        verify(session).setCurrentTrace(explicitTrace);
        verify(session).setCurrentTrace(context);
    }

    @Test
    void deployerPropagatesAndRestoresInheritedTrace() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession resolverSession = mock(RepositorySystemSession.class);
        InternalSession session = mock(InternalSession.class);
        ArtifactDeployerRequest request = mock(ArtifactDeployerRequest.class);
        RemoteRepository repository = mock(RemoteRepository.class);
        org.eclipse.aether.repository.RemoteRepository resolverRepository =
                new org.eclipse.aether.repository.RemoteRepository.Builder("test", "default", "https://repo.example")
                        .build();
        RequestTrace context = new RequestTrace("context");
        when(request.getSession()).thenReturn(session);
        when(request.getArtifacts()).thenReturn(List.of());
        when(request.getRepository()).thenReturn(repository);
        when(session.getCurrentTrace()).thenReturn(context);
        when(session.getSession()).thenReturn(resolverSession);
        when(session.getRepositorySystem()).thenReturn(repositorySystem);
        when(session.toRepository(repository)).thenReturn(resolverRepository);
        when(session.toArtifacts(List.of())).thenReturn(List.of());

        new DefaultArtifactDeployer().deploy(request);

        ArgumentCaptor<DeployRequest> captor = ArgumentCaptor.forClass(DeployRequest.class);
        verify(repositorySystem).deploy(eq(resolverSession), captor.capture());
        assertEquals(request, captor.getValue().getTrace().getData());
        verify(session).setCurrentTrace(context);
    }
}
