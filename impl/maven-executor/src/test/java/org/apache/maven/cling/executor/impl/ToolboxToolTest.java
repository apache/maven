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

import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.ExecutorHelper.Mode;
import org.apache.maven.cling.executor.internal.HelperImpl;
import org.apache.maven.cling.executor.internal.ToolboxTool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static java.lang.System.getProperty;
import static org.apache.maven.cling.executor.MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR;
import static org.apache.maven.cling.executor.MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR;
import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn3ExecutorRequestBuilder;
import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn4ExecutorRequestBuilder;
import static org.apache.maven.cling.executor.MimirInfuser.infuse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.MAC;

@Timeout(15)
public class ToolboxToolTest {
    public static final String MAVEN_3_VERSION = "maven3version";
    public static final String MAVEN_4_VERSION = "maven4version";

    @TempDir
    private static Path userHome;

    @BeforeAll
    static void beforeAll() throws Exception {
        infuse(userHome);
    }

    @DisabledOnOs(
            value = MAC,
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    @ParameterizedTest
    @EnumSource(Mode.class)
    void dump3(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        assertEquals(
                getProperty(MAVEN_3_VERSION),
                new ToolboxTool(helper).dump(helper.executorRequest()).get("maven.version"));
    }

    @DisabledOnOs(
            value = MAC,
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    @ParameterizedTest
    @EnumSource(Mode.class)
    void dump4(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        assertEquals(
                getProperty(MAVEN_4_VERSION),
                new ToolboxTool(helper).dump(helper.executorRequest()).get("maven.version"));
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void version3(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        assertEquals(getProperty(MAVEN_3_VERSION), helper.mavenVersion());
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void version4(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        assertEquals(getProperty(MAVEN_4_VERSION), helper.mavenVersion());
    }

    @DisabledOnOs(
            value = MAC,
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    @ParameterizedTest
    @EnumSource(Mode.class)
    void localRepository3(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        assertTrue(Files.isDirectory(Paths.get(new ToolboxTool(helper).localRepository(helper.executorRequest()))));
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    @Disabled("disable temporarily so that we can get the debug statement")
    void localRepository4(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        assertTrue(Files.isDirectory(Paths.get(new ToolboxTool(helper).localRepository(helper.executorRequest()))));
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void artifactPath3(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        String path = new ToolboxTool(helper)
                .artifactPath(helper.executorRequest(), "aopalliance:aopalliance:1.0", "central");
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(
                path.endsWith("aopalliance" + File.separator + "aopalliance" + File.separator + "1.0" + File.separator
                        + "aopalliance-1.0.jar"),
                "path=" + path);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void artifactPath4(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        String path = new ToolboxTool(helper)
                .artifactPath(helper.executorRequest(), "aopalliance:aopalliance:1.0", "central");
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(
                path.endsWith("aopalliance" + File.separator + "aopalliance" + File.separator + "1.0" + File.separator
                        + "aopalliance-1.0.jar"),
                "path=" + path);
    }

    @DisabledOnOs(
            value = MAC,
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    @EnumSource(Mode.class)
    @ParameterizedTest
    void metadataPath3(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        String path = new ToolboxTool(helper).metadataPath(helper.executorRequest(), "aopalliance", "someremote");
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(path.endsWith("aopalliance" + File.separator + "maven-metadata-someremote.xml"), "path=" + path);
    }

    @DisabledOnOs(MAC)
    @ParameterizedTest
    @EnumSource(Mode.class)
    void metadataPath4(Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                EMBEDDED_MAVEN_EXECUTOR,
                FORKED_MAVEN_EXECUTOR);
        String path = new ToolboxTool(helper).metadataPath(helper.executorRequest(), "aopalliance", "someremote");
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(path.endsWith("aopalliance" + File.separator + "maven-metadata-someremote.xml"), "path=" + path);
    }
}
