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

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.cling.invoker.BaseParser;

public class EncryptParser extends BaseParser {

    @Override
    protected EncryptOptions emptyOptions() {
        try {
            return CommonsCliEncryptOptions.parse(new String[0]);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected EncryptInvokerRequest getInvokerRequest(LocalContext context) {
        return new EncryptInvokerRequest(
                context.parserRequest,
                context.parsingFailed,
                context.cwd,
                context.installationDirectory,
                context.userHomeDirectory,
                context.userProperties,
                context.systemProperties,
                context.topDirectory,
                context.rootDirectory,
                context.extensions,
                context.ciInfo,
                (EncryptOptions) context.options);
    }

    @Override
    protected List<Options> parseCliOptions(LocalContext context) {
        return Collections.singletonList(parseEncryptCliOptions(context.parserRequest.args()));
    }

    protected CommonsCliEncryptOptions parseEncryptCliOptions(List<String> args) {
        try {
            return CommonsCliEncryptOptions.parse(args.toArray(new String[0]));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse command line options: " + e.getMessage(), e);
        }
    }

    @Override
    protected Options assembleOptions(List<Options> parsedOptions) {
        // nothing to assemble, we deal with CLI only
        return parsedOptions.getFirst();
    }
}
