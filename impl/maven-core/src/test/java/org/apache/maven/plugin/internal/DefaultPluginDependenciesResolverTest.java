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
package org.apache.maven.plugin.internal;

import javax.inject.Inject;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.maven.MavenTestHelper;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the pom-less repository fallback in {@link DefaultPluginDependenciesResolver}.
 *
 * <p>In a pom-less (standalone) build the stub project has no {@code <pluginRepositories>}, so plugin
 * resolution is handed an empty remote-repository list. Maven Resolver 2.x treats artifacts cached from
 * an origin repository that is absent from the current resolution context as unavailable and re-verifies
 * them against the (empty) list, which fails with "Can't resolve the plugin locally". The fix restores
 * the Maven 3.9.x / Resolver 1.x behaviour by falling back to the repositories configured on the
 * execution request when the build is pom-less.
 */
@PlexusTest
class DefaultPluginDependenciesResolverTest {

    @Inject
    private MavenRepositorySystem repositorySystem;

    @Inject
    private PlexusContainer container;

    private Method effectiveRepositoriesMethod() throws Exception {
        Method method = DefaultPluginDependenciesResolver.class.getDeclaredMethod(
                "effectiveRepositories", List.class, RepositorySystemSession.class);
        method.setAccessible(true);
        return method;
    }

    @SuppressWarnings("unchecked")
    private List<RemoteRepository> effectiveRepositories(
            DefaultRepositorySystemSession repoSession, List<RemoteRepository> repositories) throws Exception {
        RepositorySystem repoSystem = mock(RepositorySystem.class);
        when(repoSystem.newResolutionRepositories(eq(repoSession), anyList()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        DefaultPluginDependenciesResolver resolver = new DefaultPluginDependenciesResolver(repoSystem, List.of());
        return (List<RemoteRepository>) effectiveRepositoriesMethod().invoke(resolver, repositories, repoSession);
    }

    private ArtifactRepository repo(String id, String url) {
        return MavenRepositorySystem.createArtifactRepository(id, url, new DefaultRepositoryLayout(), null, null);
    }

    private List<RemoteRepository> toAether(List<ArtifactRepository> repositories) {
        return org.apache.maven.RepositoryUtils.toRepos(repositories);
    }

    private MavenExecutionRequest request(DefaultRepositorySystemSession repoSession) {
        return InternalMavenSession.from(repoSession).getMavenSession().getRequest();
    }

    @Test
    void nonEmptyRepositoriesAreUsedUnchanged() throws Exception {
        DefaultRepositorySystemSession repoSession = MavenTestHelper.createSession(repositorySystem, container);
        List<RemoteRepository> provided = toAether(List.of(repo("central", "https://repo.maven.apache.org/maven2")));

        List<RemoteRepository> effective = effectiveRepositories(repoSession, provided);

        assertSame(provided, effective);
    }

    @Test
    void emptyRepositoriesInPomLessBuildFallBackToRequestRepositories() throws Exception {
        DefaultRepositorySystemSession repoSession = MavenTestHelper.createSession(repositorySystem, container);
        MavenExecutionRequest request = request(repoSession);
        request.setProjectPresent(false);
        request.setPluginArtifactRepositories(List.of(repo("central", "https://repo.maven.apache.org/maven2")));

        List<RemoteRepository> effective = effectiveRepositories(repoSession, Collections.emptyList());

        assertEquals(1, effective.size());
        assertEquals("central", effective.get(0).getId());
    }

    @Test
    void emptyRepositoriesMergedPluginAndProjectRepositories() throws Exception {
        DefaultRepositorySystemSession repoSession = MavenTestHelper.createSession(repositorySystem, container);
        MavenExecutionRequest request = request(repoSession);
        request.setProjectPresent(false);
        request.setPluginArtifactRepositories(List.of(repo("plugin-repo", "https://example.org/plugins")));
        request.setRemoteRepositories(List.of(repo("central", "https://repo.maven.apache.org/maven2")));

        List<RemoteRepository> effective = effectiveRepositories(repoSession, Collections.emptyList());

        assertEquals(2, effective.size());
        assertEquals("plugin-repo", effective.get(0).getId());
        assertEquals("central", effective.get(1).getId());
    }

    @Test
    void emptyRepositoriesInProjectBuildAreLeftAlone() throws Exception {
        DefaultRepositorySystemSession repoSession = MavenTestHelper.createSession(repositorySystem, container);
        MavenExecutionRequest request = request(repoSession);
        request.setProjectPresent(true);
        request.setPluginArtifactRepositories(List.of(repo("central", "https://repo.maven.apache.org/maven2")));

        List<RemoteRepository> effective = effectiveRepositories(repoSession, Collections.emptyList());

        assertEquals(0, effective.size());
    }
}
