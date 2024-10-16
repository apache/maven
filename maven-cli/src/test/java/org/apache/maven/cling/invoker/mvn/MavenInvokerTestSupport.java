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
package org.apache.maven.cling.invoker.mvn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.ProtoLogger;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class MavenInvokerTestSupport<O extends MavenOptions, R extends MavenInvokerRequest<O>> {

    protected void invoke(Path cwd, Collection<String> goals) throws Exception {
        // works only in recent Maven4
        Assumptions.assumeTrue(
                Files.isRegularFile(Paths.get(System.getProperty("maven.home"))
                        .resolve("conf")
                        .resolve("maven.properties")),
                "${maven.home}/conf/maven.properties must be a file");

        Files.createDirectory(cwd.resolve(".mvn"));

        String pomString =
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
        Path pom = cwd.resolve("pom.xml").toAbsolutePath();
        Files.writeString(pom, pomString);

        String appJavaString =
                """
                package org.apache.maven.samples.sample;

                public class App {
                    public static void main(String... args) {
                        System.out.println("Hello World!");
                    }
                }
                """;
        Path appJava = cwd.resolve("src/main/java/org/apache/maven/samples/sample/App.java");
        Files.createDirectories(appJava.getParent());
        Files.writeString(appJava, appJavaString);

        Parser<R> parser = createParser();
        try (Invoker<R> invoker = createInvoker()) {
            for (String goal : goals) {
                Path logFile = cwd.resolve(goal + "-build.log").toAbsolutePath();
                int exitCode = invoker.invoke(parser.parse(ParserRequest.mvn(
                                List.of("-l", logFile.toString(), goal),
                                new ProtoLogger(),
                                new JLineMessageBuilderFactory())
                        .cwd(cwd)
                        .build()));
                String log = Files.readString(logFile);
                assertEquals(0, exitCode, log);
            }
        }
    }

    protected abstract Invoker<R> createInvoker();

    protected abstract Parser<R> createParser();
}
