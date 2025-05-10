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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3652">MNG-3652</a>.
 */
@SuppressWarnings("checkstyle:UnusedLocalVariable")
class MavenITmng3652UserAgentHeaderTest extends AbstractMavenIntegrationTestCase {
    private Server server;

    private int port;

    private String userAgent;

    private String customHeader;

    MavenITmng3652UserAgentHeaderTest() {
        super("[3.0-beta-3,)");
    }

    @BeforeEach
    protected void setUp() {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                System.out.println("Handling URL: '" + request.getRequestURL() + "'");

                userAgent = request.getHeader("User-Agent");

                customHeader = request.getHeader("Custom-Header");

                System.out.println("Got User-Agent: '" + userAgent + "'");

                response.setContentType("text/plain");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("some content");
                response.getWriter().println();

                ((Request) request).setHandled(true);
            }
        };

        server = new Server(0);
        server.setHandler(handler);
        server.start();
        if (server.isFailed()) {
            fail("Couldn't bind the server socket to a free port!");
        }
        port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
        System.out.println("Bound server socket to the port " + port);
    }

    @AfterEach
    protected void tearDown() {
        if (server != null) {
            server.stop();
            server.join();
        }
    }

    /**
     * Test that the user agent header is configured in the wagon manager.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testmng3652UnConfiguredHttp() throws Exception {
        File testDir = extractResources("/mng-3652");
        File pluginDir = new File(testDir, "test-plugin");
        File projectDir = new File(testDir, "test-project");

        Verifier verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());

        verifier.addCliArgument("-DtestPort=" + port);
        verifier.addCliArgument("-X");

        verifier.setLogFileName("log-unConfiguredHttp.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String userAgent = this.userAgent;
        assertNotNull(userAgent);

        File touchFile = new File(projectDir, "target/touch.txt");
        assertTrue(touchFile.exists());

        List<String> lines = verifier.loadFile(touchFile, false);

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = lines.get(0);
        String javaVersion = lines.get(1);
        String os = lines.get(2) + " " + lines.get(3);
        String artifactVersion = lines.get(4);

        assertEquals(
                "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")",
                userAgent,
                "Comparing User-Agent '" + userAgent + "'");
    }

    @Test
    public void testmng3652UnConfiguredDAV() throws Exception {
        File testDir = extractResources("/mng-3652");
        File pluginDir = new File(testDir, "test-plugin");
        File projectDir = new File(testDir, "test-project");

        Verifier verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());

        // test webdav
        verifier.addCliArgument("-DtestPort=" + port);
        verifier.addCliArgument("-DtestProtocol=dav:http");
        verifier.addCliArgument("-X");

        verifier.setLogFileName("log-unConfiguredDAV.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File touchFile = new File(projectDir, "target/touch.txt");
        assertTrue(touchFile.exists());

        List<String> lines = verifier.loadFile(touchFile, false);

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = lines.get(0);
        String javaVersion = lines.get(1);
        String os = lines.get(2) + " " + lines.get(3);
        String artifactVersion = lines.get(4);

        String userAgent = this.userAgent;
        assertNotNull(userAgent);

        assertEquals(
                "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")",
                userAgent,
                "Comparing User-Agent '" + userAgent + "'");
    }

    @Test
    public void testmng3652ConfigurationInSettingsWithoutUserAgent() throws Exception {
        File testDir = extractResources("/mng-3652");
        File pluginDir = new File(testDir, "test-plugin");
        File projectDir = new File(testDir, "test-project");

        Verifier verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());

        // test settings with no config

        verifier.addCliArgument("-DtestPort=" + port);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings-no-config.xml");
        verifier.addCliArgument("-X");

        verifier.setLogFileName("log-configWithoutUserAgent.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File touchFile = new File(projectDir, "target/touch.txt");
        assertTrue(touchFile.exists());

        List<String> lines = verifier.loadFile(touchFile, false);

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = lines.get(0);
        String javaVersion = lines.get(1);
        String os = lines.get(2) + " " + lines.get(3);
        String artifactVersion = lines.get(4);

        String userAgent = this.userAgent;
        assertNotNull(userAgent);

        assertEquals(
                "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")",
                userAgent,
                "Comparing User-Agent '" + userAgent + "'");
    }

    @Test
    public void testmng3652UserAgentConfiguredInSettings() throws Exception {
        File testDir = extractResources("/mng-3652");
        File pluginDir = new File(testDir, "test-plugin");
        File projectDir = new File(testDir, "test-project");

        Verifier verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());

        // test settings with config

        verifier.addCliArgument("-DtestPort=" + port);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("-X");

        verifier.setLogFileName("log-configWithUserAgent.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String userAgent = this.userAgent;
        assertNotNull(userAgent);

        assertEquals("Maven Fu", userAgent);
        assertEquals("My wonderful header", customHeader);
    }

    @Test
    public void testmng3652AdditionnalHttpHeaderConfiguredInSettings() throws Exception {
        File testDir = extractResources("/mng-3652");
        File pluginDir = new File(testDir, "test-plugin");
        File projectDir = new File(testDir, "test-project");

        Verifier verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());

        // test settings with config

        verifier.addCliArgument("-DtestPort=" + port);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("-X");

        verifier.setLogFileName("log-configWithUserAgent.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String userAgent = this.userAgent;
        assertNotNull(userAgent);

        assertEquals("Maven Fu", userAgent);
        assertEquals("My wonderful header", customHeader);
    }
}
