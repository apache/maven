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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderException;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.settings.Mirror;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@ExtendWith(MockitoExtension.class)
class DefaultSettingsBuilderFactoryTest {

    @Mock
    Session session;

    @BeforeEach
    void setup() {
        Mockito.lenient()
                .when(session.getService(SettingsXmlFactory.class))
                .thenReturn(new DefaultSettingsXmlFactory());
    }

    SettingsBuilderResult execute(String settingsName) {
        return execute(settingsName, Map.of(), Map.of());
    }

    SettingsBuilderResult execute(String settingsName, Map<String, String> userProperties) {
        return execute(settingsName, userProperties, Map.of());
    }

    SettingsBuilderResult execute(
            String settingsName, Map<String, String> userProperties, Map<String, String> systemProperties) {
        Mockito.lenient().when(session.getUserProperties()).thenReturn(userProperties);
        Mockito.lenient().when(session.getSystemProperties()).thenReturn(systemProperties);
        Map<String, String> effectiveProperties = new java.util.HashMap<>(systemProperties);
        effectiveProperties.putAll(userProperties);
        Mockito.lenient().when(session.getEffectiveProperties()).thenReturn(effectiveProperties);

        SettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
        assertNotNull(builder);

        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(Sources.buildSource(getSettings(settingsName)))
                .build();

        SettingsBuilderResult result = builder.build(request);
        assertNotNull(result);
        return result;
    }

    @Test
    void testCompleteWiring() {
        Settings settings = execute("settings-simple").getEffectiveSettings();

        String localRepository = settings.getLocalRepository();
        assertTrue(localRepository.equals("${user.home}/.m2/repository")
                || localRepository.endsWith("\\${user.home}\\.m2\\repository"));
    }

    @Test
    void testSettingsWithServers() {
        Settings settings = execute("settings-servers-1").getEffectiveSettings();

        List<Server> servers = settings.getServers();
        assertEquals(2, servers.size());

        Server server1 = getServerById(servers, "server-1");
        assertEquals("username1", server1.getUsername());
        assertEquals("password1", server1.getPassword());

        Server server2 = getServerById(servers, "server-2");
        assertEquals("username2", server2.getUsername());
        assertEquals("password2", server2.getPassword());
    }

    @Test
    void testSettingsWithServersAndAliases() {
        Settings settings = execute("settings-servers-2").getEffectiveSettings();

        assertEquals("${user.home}/.m2/repository", settings.getLocalRepository());

        List<Server> servers = settings.getServers();
        assertEquals(6, servers.size());

        Server server1 = getServerById(servers, "server-1");
        assertEquals("username1", server1.getUsername());
        assertEquals("password1", server1.getPassword());
        assertEquals(List.of("server-11", "server-12"), server1.getAliases());

        Server server11 = getServerById(servers, "server-11");
        assertEquals("username1", server11.getUsername());
        assertEquals("password1", server11.getPassword());
        assertTrue(server11.getAliases().isEmpty());

        Server server12 = getServerById(servers, "server-12");
        assertEquals("username1", server12.getUsername());
        assertEquals("password1", server12.getPassword());
        assertTrue(server12.getAliases().isEmpty());

        Server server2 = getServerById(servers, "server-2");
        assertEquals("username2", server2.getUsername());
        assertEquals("password2", server2.getPassword());
        assertEquals(List.of("server-21"), server2.getAliases());

        Server server21 = getServerById(servers, "server-21");
        assertEquals("username2", server21.getUsername());
        assertEquals("password2", server21.getPassword());
        assertTrue(server21.getAliases().isEmpty());

        Server server3 = getServerById(servers, "server-3");
        assertEquals("username3", server3.getUsername());
        assertEquals("password3", server3.getPassword());
        assertTrue(server3.getAliases().isEmpty());
    }

    private Server getServerById(List<Server> servers, String id) {
        return servers.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Server with id " + id + " not found on list: " + servers));
    }

