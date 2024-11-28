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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.Executor;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.ExecutorTool;
import org.apache.maven.cling.executor.embedded.EmbeddedMavenExecutor;
import org.apache.maven.cling.executor.forked.ForkedMavenExecutor;

/**
 * Helper class for some common tasks.
 */
public class HelperImpl implements ExecutorHelper {
    private final Path installationDirectory;
    private final ExecutorTool executorTool;
    private final HashMap<String, Executor> executors;

    private final ConcurrentHashMap<String, String> cache;

    public HelperImpl(@Nullable Path installationDirectory) {
        this.installationDirectory = installationDirectory != null
                ? ExecutorRequest.getCanonicalPath(installationDirectory)
                : ExecutorRequest.discoverMavenHome();
        this.executorTool = new ToolboxTool(this);
        this.executors = new HashMap<>();

        this.executors.put(EmbeddedMavenExecutor.class.getSimpleName(), new EmbeddedMavenExecutor());
        this.executors.put(ForkedMavenExecutor.class.getSimpleName(), new ForkedMavenExecutor());
        this.cache = new ConcurrentHashMap<>();
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
    public void close() throws ExecutorException {
        ArrayList<Exception> exceptions = new ArrayList<>();
        for (Executor executor : executors.values()) {
            try {
                executor.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            ExecutorException e = new ExecutorException("Could not cleanly close");
            exceptions.forEach(e::addSuppressed);
            throw e;
        }
    }

    @Override
    public String mavenVersion() {
        return cache.computeIfAbsent("maven.version", k -> {
            ExecutorRequest request = executorRequest().build();
            return getExecutor(Mode.AUTO, request).mavenVersion(request);
        });
    }

    @Override
    public String localRepository(ExecutorRequest.Builder request) throws ExecutorException {
        if (mavenVersion().startsWith("5.")) {
            request.argument("--raw-streams");
        }
        return executorTool.localRepository(request);
    }

    @Override
    public String artifactPath(ExecutorRequest.Builder request, String gav, String repositoryId)
            throws ExecutorException {
        if (mavenVersion().startsWith("5.")) {
            request.argument("--raw-streams");
        }
        return executorTool.artifactPath(request, gav, repositoryId);
    }

    @Override
    public String metadataPath(ExecutorRequest.Builder request, String gav, String repositoryId)
            throws ExecutorException {
        if (mavenVersion().startsWith("5.")) {
            request.argument("--raw-streams");
        }
        return executorTool.metadataPath(request, gav, repositoryId);
    }

    protected Executor getExecutor(Mode mode, ExecutorRequest request) throws ExecutorException {
        return switch (mode) {
            case AUTO -> getExecutorByRequest(request);
            case EMBEDDED -> executors.get(EmbeddedMavenExecutor.class.getSimpleName());
            case FORKED -> executors.get(ForkedMavenExecutor.class.getSimpleName());
        };
    }

    private Executor getExecutorByRequest(ExecutorRequest request) {
        if (request.environmentVariables().isEmpty() && request.jvmArguments().isEmpty()) {
            return getExecutor(Mode.EMBEDDED, request);
        } else {
            return getExecutor(Mode.FORKED, request);
        }
    }
}
