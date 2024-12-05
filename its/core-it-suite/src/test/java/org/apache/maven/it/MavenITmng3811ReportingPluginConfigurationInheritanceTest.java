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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3811">MNG-3811</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng3811ReportingPluginConfigurationInheritanceTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3811ReportingPluginConfigurationInheritanceTest() {
        // TODO: fix for 3.0+
        super("[2.0.11,2.1.0-M1),[2.1.0,)");
    }

    /**
     * Verifies that reporting configuration is inherited properly.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3811() throws Exception {
        File testDir = extractResources("/mng-3811");

        Verifier verifier = newVerifier(new File(testDir, "child").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/config.properties");
        String p = "project.reporting.plugins.0.configuration.children.";

        assertEquals("2", props.getProperty(p + "stringParams.0.children"));
        assertEquals("parentParam", props.getProperty(p + "stringParams.0.children.stringParam.0.value"));
        assertEquals("childParam", props.getProperty(p + "stringParams.0.children.stringParam.1.value"));
        assertEquals("true", props.getProperty(p + "booleanParam.0.value"));
    }
}
