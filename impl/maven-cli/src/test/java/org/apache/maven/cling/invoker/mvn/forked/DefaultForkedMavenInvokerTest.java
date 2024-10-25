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
import java.util.Arrays;

import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.Parser;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.cli.mvn.forked.ForkedMavenInvokerRequest;
import org.apache.maven.cling.invoker.mvn.MavenInvokerTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Forked UT: it cannot use jimFS as it runs in child process.
 */
public class DefaultForkedMavenInvokerTest extends MavenInvokerTestSupport<MavenOptions, ForkedMavenInvokerRequest> {

    @Override
    protected Invoker<ForkedMavenInvokerRequest> createInvoker() {
        return new DefaultForkedMavenInvoker();
    }

    @Override
    protected Parser<ForkedMavenInvokerRequest> createParser() {
        return new DefaultForkedMavenParser();
    }

    @Test
    void defaultFs(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
        invoke(tempDir, Arrays.asList("clean", "verify"));
    }
}
