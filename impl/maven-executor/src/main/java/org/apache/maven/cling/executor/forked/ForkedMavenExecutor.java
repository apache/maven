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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.api.cli.ExecutorRequest.getCanonicalPath;

/**
 * Forked executor implementation, that spawns a subprocess with Maven from the installation directory. Very costly
 * but provides the best isolation.
 */
public class ForkedMavenExecutor implements Executor {
    protected final boolean useMavenArgsEnv;

    public ForkedMavenExecutor() {
        this(true);
    }

    public ForkedMavenExecutor(boolean useMavenArgsEnv) {
        this.useMavenArgsEnv = useMavenArgsEnv;
    }

    @Override
    public int execute(ExecutorRequest executorRequest) throws ExecutorException {
        requireNonNull(executorRequest);
        validate(executorRequest);

        return doExecute(executorRequest);
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
                        .stdOut(stdout)
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

    protected int doExecute(ExecutorRequest executorRequest) throws ExecutorException {
        ArrayList<String> cmdAndArguments = new ArrayList<>();
        cmdAndArguments.add(executorRequest
                .installationDirectory()
                .resolve("bin")
                .resolve(IS_WINDOWS ? executorRequest.command() + ".cmd" : executorRequest.command())
                .toString());

        String mavenArgsEnv = System.getenv("MAVEN_ARGS");
        if (useMavenArgsEnv && mavenArgsEnv != null && !mavenArgsEnv.isEmpty()) {
            Arrays.stream(mavenArgsEnv.split(" "))
                    .filter(s -> !s.trim().isEmpty())
                    .forEach(cmdAndArguments::add);
        }

        cmdAndArguments.addAll(executorRequest.arguments());

        ArrayList<String> jvmArgs = new ArrayList<>();
        if (!executorRequest.userHomeDirectory().equals(getCanonicalPath(Path.of(System.getProperty("user.home"))))) {
            jvmArgs.add("-Duser.home=" + executorRequest.userHomeDirectory().toString());
        }
        if (executorRequest.jvmArguments().isPresent()) {
            jvmArgs.addAll(executorRequest.jvmArguments().get());
        }
        if (executorRequest.jvmSystemProperties().isPresent()) {
            jvmArgs.addAll(executorRequest.jvmSystemProperties().get().entrySet().stream()
                    .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                    .toList());
        }

        HashMap<String, String> env = new HashMap<>();
        if (executorRequest.environmentVariables().isPresent()) {
            env.putAll(executorRequest.environmentVariables().get());
        }
        if (!jvmArgs.isEmpty()) {
            String mavenOpts = env.getOrDefault("MAVEN_OPTS", "");
            if (!mavenOpts.isEmpty()) {
                mavenOpts += " ";
            }
            mavenOpts += String.join(" ", jvmArgs);
            env.put("MAVEN_OPTS", mavenOpts);
        }
        env.remove("MAVEN_ARGS"); // we already used it if configured to do so

        try {
            ProcessBuilder pb = new ProcessBuilder()
                    .directory(executorRequest.cwd().toFile())
                    .command(cmdAndArguments);
            if (!env.isEmpty()) {
                pb.environment().putAll(env);
            }

            Process process = pb.start();
            pump(process, executorRequest).await();
            return process.waitFor();
        } catch (IOException e) {
            throw new ExecutorException("IO problem while executing command: " + cmdAndArguments, e);
        } catch (InterruptedException e) {
            throw new ExecutorException("Interrupted while executing command: " + cmdAndArguments, e);
        }
    }

    protected CountDownLatch pump(Process p, ExecutorRequest executorRequest) {
        CountDownLatch latch = new CountDownLatch(3);
        String suffix = "-pump-" + p.pid();
        Thread stdoutPump = new Thread(() -> {
            try {
                OutputStream stdout = executorRequest.stdOut().orElse(OutputStream.nullOutputStream());
                p.getInputStream().transferTo(stdout);
                stdout.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                latch.countDown();
            }
        });
        stdoutPump.setName("stdout" + suffix);
        stdoutPump.start();
        Thread stderrPump = new Thread(() -> {
            try {
                OutputStream stderr = executorRequest.stdErr().orElse(OutputStream.nullOutputStream());
                p.getErrorStream().transferTo(stderr);
                stderr.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                latch.countDown();
            }
        });
        stderrPump.setName("stderr" + suffix);
        stderrPump.start();
        Thread stdinPump = new Thread(() -> {
            try {
                OutputStream in = p.getOutputStream();
                executorRequest.stdIn().orElse(InputStream.nullInputStream()).transferTo(in);
                in.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                latch.countDown();
            }
        });
        stdinPump.setName("stdin" + suffix);
        stdinPump.start();
        return latch;
    }
}
