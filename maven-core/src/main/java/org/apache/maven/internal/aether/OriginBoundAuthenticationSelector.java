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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.RemoteRepository;

import static java.util.Objects.requireNonNull;

/**
 * An {@link AuthenticationSelector} that scopes server credentials to the origin (protocol, host and
 * port) of the repository or mirror the operator declared for the same server id.
 * <p>
 * A repository's id and its origin are independent: this selector serves a server id's credentials
 * only to a repository whose origin matches one the operator declared for that id, either by declaring
 * a repository or mirror with that id in settings or on the command line, or by listing the origin in
 * that server's {@code <repositoryOrigins>} in settings. Ids with no operator-declared origin keep the
 * previous behaviour unless {@code strict} scope is requested, and a warning naming the target origin
 * is emitted once per id/origin pair.
 * <p>
 * The one origin bound without a declaration is where Maven Central is published to, for the id of the
 * default repository; see {@link #addCentralPublishingOrigins(Map)}. Credentials used through that binding
 * are reported once per session, so that the server can be given the origins explicitly before the
 * binding is removed.
 *
 * @see DefaultRepositorySystemSessionFactory#MAVEN_REPOSITORY_CREDENTIAL_SCOPE
 */
class OriginBoundAuthenticationSelector implements AuthenticationSelector {
    /**
     * Credentials are bound to operator-declared origins; ids without a declared origin keep legacy
     * behavior, with a warning.
     */
    static final String SCOPE_ORIGIN = "origin";

    /**
     * Credentials are bound to operator-declared origins; ids without a declared origin get no
     * credentials.
     */
    static final String SCOPE_STRICT = "strict";

    /**
     * Legacy behavior: credentials are matched by server id only.
     */
    static final String SCOPE_ID = "id";

