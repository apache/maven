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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.logging.OutputCapabilities.Destination;
import org.apache.maven.logging.internal.DefaultOutputCapabilities;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
class OutputCapabilitiesRequestTest extends AbstractCoreMavenComponentTestCase {
    private static final String KEY = "maven.logging.outputCapabilities";

    @Inject
    private Maven maven;

    @Inject
    private DefaultOutputCapabilities capabilities;

    @Override
    protected String getProjectsDirectory() {
        return "src/test/projects/default-maven";
    }

    @Test
    void capturesEveryConfigurationBeforeRepositoryValidation(@TempDir Path directory) throws Exception {
        Path invalidRepository = Files.createFile(directory.resolve("repository-file"));
        MavenExecutionRequest request =
                new DefaultMavenExecutionRequest().setLocalRepositoryPath(invalidRepository.toFile());
        request.getData().put("unrelated", "retained");
        assertNull(request.getData().get(KEY));
        for (Destination destination : Destination.values()) {
            for (Charset encoding : Arrays.asList(null, StandardCharsets.UTF_8, Charset.forName("latin1"))) {
                Map<String, String> captured;
                try (AutoCloseable cleanup =
                        capabilities.install(DefaultOutputCapabilities.snapshot(destination, encoding))) {
                    captured = capabilities.asMap();
                    MavenExecutionResult result = maven.execute(request);
                    assertFalse(result.getExceptions().isEmpty());
                    assertSame(captured, request.getData().get(KEY));
                    assertEquals(destination.name(), captured.get("destination"));
                    assertEquals(encoding == null ? null : encoding.name(), captured.get("encoding"));
                    assertEquals("retained", request.getData().get("unrelated"));
                }
                assertSame(captured, request.getData().get(KEY));
                assertEquals(destination.name(), captured.get("destination"));
                assertEquals(encoding == null ? null : encoding.name(), captured.get("encoding"));
                assertEquals(Destination.UNKNOWN, capabilities.getDestination());
            }
        }
    }

    @Test
    void publishesBeforeLifecycleCallbacksAndParallelReaders() throws Exception {
        MavenExecutionRequest request = request("simple");
        assertNull(request.getData().get(KEY));
        RequestObserver observer = observer();
        try (AutoCloseable cleanup =
                capabilities.install(DefaultOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8))) {
            executeSuccessfully(request);
            Map<String, String> captured = capabilities.asMap();
            assertSame(request, observer.session.getRequest());
            assertSame(captured, observer.captured);
            assertNull(observer.resolverMetadata);
            assertSame(
                    captured,
                    CompletableFuture.supplyAsync(() -> observer.session
                                    .clone()
                                    .getRequest()
                                    .getData()
                                    .get(KEY))
                            .get(10, TimeUnit.SECONDS));
            assertEquals("UTF-8", captured.get("encoding"));
        }
        assertEquals("FILE", observer.captured.get("destination"));
        assertEquals(Destination.UNKNOWN, capabilities.getDestination());
        assertEquals(Optional.empty(), capabilities.getEncoding());
    }

    @Test
    void copiedIndependentAndReusedRequestsCaptureTheirOwnExecution() throws Exception {
        MavenExecutionRequest original = request("simple");
        original.getData().put("unrelated", "original");
        Map<String, String> fileMap;
        Map<String, String> consoleMap;
        try (AutoCloseable file =
                capabilities.install(DefaultOutputCapabilities.snapshot(Destination.FILE, StandardCharsets.UTF_8))) {
            executeSuccessfully(original);
            fileMap = capabilities.asMap();
            MavenExecutionRequest copy = DefaultMavenExecutionRequest.copy(original);
            assertTrue(copy.getData().isEmpty());
            copy.getData().put("unrelated", "copy");
            MavenExecutionRequest independent = request("simple");
            assertNull(independent.getData().get(KEY));
            try (AutoCloseable console = capabilities.install(
                    DefaultOutputCapabilities.snapshot(Destination.CONSOLE, StandardCharsets.ISO_8859_1))) {
                consoleMap = capabilities.asMap();
                executeSuccessfully(copy);
                executeSuccessfully(independent);
                assertSame(consoleMap, copy.getData().get(KEY));
                assertSame(consoleMap, independent.getData().get(KEY));
                assertSame(fileMap, original.getData().get(KEY));
                assertEquals("original", original.getData().get("unrelated"));
                assertEquals("copy", copy.getData().get("unrelated"));
                assertNull(independent.getData().get("unrelated"));
                executeSuccessfully(original);
                assertSame(consoleMap, original.getData().get(KEY));
                assertEquals("original", original.getData().get("unrelated"));
                assertEquals("FILE", fileMap.get("destination"));
            }
            assertSame(consoleMap, original.getData().get(KEY));
            executeSuccessfully(original);
            assertSame(fileMap, original.getData().get(KEY));
            assertEquals("CONSOLE", consoleMap.get("destination"));
            assertEquals("ISO-8859-1", consoleMap.get("encoding"));
        }
        assertSame(fileMap, original.getData().get(KEY));
        executeSuccessfully(original);
        assertSame(capabilities.asMap(), original.getData().get(KEY));
        assertEquals("UNKNOWN", capabilities.asMap().get("destination"));
        assertFalse(capabilities.asMap().containsKey("encoding"));
        assertEquals("original", original.getData().get("unrelated"));
        assertEquals("FILE", fileMap.get("destination"));
        assertEquals("UTF-8", fileMap.get("encoding"));
        assertEquals("CONSOLE", consoleMap.get("destination"));
    }

    @Test
    void projectDiscoveryFailurePreservesTheCapturedMap() throws Exception {
        MavenExecutionRequest request = request("cyclic-reference");
        RequestObserver observer = observer();
        Map<String, String> captured;
        try (AutoCloseable cleanup = capabilities.install(
                DefaultOutputCapabilities.snapshot(Destination.UNKNOWN, StandardCharsets.ISO_8859_1))) {
            captured = capabilities.asMap();
            MavenExecutionResult result = maven.execute(request);
            assertEquals(
                    ProjectCycleException.class, result.getExceptions().get(0).getClass());
            assertSame(captured, observer.captured);
            assertNull(observer.resolverMetadata);
        }
        assertSame(captured, request.getData().get(KEY));
        assertEquals("UNKNOWN", captured.get("destination"));
        assertEquals("ISO-8859-1", captured.get("encoding"));
        assertEquals(Optional.empty(), capabilities.getEncoding());
    }

    private MavenExecutionRequest request(String project) throws Exception {
        return createMavenExecutionRequest(getProject(project)).setGoals(Collections.singletonList("validate"));
    }

    private void executeSuccessfully(MavenExecutionRequest request) {
        assertEquals(Collections.emptyList(), maven.execute(request).getExceptions());
    }

    private RequestObserver observer() throws Exception {
        return (RequestObserver)
                container.lookup(AbstractMavenLifecycleParticipant.class, "output-capabilities-request");
    }

    @Named("output-capabilities-request")
    @Singleton
    private static final class RequestObserver extends AbstractMavenLifecycleParticipant {
        private MavenSession session;
        private Map<?, ?> captured;
        private Object resolverMetadata;

        @Override
        public void afterSessionStart(MavenSession session) {
            this.session = session;
            captured = (Map<?, ?>) session.getRequest().getData().get(KEY);
            resolverMetadata = session.getRepositorySession().getData().get(KEY);
        }
    }
}
