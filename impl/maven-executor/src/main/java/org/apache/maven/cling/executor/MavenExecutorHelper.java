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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.forked.ForkedMavenExecutor;

import static java.util.Objects.requireNonNull;

/**
 * Helper class for some common tasks.
 */
public class MavenExecutorHelper {
    public enum Mode {
        AUTO,
        EMBEDDED,
        FORKED
    }

    private final Path installationDirectory;

    public MavenExecutorHelper(Path installationDirectory) {
        this.installationDirectory = requireNonNull(installationDirectory);
    }

    /**
     * Returns the location of local repository, as detected by Maven. The {@code userSettings} param may contain
     * and override (equivalent of {@code -s settings.xml} on CLI).
     */
    public Path localRepository(@Nullable Path userSettings) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try (Executor ex = new ForkedMavenExecutor()) {
            ex.execute(ExecutorRequest.mavenBuilder(installationDirectory)
                    .argument("help:evaluate")
                    .argument("-Dexpression=settings.localRepository")
                    .argument("-DforceStdout=true")
                    .argument("--quiet")
                    .stdoutConsumer(stdout)
                    .build());
        }
        return Paths.get(stdout.toString().replace("\n", "").replace("\r", ""));
    }
}
