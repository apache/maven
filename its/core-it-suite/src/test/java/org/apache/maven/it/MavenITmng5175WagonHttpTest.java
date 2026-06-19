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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5175">MNG-5175</a>.
 * test correct integration of wagon http: read time out configuration from settings.xml
 *
 *
 * @since 3.0.4
 *
 */
public class MavenITmng5175WagonHttpTest extends AbstractMavenIntegrationTestCase {
    private Server server;

    private int port;

    @BeforeEach
    protected void setUp() throws Exception {
        Handler handler = new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                try {
                    // wait long enough for read timeout to happen in client
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new IOException(e.getMessage());
                }
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain");
                response.setStatus(200);
                PrintWriter writer = new PrintWriter(Content.Sink.asOutputStream(response));
                writer.println("some content");
                writer.println();
                writer.flush();

                callback.succeeded();
                return true;
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
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            server.join();
        }
    }

    /**
     * Test that the read time out from settings is used.
     * basically use a 1ms time out and wait a bit in the handler
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testmng5175ReadTimeOutFromSettings() throws Exception {
        File testDir = extractResources("/mng-5175");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        Map<String, String> filterProps = new HashMap<>();
        filterProps.put("@port@", Integer.toString(port));

        verifier.filterFile("settings-template.xml", "settings.xml", filterProps);

        verifier.addCliArgument("-U");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("--fail-never");
        verifier.addCliArgument("--errors");
        verifier.addCliArgument("-X");
        verifier.addCliArgument("validate");
        verifier.addCliArgument(
                "-Dmaven.resolver.transport=wagon"); // this tests Wagon integration and uses Wagon specific config
        verifier.execute();

        verifier.verifyTextInLog(
                "Could not transfer artifact org.apache.maven.its.mng5175:fake-dependency:pom:1.0-SNAPSHOT");
        verifier.verifyTextInLog("Read timed out");
    }
}
