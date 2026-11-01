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
import java.util.Properties;
import java.util.Set;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
     * Verifies that resolving a downstream project (war) does not throw when a reactor sibling
     * (lib) has a {@code null} artifact file — i.e. the sibling has not yet packaged its jar.
     *
     * <p>This guards the null-file code path introduced by the fix in
     * <a href=https://github.com/apache/maven/pull/13728>#13728</a>. The concurrent-builder
     * race (sibling file is {@code null} at map-build time but non-null at lookup time within the
     * same call) cannot be reproduced in a sequential unit test; this test covers the observable
     * contract: when the sibling file is {@code null}, resolution completes without error and the
     * resolved artifact's file remains {@code null} rather than causing an NPE downstream.
     */
    @Test
    void testReactorDependencyWithNullArtifactFileDoesNotThrow() throws Exception {
        MavenSession session = createMavenSession(
                new File("src/test/projects/lifecycle-dependency-resolver/pom.xml"), new Properties(), true);
        Collection<String> scopesToCollect = null;
        Collection<String> scopesToResolve = Collections.singletonList("compile");
        boolean aggregating = false;

        MavenProject root = session.getProjects().get(0);
        MavenProject lib = session.getProjects().get(1);
        MavenProject war = session.getProjects().get(2);

        // lib has not yet produced its jar (concurrent builder: packaging not yet complete)
        lib.getArtifact().setFile(null);

        Set<Artifact> reactorArtifacts = new HashSet<>();
        root.setArtifactFilter(artifact -> true);
        lib.setArtifactFilter(artifact -> true);
        war.setArtifactFilter(artifact -> true);

        resolver.resolveProjectDependencies(
                root, scopesToCollect, scopesToResolve, session, aggregating, reactorArtifacts);
        reactorArtifacts.add(root.getArtifact());

        resolver.resolveProjectDependencies(
                lib, scopesToCollect, scopesToResolve, session, aggregating, reactorArtifacts);
        reactorArtifacts.add(lib.getArtifact());

        // Resolution of war must complete without NPE even though lib's artifact file is null
        resolver.resolveProjectDependencies(
                war, scopesToCollect, scopesToResolve, session, aggregating, reactorArtifacts);

        assertNull(
                war.getArtifactMap()
                        .get("org.apache.maven.its.mng6300:mng6300-lib")
                        .getFile(),
                "resolved lib artifact file should remain null when lib has not yet packaged its jar");
    }

    @Test
    void testTestJarNotResolvedWhenTestSkipped() throws Exception {
        File pom = new File("src/test/projects/lifecycle-dependency-resolver-test-jar/pom.xml");
        Properties userProps = new Properties();
        userProps.setProperty("maven.test.skip", "true");

        MavenSession session = createMavenSession(pom, new Properties(), true);
        session.getUserProperties().putAll(userProps);

        MavenProject common = session.getProjects().get(1);
        MavenProject core = session.getProjects().get(2);

        common.getArtifact().setFile(new File("common.jar"));

        Collection<String> scopesToResolve = Collections.singletonList("test");

        resolver.resolveProjectDependencies(core, null, scopesToResolve, session, false, Collections.emptySet());

        assertNull(core.getArtifactMap().get("org.apache.maven.its.mng8074:mng8074-common:test-jar:tests"));
    }

    @Test
    void testTestJarResolutionFailsWhenTestNotSkipped() throws Exception {
        File pom = new File("src/test/projects/lifecycle-dependency-resolver-test-jar/pom.xml");

        MavenSession session = createMavenSession(pom, new Properties(), true);

        MavenProject common = session.getProjects().get(1);
        MavenProject core = session.getProjects().get(2);

        common.getArtifact().setFile(new File("common.jar"));

        Collection<String> scopesToResolve = Collections.singletonList("test");

        assertThrows(
                LifecycleExecutionException.class,
                () -> resolver.resolveProjectDependencies(
                        core, null, scopesToResolve, session, false, Collections.emptySet()));
    }
}
