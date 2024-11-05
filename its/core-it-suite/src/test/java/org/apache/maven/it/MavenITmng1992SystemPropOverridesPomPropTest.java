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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1992">MNG-1992</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng1992SystemPropOverridesPomPropTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng1992SystemPropOverridesPomPropTest() {
        super("(2.1.0-M1,)");
    }

    /**
     * Test that system/execution properties take precedence over the POM's properties section when configuring a
     * plugin parameter that is annotated with @parameter expression="prop". Note that this issue is not about POM
     * interpolation but rather plugin parameter expression evaluation.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG1992() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-1992");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.getSystemProperties().setProperty("config.stringParam", "PASSED");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties configProps = verifier.loadProperties("target/config.properties");
        assertEquals("PASSED", configProps.getProperty("stringParam"));
    }
}
