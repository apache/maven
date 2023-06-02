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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4747">MNG-4747</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4747JavaAgentUsedByPluginTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4747JavaAgentUsedByPluginTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-beta-2,)");
    }

    /**
     * Verify that classes from JRE agents can be loaded from plugins. Agents are loaded into the system class loader
     * and hence plugins must have access to the system class loader.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        requiresJavaVersion("[1.5,)");

        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4747");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setEnvironmentVariable("MAVEN_OPTS", "-javaagent:agent.jar");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props1 = verifier.loadProperties("target/agent.properties");
        Properties props2 = verifier.loadProperties("target/plugin.properties");
        assertNotNull(props1.get("Mng4747Agent"));
        assertEquals(props1.get("Mng4747Agent"), props2.get("Mng4747Agent"));
    }
}
