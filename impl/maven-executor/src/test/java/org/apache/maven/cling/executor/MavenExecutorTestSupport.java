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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.embedded.EmbeddedMavenExecutor;
import org.apache.maven.cling.executor.forked.ForkedMavenExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.MAC;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.junit.jupiter.api.io.CleanupMode.ALWAYS;

@Timeout(15)
public abstract class MavenExecutorTestSupport {

    @Test
    void mvnenc(@TempDir(cleanup = ALWAYS) Path cwd, @TempDir(cleanup = ALWAYS) Path userHome) throws Exception {
        execute(
                cwd.resolve(M4_LOG),
                List.of(mvn4ExecutorRequestBuilder()
                        .command("mvnenc")
                        .cwd(cwd)
                        .userHomeDirectory(userHome)
                        .argument("diag")
                        .argument("-l")
                        .argument(M4_LOG)
                        .build()));
        assertFalse(readString(cwd.resolve(M4_LOG)).isBlank());
    }

    @DisabledOnOs(
            value = {WINDOWS, MAC},
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    @Test
    void dump3(@TempDir(cleanup = ALWAYS) Path cwd, @TempDir(cleanup = ALWAYS) Path userHome) throws Exception {
        execute(
                cwd.resolve(M3_LOG),
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(cwd)
                        .userHomeDirectory(userHome)
                        .argument("eu.maveniverse.maven.plugins:toolbox:0.7.4:gav-dump")
                        .argument("-l")
                        .argument(M3_LOG)
                        .build()));
        assertFalse(readString(cwd.resolve(M3_LOG)).isBlank());
    }

    @Test
    @DisabledOnOs(
            value = MAC,
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    void dump4(@TempDir(cleanup = ALWAYS) Path cwd, @TempDir(cleanup = ALWAYS) Path userHome) throws Exception {
        execute(
                cwd.resolve(M4_LOG),
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(cwd)
                        .userHomeDirectory(userHome)
                        .argument("eu.maveniverse.maven.plugins:toolbox:0.7.4:gav-dump")
                        .argument("-l")
                        .argument(M4_LOG)
                        .build()));
        assertFalse(readString(cwd.resolve(M4_LOG)).isBlank());
    }

    @Test
    void defaultFs(@TempDir(cleanup = ALWAYS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        execute(
                tempDir.resolve(M4_LOG),
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("-l")
                        .argument(M4_LOG)
                        .build()));
        assertFalse(readString(tempDir.resolve(M4_LOG)).contains(M4_LOG));
    }

    @Test
    void version() throws Exception {
        assertEquals(
                System.getProperty("maven4version"),
                mavenVersion(mvn4ExecutorRequestBuilder().build()));
    }

    @DisabledOnOs(
            value = {WINDOWS, MAC},
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    @Test
    void defaultFs3x(@TempDir(cleanup = ALWAYS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        execute(
                tempDir.resolve(M3_LOG),
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("-l")
                        .argument(M3_LOG)
                        .build()));
        assertFalse(readString(tempDir.resolve(M3_LOG)).contains(M3_LOG));
    }

    @Test
    void version3x() throws Exception {
        assertEquals(
                System.getProperty("maven3version"),
                mavenVersion(mvn3ExecutorRequestBuilder().build()));
    }

    @Test
    void defaultFsCaptureOutput(@TempDir(cleanup = ALWAYS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .stdOut(stdout)
                        .build()));
        assertNoColorCodeANSI(stdout);
        assertInfo(stdout);
    }