    /**
     * The origins Maven Central is published to: the Central Portal, which also receives snapshots, and
     * its OSSRH Staging API compatibility service.
     */
    private static final Set<String> CENTRAL_PUBLISHING_ORIGINS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("https://central.sonatype.com", "https://ossrh-staging-api.central.sonatype.com")));

    private final AuthenticationSelector delegate;

    private final Map<String, Set<String>> declaredOrigins;

    private final boolean strict;

    private final boolean centralPublishingOriginsImplied;

    private final Logger logger;

    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    private OriginBoundAuthenticationSelector(
            AuthenticationSelector delegate,
            Map<String, Set<String>> declaredOrigins,
            boolean strict,
            boolean centralPublishingOriginsImplied,
            Logger logger) {
        this.delegate = requireNonNull(delegate, "delegate");
        this.declaredOrigins = requireNonNull(declaredOrigins, "declaredOrigins");
        this.strict = strict;
        this.centralPublishingOriginsImplied = centralPublishingOriginsImplied;
        this.logger = requireNonNull(logger, "logger");
    }

    /**
     * Wraps the given selector according to the requested credential scope.
     *
     * @param delegate the selector holding the actual credentials, keyed by server id
     * @param credentialScope one of {@link #SCOPE_ORIGIN}, {@link #SCOPE_STRICT} or {@link #SCOPE_ID}
     * @param declaredOrigins origins of operator-declared repositories and mirrors, keyed by id
     * @param logger logger used to report id/origin mismatches
     * @return the delegate itself for {@link #SCOPE_ID}, an origin-bound wrapper otherwise
     */
    static AuthenticationSelector wrap(
            AuthenticationSelector delegate,
            String credentialScope,
            Map<String, Set<String>> declaredOrigins,
            Logger logger) {
        return wrap(delegate, credentialScope, declaredOrigins, false, logger);
    }

    /**
     * Wraps the given selector according to the requested credential scope.
     *
     * @param delegate the selector holding the actual credentials, keyed by server id
     * @param credentialScope one of {@link #SCOPE_ORIGIN}, {@link #SCOPE_STRICT} or {@link #SCOPE_ID}
     * @param declaredOrigins origins of operator-declared repositories and mirrors, keyed by id
     * @param centralPublishingOriginsImplied whether {@link #addCentralPublishingOrigins(Map)} bound the
     *     publishing origins of Maven Central, so that credentials used there are reported
     * @param logger logger used to report id/origin mismatches
     * @return the delegate itself for {@link #SCOPE_ID}, an origin-bound wrapper otherwise
     */
    static AuthenticationSelector wrap(
            AuthenticationSelector delegate,
            String credentialScope,
            Map<String, Set<String>> declaredOrigins,
            boolean centralPublishingOriginsImplied,
            Logger logger) {
        if (SCOPE_ID.equals(credentialScope)) {
            return delegate;
        } else if (SCOPE_ORIGIN.equals(credentialScope) || SCOPE_STRICT.equals(credentialScope)) {
            return new OriginBoundAuthenticationSelector(
                    delegate,
                    declaredOrigins,
                    SCOPE_STRICT.equals(credentialScope),
                    centralPublishingOriginsImplied,
                    logger);
        } else {
            throw new IllegalArgumentException("Unknown value '" + credentialScope + "' for "
                    + DefaultRepositorySystemSessionFactory.MAVEN_REPOSITORY_CREDENTIAL_SCOPE
                    + ". Supported values are: " + SCOPE_ORIGIN + ", " + SCOPE_STRICT + ", " + SCOPE_ID);
        }
    }

    /**
     * Records the origin of an operator-declared repository or mirror for the given id. URLs without a
     * parseable server authority (for example {@code file:} URLs) are ignored.
     */
    static void addOrigin(Map<String, Set<String>> declaredOrigins, String id, String url) {
        String origin = originOf(url);
        if (id != null && origin != null) {
            declaredOrigins.computeIfAbsent(id, k -> new HashSet<>()).add(origin);
        }
    }

    /**
     * Binds the id of the default repository to the origins Maven Central is published to, in addition to
     * the origin it is read from: that id is also the server id commonly used for publishing. Nothing is
     * added unless the built-in location of the default repository is the only origin of that id, so a
     * mirror, a repository or a server {@code <repositoryOrigins>} giving it another origin takes over.
     *
     * @param declaredOrigins all origins collected for the session, including the ones servers declare
     * @return whether the publishing origins were added
     */
    static boolean addCentralPublishingOrigins(Map<String, Set<String>> declaredOrigins) {
        Set<String> origins = declaredOrigins.get(RepositorySystem.DEFAULT_REMOTE_REPO_ID);
        if (origins != null
                && origins.equals(Collections.singleton(originOf(RepositorySystem.DEFAULT_REMOTE_REPO_URL)))) {
            origins.addAll(CENTRAL_PUBLISHING_ORIGINS);
            return true;
        }
        return false;
    }

    /**
     * Records the origin of a deployment repository given in the {@code id::url} form of the Maven Deploy
     * Plugin, or its former {@code id::layout::url} form. Other values are ignored.
     *
     * @param declaredOrigins the origins collected so far
     * @param deploymentRepository the value of an {@code alt*DeploymentRepository} user property, may be
     *     {@code null}
     */
    static void addDeploymentRepositoryOrigin(Map<String, Set<String>> declaredOrigins, String deploymentRepository) {
        if (deploymentRepository == null) {
            return;
        }
        int idEnd = deploymentRepository.indexOf("::");
        if (idEnd > 0) {
            String id = deploymentRepository.substring(0, idEnd).trim();
            String url = deploymentRepository
                    .substring(deploymentRepository.lastIndexOf("::") + 2)
                    .trim();
            if (!id.isEmpty()) {
                addOrigin(declaredOrigins, id, url);
            }
        }
    }

    @Override
    public Authentication getAuthentication(RemoteRepository repository) {
        Authentication auth = delegate.getAuthentication(repository);
        if (auth == null) {
            return null;
        }
        String id = repository.getId();
        String origin = originOf(repository.getUrl());
        Set<String> origins = declaredOrigins.get(id);
        if (origins != null && !origins.isEmpty()) {
            if (origin != null && origins.contains(origin)) {
                if (centralPublishingOriginsImplied
                        && RepositorySystem.DEFAULT_REMOTE_REPO_ID.equals(id)
                        && CENTRAL_PUBLISHING_ORIGINS.contains(origin)) {
                    warnOnce(
                            id,
                            "publishing origins",
                            "Using credentials of server '" + id + "' for repository " + repository.getUrl()
                                    + " because Maven binds this id to the origins Maven Central is published to, "
                                    + "https://central.sonatype.com and https://ossrh-staging-api.central.sonatype.com,"
                                    + " while nothing else gives this id an origin. This binding will be removed in a"
                                    + " future release: add these origins to <repositoryOrigins> of that server in"
                                    + " settings, or publish with a server id of its own, such as the"
                                    + " publishingServerId of central-publishing-maven-plugin.");
                }
                return auth;
            }
            warnOnce(
                    id,
                    origin,
                    "Not using credentials of server '" + id + "' for repository " + repository.getUrl()
                            + ": the origins declared for this id are " + origins
                            + ". Add " + originHint(origin)
                            + " to <repositoryOrigins> of that server in settings if these credentials belong"
                            + " there, or set "
                            + DefaultRepositorySystemSessionFactory.MAVEN_REPOSITORY_CREDENTIAL_SCOPE + "="
                            + SCOPE_ID + " to restore legacy id-only credential matching.");
            return null;
        }
        if (strict) {
            warnOnce(
                    id,
                    origin,
                    "Not using credentials of server '" + id + "' for repository " + repository.getUrl()
                            + ": no repository or mirror with this id is declared in settings or on the command"
                            + " line and the server declares no <repositoryOrigins>, and "
                            + DefaultRepositorySystemSessionFactory.MAVEN_REPOSITORY_CREDENTIAL_SCOPE
                            + "=" + SCOPE_STRICT + " is in effect. Declare " + originHint(origin)
                            + " in <repositoryOrigins> of that server in settings to allow it.");
            return null;
        }
        warnOnce(
                id,
                origin,
                "Using credentials of server '" + id + "' for repository " + repository.getUrl()
                        + ", although no repository or mirror with this id is declared in settings or on the"
                        + " command line and the server declares no <repositoryOrigins>. Declare "
                        + originHint(origin)
                        + " in <repositoryOrigins> of that server in settings to bind these credentials"
                        + " explicitly, or set "
                        + DefaultRepositorySystemSessionFactory.MAVEN_REPOSITORY_CREDENTIAL_SCOPE
                        + "=" + SCOPE_STRICT + " to refuse such credential use.");
        return auth;
    }

    private static String originHint(String origin) {
        return origin != null ? "'" + origin + "'" : "its origin";
    }

    private void warnOnce(String id, String origin, String message) {
        if (reported.add(id + "->" + origin)) {
            logger.warn(message);
        }
    }

    /**
     * Returns the normalized origin ({@code protocol://host[:port]}, lower-cased, default http/https
     * ports elided) of the given URL, or {@code null} if the URL has no parseable server authority.
     */
    static String originOf(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url).parseServerAuthority();
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            host = host.toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if ((port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
                port = -1;
            }
            return port >= 0 ? scheme + "://" + host + ":" + port : scheme + "://" + host;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
