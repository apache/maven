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
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3535">MNG-3535</a>.
 */
public class MavenITmng3535SelfReferentialPropertiesTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testitMNG3535ShouldSucceed() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-3535/success");

        Verifier verifier = newVerifier(testDir.toString());

        verifier.addCliArgument("-X");

        verifier.setAutoclean(false);
        verifier.addCliArgument("verify");

        assertThrows(
                Exception.class,
                () -> {
                    verifier.execute();
                    verifier.verifyErrorFreeLog();
                },
                "There is a self-referential property in this build; it should fail.");
    }

    @Test
    public void testitMNG3535ShouldFail() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-3535/failure");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.addCliArgument("-X");
        verifier.addCliArgument("verify");

        assertThrows(
                Exception.class,
                () -> {
                    verifier.execute();
                    verifier.verifyErrorFreeLog();
                },
                "There is a self-referential property in this build; it should fail.");
    }
}
