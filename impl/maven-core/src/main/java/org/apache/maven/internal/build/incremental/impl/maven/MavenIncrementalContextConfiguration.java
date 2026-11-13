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
package org.apache.maven.internal.build.incremental.impl.maven;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.build.incremental.spi.IncrementalContextEnvironment;
import org.apache.maven.api.build.incremental.spi.IncrementalContextFinalizer;
import org.apache.maven.api.build.incremental.spi.Workspace;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.MojoExecutionScoped;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.internal.build.incremental.impl.maven.digest.MojoConfigurationDigester;

/**
 * Provides the {@link IncrementalContextEnvironment} for a single mojo execution.
 *
 * <p>This class wires together the pieces that initialize a
 * {@link org.apache.maven.internal.build.incremental.impl.DefaultIncrementalContext DefaultIncrementalContext}:</p>
 * <ul>
 *   <li><strong>State file</strong> — persisted at
 *       {@code ${project.build.directory}/incremental/<groupId>_<artifactId>_<goal>_<executionId>}.
 *       A {@code clean} build deletes the entire {@code target/} directory, so the context
 *       starts fresh.</li>
 *   <li><strong>Parameters</strong> — computed by {@link MojoConfigurationDigester}, which
 *       digests all mojo parameters and the plugin classpath. Changes between builds
 *       trigger escalation.</li>
 *   <li><strong>Workspace</strong> — the {@link ProjectWorkspace} for file-system access.</li>
 *   <li><strong>Finalizer</strong> — the {@link MavenIncrementalContextFinalizer} that commits
 *       the context after mojo execution.</li>
 * </ul>
 *
 * @since 4.1.0
 * @see MojoConfigurationDigester
 * @see MavenIncrementalContextFinalizer
 */
@Named
@MojoExecutionScoped
public class MavenIncrementalContextConfiguration implements IncrementalContextEnvironment {

    /**
     * User property that disables the build context entirely.
     *
     * <p>When set to {@code true} (via {@code -Dmaven.buildcontext.skip=true}),
     * the configuration digester is not run and no state file is written. This
     * means every mojo execution performs a full build and the next build will
     * also start from scratch. Useful for CI clean builds or release builds
     * where incremental overhead is wasted.</p>
     */
    static final String SKIP_PROPERTY = "maven.buildcontext.skip";

    private final ProjectWorkspace workspace;
    private final Path stateFile;
    private final Map<String, Serializable> parameters;
    private final MavenIncrementalContextFinalizer finalizer;

    @Inject
    public MavenIncrementalContextConfiguration(
            ProjectWorkspace workspace,
            MojoConfigurationDigester digester,
            MavenIncrementalContextFinalizer finalizer,
            Session session,
            Project project,
            MojoExecution execution)
            throws IOException {
        this.workspace = workspace;
        this.finalizer = finalizer;

        boolean skip = Boolean.parseBoolean(session.getUserProperties().get(SKIP_PROPERTY));
        if (skip) {
            // Skip all digester computation and state persistence.
            // DefaultIncrementalContext will see no old state and escalate to a full build.
            this.stateFile = null;
            this.parameters = Collections.emptyMap();
        } else {
            this.stateFile = getExecutionStateLocation(project, execution);
            this.parameters = digester.digest();
        }
    }

    @Override
    public Path getStateFile() {
        return stateFile;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public Map<String, Serializable> getParameters() {
        return parameters;
    }

    @Override
    public IncrementalContextFinalizer getFinalizer() {
        return finalizer;
    }

    /**
     * Returns conventional location of MojoExecution incremental build state
     */
    public Path getExecutionStateLocation(Project project, MojoExecution execution) {
        Path stateDirectory = getProjectStateLocation(project);
        String builderId = getExecutionId(execution);
        return stateDirectory.resolve(builderId);
    }

    /**
     * Returns conventional MojoExecution identifier used by incremental build tools.
     */
    public String getExecutionId(MojoExecution execution) {
        PluginDescriptor pluginDescriptor = execution.getPlugin().getDescriptor();
        String builderId = pluginDescriptor.getGroupId()
                + '_'
                + pluginDescriptor.getArtifactId()
                + '_'
                + execution.getGoal()
                + '_'
                + execution.getExecutionId();
        return builderId;
    }

    /**
     * Returns conventional location of MavenProject incremental build state
     */
    public Path getProjectStateLocation(Project project) {
        return Paths.get(project.getBuild().getDirectory(), "incremental");
    }
}
