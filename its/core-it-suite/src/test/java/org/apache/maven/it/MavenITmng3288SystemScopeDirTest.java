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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test for <a href="https://issues.apache.org/jira/browse/MNG-3288">MNG-3288</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3288SystemScopeDirTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3288SystemScopeDirTest() {
        super("[2.0.9,)");
    }

    /**
     * Test the use of a system scoped dependency to a directory instead of a JAR which should fail early.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3288() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3288");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        try {
            verifier.addCliArgument("validate");
            verifier.execute();
            fail("Usage of directory instead of file for system-scoped dependency did not fail dependency resolution");
        } catch (VerificationException e) {
            // expected, <systemPath> of a system-scoped dependency should be a file, not a directory
        }
    }
}
