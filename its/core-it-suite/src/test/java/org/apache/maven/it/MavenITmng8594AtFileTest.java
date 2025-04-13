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

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8594">MNG-8594</a>.
 */
class MavenITmng8594AtFileTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8594AtFileTest() {
        super("[4.0.0-rc-3-SNAPSHOT,)");
    }

    /**
     *  Verify Maven picks up params/goals from atFile.
     */
    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/mng-8594").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("-af");
        verifier.addCliArgument("cmd.txt");
        verifier.addCliArgument("-Dcolor1=green");
        verifier.addCliArgument("-Dcolor2=blue");
        verifier.addCliArgument("clean");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // clean did run
        verifier.verifyTextInLog("(default-clean) @ root");
        // validate bound plugin did run
        verifier.verifyTextInLog("(eval) @ root");

        // validate properties
        List<String> properties = verifier.loadLines("target/pom.properties");
        assertTrue(properties.contains("session.executionProperties.color1=green")); // CLI only
        assertTrue(properties.contains("session.executionProperties.color2=blue")); // both
        assertTrue(properties.contains("session.executionProperties.color3=yellow")); // cmd.txt only
    }
}
