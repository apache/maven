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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.extensions.CoreExtension;

import static java.util.Objects.requireNonNull;

public abstract class BaseInvokerRequest implements InvokerRequest {
    private final ParserRequest parserRequest;
    private final Path cwd;
    private final Path installationDirectory;
    private final Path userHomeDirectory;
    private final Map<String, String> userProperties;
    private final Map<String, String> systemProperties;
    private final Path topDirectory;
    private final Path rootDirectory;

    private final InputStream in;
    private final OutputStream out;
    private final OutputStream err;
    private final List<CoreExtension> coreExtensions;
    private final List<String> jvmArguments;

    @SuppressWarnings("ParameterNumber")
    public BaseInvokerRequest(
            @Nonnull ParserRequest parserRequest,
            @Nonnull Path cwd,
            @Nonnull Path installationDirectory,
            @Nonnull Path userHomeDirectory,
            @Nonnull Map<String, String> userProperties,
            @Nonnull Map<String, String> systemProperties,
            @Nonnull Path topDirectory,
            @Nullable Path rootDirectory,
            @Nullable InputStream in,
            @Nullable OutputStream out,
            @Nullable OutputStream err,
            @Nullable List<CoreExtension> coreExtensions,
            @Nullable List<String> jvmArguments) {
        this.parserRequest = requireNonNull(parserRequest);
        this.cwd = requireNonNull(cwd);
        this.installationDirectory = requireNonNull(installationDirectory);
        this.userHomeDirectory = requireNonNull(userHomeDirectory);
        this.userProperties = requireNonNull(userProperties);
        this.systemProperties = requireNonNull(systemProperties);
        this.topDirectory = requireNonNull(topDirectory);
        this.rootDirectory = rootDirectory;

        this.in = in;
        this.out = out;
        this.err = err;
        this.coreExtensions = coreExtensions;
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
    public Map<String, String> userProperties() {
        return userProperties;
    }

    @Override
    public Map<String, String> systemProperties() {
        return systemProperties;
    }

    @Override
    public Path topDirectory() {
        return topDirectory;
    }

    @Override
    public Optional<Path> rootDirectory() {
        return Optional.ofNullable(rootDirectory);
    }

    @Override
    public Optional<InputStream> in() {
        return Optional.ofNullable(in);
    }

    @Override
    public Optional<OutputStream> out() {
        return Optional.ofNullable(out);
    }

    @Override
    public Optional<OutputStream> err() {
        return Optional.ofNullable(err);
    }

    @Override
    public Optional<List<CoreExtension>> coreExtensions() {
        return Optional.ofNullable(coreExtensions);
    }

    @Override
    public Optional<List<String>> jvmArguments() {
        return Optional.ofNullable(jvmArguments);
    }
}
