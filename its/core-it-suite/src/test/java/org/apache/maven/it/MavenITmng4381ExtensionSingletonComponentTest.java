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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4381">MNG-4381</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4381ExtensionSingletonComponentTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4381ExtensionSingletonComponentTest() {
        super("[3.0-alpha-3,)");
    }

    /**
     * Test that extension plugins can contribute non-core components that can be accessed by other plugins in the same
     * project and in projects with the same extension.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4381");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("sub-a/target");
        verifier.deleteDirectory("sub-b/target");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("sub-b/target/singleton.properties");
        assertEquals("called", props.getProperty("sub-a-provider"));
        assertEquals("called", props.getProperty("sub-a-consumer"));
        assertEquals("called", props.getProperty("sub-b-provider"));
    }
}
