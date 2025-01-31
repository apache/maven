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
package org.apache.maven.cling.invoker.mvnsh;

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.mvnsh.ShellOptions;
import org.apache.maven.cling.invoker.BaseParser;

public class ShellParser extends BaseParser {
    @Override
    protected ShellOptions emptyOptions() {
        try {
            return CommonsCliShellOptions.parse(new String[0]);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected ShellInvokerRequest getInvokerRequest(LocalContext context) {
        return new ShellInvokerRequest(
                context.parserRequest,
                context.parsingFailed,
                context.cwd,
                context.installationDirectory,
                context.userHomeDirectory,
                context.userProperties,
                context.systemProperties,
                context.topDirectory,
                context.rootDirectory,
                context.parserRequest.in(),
                context.parserRequest.out(),
                context.parserRequest.err(),
                context.extensions,
                (ShellOptions) context.options);
    }

    @Override
    protected List<Options> parseCliOptions(LocalContext context) throws ParserException {
        return Collections.singletonList(parseShellCliOptions(context.parserRequest.args()));
    }

    protected CommonsCliShellOptions parseShellCliOptions(List<String> args) throws ParserException {
        try {
            return CommonsCliShellOptions.parse(args.toArray(new String[0]));
        } catch (ParseException e) {
            throw new ParserException("Failed to parse command line options: " + e.getMessage(), e);
        }
    }

    @Override
    protected Options assembleOptions(List<Options> parsedOptions) {
        // nothing to assemble, we deal with CLI only
        return parsedOptions.get(0);
    }
}
