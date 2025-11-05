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

import eu.maveniverse.maven.mimir.testing.MimirInfuser;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

@Timeout(60)
public abstract class MavenExecutorTestSupport {
    @TempDir(cleanup = CleanupMode.NEVER)
    private static Path tempDir;

    private Path cwd;

    private Path userHome;

    @BeforeEach
    void beforeEach(TestInfo testInfo) throws Exception {
        cwd = tempDir.resolve(testInfo.getTestMethod().orElseThrow().getName()).resolve("cwd");
        Files.createDirectories(cwd.resolve(".mvn"));
        userHome = tempDir.resolve(testInfo.getTestMethod().orElseThrow().getName())
                .resolve("home");
        Files.createDirectories(userHome);

        System.out.println("=== " + testInfo.getTestMethod().orElseThrow().getName());
    }

    private static Executor executor;

    protected final Executor createAndMemoizeExecutor() {
        if (executor == null) {
            executor = doSelectExecutor();
        }
        return executor;
    }

    @AfterAll
    static void afterAll() {
        if (executor != null) {
            executor.close();
            executor = null;
        }
    }

    protected abstract Executor doSelectExecutor();

    @Test
    void mvnenc4() throws Exception {
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

    @DisabledOnOs(
            value = WINDOWS,
            disabledReason = "JUnit on Windows fails to clean up as mvn3 does not close log file properly")
    @Test
    void dump3() throws Exception {
        String logfile = "m3.log";
        execute(
                cwd.resolve(logfile),
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(cwd)
                        .userHomeDirectory(userHome)
                        .argument("eu.maveniverse.maven.plugins:toolbox:" + Environment.TOOLBOX_VERSION + ":gav-dump")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
        System.out.println(Files.readString(cwd.resolve(logfile)));
    }

    @Test
    void dump4() throws Exception {
        String logfile = "m4.log";
        execute(
                cwd.resolve(logfile),
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(cwd)
                        .userHomeDirectory(userHome)
                        .argument("eu.maveniverse.maven.plugins:toolbox:" + Environment.TOOLBOX_VERSION + ":gav-dump")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
        System.out.println(Files.readString(cwd.resolve(logfile)));
    }

    @DisabledOnOs(
            value = WINDOWS,
            disabledReason = "JUnit on Windows fails to clean up as mvn3 does not close log file properly")
    @Test
    void defaultFs3() throws Exception {
        layDownFiles(cwd);
        String logfile = "m3.log";
        execute(
                cwd.resolve(logfile),
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(cwd)
                        .argument("-V")
                        .argument("verify")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
        System.out.println(Files.readString(cwd.resolve(logfile)));
    }

    @Test
    void defaultFs4() throws Exception {
        layDownFiles(cwd);
        String logfile = "m4.log";
        execute(
                cwd.resolve(logfile),
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(cwd)
                        .argument("-V")
                        .argument("verify")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
        System.out.println(Files.readString(cwd.resolve(logfile)));
    }

    @Test
    void version3() throws Exception {
        assertEquals(
                System.getProperty("maven3version"),
                mavenVersion(mvn3ExecutorRequestBuilder().build()));
    }

    @Test
    void version4() throws Exception {
        assertEquals(
                System.getProperty("maven4version"),
                mavenVersion(mvn4ExecutorRequestBuilder().build()));
    }

    @Test
    void defaultFs4CaptureOutput() throws Exception {
        layDownFiles(cwd);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(cwd)
                        .argument("-V")
                        .argument("verify")
                        .stdOut(stdout)
                        .build()));
        System.out.println(stdout);
        assertFalse(stdout.toString().contains("[\u001B["), "By default no ANSI color codes");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFs4CaptureOutputWithForcedColor() throws Exception {
        layDownFiles(cwd);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(cwd)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=yes")
                        .stdOut(stdout)
                        .build()));
        System.out.println(stdout);
        assertTrue(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFs4CaptureOutputWithForcedOffColor() throws Exception {
        layDownFiles(cwd);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(cwd)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=no")
                        .stdOut(stdout)
                        .build()));
        System.out.println(stdout);
        assertFalse(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFs3CaptureOutput() throws Exception {
        layDownFiles(cwd);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(cwd)
                        .argument("-V")
                        .argument("verify")
                        .stdOut(stdout)
                        .build()));
        System.out.println(stdout);
        // Note: we do not validate ANSI as Maven3 is weird in this respect (thinks is color but is not)
        // assertTrue(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFs3CaptureOutputWithForcedColor() throws Exception {
        layDownFiles(cwd);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(cwd)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=yes")
                        .stdOut(stdout)
                        .build()));
        System.out.println(stdout);
        assertTrue(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    @Test
    void defaultFs3CaptureOutputWithForcedOffColor() throws Exception {
        layDownFiles(cwd);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(cwd)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=no")
                        .stdOut(stdout)
                        .build()));
        System.out.println(stdout);
        assertFalse(stdout.toString().contains("[\u001B["), "No ANSI codes present");
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
    }

    public static final String POM_STRING = """
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

    public static final String APP_JAVA_STRING = """
            package org.apache.maven.samples.sample;

            public class App {
                public static void main(String... args) {
                    System.out.println("Hello World!");
                }
            }
            """;

    protected void execute(@Nullable Path logFile, Collection<ExecutorRequest> requests) throws Exception {
        Executor invoker = createAndMemoizeExecutor();
        for (ExecutorRequest request : requests) {
            if (MimirInfuser.isMimirPresentUW()) {
                if (maven3Home().equals(request.installationDirectory())) {
                    MimirInfuser.doInfusePW(request.cwd(), request.userHomeDirectory());
                } else if (maven4Home().equals(request.installationDirectory())) {
                    MimirInfuser.doInfuseUW(request.userHomeDirectory());
                }
                MimirInfuser.preseedItselfIntoInnerUserHome(request.userHomeDirectory());
            }
            int exitCode = invoker.execute(request);
            if (exitCode != 0) {
                throw new FailedExecution(request, exitCode, logFile == null ? "" : Files.readString(logFile));
            }
        }
    }

    protected String mavenVersion(ExecutorRequest request) throws Exception {
        return createAndMemoizeExecutor().mavenVersion(request);
    }

    public ExecutorRequest.Builder mvn3ExecutorRequestBuilder() {
        return customize(ExecutorRequest.mavenBuilder(maven3Home()));
    }

    private Path maven3Home() {
        return ExecutorRequest.getCanonicalPath(Paths.get(System.getProperty("maven3home")));
    }

    public ExecutorRequest.Builder mvn4ExecutorRequestBuilder() {
        return customize(ExecutorRequest.mavenBuilder(maven4Home()));
    }

    private Path maven4Home() {
        return ExecutorRequest.getCanonicalPath(Paths.get(System.getProperty("maven4home")));
    }

    private ExecutorRequest.Builder customize(ExecutorRequest.Builder builder) {
        builder =
                builder.cwd(cwd).userHomeDirectory(userHome).argument("-Daether.remoteRepositoryFilter.prefixes=false");
        if (System.getProperty("localRepository") != null) {
            builder.argument("-Dmaven.repo.local.tail=" + System.getProperty("localRepository"));
        }
        return builder;
    }

    protected void layDownFiles(Path cwd) throws IOException {
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
}
