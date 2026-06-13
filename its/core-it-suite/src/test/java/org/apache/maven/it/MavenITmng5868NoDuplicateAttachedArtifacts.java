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
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @BeforeEach
    protected void setUp() throws Exception {
        testDir = extractResources("/mng-5868");

        Handler repoHandler = new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                System.out.println("Handling " + request.getMethod() + " " + request.getHttpURI().toString());

                if ("PUT".equalsIgnoreCase(request.getMethod())) {
                    String uri = Request.getPathInContext(request);
                    if (uri.startsWith("/repo/org/apache/maven/its/mng5868/mng5868/1.0-SNAPSHOT/mng5868-1.0")
                            && uri.endsWith("-run.jar")) {
                        deployedJarArtifactNumber++;
                    }
                    response.setStatus(200);
                } else {
                    response.setStatus(404);
                }

                callback.succeeded();
                return true;
            }
        };

        server = new Server(0);

        Handler.Sequence handlerList = new Handler.Sequence();
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
        verifier.addCliArguments("org.apache.maven.its.plugins:maven-it-plugin-artifact:2.1-SNAPSHOT:attach", "deploy");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        assertEquals(1, deployedJarArtifactNumber, "deployedJarArtifactNumber: " + deployedJarArtifactNumber);
    }
}
