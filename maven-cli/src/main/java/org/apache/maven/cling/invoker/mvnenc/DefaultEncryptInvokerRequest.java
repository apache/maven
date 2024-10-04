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
package org.apache.maven.cling.invoker.mvnenc;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.cli.mvnenc.EncryptInvokerRequest;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.cling.invoker.BaseInvokerRequest;

import static java.util.Objects.requireNonNull;

public class DefaultEncryptInvokerRequest extends BaseInvokerRequest<EncryptOptions> implements EncryptInvokerRequest {
    private final EncryptOptions options;

    @SuppressWarnings("ParameterNumber")
    public DefaultEncryptInvokerRequest(
            ParserRequest parserRequest,
            Path cwd,
            Path installationDirectory,
            Path userHomeDirectory,
            Map<String, String> userProperties,
            Map<String, String> systemProperties,
            Path topDirectory,
            Path rootDirectory,
            InputStream in,
            OutputStream out,
            OutputStream err,
            List<CoreExtension> coreExtensions,
            Options options) {
        super(
                parserRequest,
                cwd,
                installationDirectory,
                userHomeDirectory,
                userProperties,
                systemProperties,
                topDirectory,
                rootDirectory,
                in,
                out,
                err,
                coreExtensions);
        this.options = (EncryptOptions) requireNonNull(options);
    }

    /**
     * The mandatory Encrypt options.
     */
    @Nonnull
    public EncryptOptions options() {
        return options;
    }
}
