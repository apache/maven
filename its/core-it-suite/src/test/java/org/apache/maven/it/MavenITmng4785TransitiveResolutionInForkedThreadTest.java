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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4785">MNG-4785</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4785TransitiveResolutionInForkedThreadTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4785TransitiveResolutionInForkedThreadTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-beta-4,)");
    }

    /**
     * Verify that dependency resolution using the 2.x API in forked threads works (e.g. has access to any required
     * session state).
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4785");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4785");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/artifacts.properties");
        String path = props.getProperty("org.apache.maven.its.mng4785:dep:jar:0.1-SNAPSHOT", "");
        assertTrue(path.endsWith("dep-0.1-SNAPSHOT.jar"), path);
    }
}
