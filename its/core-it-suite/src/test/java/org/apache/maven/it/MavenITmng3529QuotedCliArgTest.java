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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3529">MNG-3529</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3529QuotedCliArgTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3529QuotedCliArgTest() {
        super("[3.0-beta-1,)");
    }

    /**
     * Verify that the command line processing doesn't choke on things like -Da=" ".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3529");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true); // NOTE: We want to go through the launcher script
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dtest.a=  ");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("-  -", props.getProperty("project.properties.propA"));
    }
}
