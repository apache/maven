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

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

/**
 * A Mojo that displays project information using the Maven 3 API, then
 * automatically enhances the output with Maven 4 API data when available.
 * <p>
 * Extends {@link AbstractMojo} (Maven 3 API) — this class has <b>no
 * imports from {@code org.apache.maven.api.*}</b>, so it loads cleanly
 * on both Maven 3 and Maven 4.
 * <p>
 * Maven 4 code lives in {@link Maven4Enhancer} and
 * {@link MavenRuntimeDetector}, which DO import Maven 4 types. The JVM
 * only loads those classes when first referenced; on Maven 3, the load
 * fails with {@link NoClassDefFoundError}, which we catch right here
 * at the boundary.
 * <p>
 * Log output uses bracketed tags for easy assertion:
 * <ul>
 *   <li>{@code [RUNTIME] Maven 3} or {@code [RUNTIME] Maven 4}</li>
 *   <li>{@code [MVN3] key = value} — always emitted</li>
 *   <li>{@code [MVN4] key = value} — only on Maven 4</li>
 * </ul>
 */
@Mojo(name = "project-info", requiresProject = true)
public class ProjectInfoMojo extends AbstractMojo {

    private final MavenProject project;
    private final MavenSession mavenSession;

    @Inject
    public ProjectInfoMojo(MavenProject project, MavenSession mavenSession) {
        this.project = project;
        this.mavenSession = mavenSession;
    }

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();

        // ── Maven 3 baseline (always available) ────────────────
        log.info("[MVN3] groupId = " + project.getGroupId());
        log.info("[MVN3] artifactId = " + project.getArtifactId());
        log.info("[MVN3] version = " + project.getVersion());
        log.info("[MVN3] packaging = " + project.getPackaging());
        log.info("[MVN3] dependencies.size = " + project.getDependencies().size());

        // ── Maven 4 enhanced output ────────────────────────────
        // This try block is the SOLE boundary where we catch
        // NoClassDefFoundError.  Maven4Enhancer imports
        // org.apache.maven.api.Session — on Maven 3 that class
        // doesn't exist, so the JVM throws NoClassDefFoundError
        // the instant it tries to load Maven4Enhancer.
        //
        // NOTE: this class (ProjectInfoMojo) must NOT import
        // anything from org.apache.maven.api.*, otherwise it
        // would itself fail to load on Maven 3, before we ever
        // reach this try block.
        try {
            Maven4Enhancer.tryEnhance(mavenSession, log);
        } catch (NoClassDefFoundError e) {
            // Maven 3: org.apache.maven.api.Session doesn't exist,
            // so loading Maven4Enhancer (which imports it) fails.
            // This is the expected, designed-for code path.
            log.info("[RUNTIME] Maven 3");
        }
    }
}
