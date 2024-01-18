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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.internal.impl.DefaultTypeRegistry;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.graph.version.*;
import org.eclipse.aether.version.VersionScheme;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UT for {@link DefaultRepositorySystemSessionFactory}.
 */
@PlexusTest
public class DefaultRepositorySystemSessionFactoryTest {
    @Inject
    protected MavenRepositorySystem mavenRepositorySystem;

    @Inject
    protected EventSpyDispatcher eventSpyDispatcher;

    @Inject
    protected SettingsDecrypter settingsDecrypter;

    @Inject
    protected org.eclipse.aether.RepositorySystem aetherRepositorySystem;

    @Inject
    protected ArtifactHandlerManager artifactHandlerManager;

    @Inject
    protected RuntimeInformation information;

    @Inject
    protected DefaultTypeRegistry defaultTypeRegistry;

    @Inject
    protected VersionScheme versionScheme;

    @Test
    void isNoSnapshotUpdatesTest() throws InvalidRepositoryException {
        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        assertNull(systemSessionFactory.newRepositorySession(request).getUpdatePolicy());

        request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        request.setNoSnapshotUpdates(true);
        assertEquals(
                RepositoryPolicy.UPDATE_POLICY_NEVER,
                systemSessionFactory.newRepositorySession(request).getUpdatePolicy());
    }

    @Test
    void isSnapshotUpdatesTest() throws InvalidRepositoryException {
        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        request.setUpdateSnapshots(true);
        assertEquals(
                RepositoryPolicy.UPDATE_POLICY_ALWAYS,
                systemSessionFactory.newRepositorySession(request).getUpdatePolicy());
    }

    @Test
    void wagonProviderConfigurationTest() throws InvalidRepositoryException {
        Server server = new Server();
        server.setId("repository");
        server.setUsername("jason");
        server.setPassword("abc123");
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom wagonProvider = new Xpp3Dom("wagonProvider");
        wagonProvider.setValue("httpclient");
        configuration.addChild(wagonProvider);
        server.setConfiguration(configuration);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        List<Server> servers = new ArrayList<>();
        servers.add(server);
        request.setServers(servers);

        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        PlexusConfiguration plexusConfiguration = (PlexusConfiguration) systemSessionFactory
                .newRepositorySession(request)
                .getConfigProperties()
                .get("aether.transport.wagon.config.repository");
        assertNotNull(plexusConfiguration);
        assertEquals(0, plexusConfiguration.getChildCount());
    }

    @Test
    void httpConfigurationWithHttpHeadersTest() throws InvalidRepositoryException {
        Server server = new Server();
        server.setId("repository");
        server.setUsername("jason");
        server.setPassword("abc123");
        Xpp3Dom httpConfiguration = new Xpp3Dom("httpConfiguration");
        Xpp3Dom httpHeaders = new Xpp3Dom("httpHeaders");
        Xpp3Dom property = new Xpp3Dom("property");
        Xpp3Dom headerName = new Xpp3Dom("name");
        headerName.setValue("header");
        Xpp3Dom headerValue = new Xpp3Dom("value");
        headerValue.setValue("value");
        property.addChild(headerName);
        property.addChild(headerValue);
        httpHeaders.addChild(property);
        httpConfiguration.addChild(httpHeaders);

        server.setConfiguration(httpConfiguration);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        List<Server> servers = new ArrayList<>();
        servers.add(server);
        request.setServers(servers);

        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        Map<String, String> headers = (Map<String, String>) systemSessionFactory
                .newRepositorySession(request)
                .getConfigProperties()
                .get(ConfigurationProperties.HTTP_HEADERS + "." + server.getId());
        assertNotNull(headers);
        assertEquals(1, headers.size());
        assertEquals("value", headers.get("header"));
    }

