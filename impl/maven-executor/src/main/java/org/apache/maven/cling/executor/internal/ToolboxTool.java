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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.ExecutorTool;

import static java.util.Objects.requireNonNull;

/**
 * {@link ExecutorTool} implementation based on Maveniverse Toolbox. It uses Toolbox mojos to implement all the
 * required operations.
 *
 * @see <a href="https://github.com/maveniverse/toolbox">Maveniverse Toolbox</a>
 */
public class ToolboxTool implements ExecutorTool {
    private static final String TOOLBOX = "eu.maveniverse.maven.plugins:toolbox:0.5.2:";

    private final ExecutorHelper helper;

    public ToolboxTool(ExecutorHelper helper) {
        this.helper = requireNonNull(helper);
    }

    @Override
    public Map<String, String> dump(ExecutorRequest.Builder executorRequest) throws ExecutorException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ExecutorRequest.Builder builder = mojo(executorRequest, "gav-dump")
                .argument("-DasProperties")
                .stdoutConsumer(stdout)
                .stderrConsumer(stderr);
        doExecute(builder);
        try {
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(stdout.toByteArray()));
            return properties.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> String.valueOf(e.getValue()),
                            (prev, next) -> next,
                            HashMap::new));
        } catch (IOException e) {
            throw new ExecutorException("Unable to parse properties", e);
        }
    }

    @Override
    public String localRepository(ExecutorRequest.Builder executorRequest) throws ExecutorException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ExecutorRequest.Builder builder = mojo(executorRequest, "gav-local-repository-path")
                .stdoutConsumer(stdout)
                .stderrConsumer(stderr);
        doExecute(builder);
        return shaveStdout(stdout);
    }

    @Override
    public String artifactPath(ExecutorRequest.Builder executorRequest, String gav, String repositoryId)
            throws ExecutorException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ExecutorRequest.Builder builder = mojo(executorRequest, "gav-artifact-path")
                .argument("-Dgav=" + gav)
                .stdoutConsumer(stdout)
                .stderrConsumer(stderr);
        if (repositoryId != null) {
            builder.argument("-Drepository=" + repositoryId + "::unimportant");
        }
        doExecute(builder);
        return shaveStdout(stdout);
    }

    @Override
    public String metadataPath(ExecutorRequest.Builder executorRequest, String gav, String repositoryId)
            throws ExecutorException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ExecutorRequest.Builder builder = mojo(executorRequest, "gav-metadata-path")
                .argument("-Dgav=" + gav)
                .stdoutConsumer(stdout)
                .stderrConsumer(stderr);
        if (repositoryId != null) {
            builder.argument("-Drepository=" + repositoryId + "::unimportant");
        }
        doExecute(builder);
        return shaveStdout(stdout);
    }

    private ExecutorRequest.Builder mojo(ExecutorRequest.Builder builder, String mojo) {
        if (helper.mavenVersion().startsWith("4.")) {
            builder.argument("--raw-streams");
        }
        return builder.argument(TOOLBOX + mojo).argument("--quiet").argument("-DforceStdout");
    }

    private void doExecute(ExecutorRequest.Builder builder) {
        ExecutorRequest request = builder.build();
        int ec = helper.execute(request);
        if (ec != 0) {
            throw new ExecutorException("Unexpected exit code=" + ec + "; stdout="
                    + request.stdoutConsumer().orElse(null) + "; stderr="
                    + request.stderrConsumer().orElse(null));
        }
    }

    private String shaveStdout(ByteArrayOutputStream stdout) {
        return stdout.toString().replace("\n", "").replace("\r", "");
    }
}
