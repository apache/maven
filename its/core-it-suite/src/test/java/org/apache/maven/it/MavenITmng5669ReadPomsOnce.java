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
package org.apache.maven.it;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An integration test to ensure any pomfile is only read once.
 * This is confirmed by adding a Java Agent to the DefaultModelReader and output the options, including the source location
 *
 * <a href="https://issues.apache.org/jira/browse/MNG-5669">MNG-5669</a>.
 *
 */
public class MavenITmng5669ReadPomsOnce extends AbstractMavenIntegrationTestCase {

    public MavenITmng5669ReadPomsOnce() {
        super("[4.0.0-alpha-1,)");
    }

    @Test
    public void testWithoutBuildConsumer() throws Exception {
        // prepare JavaAgent
        File testDir = extractResources("/mng-5669-read-poms-once");
        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        Map<String, String> filterProperties = Collections.singletonMap(
                "${javaAgentJar}",
                verifier.getSupportArtifactPath("org.apache.maven.its", "core-it-javaagent", "2.1-SNAPSHOT", "jar"));
        verifier.filterFile(".mvn/jvm.config", ".mvn/jvm.config", null, filterProperties);

        verifier.setForkJvm(true); // pick up agent
        verifier.setAutoclean(false);
        verifier.addCliArgument("-q");
        verifier.addCliArgument("-U");
        verifier.addCliArgument("-Dmaven.consumerpom=false");
        verifier.addCliArgument("verify");
        verifier.execute();

        List<String> logTxt = verifier.loadLines("log.txt");

        // count source items
        Map<String, Long> sourceMap = logTxt.stream()
                .map(this::getSourceFromLogLine)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // find duplicates
        List<String> duplicates = sourceMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertTrue(duplicates.isEmpty(), "Duplicate items: " + String.join(System.lineSeparator(), duplicates));
    }

    @Test
    public void testWithBuildConsumer() throws Exception {
        // prepare JavaAgent
        File testDir = extractResources("/mng-5669-read-poms-once");
        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        Map<String, String> filterProperties = Collections.singletonMap(
                "${javaAgentJar}",
                verifier.getArtifactPath("org.apache.maven.its", "core-it-javaagent", "2.1-SNAPSHOT", "jar"));
        verifier.filterFile(".mvn/jvm.config", ".mvn/jvm.config", null, filterProperties);

        verifier.setLogFileName("log-bc.txt");
        verifier.setForkJvm(true); // pick up agent
        verifier.setAutoclean(false);
        verifier.addCliArgument("-q");
        verifier.addCliArgument("-U");
        verifier.addCliArgument("-Dmaven.consumerpom=true");
        verifier.addCliArgument("verify");
        verifier.execute();

        List<String> logTxt = verifier.loadLines("log-bc.txt");

        // count source items
        Map<String, Long> sourceMap = logTxt.stream()
                .map(this::getSourceFromLogLine)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // find duplicates
        List<String> duplicates = sourceMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertTrue(duplicates.isEmpty(), "Duplicate items: " + String.join(System.lineSeparator(), duplicates));
    }

    private String getSourceFromLogLine(String line) {

        final String buildSourceKey = "org.apache.maven.model.building.source=";
        final int keyLength = buildSourceKey.length();
        int start = line.indexOf(buildSourceKey);
        if (start < 0) {
            return null;
        }

        int end = line.indexOf(", ", start);
        if (end < 0) {
            end = line.length() - 1; // is the }
        }

        return line.substring(start + keyLength, end);
    }
}
