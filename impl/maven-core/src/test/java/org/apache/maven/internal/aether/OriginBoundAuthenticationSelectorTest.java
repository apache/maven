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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UT for {@link OriginBoundAuthenticationSelector}.
 */
class OriginBoundAuthenticationSelectorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OriginBoundAuthenticationSelectorTest.class);

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
}
