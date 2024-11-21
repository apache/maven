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
package org.apache.maven.cling.executor.forked;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.cling.executor.MavenExecutorTestSupport;
import org.apache.maven.cling.executor.embedded.EmbeddedMavenExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Forked UT: it cannot use jimFS as it runs in child process.
 */
public class ForkedMavenExecutorTest extends MavenExecutorTestSupport {

    @Override
    protected Executor createExecutor() {
        return new EmbeddedMavenExecutor();
    }

    @Test
    void defaultFs(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        String logfile = "forked4.log";
        execute(
                tempDir.resolve(logfile),
                List.of(mvn4ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("verify")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
    }

    @Test
    void defaultFs3x(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
        layDownFiles(tempDir);
        String logfile = "forked3.log";
        execute(
                tempDir.resolve(logfile),
                List.of(mvn3ExecutorRequestBuilder()
                        .cwd(tempDir)
                        .argument("verify")
                        .argument("-l")
                        .argument(logfile)
                        .build()));
    }
}