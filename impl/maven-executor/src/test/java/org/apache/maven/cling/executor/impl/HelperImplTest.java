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
import org.apache.maven.cling.executor.internal.HelperImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn3ExecutorRequestBuilder;
import static org.apache.maven.cling.executor.MavenExecutorTestSupport.mvn4ExecutorRequestBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelperImplTest {
    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void dump3(ExecutorHelper.Mode mode) throws Exception {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn3ExecutorRequestBuilder().build().installationDirectory())) {
            Map<String, String> dump = helper.dump(helper.executorRequest());
            assertEquals(System.getProperty("maven3version"), dump.get("maven.version"));
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void dump4(ExecutorHelper.Mode mode) throws Exception {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn4ExecutorRequestBuilder().build().installationDirectory())) {
            Map<String, String> dump = helper.dump(helper.executorRequest());
            assertEquals(System.getProperty("maven4version"), dump.get("maven.version"));
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void version3(ExecutorHelper.Mode mode) {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn3ExecutorRequestBuilder().build().installationDirectory())) {
            assertEquals(System.getProperty("maven3version"), helper.mavenVersion());
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void version4(ExecutorHelper.Mode mode) {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn4ExecutorRequestBuilder().build().installationDirectory())) {
            assertEquals(System.getProperty("maven4version"), helper.mavenVersion());
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void localRepository3(ExecutorHelper.Mode mode) {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn3ExecutorRequestBuilder().build().installationDirectory())) {
            String localRepository = helper.localRepository(helper.executorRequest());
            Path local = Paths.get(localRepository);
            assertTrue(Files.isDirectory(local));
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void localRepository4(ExecutorHelper.Mode mode) {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn4ExecutorRequestBuilder().build().installationDirectory())) {
            String localRepository = helper.localRepository(helper.executorRequest());
            Path local = Paths.get(localRepository);
            assertTrue(Files.isDirectory(local));
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void artifactPath3(ExecutorHelper.Mode mode) {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn3ExecutorRequestBuilder().build().installationDirectory())) {
            String path = helper.artifactPath(helper.executorRequest(), "aopalliance:aopalliance:1.0", "central");
            assertEquals(
                    "aopalliance" + File.separator + "aopalliance" + File.separator + "1.0" + File.separator
                            + "aopalliance-1.0.jar",
                    path);
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void artifactPath4(ExecutorHelper.Mode mode) {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn4ExecutorRequestBuilder().build().installationDirectory())) {
            String path = helper.artifactPath(helper.executorRequest(), "aopalliance:aopalliance:1.0", "central");
            assertEquals(
                    "aopalliance" + File.separator + "aopalliance" + File.separator + "1.0" + File.separator
                            + "aopalliance-1.0.jar",
                    path);
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void metadataPath3(ExecutorHelper.Mode mode) {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn3ExecutorRequestBuilder().build().installationDirectory())) {
            String path = helper.metadataPath(helper.executorRequest(), "aopalliance", "someremote");
            assertEquals("aopalliance" + File.separator + "maven-metadata-someremote.xml", path);
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorHelper.Mode.class)
    void metadataPath4(ExecutorHelper.Mode mode) {
        try (ExecutorHelper helper =
                new HelperImpl(mode, mvn4ExecutorRequestBuilder().build().installationDirectory())) {
            String path = helper.metadataPath(helper.executorRequest(), "aopalliance", "someremote");
            assertEquals("aopalliance" + File.separator + "maven-metadata-someremote.xml", path);
        }
    }
}
