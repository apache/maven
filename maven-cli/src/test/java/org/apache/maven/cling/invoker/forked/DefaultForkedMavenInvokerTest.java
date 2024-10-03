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
package org.apache.maven.cling.invoker.forked;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.cling.invoker.ProtoLogger;
import org.apache.maven.cling.invoker.mvn.forked.DefaultForkedMavenInvoker;
import org.apache.maven.cling.invoker.mvn.forked.DefaultForkedMavenParser;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Disabled
public class DefaultForkedMavenInvokerTest {
    @Test
    void smoke(@TempDir Path tempDir) throws Exception {
        try (DefaultForkedMavenInvoker invoker = new DefaultForkedMavenInvoker()) {
            Files.createDirectory(tempDir.resolve(".mvn"));
            Path log = tempDir.resolve("build.log").toAbsolutePath();
            int exitcode = invoker.invoke(new DefaultForkedMavenParser()
                    .parse(
                            "mvn",
                            new String[] {"-l", log.toString(), "clean"},
                            new ProtoLogger(),
                            new JLineMessageBuilderFactory()));
            System.out.println("exit code: " + exitcode);
            System.out.println("log:");
            System.out.println(Files.readString(log));
        }
    }
}
