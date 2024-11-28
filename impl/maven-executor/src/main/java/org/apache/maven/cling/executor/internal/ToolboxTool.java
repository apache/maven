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

import java.io.ByteArrayOutputStream;

import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.ExecutorHelper;
import org.apache.maven.cling.executor.ExecutorTool;

import static java.util.Objects.requireNonNull;

/**
 * {@link ExecutorTool} implementation based on Maveniverse Toolbox.
 *
 * @see <a href="https://github.com/maveniverse/toolbox">Maveniverse Toolbox</a>
 */
public class ToolboxTool implements ExecutorTool {
    private static final String TOOLBOX = "eu.maveniverse.maven.plugins:toolbox:0.5.1:";

    private final ExecutorHelper helper;

    public ToolboxTool(ExecutorHelper helper) {
        this.helper = requireNonNull(helper);
    }

    @Override
    public String localRepository(ExecutorRequest.Builder executorRequest) throws ExecutorException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ExecutorRequest.Builder builder =
                mojo(executorRequest, "gav-local-repository-path").stdoutConsumer(stdout);
        int ec = helper.execute(builder.build());
        if (ec != 0) {
            throw new ExecutorException("Unexpected exit code=" + ec);
        }
        return shaveStdout(stdout);
    }

    @Override
    public String artifactPath(ExecutorRequest.Builder executorRequest, String gav, String repositoryId)
            throws ExecutorException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ExecutorRequest.Builder builder = mojo(executorRequest, "gav-artifact-path")
                .argument("-Dgav=" + gav)
                .stdoutConsumer(stdout);
        if (repositoryId != null) {
            builder.argument("-Drepository=" + repositoryId + "::unimportant");
        }
        int ec = helper.execute(builder.build());
        if (ec != 0) {
            throw new ExecutorException("Unexpected exit code=" + ec);
        }
        return shaveStdout(stdout);
    }

    @Override
    public String metadataPath(ExecutorRequest.Builder executorRequest, String gav, String repositoryId)
            throws ExecutorException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ExecutorRequest.Builder builder = mojo(executorRequest, "gav-metadata-path")
                .argument("-Dgav=" + gav)
                .stdoutConsumer(stdout);
        if (repositoryId != null) {
            builder.argument("-Drepository=" + repositoryId + "::unimportant");
        }
        int ec = helper.execute(builder.build());
        if (ec != 0) {
            throw new ExecutorException("Unexpected exit code=" + ec);
        }
        return shaveStdout(stdout);
    }

    private ExecutorRequest.Builder mojo(ExecutorRequest.Builder builder, String mojo) {
        return builder.argument(TOOLBOX + mojo).argument("--quiet").argument("-DforceStdout");
    }

    private String shaveStdout(ByteArrayOutputStream stdout) {
        return stdout.toString().replace("\n", "").replace("\r", "");
    }
}
