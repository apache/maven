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
package org.apache.maven.internal.build;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.ReactorFailureBehavior;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultBuildInvocationTest {

    private static MavenExecutionRequest newRequest() {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepositoryPath(new File("/tmp/test-repo"));
        return request;
    }

    @Test
    void fromBasicRequest() {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(List.of("clean", "install"));
        request.setOffline(true);
        request.setUpdateSnapshots(true);
        request.setInteractiveMode(false);
        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);
        request.setLocalRepositoryPath(new File("/tmp/repo"));

        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertEquals(List.of("clean", "install"), inv.goals());
        assertTrue(inv.offline());
        assertTrue(inv.updateSnapshots());
        assertTrue(inv.batchMode());
        assertEquals(ReactorFailureBehavior.FAIL_AT_END, inv.reactorFailureBehavior());
        assertEquals(Path.of("/tmp/repo"), inv.localRepository());
    }

    @Test
    void fromNullGoals() {
        MavenExecutionRequest request = newRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertNotNull(inv.goals());
        assertTrue(inv.goals().isEmpty());
    }

    @Test
    void fromNullActiveProfiles() {
        MavenExecutionRequest request = newRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertNotNull(inv.activeProfiles());
        assertTrue(inv.activeProfiles().isEmpty());
    }

    @Test
    void fromNullSelectedProjects() {
        MavenExecutionRequest request = newRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertNotNull(inv.selectedProjects());
        assertTrue(inv.selectedProjects().isEmpty());
    }

    @Test
    void fromNullLocalRepositoryPathThrows() {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        assertThrows(IllegalStateException.class, () -> DefaultBuildInvocation.from(session));
    }

    @Test
    void fromNullReactorFailureBehaviorDefaultsToFailFast() {
        MavenExecutionRequest request = newRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertEquals(ReactorFailureBehavior.FAIL_FAST, inv.reactorFailureBehavior());
    }

    @Test
    void resumeFrom() {
        MavenExecutionRequest request = newRequest();
        request.setResumeFrom(":my-module");
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertEquals(":my-module", inv.resumeFrom());
    }

    @Test
    void resumeFromNull() {
        MavenExecutionRequest request = newRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertNull(inv.resumeFrom());
    }

    @Test
    void noTransferProgress() {
        MavenExecutionRequest request = newRequest();
        request.setNoTransferProgress(true);
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertTrue(inv.noTransferProgress());
    }

    @Test
    void sensitiveKeyRedaction() {
        MavenExecutionRequest request = newRequest();
        Properties userProps = new Properties();
        userProps.setProperty("my.password", "secret123");
        userProps.setProperty("api_key", "key123");
        userProps.setProperty("auth.token", "tok123");
        userProps.setProperty("safe.property", "visible");
        request.setUserProperties(userProps);
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertEquals("***", inv.userProperties().get("my.password"));
        assertEquals("***", inv.userProperties().get("api_key"));
        assertEquals("***", inv.userProperties().get("auth.token"));
        assertEquals("visible", inv.userProperties().get("safe.property"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "password",
                "PASSWORD",
                "my.passwd",
                "db_secret",
                "auth_token",
                "APIKEY",
                "my.api_key",
                "credential",
                "ssh.passphrase"
            })
    void sensitiveKeyFragments(String key) {
        assertTrue(DefaultBuildInvocation.isSensitiveKey(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"safe.property", "skip.tests", "maven.compiler.source", "project.build.outputDirectory"})
    void nonSensitiveKeys(String key) {
        assertFalse(DefaultBuildInvocation.isSensitiveKey(key));
    }

    @Test
    void systemInfoCapturesExpectedKeys() {
        MavenExecutionRequest request = newRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        Map<String, String> sysInfo = inv.systemInfo();
        assertNotNull(sysInfo.get("os.name"));
        assertNotNull(sysInfo.get("os.arch"));
        assertNotNull(sysInfo.get("java.vm.version"));
        assertNotNull(sysInfo.get("available.processors"));
    }

    @Test
    void collectionsAreImmutable() {
        DefaultBuildInvocation inv = new DefaultBuildInvocation(
                List.of("compile"),
                Map.of("key", "val"),
                Map.of("os.name", "Linux"),
                Path.of("/tmp/repo"),
                List.of("profile1"),
                List.of(":module1"),
                null,
                ReactorFailureBehavior.FAIL_FAST,
                false,
                false,
                false,
                false,
                1);

        assertThrows(UnsupportedOperationException.class, () -> inv.goals().add("test"));
        assertThrows(
                UnsupportedOperationException.class, () -> inv.userProperties().put("new", "val"));
        assertThrows(
                UnsupportedOperationException.class, () -> inv.activeProfiles().add("p2"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> inv.selectedProjects().add(":m2"));
        assertThrows(UnsupportedOperationException.class, () -> inv.systemInfo().put("k", "v"));
    }

    @Test
    void reactorFailureBehaviorValues() {
        MavenExecutionRequest request = newRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_FAST);
        assertEquals(
                ReactorFailureBehavior.FAIL_FAST,
                DefaultBuildInvocation.from(session).reactorFailureBehavior());

        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);
        assertEquals(
                ReactorFailureBehavior.FAIL_AT_END,
                DefaultBuildInvocation.from(session).reactorFailureBehavior());

        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_NEVER);
        assertEquals(
                ReactorFailureBehavior.FAIL_NEVER,
                DefaultBuildInvocation.from(session).reactorFailureBehavior());
    }

    @Test
    void unrecognizedReactorFailureBehaviorDefaultsToFailFast() {
        MavenExecutionRequest request = newRequest();
        request.setReactorFailureBehavior("UNKNOWN_VALUE");
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertEquals(ReactorFailureBehavior.FAIL_FAST, inv.reactorFailureBehavior());
    }

    @Test
    void threadsDefault() {
        MavenExecutionRequest request = newRequest();
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertEquals(1, inv.threads());
    }

    @Test
    void threadsExplicit() {
        MavenExecutionRequest request = newRequest();
        request.setDegreeOfConcurrency(4);
        MavenSession session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);

        BuildInvocation inv = DefaultBuildInvocation.from(session);

        assertEquals(4, inv.threads());
    }
}
