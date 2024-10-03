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
package org.apache.maven.cling.invoker.mvn.resident;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cling.invoker.ProtoLogger;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DefaultResidentMavenInvokerTest {
    @Test
    void smoke(@TempDir Path tempDir) throws Exception {
        // works only in recent Maven4
        Assumptions.assumeTrue(Files.isRegularFile(
                Paths.get(System.getProperty("maven.home")).resolve("conf").resolve("maven.properties")));
        try (ClassWorld classWorld = new ClassWorld("plexus.core", ClassLoader.getSystemClassLoader());
                DefaultResidentMavenInvoker invoker = new DefaultResidentMavenInvoker(ProtoLookup.builder()
                        .addMapping(ClassWorld.class, classWorld)
                        .build())) {
            DefaultResidentMavenParser parser = new DefaultResidentMavenParser();
            Files.createDirectory(tempDir.resolve(".mvn"));
            Path log = tempDir.resolve("build.log").toAbsolutePath();

            String pomString =
                    """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/maven-v4_0_0.xsd">

                            <modelVersion>4.0.0</modelVersion>

                            <groupId>org.apache.maven.samples</groupId>
                            <artifactId>resident-invoker</artifactId>
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
                                <dependency>
                                  <groupId>org.junit.jupiter</groupId>
                                  <artifactId>junit-jupiter-api</artifactId>
                                  <scope>test</scope>
                                </dependency>
                              </dependency>
                            </dependencies>

                        </project>
                        """;
            Path pom = tempDir.resolve("pom.xml").toAbsolutePath();
            Files.writeString(pom, pomString);

            int exitCode;

            exitCode = invoker.invoke(parser.parse(ParserRequest.builder(
                            "mvn",
                            new String[] {"-l", log.toString(), "clean"},
                            new ProtoLogger(),
                            new JLineMessageBuilderFactory())
                    .cwd(tempDir)
                    .build()));
            System.out.println("1st exit code: " + exitCode);
            System.out.println("log:");
            System.out.println(Files.readString(log));

            exitCode = invoker.invoke(parser.parse(ParserRequest.builder(
                            "mvn",
                            new String[] {"-l", log.toString(), "clean"},
                            new ProtoLogger(),
                            new JLineMessageBuilderFactory())
                    .cwd(tempDir)
                    .build()));
            System.out.println("2nd exit code: " + exitCode);
            System.out.println("log:");
            System.out.println(Files.readString(log));
        }
    }
}
