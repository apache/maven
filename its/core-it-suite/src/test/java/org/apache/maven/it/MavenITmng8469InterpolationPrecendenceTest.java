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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8469">MNG-8469</a>.
 */
class MavenITmng8469InterpolationPrecendenceTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8469InterpolationPrecendenceTest() {
        super("[4.0.0-rc-3-SNAPSHOT,)");
    }

    /**
     *  Verify project is buildable.
     */
    @Test
    void testIt() throws Exception {
        Path basedir = extractResources("/mng-8469").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("help:effective-pom");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // 4.0.0-rc-2 fails as
        // [ERROR] Some problems were encountered while processing the POMs
        // [ERROR] The build could not read 1 project -> [Help 1]
        // [ERROR]
        // [ERROR]   The project org.apache.maven.its.mng8469:test:1.0 (...pom.xml) has 1 error
        // [ERROR]     recursive variable reference: scm.connection

        verifier.verifyTextInLog("<connection>foobar</connection>");
    }
}
