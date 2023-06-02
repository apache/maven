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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4776">MNG-4776</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4776ForkedReactorPluginVersionResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4776ForkedReactorPluginVersionResolutionTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-beta-3,)");
    }

    /**
     * Verify that missing plugin versions in the POM are resolved for all projects on which a forking aggregator mojo
     * will be run and not just the top-level project. This test checks the case of the mojo being invoked from a
     * lifecycle phase.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitLifecycle() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4776");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), "remote");
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("sub/target");
        verifier.setLogFileName("log-lifecycle.txt");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("sub/target/log.txt");
    }

    /**
     * Verify that missing plugin versions in the POM are resolved for all projects on which a forking aggregator mojo
     * will be run and not just the top-level project. This test checks the case of the mojo being invoked from the
     * command line
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCmdLine() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4776");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), "remote");
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("sub/target");
        verifier.setLogFileName("log-cli.txt");
        verifier.addCliArgument(
                "org.apache.maven.its.plugins:maven-it-plugin-fork:2.1-SNAPSHOT:fork-lifecycle-aggregator");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("sub/target/log.txt");
    }
}
