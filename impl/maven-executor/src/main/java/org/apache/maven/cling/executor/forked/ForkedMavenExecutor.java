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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.api.annotations.Nullable;
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

        return doExecute(
                executorRequest,
                wrapStdoutConsumer(executorRequest.stdoutConsumer().orElse(null)));
    }

    @Override
    public String mavenVersion(ExecutorRequest executorRequest) throws ExecutorException {
        requireNonNull(executorRequest);
        validate(executorRequest);
        try {
            Path cwd = Files.createTempDirectory("forked-executor-maven-version");
            try {
                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                int exitCode = execute(executorRequest.toBuilder()
                        .cwd(cwd)
                        .arguments(List.of("--version", "--quiet"))
                        .stdoutConsumer(stdout)
                        .build());
                if (exitCode == 0) {
                    if (stdout.size() > 0) {
                        return stdout.toString()
                                .replace("\n", "")
                                .replace("\r", "")
                                .trim();
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

    @Nullable
    protected Consumer<Process> wrapStdoutConsumer(@Nullable OutputStream stdoutConsumer) {
        if (stdoutConsumer == null) {
            return null;
        } else {
            return p -> {
                try {
                    p.getInputStream().transferTo(stdoutConsumer);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }
    }

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
            if (executorRequest.environmentVariables().isPresent()) {
                pb.environment().putAll(executorRequest.environmentVariables().get());
            }
            if (executorRequest.jvmArguments().isPresent()) {
                if (pb.environment().containsKey("MAVEN_OPTS")) {
                    String mavenOpts = pb.environment().get("MAVEN_OPTS");
                    mavenOpts += " "
                            + String.join(" ", executorRequest.jvmArguments().get());
                    pb.environment().put("MAVEN_OPTS", mavenOpts);
                } else {
                    pb.environment()
                            .put(
                                    "MAVEN_OPTS",
                                    String.join(
                                            " ", executorRequest.jvmArguments().get()));
                }
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