    @Test
    void connectTimeoutConfigurationTest() throws InvalidRepositoryException {
        Server server = new Server();
        server.setId("repository");
        server.setUsername("jason");
        server.setPassword("abc123");
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom connectTimeoutConfiguration = new Xpp3Dom("connectTimeout");
        connectTimeoutConfiguration.setValue("3000");
        configuration.addChild(connectTimeoutConfiguration);

        server.setConfiguration(configuration);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        List<Server> servers = new ArrayList<>();
        servers.add(server);
        request.setServers(servers);

        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        int connectionTimeout = (Integer) systemSessionFactory
                .newRepositorySession(request)
                .getConfigProperties()
                .get(ConfigurationProperties.CONNECT_TIMEOUT + "." + server.getId());
        assertEquals(3000, connectionTimeout);
    }

    @Test
    void connectionTimeoutFromHttpConfigurationTest() throws InvalidRepositoryException {
        Server server = new Server();
        server.setId("repository");
        server.setUsername("jason");
        server.setPassword("abc123");

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom httpConfiguration = new Xpp3Dom("httpConfiguration");
        Xpp3Dom all = new Xpp3Dom("all");
        Xpp3Dom connectTimeoutConfiguration = new Xpp3Dom("connectionTimeout");
        connectTimeoutConfiguration.setValue("3000");

        all.addChild(connectTimeoutConfiguration);
        httpConfiguration.addChild(all);
        configuration.addChild(httpConfiguration);

        server.setConfiguration(configuration);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        List<Server> servers = new ArrayList<>();
        servers.add(server);
        request.setServers(servers);

        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        int connectionTimeout = (Integer) systemSessionFactory
                .newRepositorySession(request)
                .getConfigProperties()
                .get(ConfigurationProperties.CONNECT_TIMEOUT + "." + server.getId());
        assertEquals(3000, connectionTimeout);
    }

    @Test
    void requestTimeoutConfigurationTest() throws InvalidRepositoryException {
        Server server = new Server();
        server.setId("repository");
        server.setUsername("jason");
        server.setPassword("abc123");
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom requestTimeoutConfiguration = new Xpp3Dom("requestTimeout");
        requestTimeoutConfiguration.setValue("3000");
        configuration.addChild(requestTimeoutConfiguration);

        server.setConfiguration(configuration);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        List<Server> servers = new ArrayList<>();
        servers.add(server);
        request.setServers(servers);

        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        int requestTimeout = (Integer) systemSessionFactory
                .newRepositorySession(request)
                .getConfigProperties()
                .get(ConfigurationProperties.REQUEST_TIMEOUT + "." + server.getId());
        assertEquals(3000, requestTimeout);
    }

    @Test
    void readTimeoutFromHttpConfigurationTest() throws InvalidRepositoryException {
        Server server = new Server();
        server.setId("repository");
        server.setUsername("jason");
        server.setPassword("abc123");

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom httpConfiguration = new Xpp3Dom("httpConfiguration");
        Xpp3Dom all = new Xpp3Dom("all");
        Xpp3Dom readTimeoutConfiguration = new Xpp3Dom("readTimeout");
        readTimeoutConfiguration.setValue("3000");

        all.addChild(readTimeoutConfiguration);
        httpConfiguration.addChild(all);
        configuration.addChild(httpConfiguration);

        server.setConfiguration(configuration);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        List<Server> servers = new ArrayList<>();
        servers.add(server);
        request.setServers(servers);

        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        int requestTimeout = (Integer) systemSessionFactory
                .newRepositorySession(request)
                .getConfigProperties()
                .get(ConfigurationProperties.REQUEST_TIMEOUT + "." + server.getId());
        assertEquals(3000, requestTimeout);
    }

    @Test
    void transportConfigurationTest() throws InvalidRepositoryException {
        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());

        // native
        Properties properties = new Properties();
        properties.setProperty("maven.resolver.transport", "apache");
        request.setSystemProperties(properties);
        Map<String, Object> configProperties =
                systemSessionFactory.newRepositorySession(request).getConfigProperties();
        assertEquals(String.valueOf(Float.MAX_VALUE), configProperties.get("aether.priority.FileTransporterFactory"));
        assertEquals(String.valueOf(Float.MAX_VALUE), configProperties.get("aether.priority.ApacheTransporterFactory"));
        properties.remove("maven.resolver.transport");

