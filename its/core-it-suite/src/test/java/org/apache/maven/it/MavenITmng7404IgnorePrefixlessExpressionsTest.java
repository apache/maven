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
package org.apache.maven.it;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

public class MavenITmng7404IgnorePrefixlessExpressionsTest extends AbstractMavenIntegrationTestCase {
    private static final String PROJECT_PATH = "/mng-7404-ignore-prefixless-expressions";

    public MavenITmng7404IgnorePrefixlessExpressionsTest() {
        super("[4.0.0-alpha-1,)");
    }

    @Test
    public void testIgnorePrefixlessExpressions() throws IOException, VerificationException {
        final File projectDir = extractResources(PROJECT_PATH);
        final Verifier verifier = newVerifier(projectDir.getAbsolutePath());

        verifier.addCliArgument("validate");
        verifier.execute();

        verifyLogDoesNotContainUnexpectedWarning(verifier);
    }

    private void verifyLogDoesNotContainUnexpectedWarning(Verifier verifier) throws IOException {
        List<String> loadedLines = verifier.loadLines("log.txt");
        for (String line : loadedLines) {
            if (line.startsWith("[WARNING]") && line.contains("The expression ${version} is deprecated.")) {
                fail("Log contained unexpected deprecation warning");
            }
        }
    }
}
