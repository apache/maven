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

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;

import org.apache.maven.api.services.Lookup;
import org.apache.maven.execution.BuildResumptionAnalyzer;
import org.apache.maven.execution.BuildResumptionDataRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.internal.impl.DefaultSessionFactory;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.resolver.RepositorySystemSessionFactory;
import org.apache.maven.session.scope.internal.SessionScope;
import org.eclipse.aether.repository.WorkspaceReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultMavenSessionScopeTest {

    @Test
    void testSessionScopeIsExitedOnWorkspaceReaderError() throws Exception {
        WorkspaceReader badReader = mock(WorkspaceReader.class);
        when(badReader.getRepository()).thenReturn(null);

        File localRepoDir = Files.createTempDirectory("local-repo").toFile();
        localRepoDir.deleteOnExit();
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
                .setLocalRepositoryPath(localRepoDir)
                .setWorkspaceReader(badReader);

        SessionScope sessionScope = new SessionScope();

        DefaultMaven defaultMaven = new DefaultMaven(
                mock(Lookup.class),
                mock(ExecutionEventCatapult.class),
                mock(LegacySupport.class),
                sessionScope,
                mock(RepositorySystemSessionFactory.class),
                mock(GraphBuilder.class),
                mock(BuildResumptionAnalyzer.class),
                mock(BuildResumptionDataRepository.class),
                null,
                mock(DefaultSessionFactory.class),
                null);

        MavenExecutionResult result = defaultMaven.execute(request);

        assertFalse(result.getExceptions().isEmpty());

        Field valuesField = SessionScope.class.getSuperclass().getDeclaredField("values");
        valuesField.setAccessible(true);
        List<?> values = (List<?>) valuesField.get(sessionScope);
        assertTrue(values.isEmpty(), "Session scope must be exited even on workspace reader error");
    }
}
