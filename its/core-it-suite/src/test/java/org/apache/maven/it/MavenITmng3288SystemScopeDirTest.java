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
 * This is a test for <a href="https://issues.apache.org/jira/browse/MNG-3288">MNG-3288</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3288SystemScopeDirTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test the use of a system scoped dependency to a directory instead of a JAR which should fail early.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3288() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-3288");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        assertThrows(
                VerificationException.class,
                verifier::execute,
                "Usage of directory instead of file for system-scoped dependency did not fail dependency resolution");
    }
}
