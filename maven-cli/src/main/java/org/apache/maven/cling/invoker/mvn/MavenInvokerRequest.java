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
package org.apache.maven.cling.invoker.mvn;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cling.invoker.BaseInvokerRequest;
import org.slf4j.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Maven execution request.
 */
public class MavenInvokerRequest extends BaseInvokerRequest {
    private final MavenOptions options;

    @SuppressWarnings("ParameterNumber")
    public MavenInvokerRequest(
            Path cwd,
            Path installationDirectory,
            Path userHomeDirectory,
            Map<String, String> userProperties,
            Map<String, String> systemProperties,
            Logger logger,
            MessageBuilderFactory messageBuilderFactory,
            Path topDirectory,
            Path rootDirectory,
            InputStream in,
            PrintStream out,
            PrintStream err,
            List<CoreExtension> coreExtensions,
            MavenOptions options) {
        super(
                cwd,
                installationDirectory,
                userHomeDirectory,
                userProperties,
                systemProperties,
                logger,
                messageBuilderFactory,
                topDirectory,
                rootDirectory,
                in,
                out,
                err,
                coreExtensions);
        this.options = requireNonNull(options);
    }

    /**
     * The mandatory Maven options.
     */
    @Nonnull
    public MavenOptions options() {
        return options;
    }
}
