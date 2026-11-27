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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT for {@link OriginBoundAuthenticationSelector}.
 */
class OriginBoundAuthenticationSelectorTest {

    private static final Logger LOGGER = new ConsoleLogger(Logger.LEVEL_DEBUG, "test");

    private static AuthenticationSelector serverCredentials(String... ids) {
        DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
        for (String id : ids) {
            selector.add(
                    id,
                    new AuthenticationBuilder()
                            .addUsername("user")
                            .addPassword("pass")
                            .build());
        }
        return selector;
    }

    private static Map<String, Set<String>> declared(String id, String url) {
        Map<String, Set<String>> origins = new HashMap<>();
        OriginBoundAuthenticationSelector.addOrigin(origins, id, url);
        return origins;
    }

    private static RemoteRepository repo(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }

    @Test
    void warnScopeServesCredentialsWhereOriginScopeWouldNot() {
        AuthenticationSelector selector = OriginBoundAuthenticationSelector.wrap(
                serverCredentials("releases", "deploy-server"),
                OriginBoundAuthenticationSelector.SCOPE_WARN,
                declared("releases", "https://repo.example.org/releases/"),
                LOGGER);

        assertNotNull(selector.getAuthentication(repo("releases", "https://repo.example.org/releases/")));
        assertNotNull(selector.getAuthentication(repo("releases", "https://other.example.org/m2/")));
        assertNotNull(selector.getAuthentication(repo("deploy-server", "https://deploy.example.org/releases/")));
    }

    @Test
    void credentialsServedForDeclaredOrigin() {
        AuthenticationSelector selector = OriginBoundAuthenticationSelector.wrap(
                serverCredentials("releases"),
                OriginBoundAuthenticationSelector.SCOPE_ORIGIN,
                declared("releases", "https://repo.example.org/releases/"),
                LOGGER);

        assertNotNull(selector.getAuthentication(repo("releases", "https://repo.example.org/releases/")));
    }

    @Test
    void authenticationScopedToDeclaredOrigin() {
        // credentials are scoped to the declared origin, so a different-origin
        // repository with the same id is not served
        AuthenticationSelector selector = OriginBoundAuthenticationSelector.wrap(
                serverCredentials("releases"),
                OriginBoundAuthenticationSelector.SCOPE_ORIGIN,
                declared("releases", "https://repo.example.org/releases/"),
                LOGGER);

        assertNull(selector.getAuthentication(repo("releases", "https://other.example.org/m2/")));
        // an unparseable URL on a bound id fails closed as well
        assertNull(selector.getAuthentication(repo("releases", "notaurl")));
    }

    @Test
    void deploymentRepositoryDeclaresTheOriginOfItsId() {
        Map<String, Set<String>> origins = new HashMap<>();
        OriginBoundAuthenticationSelector.addDeploymentRepositoryOrigin(
                origins, "deploy-server::https://deploy.example.org/releases/");
        OriginBoundAuthenticationSelector.addDeploymentRepositoryOrigin(
                origins, "legacy-server::default::https://legacy.example.org:8443/releases/");
        OriginBoundAuthenticationSelector.addDeploymentRepositoryOrigin(origins, "https://no-id.example.org/");
        OriginBoundAuthenticationSelector.addDeploymentRepositoryOrigin(origins, "::https://empty-id.example.org/");
        OriginBoundAuthenticationSelector.addDeploymentRepositoryOrigin(origins, null);

        Map<String, Set<String>> expected = new HashMap<>();
        OriginBoundAuthenticationSelector.addOrigin(expected, "deploy-server", "https://deploy.example.org");
        OriginBoundAuthenticationSelector.addOrigin(expected, "legacy-server", "https://legacy.example.org:8443");
        assertEquals(expected, origins);
    }

    @Test
    void undeclaredIdKeepsLegacyBehaviorInOriginScope() {
        // e.g. a pure deployment server whose URL only exists in the project's distributionManagement
        AuthenticationSelector selector = OriginBoundAuthenticationSelector.wrap(
                serverCredentials("deploy-server"),
                OriginBoundAuthenticationSelector.SCOPE_ORIGIN,
                new HashMap<>(),
                LOGGER);

        assertNotNull(selector.getAuthentication(repo("deploy-server", "https://deploy.example.org/releases/")));
    }

