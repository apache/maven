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
package org.apache.maven.execution;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MavenSession}.
 */
class MavenSessionTest {

    @Test
    void testReadingCurrentProjectOnOtherThreadDoesNotRetainProject() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            WeakReference<MavenProject> root = readCurrentProjectOnThread(executor);
            for (int i = 0; i < 10 && root.get() != null; i++) {
                System.gc();
                Thread.sleep(10);
            }
            assertNull(root.get(), "the thread must not retain the project after the session is discarded");
        } finally {
            executor.shutdownNow();
        }
    }

    private WeakReference<MavenProject> readCurrentProjectOnThread(ExecutorService executor) throws Exception {
        MavenSession session = createSession();
        MavenProject root = new MavenProject();
        root.setExecutionRoot(true);
        session.setProjects(Collections.singletonList(root));
        executor.submit(() -> {
                    assertSame(root, session.getCurrentProject());
                    MavenSession clone = session.clone();
                    assertSame(root, clone.getCurrentProject());
                })
                .get(10, TimeUnit.SECONDS);
        return new WeakReference<>(root);
    }

    private MavenSession createSession() {
        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        return new MavenSession(null, repoSession, request, result);
    }
}
