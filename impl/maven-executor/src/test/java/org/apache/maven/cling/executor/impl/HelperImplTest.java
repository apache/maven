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

import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.MavenExecutorTestSupport;
import org.apache.maven.cling.executor.internal.HelperImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn3ExecutorRequestBuilder;
import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn4ExecutorRequestBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelperImplTest {
    @TempDir
    private static Path userHome;

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void dump3(ExecutorHelper.Mode mode) throws Exception {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        Map<String, String> dump = helper.dump(helper.executorRequest());
        assertEquals(System.getProperty("maven3version"), dump.get("maven.version"));
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void dump4(ExecutorHelper.Mode mode) throws Exception {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        Map<String, String> dump = helper.dump(helper.executorRequest());
        assertEquals(System.getProperty("maven4version"), dump.get("maven.version"));
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void version3(ExecutorHelper.Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        assertEquals(System.getProperty("maven3version"), helper.mavenVersion());
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void version4(ExecutorHelper.Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        assertEquals(System.getProperty("maven4version"), helper.mavenVersion());
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void localRepository3(ExecutorHelper.Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        String localRepository = helper.localRepository(helper.executorRequest());
        Path local = Paths.get(localRepository);
        assertTrue(Files.isDirectory(local));
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    @Disabled("disable temporarily so that we can get the debug statement")
    void localRepository4(ExecutorHelper.Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        String localRepository = helper.localRepository(helper.executorRequest());
        Path local = Paths.get(localRepository);
        assertTrue(Files.isDirectory(local));
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void artifactPath3(ExecutorHelper.Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        String path = helper.artifactPath(helper.executorRequest(), "aopalliance:aopalliance:1.0", "central");
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(
                path.endsWith("aopalliance" + File.separator + "aopalliance" + File.separator + "1.0" + File.separator
                        + "aopalliance-1.0.jar"),
                "path=" + path);
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void artifactPath4(ExecutorHelper.Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        String path = helper.artifactPath(helper.executorRequest(), "aopalliance:aopalliance:1.0", "central");
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(
                path.endsWith("aopalliance" + File.separator + "aopalliance" + File.separator + "1.0" + File.separator
                        + "aopalliance-1.0.jar"),
                "path=" + path);
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void metadataPath3(ExecutorHelper.Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn3ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        String path = helper.metadataPath(helper.executorRequest(), "aopalliance", "someremote");
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(path.endsWith("aopalliance" + File.separator + "maven-metadata-someremote.xml"), "path=" + path);
    }

    @Timeout(15)
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void metadataPath4(ExecutorHelper.Mode mode) {
        ExecutorHelper helper = new HelperImpl(
                mode,
                mvn4ExecutorRequestBuilder().build().installationDirectory(),
                userHome,
                MavenExecutorTestSupport.EMBEDDED_MAVEN_EXECUTOR,
                MavenExecutorTestSupport.FORKED_MAVEN_EXECUTOR);
        String path = helper.metadataPath(helper.executorRequest(), "aopalliance", "someremote");
        // split repository: assert "ends with" as split may introduce prefixes
        assertTrue(path.endsWith("aopalliance" + File.separator + "maven-metadata-someremote.xml"), "path=" + path);
    }
}