    @Test
    void defaultFsCaptureOutputWithForcedColor(@TempDir(cleanup = ALWAYS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=yes")
                        .stdOut(stdout)
                        .build()));
        assertColorCodeANSI(stdout);
        assertInfo(stdout);
    }

    @Test
    void defaultFsCaptureOutputWithForcedOffColor(@TempDir(cleanup = ALWAYS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=no")
                        .stdOut(stdout)
                        .build()));
        assertNoColorCodeANSI(stdout);
        assertInfo(stdout);
    }

    @Test
    @DisabledOnOs(
            value = MAC,
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    void defaultFs3xCaptureOutput(@TempDir(cleanup = ALWAYS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .stdOut(stdout)
                        .build()));
        // Note: we do not validate ANSI, as Maven3 is wierd in this respect (thinks is color but is not)
        // assertANSIColor(stdout);
        assertInfo(stdout);
    }

    @Test
    @DisabledOnOs(
            value = MAC,
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    void defaultFs3xCaptureOutputWithForcedColor(@TempDir(cleanup = ALWAYS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=yes")
                        .stdOut(stdout)
                        .build()));
        assertColorCodeANSI(stdout);
        assertInfo(stdout);
    }

    @Test
    @DisabledOnOs(
            value = MAC,
            disabledReason = "mvn3 fails to close log file properly, therefore JUnit fails to clean up as well.")
    void defaultFs3xCaptureOutputWithForcedOffColor(@TempDir(cleanup = ALWAYS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        execute(
                null,
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("-V")
                        .argument("verify")
                        .argument("--color=no")
                        .stdOut(stdout)
                        .build()));
        assertNoColorCodeANSI(stdout);
        assertInfo(stdout);
    }

    private static void assertColorCodeANSI(ByteArrayOutputStream stdout) {
        assertTrue(stdout.toString().contains("[\u001B["));
    }

    private static void assertNoColorCodeANSI(ByteArrayOutputStream stdout) {
        assertFalse(stdout.toString().contains("[\u001B["));
    }

    private static void assertInfo(ByteArrayOutputStream stdout) {
        assertTrue(stdout.toString().contains("INFO"), "No INFO found");
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
        for (ExecutorRequest request : requests) {
            MimirInfuser.infuse(request.userHomeDirectory());
            int exitCode = createAndMemoizeExecutor().execute(request);
            if (exitCode != 0) {
                throw new FailedExecution(request, exitCode, logFile == null ? "" : readString(logFile));
            }
        }
    }

    protected String mavenVersion(ExecutorRequest request) throws Exception {
        return createAndMemoizeExecutor().mavenVersion(request);
    }

    public static ExecutorRequest.Builder mvn3ExecutorRequestBuilder() {
        return ExecutorRequest.mavenBuilder(Paths.get(System.getProperty("maven3home")));
    }

    public static ExecutorRequest.Builder mvn4ExecutorRequestBuilder() {
        return ExecutorRequest.mavenBuilder(Paths.get(System.getProperty("maven4home")));
    }

    protected void layDownFiles(Path cwd) throws IOException {
        createDirectory(cwd.resolve(".mvn"));
        writeString(cwd.resolve("pom.xml").toAbsolutePath(), POM_STRING);
        Path appJava = cwd.resolve("src/main/java/org/apache/maven/samples/sample/App.java");
        createDirectories(appJava.getParent());
        writeString(appJava, APP_JAVA_STRING);
    }

    protected static class FailedExecution extends Exception {
        public FailedExecution(ExecutorRequest request, int exitCode, String log) {
            super(request.toString() + " => " + exitCode + "\n" + log);
        }
    }

    protected final Executor createAndMemoizeExecutor() {
        if (executor == null) {
            executor = doSelectExecutor();
        }
        return executor;
    }

    @AfterAll
    static void afterAll() {
        if (executor != null) {
            executor = null;
        }
    }

    // NOTE: we keep these instances alive to make sure JVM (running tests) loads JAnsi/JLine native library ONLY once
    // in real life you'd anyway keep these alive as long needed, but here, we repeat a series of tests against the same
    // instance, to prevent them attempting native load more than once.
    public static final EmbeddedMavenExecutor EMBEDDED_MAVEN_EXECUTOR = new EmbeddedMavenExecutor();
    public static final ForkedMavenExecutor FORKED_MAVEN_EXECUTOR = new ForkedMavenExecutor();
    private static final String M3_LOG = "m3.log";
    private static final String M4_LOG = "m4.log";
    private static Executor executor;

    protected abstract Executor doSelectExecutor();
}
