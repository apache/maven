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
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4360">MNG-4360</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4360WebDavSupportTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4360WebDavSupportTest() {
        super("[2.1.0-M1,)");
    }

    /**
     * Verify that WebDAV works in principle. This test is not actually concerned about proper transfers but more
     * that the Jackrabbit based wagon can be properly loaded and doesn't die due to some class realm issue.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitJackrabbitBasedImpl() throws Exception {
        test("jackrabbit");
    }

    /**
     * Verify that WebDAV works in principle. This test is not actually concerned about proper transfers but more
     * that the Slide based wagon can be properly loaded and doesn't die due to some class realm issue.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitSlideBasedImpl() throws Exception {
        test("slide");
    }

    private void test(String project) throws Exception {
        File testDir = extractResources("/mng-4360");

        testDir = new File(testDir, project);

        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        Handler repoHandler = new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                System.out.println("Handling " + request.getMethod() + " " + request.getHttpURI().toString());

                PrintWriter writer = new PrintWriter(Content.Sink.asOutputStream(response));

                response.setStatus(200);

                if (Request.getPathInContext(request).endsWith(".pom")) {
                    writer.println("<project>");
                    writer.println("  <modelVersion>4.0.0</modelVersion>");
                    writer.println("  <groupId>org.apache.maven.its.mng4360</groupId>");
                    writer.println("  <artifactId>dep</artifactId>");
                    writer.println("  <version>0.1</version>");
                    writer.println("</project>");
                } else if (Request.getPathInContext(request).endsWith(".jar")) {
                    writer.println("empty");
                } else if (Request.getPathInContext(request).endsWith(".md5")
                        || Request.getPathInContext(request).endsWith(".sha1")) {
                    response.setStatus(404);
                }
                writer.flush();

                callback.succeeded();
                return true;
            }
        };

        // NOTE: The WebDAV wagons under test request "//org/apache/..." with an empty leading path segment. Jetty 9
        // served those, Jetty 12 rejects them with 400 unless the ambiguous URI is explicitly allowed.
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setUriCompliance(UriCompliance.LEGACY);

        Server server = new Server();
        server.addConnector(new ServerConnector(server, new HttpConnectionFactory(httpConfiguration)));
        server.setHandler(repoHandler);

        try {
            server.start();
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }
            int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            System.out.println("Bound server socket to the port " + port);
            verifier.setAutoclean(false);
            verifier.deleteArtifacts("org.apache.maven.its.mng4360");
            verifier.deleteDirectory("target");
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            filterProps.put("@port@", Integer.toString(port));
            verifier.filterFile("../settings-template.xml", "settings.xml", filterProps);
            verifier.addCliArgument("--settings");
            verifier.addCliArgument("settings.xml");
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
        } finally {
            server.stop();
            server.join();
        }

        List<String> cp = verifier.loadLines("target/classpath.txt");
        assertTrue(cp.contains("dep-0.1.jar"), cp.toString());
    }
}
