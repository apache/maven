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
package org.apache.maven.cling.invoker;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.api.cli.ParserRequest;

import static java.util.Objects.requireNonNull;

public class BaseExecutorRequest implements ExecutorRequest {
    private final ParserRequest parserRequest;
    private final Path cwd;
    private final Path installationDirectory;
    private final Path userHomeDirectory;
    private final List<String> jvmArguments;

    @SuppressWarnings("ParameterNumber")
    public BaseExecutorRequest(
            @Nonnull ParserRequest parserRequest,
            @Nonnull Path cwd,
            @Nonnull Path installationDirectory,
            @Nonnull Path userHomeDirectory,
            @Nullable List<String> jvmArguments) {
        this.parserRequest = requireNonNull(parserRequest);
        this.cwd = requireNonNull(cwd);
        this.installationDirectory = requireNonNull(installationDirectory);
        this.userHomeDirectory = requireNonNull(userHomeDirectory);
        this.jvmArguments = jvmArguments;
    }

    @Override
    public ParserRequest parserRequest() {
        return parserRequest;
    }

    @Override
    public Path cwd() {
        return cwd;
    }

    @Override
    public Path installationDirectory() {
        return installationDirectory;
    }

    @Override
    public Path userHomeDirectory() {
        return userHomeDirectory;
    }

    @Override
    public Optional<List<String>> jvmArguments() {
        return Optional.ofNullable(jvmArguments);
    }
}
