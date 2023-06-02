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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-823">MNG-823</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng0823MojoContextPassingTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng0823MojoContextPassingTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Tests context passing between mojos in the same plugin.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG0823() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-0823");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArguments(
                "org.apache.maven.its.plugins:maven-it-plugin-context-passing:throw",
                "org.apache.maven.its.plugins:maven-it-plugin-context-passing:catch");
        verifier.execute();
        verifier.verifyFilePresent("target/thrown-value");
        verifier.verifyErrorFreeLog();
    }
}
