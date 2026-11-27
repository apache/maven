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
import java.util.List;
import java.util.Properties;

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.MakeBehavior;
import org.apache.maven.api.ReactorFailureBehavior;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.build.DefaultBuildInvocation;
import org.apache.maven.internal.impl.DefaultSession;
import org.eclipse.aether.DefaultRepositorySystemSession;
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

/**
 * Tests {@link DefaultBuildInvocation} construction from a real {@link MavenExecutionRequest}
 * via {@link DefaultSession#buildInvocation()}.
 */
class DefaultBuildInvocationTest {

    private static MavenExecutionRequest newRequest() {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepositoryPath(new File("/tmp/test-repo"));
        return request;
    }

    private static BuildInvocation invocationFrom(MavenExecutionRequest request) {
        var rss = new DefaultRepositorySystemSession(h -> false);
        var ms = new MavenSession(null, rss, request, null);
        var session =
                new DefaultSession(ms, mock(org.eclipse.aether.RepositorySystem.class), List.of(), null, null, null);
        return session.buildInvocation();
    }

    @Test
    void fromBasicRequest() {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(List.of("clean", "install"));
        request.setOffline(true);
        request.setUpdateSnapshots(true);
        request.setInteractiveMode(false);
        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);

        BuildInvocation inv = invocationFrom(request);

        assertEquals(List.of("clean", "install"), inv.goals());
        assertTrue(inv.offline());
        assertTrue(inv.updateSnapshots());
        assertTrue(inv.batchMode());
        assertEquals(ReactorFailureBehavior.FAIL_AT_END, inv.reactorFailureBehavior());
        assertTrue(inv.recursive());
        assertFalse(inv.resume());
        assertNull(inv.makeBehavior());
        assertTrue(inv.excludedProjects().isEmpty());
    }

    @Test
    void fromNullGoals() {
        BuildInvocation inv = invocationFrom(newRequest());

        assertNotNull(inv.goals());
        assertTrue(inv.goals().isEmpty());
    }

    @Test
    void fromNullActiveProfiles() {
        BuildInvocation inv = invocationFrom(newRequest());

        assertNotNull(inv.activeProfiles());
        assertTrue(inv.activeProfiles().isEmpty());
    }

    @Test
    void fromNullSelectedProjects() {
        BuildInvocation inv = invocationFrom(newRequest());

        assertNotNull(inv.selectedProjects());
        assertTrue(inv.selectedProjects().isEmpty());
    }

    @Test
    void fromNullReactorFailureBehaviorDefaultsToFailFast() {
        BuildInvocation inv = invocationFrom(newRequest());

        assertEquals(ReactorFailureBehavior.FAIL_FAST, inv.reactorFailureBehavior());
    }

    @Test
    void resumeFrom() {
        MavenExecutionRequest request = newRequest();
        request.setResumeFrom(":my-module");

        BuildInvocation inv = invocationFrom(request);

        assertEquals(":my-module", inv.resumeFrom());
    }

    @Test
    void resumeFromNull() {
        BuildInvocation inv = invocationFrom(newRequest());

        assertNull(inv.resumeFrom());
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

        BuildInvocation inv = invocationFrom(request);

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
        BuildInvocation inv = invocationFrom(newRequest());

        assertNotNull(inv.systemInfo().get("os.name"));
        assertNotNull(inv.systemInfo().get("os.arch"));
        assertNotNull(inv.systemInfo().get("java.vm.version"));
        assertNotNull(inv.systemInfo().get("available.processors"));
    }

    @Test
    void collectionsAreImmutable() {
        DefaultBuildInvocation inv = new DefaultBuildInvocation(
                List.of("compile"),
                java.util.Map.of("key", "val"),
                java.util.Map.of("os.name", "Linux"),
                List.of("profile1"),
                List.of(":module1"),
                List.of(":excluded"),
                null,
                null,
                ReactorFailureBehavior.FAIL_FAST,
                false,
                false,
                false,
                true,
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
        assertThrows(
                UnsupportedOperationException.class,
                () -> inv.excludedProjects().add(":m3"));
        assertThrows(UnsupportedOperationException.class, () -> inv.systemInfo().put("k", "v"));
    }

    @Test
    void reactorFailureBehaviorValues() {
        MavenExecutionRequest request = newRequest();

        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_FAST);
        assertEquals(ReactorFailureBehavior.FAIL_FAST, invocationFrom(request).reactorFailureBehavior());

        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);
        assertEquals(ReactorFailureBehavior.FAIL_AT_END, invocationFrom(request).reactorFailureBehavior());

        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_NEVER);
        assertEquals(ReactorFailureBehavior.FAIL_NEVER, invocationFrom(request).reactorFailureBehavior());
    }

    @Test
    void unrecognizedReactorFailureBehaviorDefaultsToFailFast() {
        MavenExecutionRequest request = newRequest();
        request.setReactorFailureBehavior("UNKNOWN_VALUE");

        assertEquals(ReactorFailureBehavior.FAIL_FAST, invocationFrom(request).reactorFailureBehavior());
    }

    @Test
    void threadsDefault() {
        BuildInvocation inv = invocationFrom(newRequest());

        assertEquals(1, inv.threads());
    }

    @Test
    void threadsExplicit() {
        MavenExecutionRequest request = newRequest();
        request.setDegreeOfConcurrency(4);

        assertEquals(4, invocationFrom(request).threads());
    }

    @Test
    void excludedProjects() {
        MavenExecutionRequest request = newRequest();
        request.setExcludedProjects(List.of(":slow-module", ":flaky-module"));

        BuildInvocation inv = invocationFrom(request);

        assertEquals(2, inv.excludedProjects().size());
        assertTrue(inv.excludedProjects().contains(":slow-module"));
        assertTrue(inv.excludedProjects().contains(":flaky-module"));
    }

    @Test
    void makeBehaviorUpstream() {
        MavenExecutionRequest request = newRequest();
        request.setMakeBehavior(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        assertEquals(MakeBehavior.UPSTREAM, invocationFrom(request).makeBehavior());
    }

    @Test
    void makeBehaviorDownstream() {
        MavenExecutionRequest request = newRequest();
        request.setMakeBehavior(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);

        assertEquals(MakeBehavior.DOWNSTREAM, invocationFrom(request).makeBehavior());
    }

    @Test
    void makeBehaviorBoth() {
        MavenExecutionRequest request = newRequest();
        request.setMakeBehavior(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        assertEquals(MakeBehavior.BOTH, invocationFrom(request).makeBehavior());
    }

    @Test
    void makeBehaviorNull() {
        assertNull(invocationFrom(newRequest()).makeBehavior());
    }

    @Test
    void makeBehaviorUnknownStringReturnsNull() {
        MavenExecutionRequest request = newRequest();
        request.setMakeBehavior("invalid-value");

        assertNull(invocationFrom(request).makeBehavior());
    }

    @Test
    void nonRecursive() {
        MavenExecutionRequest request = newRequest();
        request.setRecursive(false);

        assertFalse(invocationFrom(request).recursive());
    }

    @Test
    void resume() {
        MavenExecutionRequest request = newRequest();
        request.setResume(true);

        assertTrue(invocationFrom(request).resume());
    }
}
