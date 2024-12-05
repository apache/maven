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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3714">MNG-3714</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng3714ToolchainsCliOptionTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3714ToolchainsCliOptionTest() {
        super("[2.3.0,)");
    }

    /**
     * Test --toolchains CLI option
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3714() throws Exception {
        File testDir = extractResources("/mng-3714");

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
        String tool = results.getProperty("tool.1", "");
        if (tool.endsWith(".exe")) {
            tool = tool.substring(0, tool.length() - 4);
        }
        assertEquals(new File(javaHome, "bin/javac").getAbsolutePath(), tool);

        verifier.verifyFilePresent("target/tool.properties");
        Properties toolProps = verifier.loadProperties("target/tool.properties");
        String path = toolProps.getProperty("tool.javac", "");
        assertEquals(results.getProperty("tool.1", ""), path);
    }
}
