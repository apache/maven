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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-768">MNG-768</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng0768OfflineModeTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng0768OfflineModeTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test offline mode.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG768() throws Exception {
        File testDir = extractResources("/mng-0768");

        final List<String> requestedUris = Collections.synchronizedList(new ArrayList<>());

        Handler repoHandler = new AbstractHandler() {
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                System.out.println("Handling " + request.getMethod() + " " + request.getRequestURL());

                requestedUris.add(request.getRequestURI());

                PrintWriter writer = response.getWriter();

                response.setStatus(HttpServletResponse.SC_OK);

                if (request.getRequestURI().endsWith(".pom")) {
                    writer.println("<project>");
                    writer.println("  <modelVersion>4.0.0</modelVersion>");
                    writer.println("  <groupId>org.apache.maven.its.mng0768</groupId>");
                    writer.println("  <artifactId>dep</artifactId>");
                    writer.println("  <version>0.1</version>");
                    writer.println("</project>");
                } else if (request.getRequestURI().endsWith(".jar")) {
                    writer.println("empty");
                } else if (request.getRequestURI().endsWith(".md5")
                        || request.getRequestURI().endsWith(".sha1")) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }

                ((Request) request).setHandled(true);
            }
        };

        Server server = new Server(0);
        server.setHandler(repoHandler);

        try {
            server.start();
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }

            int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();

            {
                // phase 1: run build in online mode to fill local repo
                Verifier verifier = newVerifier(testDir.getAbsolutePath());
                verifier.setAutoclean(false);
                verifier.deleteDirectory("target");
                verifier.deleteArtifacts("org.apache.maven.its.mng0768");
                verifier.setLogFileName("log1.txt");
                Map<String, String> props = Map.of("@port@", Integer.toString(port));
                verifier.filterFile("settings-template.xml", "settings.xml", props);
                verifier.addCliArgument("--settings");
                verifier.addCliArgument("settings.xml");
                verifier.addCliArgument(
                        "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile");
                verifier.execute();
                verifier.verifyFilePresent("target/compile.txt");
                verifier.verifyErrorFreeLog();
            }

            requestedUris.clear();

            {
                // phase 2: run build in offline mode to check it still passes, without network accesses
                Verifier verifier = newVerifier(testDir.getAbsolutePath());
                verifier.setAutoclean(false);
                verifier.deleteDirectory("target");
                verifier.addCliArgument("-o");
                verifier.addCliArgument("--settings");
                verifier.addCliArgument("settings.xml");
                verifier.setLogFileName("log2.txt");
                verifier.addCliArgument(
                        "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile");
                verifier.execute();
                verifier.verifyFilePresent("target/compile.txt");
                verifier.verifyErrorFreeLog();
            }

            assertTrue(requestedUris.isEmpty(), requestedUris.toString());

            {
                // phase 3: delete test artifact and run build in offline mode to check it fails now
                // NOTE: Adding the settings again to offer Maven the bad choice of using the remote repo
                Verifier verifier = newVerifier(testDir.getAbsolutePath());
                verifier.setAutoclean(false);
                verifier.deleteDirectory("target");
                verifier.deleteArtifacts("org.apache.maven.its.mng0768");
                verifier.addCliArgument("-o");
                verifier.addCliArgument("--settings");
                verifier.addCliArgument("settings.xml");
                verifier.setLogFileName("log3.txt");
                try {
                    verifier.addCliArgument(
                            "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile");
                    verifier.execute();
                    verifier.verifyErrorFreeLog();
                    fail("Build did not fail to resolve missing dependency although Maven ought to work offline!");
                } catch (VerificationException e) {
                    // expected, should fail
                }
            }

            System.out.println("Bound server socket to the port " + port);

            assertTrue(requestedUris.isEmpty(), requestedUris.toString());
        } finally {
            server.stop();
            server.join();
        }
    }
}
