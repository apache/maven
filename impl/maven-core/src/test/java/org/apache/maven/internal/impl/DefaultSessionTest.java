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
package org.apache.maven.internal.impl;

import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.model.DefaultModelProblem;
import org.apache.maven.model.root.RootLocator;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class DefaultSessionTest {

    @Test
    void testRootDirectoryWithNull() {
        RepositorySystemSession rss = new DefaultRepositorySystemSession(h -> false);
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        MavenSession ms = new MavenSession(null, rss, mer, null);
        DefaultSession session =
                new DefaultSession(ms, mock(RepositorySystem.class), Collections.emptyList(), null, null, null);

        assertEquals(
                RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE,
                assertThrows(IllegalStateException.class, session::getRootDirectory)
                        .getMessage());
    }

    @Test
    void testRootDirectory() {
        RepositorySystemSession rss = new DefaultRepositorySystemSession(h -> false);
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        MavenSession ms = new MavenSession(null, rss, mer, null);
        ms.getRequest().setRootDirectory(Paths.get("myRootDirectory"));
        DefaultSession session =
                new DefaultSession(ms, mock(RepositorySystem.class), Collections.emptyList(), null, null, null);

        assertEquals(Paths.get("myRootDirectory"), session.getRootDirectory());
    }

    @Test
    void modelProblemsAreSharedWithDerivedAndLegacySessions() {
        RepositorySystemSession rss = new DefaultRepositorySystemSession(h -> false);
        MavenSession legacySession = new MavenSession(null, rss, new DefaultMavenExecutionRequest(), null);
        DefaultSession session = new DefaultSession(
                legacySession, mock(RepositorySystem.class), Collections.emptyList(), null, null, null);
        legacySession.setSession(session);

        ModelProblem problem = new DefaultModelProblem(
                "model warning",
                BuilderProblem.Severity.WARNING,
                ModelProblem.Version.BASE,
                "pom.xml",
                12,
                4,
                "org.example:project:1",
                null);
        session.getModelProblemCollector().reportProblem(problem);

        Session derivedSession = session.withRemoteRepositories(Collections.emptyList());

        assertTrue(session.getModelProblemCollector().hasWarningProblems());
        assertTrue(derivedSession.getModelProblemCollector().hasWarningProblems());
        assertSame(session.getModelProblemCollector(), derivedSession.getModelProblemCollector());
        assertSame(
                problem,
                session.getModelProblemCollector().problems().findFirst().orElseThrow());

        assertEquals(1, legacySession.getModelProblems().size());
        org.apache.maven.model.building.ModelProblem legacyProblem =
                legacySession.getModelProblems().get(0);
        assertEquals(problem.getMessage(), legacyProblem.getMessage());
        assertEquals(problem.getSeverity().name(), legacyProblem.getSeverity().name());
        assertEquals(problem.getVersion().name(), legacyProblem.getVersion().name());
        assertEquals(problem.getSource(), legacyProblem.getSource());
        assertEquals(problem.getLineNumber(), legacyProblem.getLineNumber());
        assertEquals(problem.getColumnNumber(), legacyProblem.getColumnNumber());
        assertEquals(problem.getModelId(), legacyProblem.getModelId());
        assertThrows(
                UnsupportedOperationException.class,
                () -> legacySession.getModelProblems().clear());
    }

    @Test
    void legacyModelProblemsAreSharedThroughSessionData() {
        RepositorySystemSession rss = new DefaultRepositorySystemSession(h -> false);
        MavenSession legacySession = new MavenSession(null, rss, new DefaultMavenExecutionRequest(), null);
        DefaultSession session = new DefaultSession(
                legacySession, mock(RepositorySystem.class), Collections.emptyList(), null, null, null);
        legacySession.setSession(session);

        assertTrue(legacySession.getModelProblems().isEmpty());

        org.apache.maven.model.building.ModelProblem problem = new org.apache.maven.model.building.DefaultModelProblem(
                "legacy warning",
                org.apache.maven.model.building.ModelProblem.Severity.WARNING,
                org.apache.maven.model.building.ModelProblem.Version.BASE,
                "pom.xml",
                3,
                7,
                "org.example:legacy:1",
                null);
        legacySession.setModelProblems(Collections.singletonList(problem));

        assertEquals(Collections.singletonList(problem), legacySession.getModelProblems());
        assertEquals(Collections.singletonList(problem), SessionModelProblemsBridge.getModelProblems(session));
        Session derivedSession = session.withRemoteRepositories(Collections.emptyList());
        assertEquals(Collections.singletonList(problem), SessionModelProblemsBridge.getModelProblems(derivedSession));

        legacySession.setModelProblems(Collections.emptyList());

        assertTrue(legacySession.getModelProblems().isEmpty());
        assertTrue(SessionModelProblemsBridge.getModelProblems(derivedSession).isEmpty());
    }

    @Test
    void legacyModelProblemsReportNativeCollectorOverflow() {
        RepositorySystemSession rss = new DefaultRepositorySystemSession(h -> false);
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.getUserProperties().setProperty(Constants.MAVEN_BUILDER_MAX_PROBLEMS, "0");
        MavenSession legacySession = new MavenSession(null, rss, request, null);
        DefaultSession session = new DefaultSession(
                legacySession, mock(RepositorySystem.class), Collections.emptyList(), null, null, null);
        legacySession.setSession(session);

        ModelProblem problem = new DefaultModelProblem(
                "model warning",
                BuilderProblem.Severity.WARNING,
                ModelProblem.Version.BASE,
                "pom.xml",
                -1,
                -1,
                "org.example:project:1",
                null);
        session.getModelProblemCollector().reportProblem(problem);

        assertTrue(session.getModelProblemCollector().problemsOverflow());
        assertEquals(0, session.getModelProblemCollector().problems().count());
        assertEquals(1, legacySession.getModelProblems().size());
        assertTrue(legacySession.getModelProblems().get(0).getMessage().contains("subset"));
    }
}
