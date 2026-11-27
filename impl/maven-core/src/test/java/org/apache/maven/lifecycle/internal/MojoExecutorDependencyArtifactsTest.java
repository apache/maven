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
package org.apache.maven.lifecycle.internal;

import javax.inject.Inject;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A mojo without any dependency resolution or collection scope must still see the direct dependency artifacts
 * of its project, as it did before the scope-less step stopped being resolved (#14081).
 */
class MojoExecutorDependencyArtifactsTest extends AbstractCoreMavenComponentTestCase {

    @Inject
    private MojoExecutor mojoExecutor;

    @Override
    protected String getProjectsDirectory() {
        return null;
    }

    @Test
    void mojoWithoutScopeSeesDirectDependencyArtifacts() throws Exception {
        MavenSession session =
                createMavenSession(new File("src/test/projects/mojo-executor-dependency-artifacts/pom.xml"));
        MavenProject project = session.getCurrentProject();

        MojoDescriptor mojo = new MojoDescriptor();
        mojo.setPluginDescriptor(new PluginDescriptor());

        mojoExecutor.ensureDependenciesAreResolved(
                mojo, session, new DependencyContext(project, Collections.emptyList(), Collections.emptyList()));

        Set<Artifact> artifacts = project.getDependencyArtifacts();
        assertNotNull(artifacts, "getDependencyArtifacts() must not be null for a mojo without a scope");
        assertEquals(1, artifacts.size());
        Artifact direct = artifacts.iterator().next();
        assertEquals("direct", direct.getArtifactId());
        assertFalse(direct.isResolved(), "nothing may be resolved for a mojo without a scope");
        assertEquals(0, project.getArtifacts().size());
    }
}
