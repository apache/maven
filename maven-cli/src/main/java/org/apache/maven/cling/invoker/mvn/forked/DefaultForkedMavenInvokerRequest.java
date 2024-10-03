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
package org.apache.maven.cling.invoker.mvn.forked;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.cli.Logger;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.cli.mvn.forked.ForkedMavenInvokerRequest;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.invoker.mvn.DefaultMavenInvokerRequest;

/**
 * Maven execution request.
 */
public class DefaultForkedMavenInvokerRequest extends DefaultMavenInvokerRequest<MavenOptions>
        implements ForkedMavenInvokerRequest {
    private final List<String> jvmArguments;

    @SuppressWarnings("ParameterNumber")
    public DefaultForkedMavenInvokerRequest(
            String command,
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
            OutputStream out,
            OutputStream err,
            List<CoreExtension> coreExtensions,
            List<String> jvmArguments,
            MavenOptions options) {
        super(
                command,
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
                coreExtensions,
                options);
        this.jvmArguments = jvmArguments;
    }

    @Override
    public Optional<List<String>> jvmArguments() {
        return Optional.ofNullable(jvmArguments);
    }
}
