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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7470">MNG-7470</a>:
 * check that Maven bundled transports work as expected.
 */
public class MavenITmng7470ResolverTransportTest extends AbstractMavenIntegrationTestCase {
    private File projectDir;

    private HttpServer server;

    private int port;

    @BeforeEach
    protected void setUp() throws Exception {
        File testDir = extractResources("/mng-7470-resolver-transport");
        projectDir = new File(testDir, "project");

        server = HttpServer.builder().port(0).source(new File(testDir, "repo")).build();
        server.start();
        if (server.isFailed()) {
            fail("Couldn't bind the server socket to a free port!");
        }
        port = server.port();
        System.out.println("Bound server socket to the port " + port);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            server.join();
        }
    }

    private void performTest(/* nullable */ final String transport, final String logSnippet) throws Exception {
        Verifier verifier = newVerifier(projectDir.getAbsolutePath());

        Map<String, String> properties = new HashMap<>();
        properties.put("@port@", Integer.toString(port));
        verifier.filterFile("settings-template.xml", "settings.xml", properties);
        if (transport == null) {
            verifier.setLogFileName("default-transport.log");
        } else {
            verifier.setLogFileName(transport + "-transport.log");
        }
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.resolver.resolver-demo-maven-plugin");
        verifier.deleteArtifacts("org.apache.maven.its.resolver-transport");
        verifier.addCliArgument("-X");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(new File(projectDir, "settings.xml").getAbsolutePath());
        verifier.addCliArgument("-Pmaven-core-it-repo");
        if (transport != null) {
            verifier.addCliArgument("-Dmaven.resolver.transport=" + transport);
        }
        // Maven will fail if project dependencies cannot be resolved.
        // As dependency exists ONLY in HTTP repo, it MUST be reached using selected transport and
        // successfully resolved from it.
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        // verify maven console output contains "[DEBUG] Using transporter XXXTransporter"
        verifier.verifyTextInLog(logSnippet);
    }

    private static final String WAGON_LOG_SNIPPET = "[DEBUG] Using transporter WagonTransporter";

    private static final String APACHE_LOG_SNIPPET = "[DEBUG] Using transporter ApacheTransporter";

    private static final String JDK_LOG_SNIPPET = "[DEBUG] Using transporter JdkTransporter";

    @Test
    public void testResolverTransportDefault() throws Exception {
        performTest(null, JDK_LOG_SNIPPET);
    }

    @Test
    public void testResolverTransportWagon() throws Exception {
        performTest("wagon", WAGON_LOG_SNIPPET);
    }

    @Test
    public void testResolverTransportApache() throws Exception {
        performTest("apache", APACHE_LOG_SNIPPET);
    }

    @Test
    public void testResolverTransportJdk() throws Exception {
        performTest("jdk", JDK_LOG_SNIPPET);
    }
}
