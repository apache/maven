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
import java.util.Map;

import org.apache.maven.impl.util.Os;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CIDetectorHelperTest {
    @Test
    void smoke() throws Exception {
        assertEquals("NONE\n", runner(Map.of()));
    }

    @Test
    void generic() throws Exception {
        assertEquals(GenericCIDetector.NAME + "\n", runner(Map.of("CI", "true")));
    }

    @Test
    void jenkins() throws Exception {
        assertEquals(JenkinsCIDetector.NAME + "\n", runner(Map.of("CI", "true", "WORKSPACE", "foobar")));
    }

    @Test
    void github() throws Exception {
        assertEquals(GithubCIDetector.NAME + "\n", runner(Map.of("CI", "true", "GITHUB_ACTIONS", "true")));
    }

    @Test
    void githubDebug() throws Exception {
        assertEquals(
                GithubCIDetector.NAME + "+DEBUG\n",
                runner(Map.of("CI", "true", "GITHUB_ACTIONS", "true", "RUNNER_DEBUG", "1")));
    }

    private static String runner(Map<String, String> env) throws Exception {
        String separator = FileSystems.getDefault().getSeparator();
        String classpath = System.getProperty("java.class.path");
        String path =
                System.getProperty("java.home") + separator + "bin" + separator + (Os.IS_WINDOWS ? "java.exe" : "java");
        ProcessBuilder processBuilder =
                new ProcessBuilder(path, "-cp", classpath, CIDetectorHelperRunner.class.getName());
        processBuilder.environment().putAll(env);
        Process process = processBuilder.start();
        process.waitFor();
        return new String(process.getInputStream().readAllBytes());
    }
}
