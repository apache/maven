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
package org.apache.maven.internal.aether;

import javax.inject.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.internal.RepositorySystemSessionFactory;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UT for {@link DefaultRepositorySystemSessionFactory}, focused on how server credentials are scoped
 * to repository origins.
 */
@PlexusTest
class DefaultRepositorySystemSessionFactoryTest {

    @Inject
    private RepositorySystemSessionFactory systemSessionFactory;

    @Inject
    private MavenRepositorySystem mavenRepositorySystem;

    @Test
    void credentialsServedForOriginDeclaredOnlyByServer() throws Exception {
        MavenExecutionRequest request = requestWithServer(
                serverWithRepositoryOrigins("internal", "https://repo.example.org", "https://mirror.example.org:8443"));

        AuthenticationSelector selector = authenticationSelector(request);

        assertNotNull(selector.getAuthentication(repository("internal", "https://repo.example.org/releases/")));
        assertNotNull(selector.getAuthentication(repository("internal", "https://mirror.example.org:8443/repo/")));
        assertNull(selector.getAuthentication(repository("internal", "https://evil.example.org/releases/")));
    }

    @Test
    void credentialsServedForOriginDeclaredOnlyByServerInStrictScope() throws Exception {
        MavenExecutionRequest request =
                requestWithServer(serverWithRepositoryOrigins("internal", "https://repo.example.org"));
        request.setSystemProperties(strictCredentialScope());

        AuthenticationSelector selector = authenticationSelector(request);

        assertNotNull(selector.getAuthentication(repository("internal", "https://repo.example.org/releases/")));
        assertNull(selector.getAuthentication(repository("internal", "https://evil.example.org/releases/")));
    }

    @Test
    void serverWithoutRepositoryOriginsIsRefusedInStrictScope() throws Exception {
        MavenExecutionRequest request = requestWithServer(serverWithRepositoryOrigins("internal"));
        request.setSystemProperties(strictCredentialScope());

        AuthenticationSelector selector = authenticationSelector(request);

        assertNull(selector.getAuthentication(repository("internal", "https://repo.example.org/releases/")));
    }

    @Test
    void serverRepositoryOriginsAddToMirrorOrigins() throws Exception {
        MavenExecutionRequest request =
                requestWithServer(serverWithRepositoryOrigins("internal", "https://repo.example.org"));
        Mirror mirror = new Mirror();
        mirror.setId("internal");
        mirror.setUrl("https://mirror.example.org/repo/");
        mirror.setMirrorOf("*");
        request.setMirrors(new ArrayList<>(Collections.singletonList(mirror)));

        AuthenticationSelector selector = authenticationSelector(request);

        assertNotNull(selector.getAuthentication(repository("internal", "https://mirror.example.org/repo/")));
        assertNotNull(selector.getAuthentication(repository("internal", "https://repo.example.org/releases/")));
        assertNull(selector.getAuthentication(repository("internal", "https://evil.example.org/releases/")));
    }

    @Test
    void malformedServerRepositoryOriginIsIgnored() throws Exception {
        MavenExecutionRequest request = requestWithServer(serverWithRepositoryOrigins("internal", "not an origin"));
        request.setSystemProperties(strictCredentialScope());

        AuthenticationSelector selector = authenticationSelector(request);

        assertNull(selector.getAuthentication(repository("internal", "https://repo.example.org/releases/")));
    }

    private static Properties strictCredentialScope() {
        Properties properties = new Properties();
        properties.put(
                DefaultRepositorySystemSessionFactory.MAVEN_REPOSITORY_CREDENTIAL_SCOPE,
                OriginBoundAuthenticationSelector.SCOPE_STRICT);
        return properties;
    }

    private static Server serverWithRepositoryOrigins(String id, String... repositoryOrigins) {
        Server server = new Server();
        server.setId(id);
        server.setUsername("jason");
        server.setPassword("abc123");
        server.setRepositoryOrigins(new ArrayList<>(Arrays.asList(repositoryOrigins)));
        return server;
    }

    private MavenExecutionRequest requestWithServer(Server server) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        request.setServers(new ArrayList<>(Collections.singletonList(server)));
        return request;
    }

    private AuthenticationSelector authenticationSelector(MavenExecutionRequest request) {
        return systemSessionFactory.newRepositorySessionBuilder(request).build().getAuthenticationSelector();
    }

    private static RemoteRepository repository(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }

    private ArtifactRepository getLocalRepository() throws Exception {
        File repoDir = new File(getBasedir(), "target/local-repo").getAbsoluteFile();
        return mavenRepositorySystem.createLocalRepository(null, repoDir);
    }
}
