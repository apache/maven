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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.cache.RequestCache;
import org.apache.maven.api.cache.RequestCacheFactory;
import org.apache.maven.api.services.ArtifactDeployerRequest;
import org.apache.maven.api.services.ArtifactInstallerException;
import org.apache.maven.api.services.ArtifactInstallerRequest;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.ToolchainsBuilderRequest;
import org.apache.maven.api.services.ToolchainsBuilderResult;
import org.apache.maven.impl.cache.DefaultRequestCache;
import org.apache.maven.impl.cache.DefaultRequestCacheFactory;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.scope.ResolutionScope;
import org.eclipse.aether.scope.ScopeManager;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class OperationPipelineTest {
    private AbstractSession newSession(RepositorySystem repositorySystem) {
        Lookup lookup = mock(Lookup.class);
        when(lookup.lookup(RequestCacheFactory.class)).thenReturn(new DefaultRequestCacheFactory());
        AbstractSession session = mock(
                AbstractSession.class,
                withSettings()
                        .useConstructor(
                                new DefaultRepositorySystemSession(h -> false),
                                repositorySystem,
                                List.of(),
                                List.of(),
                                lookup)
                        .defaultAnswer(CALLS_REAL_METHODS));
        doReturn(Map.of()).when(session).getUserProperties();
        doReturn(Map.of()).when(session).getSystemProperties();
        doReturn(Map.of()).when(session).getEffectiveProperties();
        return session;
    }

    @Test
    void settingsPipelineAcceptsCustomSessionImplementations() throws Exception {
        AbstractSession storage = newSession(mock(RepositorySystem.class));
        Session session = mock(Session.class);
        doReturn(storage.getData()).when(session).getData();
        when(session.getUserProperties()).thenReturn(Map.of());
        Source source = mock(Source.class);
        when(source.getLocation()).thenReturn("settings.xml");
        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(source)
                .build();
        when(source.openStream()).thenAnswer(call -> {
            assertSame(request, RequestTraceHelper.getCurrentTrace(session).data());
            return new ByteArrayInputStream("<settings/>".getBytes(StandardCharsets.UTF_8));
        });
        DefaultSettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
        SettingsBuilderResult result = builder.build(request);
        assertSame(result, builder.build(request));
        verify(source).openStream();
        assertNull(RequestTraceHelper.getCurrentTrace(session));
    }

    @Test
    void dependencyCollectionUsesRequestCacheWithoutRecursiveExecution() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        AbstractSession session = newSession(repositorySystem);
        ScopeManager scopeManager = mock(ScopeManager.class);
        ResolutionScope scope = mock(ResolutionScope.class);
        when(scopeManager.getResolutionScope(PathScope.MAIN_RUNTIME.id())).thenReturn(Optional.of(scope));
        ((DefaultRepositorySystemSession) session.getSession()).setScopeManager(scopeManager);
        DependencyNode root = mock(DependencyNode.class);
        doReturn(mock(Node.class)).when(session).getNode(root, false);
        when(repositorySystem.collectDependencies(eq(session.getSession()), any(CollectRequest.class)))
                .thenAnswer(call -> new CollectResult(call.getArgument(1)).setRoot(root));
        DependencyResolverRequest request = DependencyResolverRequest.builder()
                .session(session)
                .requestType(DependencyResolverRequest.RequestType.COLLECT)
                .pathScope(PathScope.MAIN_RUNTIME)
                .build();
        DefaultDependencyResolver resolver = new DefaultDependencyResolver();
        var result = resolver.collect(request);
        assertSame(request, result.getRequest());
        assertSame(result, resolver.collect(request));
        assertSame(result, resolver.resolve(request));
        verify(repositorySystem).collectDependencies(eq(session.getSession()), any(CollectRequest.class));
        assertNull(session.getCurrentTrace());
    }

    @Test
    void transportsAreCreatedAgainAfterClosing() throws Exception {
        AbstractSession session = newSession(mock(RepositorySystem.class));
        doReturn(Map.of(Constants.MAVEN_CACHE_CONFIG_PROPERTY, "* { scope: session, ref: hard }"))
                .when(session)
                .getUserProperties();
        DefaultRemoteRepository repository = new DefaultRemoteRepository(
                new org.eclipse.aether.repository.RemoteRepository.Builder("test", "default", "https://repo.example/")
                        .build());
        TransporterProvider provider = mock(TransporterProvider.class);
        when(provider.newTransporter(session.getSession(), repository.getRepository()))
                .thenAnswer(call -> mock(Transporter.class));
        DefaultTransportProvider transports = new DefaultTransportProvider(provider);
        var first = transports.transport(session, repository);
        first.close();
        var second = transports.transport(session, repository);
        assertNotSame(first, second);
        second.close();
        verify(provider, times(2)).newTransporter(session.getSession(), repository.getRepository());
        assertNull(session.getCurrentTrace());
    }

    @Test
    void settingsRequestsUseSessionCache() throws Exception {
        AbstractSession session = newSession(mock(RepositorySystem.class));
        Source source = mock(Source.class);
        when(source.getLocation()).thenReturn("settings.xml");
        when(source.openStream())
                .thenAnswer(call -> new ByteArrayInputStream("<settings/>".getBytes(StandardCharsets.UTF_8)));
        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(source)
                .build();
        DefaultSettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());

        SettingsBuilderResult first = builder.build(request);
        assertSame(first, builder.build(request));
        assertSame(request, first.getRequest());
        verify(source).openStream();
        assertNull(session.getCurrentTrace());
    }

    @Test
    void toolchainsRequestsUseSessionCache() throws Exception {
        AbstractSession session = newSession(mock(RepositorySystem.class));
        Source source = mock(Source.class);
        ToolchainsBuilderRequest request = ToolchainsBuilderRequest.builder()
                .session(session)
                .userToolchainsSource(source)
                .build();
        DefaultToolchainsBuilder builder =
                new DefaultToolchainsBuilder(new DefaultInterpolator(), new DefaultToolchainsXmlFactory());

        ToolchainsBuilderResult first = builder.build(request);
        assertSame(first, builder.build(request));
        assertSame(request, first.getRequest());
        verify(source).openStream();
        assertNull(session.getCurrentTrace());
    }

    @Test
    void buildersStillWorkBeforeFullSessionExists() {
        ProtoSession session =
                ProtoSession.newBuilder().withTopDirectory(Path.of(".")).build();
        SettingsBuilderRequest settingsRequest =
                SettingsBuilderRequest.builder().session(session).build();
        ToolchainsBuilderRequest toolchainsRequest =
                ToolchainsBuilderRequest.builder().session(session).build();
        assertSame(
                settingsRequest,
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of())
                        .build(settingsRequest)
                        .getRequest());
        assertSame(
                toolchainsRequest,
                new DefaultToolchainsBuilder(new DefaultInterpolator(), new DefaultToolchainsXmlFactory())
                        .build(toolchainsRequest)
                        .getRequest());
    }

    @Test
    void bootstrapCacheIsScopedToItsSession() {
        ProtoSession first =
                ProtoSession.newBuilder().withTopDirectory(Path.of(".")).build();
        ProtoSession second =
                ProtoSession.newBuilder().withTopDirectory(Path.of(".")).build();
        DefaultSettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
        SettingsBuilderRequest request =
                SettingsBuilderRequest.builder().session(first).build();
        SettingsBuilderResult result = builder.build(request);
        assertSame(
                result,
                builder.build(SettingsBuilderRequest.builder().session(first).build()));
        SettingsBuilderRequest otherRequest =
                SettingsBuilderRequest.builder().session(second).build();
        assertSame(otherRequest, builder.build(otherRequest).getRequest());
        assertNull(RequestTraceHelper.getCurrentTrace(first));
        assertNull(RequestTraceHelper.getCurrentTrace(second));
    }

    @Test
    void bootstrapTraceIsRestoredOnFailure() throws Exception {
        ProtoSession session =
                ProtoSession.newBuilder().withTopDirectory(Path.of(".")).build();
        Source source = mock(Source.class);
        when(source.getLocation()).thenReturn("settings.xml");
        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(source)
                .build();
        IllegalStateException failure = new IllegalStateException("failed");
        when(source.openStream()).thenAnswer(call -> {
            assertSame(request, RequestTraceHelper.getCurrentTrace(session).data());
            throw failure;
        });
        DefaultSettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
        assertSame(failure, assertThrows(IllegalStateException.class, () -> builder.build(request)));
        assertNull(RequestTraceHelper.getCurrentTrace(session));
    }

    @Test
    void traceSurroundsCacheLookupAndRestoresParent() {
        AbstractSession session = newSession(mock(RepositorySystem.class));
        RequestTrace parent = new RequestTrace("parent");
        session.setCurrentTrace(parent);
        RequestCache cache = spy(new DefaultRequestCache());
        RequestCacheFactory cacheFactory = () -> cache;
        when(session.lookup.lookup(RequestCacheFactory.class)).thenReturn(cacheFactory);
        SettingsBuilderRequest request =
                SettingsBuilderRequest.builder().session(session).build();
        AtomicInteger lookups = new AtomicInteger();
        doAnswer(call -> {
                    lookups.incrementAndGet();
                    assertSame(request, session.getCurrentTrace().data());
                    assertSame(parent, session.getCurrentTrace().parent());
                    return call.callRealMethod();
                })
                .when(cache)
                .request(any(SettingsBuilderRequest.class), any());
        DefaultSettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
        SettingsBuilderResult result = builder.build(request);
        assertSame(parent, session.getCurrentTrace());
        assertSame(result, builder.build(request));
        assertEquals(2, lookups.get());
        assertSame(parent, session.getCurrentTrace());
    }

    @Test
    void installRunsEveryTimeEvenWithCacheSelector() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        AbstractSession session = newSession(repositorySystem);
        doReturn(Map.of(Constants.MAVEN_CACHE_CONFIG_PROPERTY, "* { scope: session, ref: hard }"))
                .when(session)
                .getUserProperties();
        when(repositorySystem.install(eq(session.getSession()), any(InstallRequest.class)))
                .thenAnswer(call -> new InstallResult(call.getArgument(1)));
        ArtifactInstallerRequest request = ArtifactInstallerRequest.build(session, List.of());
        DefaultArtifactInstaller installer = new DefaultArtifactInstaller(repositorySystem);

        installer.install(request);
        installer.install(request);

        assertEquals(CacheRetention.DISABLED, request.getCacheRetention());
        verify(repositorySystem, times(2)).install(eq(session.getSession()), any(InstallRequest.class));
        assertNull(session.getCurrentTrace());
    }

    @Test
    void deployRunsEveryTimeEvenWithCacheSelector() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        AbstractSession session = newSession(repositorySystem);
        doReturn(Map.of(Constants.MAVEN_CACHE_CONFIG_PROPERTY, "* { scope: session, ref: hard }"))
                .when(session)
                .getUserProperties();
        RemoteRepository repository =
                new DefaultRemoteRepository(new org.eclipse.aether.repository.RemoteRepository.Builder(
                                "test", "default", "https://repo.example.test/")
                        .build());
        when(repositorySystem.deploy(eq(session.getSession()), any(DeployRequest.class)))
                .thenAnswer(call -> new DeployResult(call.getArgument(1)));
        ArtifactDeployerRequest request = ArtifactDeployerRequest.builder()
                .session(session)
                .repository(repository)
                .artifacts(List.of())
                .build();
        DefaultArtifactDeployer deployer = new DefaultArtifactDeployer();

        deployer.deploy(request);
        deployer.deploy(request);

        assertEquals(CacheRetention.DISABLED, request.getCacheRetention());
        verify(repositorySystem, times(2)).deploy(eq(session.getSession()), any(DeployRequest.class));
        assertNull(session.getCurrentTrace());
    }

    @Test
    void failedInstallIsRetriedAndRestoresTrace() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        AbstractSession session = newSession(repositorySystem);
        when(repositorySystem.install(eq(session.getSession()), any(InstallRequest.class)))
                .thenAnswer(call -> {
                    throw new InstallationException("failed");
                });
        ArtifactInstallerRequest request = ArtifactInstallerRequest.build(session, List.of());
        DefaultArtifactInstaller installer = new DefaultArtifactInstaller(repositorySystem);

        assertThrows(ArtifactInstallerException.class, () -> installer.install(request));
        assertNull(session.getCurrentTrace());
        assertThrows(ArtifactInstallerException.class, () -> installer.install(request));
        verify(repositorySystem, times(2)).install(eq(session.getSession()), any(InstallRequest.class));
    }
}