    @Test
    void testSettingsWithDuplicateServersIds() throws Exception {
        SettingsBuilderResult result = execute("settings-servers-3");

        ProblemCollector<BuilderProblem> problems = result.getProblems();
        assertEquals(1, problems.problems().count());
        assertEquals(
                "'servers.server[0].aliases[0]' for server-1 must be unique across all server ids and aliases but found duplicate alias server-2",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testSettingsWithServerRepositoryOrigins() {
        Settings settings = execute("settings-servers-4").getEffectiveSettings();

        List<Server> servers = settings.getServers();
        assertEquals(2, servers.size());

        List<String> repositoryOrigins = List.of("https://repo.example.org", "https://mirror.example.org:8443");

        Server server1 = getServerById(servers, "server-1");
        assertEquals(repositoryOrigins, server1.getRepositoryOrigins());

        // an alias is the same credentials under another id, so it is bound to the same origins
        Server server11 = getServerById(servers, "server-11");
        assertEquals(repositoryOrigins, server11.getRepositoryOrigins());
    }

    @Test
    void testRelativeLocalRepositoryIsResolvedToAbsolute() {
        Settings settings = execute("settings-relative-local-repo").getEffectiveSettings();

        String localRepository = settings.getLocalRepository();
        assertNotNull(localRepository);
        assertFalse(localRepository.isEmpty());
        Path repoPath = Paths.get(localRepository);
        assertTrue(repoPath.isAbsolute(), "Relative local repository should be resolved to absolute");
    }

    @Test
    void testMergeUserSettingsOverInstallationSettings() {
        SettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());

        Source userSource = new Source() {
            @Override
            public Path getPath() {
                return null;
            }

            @Override
            public InputStream openStream() {
                String xml = "<settings xmlns='http://maven.apache.org/SETTINGS/1.0.0'>"
                        + "<mirrors>"
                        + "<mirror>"
                        + "<id>shared-mirror</id>"
                        + "<url>http://dominant.example.com/</url>"
                        + "<mirrorOf>central</mirrorOf>"
                        + "</mirror>"
                        + "</mirrors>"
                        + "</settings>";
                return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public String getLocation() {
                return "user-settings.xml";
            }

            @Override
            public Source resolve(String relative) {
                return null;
            }
        };

        Source installationSource = new Source() {
            @Override
            public Path getPath() {
                return null;
            }

            @Override
            public InputStream openStream() {
                String xml = "<settings xmlns='http://maven.apache.org/SETTINGS/1.0.0'>"
                        + "<mirrors>"
                        + "<mirror>"
                        + "<id>shared-mirror</id>"
                        + "<url>http://recessive.example.com/</url>"
                        + "<mirrorOf>central</mirrorOf>"
                        + "</mirror>"
                        + "<mirror>"
                        + "<id>maven-default-http-blocker</id>"
                        + "<url>http://0.0.0.0/</url>"
                        + "<mirrorOf>external:http:*</mirrorOf>"
                        + "<blocked>true</blocked>"
                        + "</mirror>"
                        + "</mirrors>"
                        + "</settings>";
                return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public String getLocation() {
                return "installation-settings.xml";
            }

            @Override
            public Source resolve(String relative) {
                return null;
            }
        };

        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(userSource)
                .installationSettingsSource(installationSource)
                .build();

        SettingsBuilderResult result = builder.build(request);
        assertNotNull(result);
        Settings effective = result.getEffectiveSettings();
        assertNotNull(effective);

        List<Mirror> mirrors = effective.getMirrors();
        assertEquals(2, mirrors.size());
        assertEquals("shared-mirror", mirrors.get(0).getId());
        assertEquals("http://dominant.example.com/", mirrors.get(0).getUrl());
        assertEquals("maven-default-http-blocker", mirrors.get(1).getId());
    }

    @Test
    void testProxyFieldInterpolation() {
        // Note: in Maven 4 the settings model stores proxy.port and proxy.active as
        // String-backed fields (portString / activeString), so both the pre-parse
        // interpolateSource() step and the post-parse SettingsTransformer-based
        // interpolate() step can resolve ${...} placeholders for these fields.
        // The pre-parse step is retained for:
        //   - forward compatibility with model changes that may introduce true typed fields
        //   - custom SettingsParser implementations that bypass the transformer
        //   - consistency with the Maven 3.x fix for the same issue
        // This test verifies end-to-end that proxy field interpolation works correctly.
        Map<String, String> userProperties = Map.of(
                "test.proxy.active", "true",
                "test.proxy.port", "9090",
                "test.proxy.nonProxyHosts", "localhost|*.example.org");

        Settings settings =
                execute("settings-proxy-interpolation", userProperties).getEffectiveSettings();

        List<Proxy> proxies = settings.getProxies();
        assertEquals(1, proxies.size());

        Proxy proxy = proxies.get(0);
        assertEquals("test-proxy", proxy.getId());
        assertTrue(proxy.isActive(), "proxy.active should be interpolated to true");
        assertEquals(9090, proxy.getPort(), "proxy.port should be interpolated to 9090");
        assertEquals("localhost|*.example.org", proxy.getNonProxyHosts());
        assertEquals("http", proxy.getProtocol());
        assertEquals("proxy.example.org", proxy.getHost());

        // Also verify that a custom interpolationSource (bypassing session properties) works.
        // This exercises the interpolationSource branch in both interpolateSource() and interpolate().
        Mockito.lenient().when(session.getUserProperties()).thenReturn(Map.of());
        Mockito.lenient().when(session.getSystemProperties()).thenReturn(Map.of());
        Mockito.lenient().when(session.getEffectiveProperties()).thenReturn(Map.of());
        SettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
        SettingsBuilderRequest requestWithSource = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(Sources.buildSource(getSettings("settings-proxy-interpolation")))
                .interpolationSource(userProperties::get)
                .build();
        Settings settingsFromSource = builder.build(requestWithSource).getEffectiveSettings();
        List<Proxy> proxiesFromSource = settingsFromSource.getProxies();
        assertEquals(1, proxiesFromSource.size());
        Proxy proxyFromSource = proxiesFromSource.get(0);
        assertTrue(proxyFromSource.isActive(), "proxy.active should be interpolated via interpolationSource");
        assertEquals(9090, proxyFromSource.getPort(), "proxy.port should be interpolated via interpolationSource");
    }

    @Test
    void testInterpolationWithXmlMarkupInPropertyValue() {
        // Regression test for MNG-4625: interpolating a property whose value contains XML special
        // characters (& < >) must not produce malformed XML and must preserve the raw value.
        Map<String, String> userProperties = Map.of("test.prop", "&x=y<>");

        Settings settings =
                execute("settings-xml-markup-interpolation", userProperties).getEffectiveSettings();

        // The jdbcUrl property should contain the raw (unescaped) value, not XML entities
        String jdbcUrl = settings.getProfiles().stream()
                .filter(p -> "xml-markup".equals(p.getId()))
                .findFirst()
                .orElseThrow()
                .getProperties()
                .get("jdbcUrl");
        assertEquals("&x=y<>", jdbcUrl, "Property value with XML markup must survive settings interpolation");
    }

    @Test
    void testStrictParsingReportsError() {
        Map<String, String> systemProperties = Map.of("maven.settings.strictParsing", "true");

        // Valid settings should parse fine even in strict mode
        Settings validSettings =
                execute("settings-simple", Map.of(), systemProperties).getEffectiveSettings();
        assertNotNull(validSettings);

        // Settings with an unknown element must throw with a fatal problem in strict mode
        SettingsBuilderException ex = assertThrows(
                SettingsBuilderException.class, () -> execute("settings-unknown-element", Map.of(), systemProperties));
        assertTrue(
                ex.getProblemCollector().hasFatalProblems(),
                "Strict parsing must produce a fatal problem for unknown XML elements");

        // Without strict mode, the same settings should only produce a warning, not throw
        SettingsBuilderResult lenientResult = execute("settings-unknown-element");
        assertFalse(
                lenientResult.getProblems().hasFatalProblems(),
                "Lenient parsing must not produce a fatal problem for unknown XML elements");
        assertNotNull(lenientResult.getEffectiveSettings());
    }

    private Path getSettings(String name) {
        return Paths.get("src/test/resources/settings/" + name + ".xml").toAbsolutePath();
    }
}
