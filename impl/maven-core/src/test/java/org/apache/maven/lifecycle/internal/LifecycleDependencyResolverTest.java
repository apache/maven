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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleDependencyResolverTest extends AbstractCoreMavenComponentTestCase {
    @Inject
    private LifecycleDependencyResolver resolver;

    @Override
    protected String getProjectsDirectory() {
        return null;
    }

    @Test
    void testCachedReactorProjectDependencies() throws Exception {
        MavenSession session = createMavenSession(
                new File("src/test/projects/lifecycle-dependency-resolver/pom.xml"), new Properties(), true);
        Collection<String> scopesToCollect = null;
        Collection<String> scopesToResolve = Collections.singletonList("compile");
        boolean aggregating = false;

        Set<Artifact> reactorArtifacts = new HashSet<>(3);
        for (MavenProject reactorProject : session.getProjects()) {
            reactorProject.setArtifactFilter(artifact -> true);
            resolver.resolveProjectDependencies(
                    reactorProject, scopesToCollect, scopesToResolve, session, aggregating, reactorArtifacts);
            reactorArtifacts.add(reactorProject.getArtifact());
        }

        MavenProject lib = session.getProjects().get(1);
        MavenProject war = session.getProjects().get(2);

        assertNull(war.getArtifactMap()
                .get("org.apache.maven.its.mng6300:mng6300-lib")
                .getFile());

        lib.getArtifact().setFile(new File("lib.jar"));

        resolver.resolveProjectDependencies(
                war, scopesToCollect, scopesToResolve, session, aggregating, reactorArtifacts);

        assertEquals(
                new File("lib.jar"),
                war.getArtifactMap()
                        .get("org.apache.maven.its.mng6300:mng6300-lib")
                        .getFile());
    }

    /**
     * The project depends on a:2.0, which depends on b:2.0, and declares b:1.0 with test scope (MNG-6058).
     * The test dependency must override b:2.0 only when the test scope is requested.
     */
    @Test
    void testTestDependencyOverridesApplicationDependencyOnlyForTestScope() throws Exception {
        MavenSession session =
                createMavenSession(new File("src/test/projects/lifecycle-dependency-resolver-scopes/pom.xml"));
        MavenProject project = session.getCurrentProject();
        project.setArtifactFilter(artifact -> true);

        List<String> compileScopes = List.of("compile", "system", "provided");
        List<String> runtimeScopes = List.of("compile", "runtime");
        List<String> testScopes = List.of("compile", "system", "provided", "runtime", "test");

        assertEquals(Set.of("a:2.0", "b:2.0"), resolve(session, project, compileScopes));
        assertEquals(Set.of("a:2.0", "b:2.0"), resolve(session, project, runtimeScopes));
        assertEquals(Set.of("a:2.0", "b:1.0"), resolve(session, project, testScopes));
        // the graph of the test scope must not leak into the graphs requested afterwards
        assertEquals(Set.of("a:2.0", "b:2.0"), resolve(session, project, compileScopes));
        assertEquals(Set.of("a:2.0", "b:2.0"), resolve(session, project, runtimeScopes));

        for (Artifact artifact : project.getArtifacts()) {
            assertTrue(artifact.getFile().isFile(), artifact.toString());
        }
    }

    private Set<String> resolve(MavenSession session, MavenProject project, Collection<String> scopesToResolve)
            throws Exception {
        resolver.resolveProjectDependencies(project, null, scopesToResolve, session, false, Collections.emptySet());
        Set<String> result = new TreeSet<>();
        for (Artifact artifact : project.getArtifacts()) {
            result.add(artifact.getArtifactId() + ":" + artifact.getVersion());
        }
        return result;
    }
}
