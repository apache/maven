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

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.codehaus.plexus.classworlds.ClassWorld;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class MavenInvokerTestSupport {
    static {
        System.setProperty(
                "library.jline.path",
                Path.of("target/dependency/org/jline/nativ").toAbsolutePath().toString());
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

    protected Map<String, String> invoke(Path cwd, Path userHome, Collection<String> goals, Collection<String> args)
            throws Exception {
        Files.createDirectories(cwd.resolve(".mvn"));
        Path pom = cwd.resolve("pom.xml").toAbsolutePath();
        Files.writeString(pom, POM_STRING);
        Path appJava = cwd.resolve("src/main/java/org/apache/maven/samples/sample/App.java");
        Files.createDirectories(appJava.getParent());
        Files.writeString(appJava, APP_JAVA_STRING);

        MimirInfuser.infuse(userHome);

        HashMap<String, String> logs = new HashMap<>();
        Parser parser = createParser();
        try (ClassWorld classWorld = createClassWorld();
                Invoker invoker = createInvoker(classWorld)) {
            for (String goal : goals) {
                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                ByteArrayOutputStream stderr = new ByteArrayOutputStream();
                List<String> mvnArgs = new ArrayList<>(args);
                mvnArgs.add(goal);
                int exitCode = invoker.invoke(
                        parser.parseInvocation(ParserRequest.mvn(mvnArgs, new JLineMessageBuilderFactory())
                                .cwd(cwd)
                                .userHome(userHome)
                                .stdOut(stdout)
                                .stdErr(stderr)
                                .embedded(true)
                                .build()));

                // dump things out
                System.out.println("===================================================");
                System.out.println("args: " + Arrays.toString(mvnArgs.toArray()));
                System.out.println("===================================================");
                System.out.println("stdout: " + stdout);
                System.out.println("===================================================");

                System.err.println("===================================================");
                System.err.println("args: " + Arrays.toString(mvnArgs.toArray()));
                System.err.println("===================================================");
                System.err.println("stderr: " + stderr);
                System.err.println("===================================================");

                logs.put(goal, stdout.toString());
                assertEquals(0, exitCode, "OUT:" + stdout + "\nERR:" + stderr);
            }
        }
        return logs;
    }

    protected ClassWorld createClassWorld() {
        return new ClassWorld("plexus.core", ClassLoader.getSystemClassLoader());
    }

    protected abstract Invoker createInvoker(ClassWorld classWorld);

    protected abstract Parser createParser();
}
