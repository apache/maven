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
package org.apache.maven.internal.build.impl.maven;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.build.spi.BuildContextEnvironment;
import org.apache.maven.api.build.spi.BuildContextFinalizer;
import org.apache.maven.api.build.spi.Workspace;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.internal.build.impl.maven.digest.MojoConfigurationDigester;

@Named
@MojoExecutionScoped
public class MavenBuildContextConfiguration implements BuildContextEnvironment {

    private final ProjectWorkspace workspace;
    private final Path stateFile;
    private final Map<String, Serializable> parameters;
    private final MavenBuildContextFinalizer finalizer;

    @Inject
    public MavenBuildContextConfiguration(
            ProjectWorkspace workspace,
            MojoConfigurationDigester digester,
            MavenBuildContextFinalizer finalizer,
            Project project,
            MojoExecution execution)
            throws IOException {
        this.workspace = workspace;
        this.finalizer = finalizer;
        this.stateFile = getExecutionStateLocation(project, execution);
        this.parameters = digester.digest();
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
    public BuildContextFinalizer getFinalizer() {
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
