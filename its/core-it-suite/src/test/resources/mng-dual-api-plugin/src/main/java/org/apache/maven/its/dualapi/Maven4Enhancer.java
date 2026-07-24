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
package org.apache.maven.its.dualapi;

import java.util.Map;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;

/**
 * Extracts enhanced information using the <b>real typed</b> Maven 4 API.
 * <p>
 * This class directly imports {@link Session}, {@link Project}, etc. —
 * no reflection needed for individual method calls. The Maven 4 API
 * dependency is {@code provided + optional} in the POM, so the code
 * compiles cleanly but the classes are absent at runtime on Maven 3.
 * <p>
 * <b>The calling code must catch {@link NoClassDefFoundError}</b> around
 * any reference to this class. On Maven 3 there are two possible
 * outcomes:
 * <ol>
 *   <li>{@code maven-api-core} is <b>not</b> on the classpath — the JVM
 *       throws {@link NoClassDefFoundError} when it tries to load this
 *       class. The Mojo's catch block handles that.</li>
 *   <li>{@code maven-api-core} <b>is</b> on the classpath (Maven 3
 *       resolved the artifact from Central) — this class loads, but
 *       {@link MavenRuntimeDetector#getMaven4Session} returns
 *       {@code null} because the {@code MavenSession.getSession()}
 *       bridge method doesn't exist on Maven 3. Handled here.</li>
 * </ol>
 * Both paths result in no Maven 4 data being emitted.
 *
 * <pre>
 * // In the Mojo — the ONE place we catch the error:
 * try {
 *     Maven4Enhancer.tryEnhance(mavenSession, log);
 * } catch (NoClassDefFoundError e) {
 *     // Maven 3 path 1: class can't load, fall back gracefully
 * }
 * </pre>
 */
public final class Maven4Enhancer {

    private Maven4Enhancer() {}

    /**
     * Single entry point called from the Mojo.
     * <p>
     * Obtains the Maven 4 {@link Session} via the
     * {@link MavenRuntimeDetector} bridge, then logs enhanced info.
     * If the bridge returns {@code null}, logs a diagnostic message.
     *
     * @param legacy the Maven 3 session (injected into the Mojo)
     * @param log    the Mojo logger
     */
    public static void tryEnhance(MavenSession legacy, Log log) {
        Session session = MavenRuntimeDetector.getMaven4Session(legacy);
        if (session != null) {
            log.info("[RUNTIME] Maven 4");
            enhance(session, log);
        } else {
            // Maven 4 API classes are on the classpath (this class loaded),
            // but MavenSession.getSession() bridge is absent — Maven 3
            // resolved the api jar from Central without actually running
            // Maven 4.  Treat as Maven 3.
            log.info("[RUNTIME] Maven 3 (API jar present but bridge absent)");
        }
    }

    /**
     * Logs enhanced project information using the Maven 4 Session API.
     * <p>
     * Every call here is a direct, type-safe method invocation — no
     * reflection, no string-based method names, full IDE navigation
     * and compile-time checking.
     */
    private static void enhance(Session session, Log log) {
        // ── Session-level info ──────────────────────────────────
        log.info("[MVN4] maven.version = " + session.getMavenVersion());
        log.info("[MVN4] root.directory = " + session.getRootDirectory());
        log.info("[MVN4] top.directory = " + session.getTopDirectory());
        log.info("[MVN4] start.time = " + session.getStartTime());
        log.info("[MVN4] degree.of.concurrency = " + session.getDegreeOfConcurrency());

        // ── Reactor ─────────────────────────────────────────────
        log.info("[MVN4] reactor.project.count = " + session.getProjects().size());
        log.info("[MVN4] remote.repository.count = " + session.getRemoteRepositories().size());

        // ── Properties (typed Map<String,String>, not Properties) ─
        Map<String, String> sysProps = session.getSystemProperties();
        log.info("[MVN4] system.properties.count = " + sysProps.size());

        Map<String, String> userProps = session.getUserProperties();
        log.info("[MVN4] user.properties.count = " + userProps.size());

        // ── Per-project info (first project in reactor) ─────────
        if (!session.getProjects().isEmpty()) {
            Project project = session.getProjects().get(0);
            // Effective properties merge system < project < user
            Map<String, String> effective = session.getEffectiveProperties(project);
            log.info("[MVN4] effective.properties.count = " + effective.size());
        }
    }
}
