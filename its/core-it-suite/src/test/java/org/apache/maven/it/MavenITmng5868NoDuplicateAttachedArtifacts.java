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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5868">MNG-5868</a>.
 *
 * @author Olivier Lamy
 */
public class MavenITmng5868NoDuplicateAttachedArtifacts extends AbstractMavenIntegrationTestCase {

    private File testDir;

    private Server server;

    private int port;

    private int deployedJarArtifactNumber = 0;

    public MavenITmng5868NoDuplicateAttachedArtifacts() {
        super("[3.8.2,)");
    }

    @BeforeEach
    protected void setUp() throws Exception {
        testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-5868");

        Handler repoHandler = new AbstractHandler() {
            @Override
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                System.out.println("Handling " + request.getMethod() + " " + request.getRequestURL());

                if ("PUT".equalsIgnoreCase(request.getMethod())) {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/repo/org/apache/maven/its/mng5868/mng5868/1.0-SNAPSHOT/mng5868-1.0")
                            && uri.endsWith("-run.jar")) {
                        deployedJarArtifactNumber++;
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }

                ((Request) request).setHandled(true);
            }
        };

        server = new Server(0);

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(repoHandler);

        server.setHandler(handlerList);
        server.start();
        if (server.isFailed()) {
            fail("Couldn't bind the server socket to a free port!");
        }
        port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
        System.out.println("Bound server socket to the port " + port);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            server.join();
        }
    }

    @Test
    public void testNoDeployNotDuplicate() throws Exception {
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        Path tmp = Files.createTempFile(testDir.toPath(), "FOO", "txt");

        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng5868");
        verifier.addCliArgument("-Dartifact.attachedFile=" + tmp.toFile().getCanonicalPath());
        verifier.addCliArgument("-DdeploymentPort=" + port);
        verifier.displayStreamBuffers();
        verifier.addCliArguments("org.apache.maven.its.plugins:maven-it-plugin-artifact:2.1-SNAPSHOT:attach", "deploy");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertEquals("deployedJarArtifactNumber: " + deployedJarArtifactNumber, 1, deployedJarArtifactNumber);
    }
}
