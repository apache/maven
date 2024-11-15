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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

public class MavenITmng7255InferredGroupIdTest extends AbstractMavenIntegrationTestCase {
    private static final String PROJECT_PATH = "/mng-7255-inferred-groupid";

    public MavenITmng7255InferredGroupIdTest() {
        super("[4.0.0-alpha-5,)");
    }

    @Test
    public void testInferredGroupId() throws IOException, VerificationException {
        final File projectDir = ResourceExtractor.simpleExtractResources(getClass(), PROJECT_PATH);
        final Verifier verifier = newVerifier(projectDir.getAbsolutePath());

        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
