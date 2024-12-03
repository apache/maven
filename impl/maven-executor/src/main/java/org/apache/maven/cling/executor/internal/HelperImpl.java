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
package org.apache.maven.cling.executor.internal;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.ExecutorTool;

import static java.util.Objects.requireNonNull;

/**
 * Helper class for some common tasks. This class statically holds instances of (stateless) embedded and forked
 * executors. The goal is to keep embedded alive as needed, as it keeps "hot" Maven.
 */
public class HelperImpl implements ExecutorHelper {
    private final Mode defaultMode;
    private final Path installationDirectory;
    private final ExecutorTool executorTool;
    private final HashMap<Mode, Executor> executors;

    private final ConcurrentHashMap<String, String> cache;

    public HelperImpl(Mode defaultMode, @Nullable Path installationDirectory, Executor embedded, Executor forked) {
        this.defaultMode = requireNonNull(defaultMode);
        this.installationDirectory = installationDirectory != null
                ? ExecutorRequest.getCanonicalPath(installationDirectory)
                : ExecutorRequest.discoverMavenHome();
        this.executorTool = new ToolboxTool(this);
        this.executors = new HashMap<>();

        this.executors.put(Mode.EMBEDDED, requireNonNull(embedded, "embedded"));
        this.executors.put(Mode.FORKED, requireNonNull(forked, "forked"));
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public Mode getDefaultMode() {
        return defaultMode;
    }

    @Override
    public ExecutorRequest.Builder executorRequest() {
        return ExecutorRequest.mavenBuilder(installationDirectory);
    }

    @Override
    public int execute(Mode mode, ExecutorRequest executorRequest) throws ExecutorException {
        return getExecutor(mode, executorRequest).execute(executorRequest);
    }

    @Override
    public String mavenVersion() {
        return cache.computeIfAbsent("maven.version", k -> {
            ExecutorRequest request = executorRequest().build();
            return getExecutor(Mode.AUTO, request).mavenVersion(request);
        });
    }

    @Override
    public Map<String, String> dump(ExecutorRequest.Builder request) throws ExecutorException {
        return executorTool.dump(request);
    }

    @Override
    public String localRepository(ExecutorRequest.Builder request) throws ExecutorException {
        return executorTool.localRepository(request);
    }

    @Override
    public String artifactPath(ExecutorRequest.Builder request, String gav, String repositoryId)
            throws ExecutorException {
        return executorTool.artifactPath(request, gav, repositoryId);
    }

    @Override
    public String metadataPath(ExecutorRequest.Builder request, String gav, String repositoryId)
            throws ExecutorException {
        return executorTool.metadataPath(request, gav, repositoryId);
    }

    protected Executor getExecutor(Mode mode, ExecutorRequest request) throws ExecutorException {
        return switch (mode) {
            case AUTO -> getExecutorByRequest(request);
            case EMBEDDED -> executors.get(Mode.EMBEDDED);
            case FORKED -> executors.get(Mode.FORKED);
        };
    }

    private Executor getExecutorByRequest(ExecutorRequest request) {
        if (Objects.equals(request.command(), ExecutorRequest.MVN)
                && request.environmentVariables().orElse(Collections.emptyMap()).isEmpty()
                && request.jvmArguments().orElse(Collections.emptyList()).isEmpty()) {
            return getExecutor(Mode.EMBEDDED, request);
        } else {
            return getExecutor(Mode.FORKED, request);
        }
    }
}
