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
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3477">MNG-3477</a>.
 * and extends for <a href="https://issues.apache.org/jira/browse/MNG-7758">MNG-7758</a>
 * @since 4.0.0-beta-4
 *
 */
class MavenITmng3477DependencyResolutionErrorMessageTest extends AbstractMavenIntegrationTestCase {

    /**
     * Tests that dependency resolution errors tell the underlying transport issue.
     *
     * @throws Exception in case of failure
     */
    void testit(int port, String[] logExpectPatterns, String projectFile) throws Exception {
        File testDir = extractResources("/mng-3477");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), "");

        Map<String, String> filterProps = new HashMap<>();
        filterProps.put("@port@", Integer.toString(port));
        verifier.filterFile("settings-template.xml", "settings.xml", filterProps);

        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng3477");
        verifier.addCliArgument("-U");
        verifier.addCliArguments("--settings", "settings.xml");
        verifier.addCliArguments("-f", projectFile);
        verifier.addCliArgument("validate");
        verifier.setLogFileName("log-" + projectFile + "-" + port + ".txt");
        try {
            verifier.execute();
            fail("Build should have failed to resolve dependency");
        } catch (VerificationException e) {
            List<String> lines = verifier.loadLogLines();
            for (String pattern : logExpectPatterns) {
                boolean foundCause = false;
                for (String line : lines) {
                    if (line.matches(pattern)) {
                        foundCause = true;
                        break;
                    }
                }
                assertTrue(foundCause, "Transfer error cause was not found - " + pattern);
            }
        }
    }

    /**
     * Only one exception is returned by DependencyCollectionException.getResult().getExceptions()
     *
     * @throws Exception
     */
    @Test
    void connectionProblems() throws Exception {
        testit(54312, new String[] {".*org.apache.maven.its.mng3477:dep:.*:1.0.*Connection.*refused.*"}, "pom.xml");
    }

    @Test
    void connectionProblemsPlugin() throws Exception {
        testit(
                54312,
                new String[] { // JDK "Connection to..." Apache "Connect to..."
                    // with removal of connector hack https://github.com/apache/maven-resolver/pull/1676
                    // the order is not stable anymore, so repoId may be any of two
                    ".*The following artifacts could not be resolved: org.apache.maven.its.plugins:maven-it-plugin-not-exists:pom:1.2.3 \\(absent\\): "
                            + "Could not transfer artifact org.apache.maven.its.plugins:maven-it-plugin-not-exists:pom:1.2.3 from/to "
                            + "(central|maven-core-it) \\(http://localhost:.*/repo\\):.*Connect.*refused.*"
                },
                "pom-plugin.xml");
    }

    @Test
    void notFoundProblems() throws Exception {
        Server server = null;
        try {
            server = new Server(0);
            server.start();
            assertFalse(server.isFailed(), "Couldn't bind the server socket to a free port!");

            int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            testit(
                    port,
                    new String[] {
                        ".*Could not find artifact org.apache.maven.its.mng3477:dep:.*:1.0 "
                                + "in central \\(http://localhost:.*/repo\\).*",
                        ".*Could not find artifact org.apache.maven.its.mng3477:dep:.*:1.0 "
                                + "in maven-core-it \\(http://localhost:.*/repo\\).*"
                    },
                    "pom.xml");

        } finally {
            if (server != null) {
                server.stop();
                server.join();
            }
        }
    }

    @Test
    void notFoundProblemsPlugin() throws Exception {
        Server server = null;
        try {
            server = new Server(0);
            server.start();
            assertFalse(server.isFailed(), "Couldn't bind the server socket to a free port!");

            int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            testit(
                    port,
                    new String[] {
                        ".*Could not find artifact org.apache.maven.its.plugins:maven-it-plugin-not-exists:jar:1.2.3 "
                                + "in central \\(http://localhost:.*/repo\\).*",
                        ".*Could not find artifact org.apache.maven.its.plugins:maven-it-plugin-not-exists:jar:1.2.3 "
                                + "in maven-core-it \\(http://localhost:.*/repo\\).*"
                    },
                    "pom-plugin.xml");

        } finally {
            if (server != null) {
                server.stop();
                server.join();
            }
        }
    }
}
