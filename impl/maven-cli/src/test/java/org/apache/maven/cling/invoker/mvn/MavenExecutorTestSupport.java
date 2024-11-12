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
import java.util.Collection;
import java.util.List;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cling.invoker.ProtoLogger;
import org.apache.maven.jline.JLineMessageBuilderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class MavenExecutorTestSupport {

    protected void execute(Path cwd, Collection<String> goals) throws Exception {
        Files.createDirectory(cwd.resolve(".mvn"));
        Path pom = cwd.resolve("pom.xml").toAbsolutePath();
        Files.writeString(pom, MavenTestSupport.POM_STRING);
        Path appJava = cwd.resolve("src/main/java/org/apache/maven/samples/sample/App.java");
        Files.createDirectories(appJava.getParent());
        Files.writeString(appJava, MavenTestSupport.APP_JAVA_STRING);

        Parser parser = createParser();
        try (Executor invoker = createExecutor()) {
            for (String goal : goals) {
                Path logFile = cwd.resolve(goal + "-build.log").toAbsolutePath();
                int exitCode = invoker.execute(parser.parseExecution(ParserRequest.mvn(
                                List.of("-l", logFile.toString(), goal),
                                new ProtoLogger(),
                                new JLineMessageBuilderFactory())
                        .cwd(cwd)
                        .build()));
                String log = Files.readString(logFile);
                System.out.println(log);
                assertEquals(0, exitCode, log);
            }
        }
    }

    protected abstract Executor createExecutor();

    protected abstract Parser createParser();
}
