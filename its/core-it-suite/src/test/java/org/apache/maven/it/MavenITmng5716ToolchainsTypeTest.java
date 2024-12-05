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
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5716">MNG-5716</a>.
 *
 * @author Hervé Boutemy
 */
public class MavenITmng5716ToolchainsTypeTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng5716ToolchainsTypeTest() {
        super("(3.2.3,)");
    }

    /**
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG5716() throws Exception {
        File testDir = extractResources("/mng-5716-toolchains-type");

        File javaHome = new File(testDir, "javaHome");
        javaHome.mkdirs();
        new File(javaHome, "bin").mkdirs();
        new File(javaHome, "bin/javac").createNewFile();
        new File(javaHome, "bin/javac.exe").createNewFile();

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        Map<String, String> properties = verifier.newDefaultFilterMap();
        properties.put("@javaHome@", javaHome.getAbsolutePath());

        verifier.filterFile("toolchains.xml", "toolchains.xml", properties);

        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("--toolchains");
        verifier.addCliArgument("toolchains.xml");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/toolchains.properties");
        Properties results = verifier.loadProperties("target/toolchains.properties");
        assertNull(results.getProperty("tool.1"), "javac tool should not be found for requested 'fake' toolchain");
    }
}
