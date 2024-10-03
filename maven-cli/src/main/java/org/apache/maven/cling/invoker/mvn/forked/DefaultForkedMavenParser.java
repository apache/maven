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
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.cli.mvn.forked.ForkedMavenInvokerRequest;
import org.apache.maven.api.cli.mvn.forked.ForkedMavenParser;
import org.apache.maven.cling.invoker.mvn.CommonsCliMavenOptions;
import org.apache.maven.cling.invoker.mvn.DefaultMavenParser;
import org.apache.maven.cling.invoker.mvn.LayeredMavenOptions;

public class DefaultForkedMavenParser extends DefaultMavenParser<MavenOptions, ForkedMavenInvokerRequest>
        implements ForkedMavenParser {

    @SuppressWarnings("ParameterNumber")
    @Override
    protected ForkedMavenInvokerRequest getInvokerRequest(
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
        return new DefaultForkedMavenInvokerRequest(
                parserRequest.command(),
                cwd,
                installationDirectory,
                userHomeDirectory,
                userProperties,
                systemProperties,
                parserRequest.logger(),
                parserRequest.messageBuilderFactory(),
                topDirectory,
                rootDirectory,
                parserRequest.in(),
                parserRequest.out(),
                parserRequest.err(),
                extensions,
                getJvmArguments(rootDirectory),
                (MavenOptions) options);
    }

    protected List<String> getJvmArguments(Path rootDirectory) {
        if (rootDirectory != null) {
            // TODO: do this
            return Collections.emptyList();
        }
        return null;
    }

    @Override
    protected MavenOptions parseArgs(String source, String[] args) throws ParserException {
        try {
            return CommonsCliMavenOptions.parse(source, args);
        } catch (ParseException e) {
            throw new ParserException("Failed to parse source " + source, e.getCause());
        }
    }

    @Override
    protected MavenOptions assembleOptions(List<MavenOptions> parsedOptions) {
        return LayeredMavenOptions.layerMavenOptions(parsedOptions);
    }
}
