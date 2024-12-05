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
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4555">MNG-4555</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4555MetaversionResolutionOfflineTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4555MetaversionResolutionOfflineTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-beta-1,)");
    }

    /**
     * Verify that resolution of the metaversion RELEASE respects offline mode.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4555");

        final Deque<String> uris = new ConcurrentLinkedDeque<>();

        Handler repoHandler = new AbstractHandler() {
            @Override
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                String uri = request.getRequestURI();

                if (uri.startsWith("/repo/org/apache/maven/its/mng4555")) {
                    uris.add(uri.substring(34));
                }

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                ((Request) request).setHandled(true);
            }
        };

        Server server = new Server(0);
        server.setHandler(repoHandler);
        server.start();

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4555");
        try {
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }
            int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            System.out.println("Bound server socket to the port " + port);
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            filterProps.put("@port@", Integer.toString(port));
            verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
            verifier.addCliArgument("--offline");
            verifier.addCliArgument("--settings");
            verifier.addCliArgument("settings.xml");
            verifier.addCliArgument("validate");
            verifier.execute();
        } catch (VerificationException e) {
            // expected
        } finally {
            server.stop();
            server.join();
        }

        assertTrue(uris.isEmpty(), uris.toString());
    }
}
