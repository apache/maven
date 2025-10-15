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
package org.apache.maven.cling.executor.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import eu.maveniverse.maven.mimir.testing.MimirInfuser;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.embedded.EmbeddedMavenExecutor;
import org.apache.maven.cling.executor.forked.ForkedMavenExecutor;
import org.apache.maven.cling.executor.internal.HelperImpl;
import org.apache.maven.cling.executor.internal.ToolboxTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(60)
public class ToolboxToolTest {
    private static final Executor EMBEDDED_MAVEN_EXECUTOR = new EmbeddedMavenExecutor();
    private static final Executor FORKED_MAVEN_EXECUTOR = new ForkedMavenExecutor();

    public static final String TOOLBOX_VERSION = System.getProperty("version.toolbox");

    @TempDir(cleanup = CleanupMode.NEVER)
    private static Path tempDir;

    private Path userHome;

    @BeforeEach
    void beforeEach(TestInfo testInfo) throws Exception {
        userHome = tempDir.resolve(testInfo.getTestMethod().orElseThrow().getName());
        Files.createDirectories(userHome);
        MimirInfuser.infuseUW(userHome);

        System.out.println("=== " + testInfo.getTestMethod().orElseThrow().getName());
    }

    private ExecutorRequest.Builder getExecutorRequest(ExecutorHelper helper) {
        ExecutorRequest.Builder builder = helper.executorRequest();
        if (System.getProperty("localRepository") != null) {
            builder.argument("-Dmaven.repo.local.tail=" + System.getProperty("localRepository"));
        }
        return builder;
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void dump3(ExecutorHelper.Mode mode) throws Exception {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn3Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        Map<String, String> dump = new ToolboxTool(helper, TOOLBOX_VERSION).dump(getExecutorRequest(helper));
        System.out.println(mode.name() + ": " + dump.toString());
        assertEquals(System.getProperty("maven3version"), dump.get("maven.version"));
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void dump4(ExecutorHelper.Mode mode) throws Exception {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn4Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        Map<String, String> dump = new ToolboxTool(helper, TOOLBOX_VERSION).dump(getExecutorRequest(helper));
        System.out.println(mode.name() + ": " + dump.toString());
        assertEquals(System.getProperty("maven4version"), dump.get("maven.version"));
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void version3(ExecutorHelper.Mode mode) {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn3Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        System.out.println(mode.name() + ": " + helper.mavenVersion());
        assertEquals(System.getProperty("maven3version"), helper.mavenVersion());
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void version4(ExecutorHelper.Mode mode) {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn4Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        System.out.println(mode.name() + ": " + helper.mavenVersion());
        assertEquals(System.getProperty("maven4version"), helper.mavenVersion());
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void localRepository3(ExecutorHelper.Mode mode) {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn3Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        String localRepository = new ToolboxTool(helper, TOOLBOX_VERSION).localRepository(getExecutorRequest(helper));
        System.out.println(mode.name() + ": " + localRepository);
        Path local = Paths.get(localRepository);
        assertTrue(Files.isDirectory(local));
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void localRepository4(ExecutorHelper.Mode mode) {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn4Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        String localRepository = new ToolboxTool(helper, TOOLBOX_VERSION).localRepository(getExecutorRequest(helper));
        System.out.println(mode.name() + ": " + localRepository);
        Path local = Paths.get(localRepository);
        assertTrue(Files.isDirectory(local));
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void artifactPath3(ExecutorHelper.Mode mode) {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn3Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        String path = new ToolboxTool(helper, TOOLBOX_VERSION)
                .artifactPath(getExecutorRequest(helper), "aopalliance:aopalliance:1.0", "central");
        System.out.println(mode.name() + ": " + path);
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(
                path.endsWith("aopalliance" + File.separator + "aopalliance" + File.separator + "1.0" + File.separator
                        + "aopalliance-1.0.jar"),
                "path=" + path);
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void artifactPath4(ExecutorHelper.Mode mode) {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn4Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        String path = new ToolboxTool(helper, TOOLBOX_VERSION)
                .artifactPath(getExecutorRequest(helper), "aopalliance:aopalliance:1.0", "central");
        System.out.println(mode.name() + ": " + path);
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(
                path.endsWith("aopalliance" + File.separator + "aopalliance" + File.separator + "1.0" + File.separator
                        + "aopalliance-1.0.jar"),
                "path=" + path);
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void metadataPath3(ExecutorHelper.Mode mode) {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn4Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        String path = new ToolboxTool(helper, TOOLBOX_VERSION)
                .metadataPath(getExecutorRequest(helper), "aopalliance", "someremote");
        System.out.println(mode.name() + ": " + path);
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(path.endsWith("aopalliance" + File.separator + "maven-metadata-someremote.xml"), "path=" + path);
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void metadataPath4(ExecutorHelper.Mode mode) {
        ExecutorHelper helper =
                new HelperImpl(mode, mvn4Home(), userHome, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);
        String path = new ToolboxTool(helper, TOOLBOX_VERSION)
                .metadataPath(getExecutorRequest(helper), "aopalliance", "someremote");
        System.out.println(mode.name() + ": " + path);
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(path.endsWith("aopalliance" + File.separator + "maven-metadata-someremote.xml"), "path=" + path);
    }

    public Path mvn3Home() {
        return Paths.get(System.getProperty("maven3home"));
    }

    public Path mvn4Home() {
        return Paths.get(System.getProperty("maven4home"));
    }
}
