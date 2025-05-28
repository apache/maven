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
package org.apache.maven.cling.invoker.mvnup;

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.BaseParser;

public class UpgradeParser extends BaseParser {

    @Override
    protected UpgradeOptions emptyOptions() {
        try {
            return CommonsCliUpgradeOptions.parse(new String[0]);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected UpgradeInvokerRequest getInvokerRequest(LocalContext context) {
        return new UpgradeInvokerRequest(
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
                (UpgradeOptions) context.options);
    }

    @Override
    protected List<Options> parseCliOptions(LocalContext context) {
        return Collections.singletonList(parseUpgradeCliOptions(context.parserRequest.args()));
    }

    protected CommonsCliUpgradeOptions parseUpgradeCliOptions(List<String> args) {
        try {
            return CommonsCliUpgradeOptions.parse(args.toArray(new String[0]));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse command line options: " + e.getMessage(), e);
        }
    }

    @Override
    protected Options assembleOptions(List<Options> parsedOptions) {
        // nothing to assemble, we deal with CLI only
        return parsedOptions.get(0);
    }
}