        // wagon
        properties.setProperty("maven.resolver.transport", "wagon");
        request.setSystemProperties(properties);
        assertEquals(
                String.valueOf(Float.MAX_VALUE),
                systemSessionFactory
                        .newRepositorySession(request)
                        .getConfigProperties()
                        .get("aether.priority.WagonTransporterFactory"));
        properties.remove("maven.resolver.transport");

        // illegal
        properties.setProperty("maven.resolver.transport", "illegal");
        request.setSystemProperties(properties);
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> systemSessionFactory.newRepositorySession(request));
        assertEquals(
                "Unknown resolver transport 'illegal'. Supported transports are: wagon, apache, jdk, auto",
                exception.getMessage());
        properties.remove("maven.resolver.transport");
    }

    @Test
    void versionFilteringTest() throws InvalidRepositoryException {
        DefaultRepositorySystemSessionFactory systemSessionFactory = new DefaultRepositorySystemSessionFactory(
                artifactHandlerManager,
                aetherRepositorySystem,
                settingsDecrypter,
                eventSpyDispatcher,
                information,
                defaultTypeRegistry,
                versionScheme,
                Collections.emptyMap(),
                Collections.emptyMap());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());

        VersionFilter versionFilter;

        // single one
        request.getUserProperties().put("maven.versionFilters", "s");
        versionFilter = systemSessionFactory.newRepositorySession(request).getVersionFilter();
        assertNotNull(versionFilter);
        assertTrue(versionFilter instanceof ContextualSnapshotVersionFilter);

        request.getUserProperties().put("maven.versionFilters", "h");
        versionFilter = systemSessionFactory.newRepositorySession(request).getVersionFilter();
        assertNotNull(versionFilter);
        assertTrue(versionFilter instanceof HighestVersionFilter);

        request.getUserProperties().put("maven.versionFilters", "h(5)");
        versionFilter = systemSessionFactory.newRepositorySession(request).getVersionFilter();
        assertNotNull(versionFilter);
        assertTrue(versionFilter instanceof HighestVersionFilter);

        request.getUserProperties().put("maven.versionFilters", "l");
        versionFilter = systemSessionFactory.newRepositorySession(request).getVersionFilter();
        assertNotNull(versionFilter);
        assertTrue(versionFilter instanceof LowestVersionFilter);

        request.getUserProperties().put("maven.versionFilters", "l(5)");
        versionFilter = systemSessionFactory.newRepositorySession(request).getVersionFilter();
        assertNotNull(versionFilter);
        assertTrue(versionFilter instanceof LowestVersionFilter);

        request.getUserProperties().put("maven.versionFilters", "e(g:a:v)");
        versionFilter = systemSessionFactory.newRepositorySession(request).getVersionFilter();
        assertNotNull(versionFilter);
        assertTrue(versionFilter instanceof PredicateVersionFilter);

        request.getUserProperties().put("maven.versionFilters", "e(g:a:[1,2])");
        versionFilter = systemSessionFactory.newRepositorySession(request).getVersionFilter();
        assertNotNull(versionFilter);
        assertTrue(versionFilter instanceof PredicateVersionFilter);

        // chained
        request.getUserProperties().put("maven.versionFilters", "h(5);s;e(org.foo:bar:1)");
        versionFilter = systemSessionFactory.newRepositorySession(request).getVersionFilter();
        assertNotNull(versionFilter);
        assertTrue(versionFilter instanceof ChainedVersionFilter);
    }

    protected ArtifactRepository getLocalRepository() throws InvalidRepositoryException {
        File repoDir = new File(getBasedir(), "target/local-repo").getAbsoluteFile();

        return mavenRepositorySystem.createLocalRepository(repoDir);
    }
}
