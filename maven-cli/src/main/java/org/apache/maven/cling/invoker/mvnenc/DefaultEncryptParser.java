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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.cli.mvnenc.EncryptInvokerRequest;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.api.cli.mvnenc.EncryptParser;
import org.apache.maven.cling.invoker.BaseParser;

public class DefaultEncryptParser extends BaseParser<EncryptOptions, EncryptInvokerRequest> implements EncryptParser {
    @SuppressWarnings("ParameterNumber")
    @Override
    protected EncryptInvokerRequest getInvokerRequest(
            ParserRequest parserRequest,
            Path cwd,
            Path installationDirectory,
            Path userHomeDirectory,
            Map<String, String> userProperties,
            Map<String, String> systemProperties,
            Path topDirectory,
            Path rootDirectory,
            ArrayList<CoreExtension> extensions,
            Options options) {
        return new DefaultEncryptInvokerRequest(
                parserRequest,
                cwd,
                installationDirectory,
                userHomeDirectory,
                userProperties,
                systemProperties,
                topDirectory,
                rootDirectory,
                parserRequest.in(),
                parserRequest.out(),
                parserRequest.err(),
                extensions,
                options);
    }

    @Override
    protected List<EncryptOptions> parseCliOptions(Path rootDirectory, List<String> args) throws ParserException {
        return Collections.singletonList(parseEncryptCliOptions(args));
    }

    protected CommonsCliEncryptOptions parseEncryptCliOptions(List<String> args) throws ParserException {
        try {
            return CommonsCliEncryptOptions.parse(args.toArray(new String[0]));
        } catch (ParseException e) {
            throw new ParserException("Failed to parse command line options: " + e.getMessage(), e);
        }
    }

    @Override
    protected EncryptOptions assembleOptions(List<EncryptOptions> parsedOptions) {
        // nothing to assemble, we deal with CLI only
        return parsedOptions.get(0);
    }
}
