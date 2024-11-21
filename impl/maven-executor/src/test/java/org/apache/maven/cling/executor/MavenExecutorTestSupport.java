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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorRequest;

public abstract class MavenExecutorTestSupport {
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

    protected void execute(Path logFile, Collection<ExecutorRequest> requests) throws Exception {
        try (Executor invoker = createExecutor()) {
            for (ExecutorRequest request : requests) {
                int exitCode = invoker.execute(request);
                if (exitCode != 0) {
                    throw new FailedExecution(request, exitCode, Files.readString(logFile));
                }
            }
        }
    }

    protected ExecutorRequest.Builder mvn3ExecutorRequestBuilder() {
        return ExecutorRequest.mavenBuilder(Paths.get(System.getProperty("maven3home")));
    }

    protected ExecutorRequest.Builder mvn4ExecutorRequestBuilder() {
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