    @Test
    void undeclaredIdRefusedInStrictScope() {
        AuthenticationSelector selector = OriginBoundAuthenticationSelector.wrap(
                serverCredentials("deploy-server"),
                OriginBoundAuthenticationSelector.SCOPE_STRICT,
                new HashMap<>(),
                LOGGER);

        assertNull(selector.getAuthentication(repo("deploy-server", "https://deploy.example.org/releases/")));
    }

    @Test
    void idScopeReturnsUnwrappedDelegate() {
        AuthenticationSelector delegate = serverCredentials("releases");
        AuthenticationSelector selector = OriginBoundAuthenticationSelector.wrap(
                delegate,
                OriginBoundAuthenticationSelector.SCOPE_ID,
                declared("releases", "https://repo.example.org/releases/"),
                LOGGER);

        assertSame(delegate, selector);
        assertNotNull(selector.getAuthentication(repo("releases", "https://other.example.org/m2/")));
    }

    @Test
    void credentialsUsedThroughTheImpliedCentralPublishingOriginsAreReportedOnce() {
        Map<String, Set<String>> origins = declared("central", "https://repo.maven.apache.org/maven2");
        boolean implied = OriginBoundAuthenticationSelector.addCentralPublishingOrigins(origins);
        RecordingLogger logger = new RecordingLogger();
        AuthenticationSelector selector = OriginBoundAuthenticationSelector.wrap(
                serverCredentials("central"), OriginBoundAuthenticationSelector.SCOPE_STRICT, origins, implied, logger);

        assertNotNull(selector.getAuthentication(repo("central", "https://repo.maven.apache.org/maven2")));
        assertEquals(0, logger.warnings.size());
        assertNotNull(selector.getAuthentication(
                repo("central", "https://central.sonatype.com/repository/maven-snapshots/")));
        assertNotNull(selector.getAuthentication(
                repo("central", "https://ossrh-staging-api.central.sonatype.com/service/local/")));
        assertNotNull(selector.getAuthentication(
                repo("central", "https://central.sonatype.com/repository/maven-snapshots/")));

        assertEquals(1, logger.warnings.size());
        assertTrue(logger.warnings.get(0).contains("will be removed in a future release"));
    }

    @Test
    void centralPublishingOriginsDeclaredByTheServerAreNotReported() {
        Map<String, Set<String>> origins = declared("central", "https://repo.maven.apache.org/maven2");
        OriginBoundAuthenticationSelector.addOrigin(origins, "central", "https://central.sonatype.com");
        boolean implied = OriginBoundAuthenticationSelector.addCentralPublishingOrigins(origins);
        RecordingLogger logger = new RecordingLogger();
        AuthenticationSelector selector = OriginBoundAuthenticationSelector.wrap(
                serverCredentials("central"), OriginBoundAuthenticationSelector.SCOPE_ORIGIN, origins, implied, logger);

        assertFalse(implied);
        assertNotNull(selector.getAuthentication(
                repo("central", "https://central.sonatype.com/repository/maven-snapshots/")));
        assertEquals(0, logger.warnings.size());
    }

    @Test
    void unknownScopeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OriginBoundAuthenticationSelector.wrap(serverCredentials(), "bogus", new HashMap<>(), LOGGER));
    }

    @Test
    void originsAreNormalized() {
        assertEquals(
                OriginBoundAuthenticationSelector.originOf("https://repo.example.org/releases/"),
                OriginBoundAuthenticationSelector.originOf("HTTPS://Repo.Example.Org:443/other/path"));
        assertEquals(
                OriginBoundAuthenticationSelector.originOf("http://repo.example.org:80/"),
                OriginBoundAuthenticationSelector.originOf("http://repo.example.org/releases/"));
        assertNull(OriginBoundAuthenticationSelector.originOf("file:/tmp/repo"));
        assertNull(OriginBoundAuthenticationSelector.originOf(null));
    }

    private static final class RecordingLogger extends AbstractLogger {
        private final List<String> warnings = new ArrayList<>();

        RecordingLogger() {
            super(Logger.LEVEL_DEBUG, "test");
        }

        @Override
        public void debug(String message, Throwable throwable) {}

        @Override
        public void info(String message, Throwable throwable) {}

        @Override
        public void warn(String message, Throwable throwable) {
            warnings.add(message);
        }

        @Override
        public void error(String message, Throwable throwable) {}

        @Override
        public void fatalError(String message, Throwable throwable) {}

        @Override
        public Logger getChildLogger(String name) {
            return this;
        }
    }
}
