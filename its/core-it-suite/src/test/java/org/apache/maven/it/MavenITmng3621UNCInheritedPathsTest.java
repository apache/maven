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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3621">MNG-3621</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng3621UNCInheritedPathsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3621UNCInheritedPathsTest() {
        super("[2.0.11,2.1.0-M1),[2.1.0,)");
    }

    /**
     * Verifies that UNC paths are inherited correctly.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3621() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3621");

        Verifier verifier = newVerifier(new File(testDir, "child").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("file:////host/site/test-child", props.getProperty("project.distributionManagement.site.url"));
    }
}
