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
package org.apache.maven.cling.executor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class MavenExecutorTestSupport {
    @Test
    void mvnenc(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path cwd,
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path userHome)
            throws Exception {
        String logfile = "m4.log";
        execute(
                cwd.resolve(logfile),
                List.of(mvn4ExecutorRequestBuilder()
                        .command("mvnenc")
                        .cwd(cwd)
                        .userHomeDirectory(userHome)
                        .argument("diag")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
        System.out.println(Files.readString(cwd.resolve(logfile)));
    }

    @Disabled("JUnit on Windows fails to clean up as mvn3 seems does not close log file properly")
    @Test
    void dump3(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path cwd,
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path userHome)
            throws Exception {
        String logfile = "m3.log";
        execute(
                cwd.resolve(logfile),
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(cwd)
                        .userHomeDirectory(userHome)
                        .argument("eu.maveniverse.maven.plugins:toolbox:0.5.2:gav-dump")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
        System.out.println(Files.readString(cwd.resolve(logfile)));
    }

    @Test
    void dump4(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path cwd,
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path userHome)
            throws Exception {
        String logfile = "m4.log";
        execute(
                cwd.resolve(logfile),
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(cwd)
                        .userHomeDirectory(userHome)
                        .argument("eu.maveniverse.maven.plugins:toolbox:0.5.2:gav-dump")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
        System.out.println(Files.readString(cwd.resolve(logfile)));
    }

    @Test
    void defaultFs(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        String logfile = "m4.log";
        execute(
                tempDir.resolve(logfile),
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
    }

    @Test
    void defaultFsCaptureOutput(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .stdoutConsumer(stdout)
                        .build()));
        assertTrue(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFsCaptureOutputWithForcedColor(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir)
            throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=yes")
                        .stdoutConsumer(stdout)
                        .build()));
        assertTrue(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFsCaptureOutputWithForcedOffColor(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir)
            throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=no")
                        .stdoutConsumer(stdout)
                        .build()));
        assertFalse(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void version() throws Exception {
        assertEquals(
                System.getProperty("maven4version"),
                mavenVersion(mvn4ExecutorRequestBuilder().build()));
    }

    @Disabled("JUnit on Windows fails to clean up as mvn3 seems does not close log file properly")
    @Test
    void defaultFs3x(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        String logfile = "m3.log";
        execute(
                tempDir.resolve(logfile),
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
    }

    @Test
    void defaultFs3xCaptureOutput(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .stdoutConsumer(stdout)
                        .build()));
        assertFalse(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFs3xCaptureOutputWithForcedColor(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir)
            throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=yes")
                        .stdoutConsumer(stdout)
                        .build()));
        assertTrue(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFs3xCaptureOutputWithForcedOffColor(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir)
            throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=no")
                        .stdoutConsumer(stdout)
                        .build()));
        assertFalse(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void version3x() throws Exception {
        assertEquals(
                System.getProperty("maven3version"),
                mavenVersion(mvn3ExecutorRequestBuilder().build()));
    }

    public static final String POM_STRING =
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/maven-v4_0_0.xsd">

                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.apache.maven.samples</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>

                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.junit</groupId>
                          <artifactId>junit-bom</artifactId>
                          <version>5.11.1</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>

                    <dependencies>
                      <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-api</artifactId>
                        <scope>test</scope>
                      </dependency>
                    </dependencies>

                </project>
                """;

    public static final String APP_JAVA_STRING =
            """
            package org.apache.maven.samples.sample;

            public class App {
                public static void main(String... args) {
                    System.out.println("Hello World!");
                }
            }
            """;

    protected void execute(@Nullable Path logFile, Collection<ExecutorRequest> requests) throws Exception {
        try (Executor invoker = createExecutor()) {
            for (ExecutorRequest request : requests) {
                int exitCode = invoker.execute(request);
                if (exitCode != 0) {
                    throw new FailedExecution(request, exitCode, logFile == null ? "" : Files.readString(logFile));
                }
            }
        }
    }

    protected String mavenVersion(ExecutorRequest request) throws Exception {
        try (Executor invoker = createExecutor()) {
            return invoker.mavenVersion(request);
        }
    }

    public static ExecutorRequest.Builder mvn3ExecutorRequestBuilder() {
        return ExecutorRequest.mavenBuilder(Paths.get(System.getProperty("maven3home")));
    }

    public static ExecutorRequest.Builder mvn4ExecutorRequestBuilder() {
        return ExecutorRequest.mavenBuilder(Paths.get(System.getProperty("maven4home")));
    }

    protected void layDownFiles(Path cwd) throws IOException {
        Files.createDirectory(cwd.resolve(".mvn"));
        Path pom = cwd.resolve("pom.xml").toAbsolutePath();
        Files.writeString(pom, POM_STRING);
        Path appJava = cwd.resolve("src/main/java/org/apache/maven/samples/sample/App.java");
        Files.createDirectories(appJava.getParent());
        Files.writeString(appJava, APP_JAVA_STRING);
    }

    protected static class FailedExecution extends Exception {
        private final ExecutorRequest request;
        private final int exitCode;
        private final String log;

        public FailedExecution(ExecutorRequest request, int exitCode, String log) {
            super(request.toString() + " => " + exitCode + "\n" + log);
            this.request = request;
            this.exitCode = exitCode;
            this.log = log;
        }

        public ExecutorRequest getRequest() {
            return request;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getLog() {
            return log;
        }
    }

    protected abstract Executor createExecutor();
}
