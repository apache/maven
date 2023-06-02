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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6071">MNG-6071</a>:
 * check that getClass().getResource("/") returns consistent results when Maven is run with <code>-f ./pom.xml</code>.
 */
public class MavenITmng6071GetResourceWithCustomPom extends AbstractMavenIntegrationTestCase {
    public MavenITmng6071GetResourceWithCustomPom() {
        super("[3.8.2,)");
    }

    /**
     * check when path to POM set by <code>-f ./pom.xml</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testRunCustomPomWithDot() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-6071");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-f");
        verifier.addCliArgument("./pom.xml");
        verifier.setForkJvm(true);
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
