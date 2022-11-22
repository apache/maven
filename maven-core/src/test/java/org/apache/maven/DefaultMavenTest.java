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
package org.apache.maven;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.junit.jupiter.api.Test;

public class DefaultMavenTest extends AbstractCoreMavenComponentTestCase {
    @Singleton
    @Named("WsrClassCatcher")
    private static final class WsrClassCatcher extends AbstractMavenLifecycleParticipant {
        private final AtomicReference<Class<?>> wsrClassRef = new AtomicReference<>(null);

        @Override
        public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
            wsrClassRef.set(session.getRepositorySession().getWorkspaceReader().getClass());
        }
    }

    @Inject
    private Maven maven;

    @Override
    protected String getProjectsDirectory() {
        return "src/test/projects/default-maven";
    }

    @Test
    public void testEnsureResolverSessionHasMavenWorkspaceReader() throws Exception {
        WsrClassCatcher wsrClassCatcher =
                (WsrClassCatcher) getContainer().lookup(AbstractMavenLifecycleParticipant.class, "WsrClassCatcher");
        Maven maven = getContainer().lookup(Maven.class);
        MavenExecutionRequest request =
                createMavenExecutionRequest(getProject("simple")).setGoals(asList("validate"));

        MavenExecutionResult result = maven.execute(request);

        Class<?> wsrClass = wsrClassCatcher.wsrClassRef.get();
        assertNotNull(wsrClass, "wsr cannot be null");
        assertTrue(MavenWorkspaceReader.class.isAssignableFrom(wsrClass), String.valueOf(wsrClass));
    }

    @Test
    public void testThatErrorDuringProjectDependencyGraphCreationAreStored() throws Exception {
        MavenExecutionRequest request =
                createMavenExecutionRequest(getProject("cyclic-reference")).setGoals(asList("validate"));

        MavenExecutionResult result = maven.execute(request);

        assertEquals(ProjectCycleException.class, result.getExceptions().get(0).getClass());
    }

    @Test
    public void testMavenProjectNoDuplicateArtifacts() throws Exception {
        MavenProjectHelper mavenProjectHelper = getContainer().lookup(MavenProjectHelper.class);
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact(new DefaultArtifact("g", "a", "1.0", Artifact.SCOPE_TEST, "jar", "", null));
        File artifactFile = Files.createTempFile("foo", "tmp").toFile();
        try {
            mavenProjectHelper.attachArtifact(mavenProject, "sources", artifactFile);
            assertEquals(1, mavenProject.getAttachedArtifacts().size());
            mavenProjectHelper.attachArtifact(mavenProject, "sources", artifactFile);
            assertEquals(1, mavenProject.getAttachedArtifacts().size());
        } finally {
            Files.deleteIfExists(artifactFile.toPath());
        }
    }
}
