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
package org.apache.maven.cling.invoker.cisupport;

import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.impl.util.Os;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CIDetectorHelperTest {
    @Test
    void smoke() throws Exception {
        assertEquals("NONE;", runner(Map.of(), List.of("CI", "WORKSPACE", "GITHUB_ACTIONS")));
    }

    @Test
    void generic() throws Exception {
        assertEquals(
                GenericCIDetector.NAME + ";", runner(Map.of("CI", "true"), List.of("WORKSPACE", "GITHUB_ACTIONS")));
    }

    @Test
    void jenkins() throws Exception {
        assertEquals(
                JenkinsCIDetector.NAME + ";",
                runner(Map.of("CI", "true", "WORKSPACE", "foobar"), List.of("GITHUB_ACTIONS")));
    }

    @Test
    void github() throws Exception {
        assertEquals(
                GithubCIDetector.NAME + ";",
                runner(Map.of("CI", "true", "GITHUB_ACTIONS", "true"), List.of("WORKSPACE")));
    }

    @Test
    void githubDebug() throws Exception {
        assertEquals(
                GithubCIDetector.NAME + "+DEBUG;",
                runner(Map.of("CI", "true", "GITHUB_ACTIONS", "true", "RUNNER_DEBUG", "1"), List.of("WORKSPACE")));
    }

    private static String runner(Map<String, String> add, Collection<String> remove) throws Exception {
        String separator = FileSystems.getDefault().getSeparator();
        String classpath = System.getProperty("java.class.path");
        String path =
                System.getProperty("java.home") + separator + "bin" + separator + (Os.IS_WINDOWS ? "java.exe" : "java");
        ProcessBuilder processBuilder =
                new ProcessBuilder(path, "-cp", classpath, CIDetectorHelperRunner.class.getName());
        processBuilder.environment().putAll(add);
        remove.forEach(k -> processBuilder.environment().remove(k));
        Process process = processBuilder.start();
        process.waitFor();
        return new String(process.getInputStream().readAllBytes());
    }
}
