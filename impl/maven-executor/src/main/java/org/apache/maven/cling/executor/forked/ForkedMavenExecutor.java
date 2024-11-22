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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;

import static java.util.Objects.requireNonNull;

/**
 * Forked executor implementation, that spawns a subprocess with Maven from the installation directory. Very costly
 * but provides the best isolation.
 */
public class ForkedMavenExecutor implements Executor {
    @Override
    public int execute(ExecutorRequest executorRequest) throws ExecutorException {
        requireNonNull(executorRequest);
        validate(executorRequest);

        return doExecute(executorRequest, null);
    }

    @Override
    public String mavenVersion(ExecutorRequest executorRequest) throws ExecutorException {
        requireNonNull(executorRequest);
        validate(executorRequest);
        try {
            Path cwd = Files.createTempDirectory("forked-executor-maven-version");
            try {
                ArrayList<String> stdout = new ArrayList<>();
                int exitCode = doExecute(
                        executorRequest.toBuilder()
                                .cwd(cwd)
                                .arguments(List.of("--version", "--color", "never"))
                                .build(),
                        p -> {
                            String line;
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                                while ((line = br.readLine()) != null) {
                                    stdout.add(line);
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                if (exitCode == 0) {
                    for (String line : stdout) {
                        if (line.startsWith("Apache Maven ")) {
                            return line.substring(13, line.indexOf("(") - 1);
                        }
                    }
                    return UNKNOWN_VERSION;
                } else {
                    throw new ExecutorException(
                            "Maven version query unexpected exitCode=" + exitCode + "\nLog: " + stdout);
                }
            } finally {
                Files.deleteIfExists(cwd);
            }
        } catch (IOException e) {
            throw new ExecutorException("Failed to determine maven version", e);
        }
    }

    protected void validate(ExecutorRequest executorRequest) throws ExecutorException {}

    protected int doExecute(ExecutorRequest executorRequest, Consumer<Process> processConsumer)
            throws ExecutorException {
        ArrayList<String> cmdAndArguments = new ArrayList<>();
        cmdAndArguments.add(executorRequest
                .installationDirectory()
                .resolve("bin")
                .resolve(IS_WINDOWS ? executorRequest.command() + ".cmd" : executorRequest.command())
                .toString());

        cmdAndArguments.addAll(executorRequest.arguments());

        try {
            ProcessBuilder pb = new ProcessBuilder()
                    .directory(executorRequest.cwd().toFile())
                    .command(cmdAndArguments);

            if (executorRequest.jvmArguments().isPresent()) {
                pb.environment()
                        .put(
                                "MAVEN_OPTS",
                                String.join(" ", executorRequest.jvmArguments().get()));
            }

            Process process = pb.start();
            if (processConsumer != null) {
                processConsumer.accept(process);
            }
            return process.waitFor();
        } catch (IOException e) {
            throw new ExecutorException("IO problem while executing command: " + cmdAndArguments, e);
        } catch (InterruptedException e) {
            throw new ExecutorException("Interrupted while executing command: " + cmdAndArguments, e);
        }
    }
}
