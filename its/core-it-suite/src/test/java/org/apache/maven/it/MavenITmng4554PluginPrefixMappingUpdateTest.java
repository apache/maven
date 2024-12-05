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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4554">MNG-4554</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4554PluginPrefixMappingUpdateTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4554PluginPrefixMappingUpdateTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-alpha-7,)");
    }

    /**
     * Test that the metadata holding the plugin prefix mapping is cached and not redownloaded upon each
     * Maven invocation.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCached() throws Exception {
        File testDir = extractResources("/mng-4554");

        String metadataUri = "/repo-1/org/apache/maven/its/mng4554/maven-metadata.xml";

        final List<String> requestedUris = Collections.synchronizedList(new ArrayList<>());

        AbstractHandler logHandler = new AbstractHandler() {
            @Override
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                requestedUris.add(request.getRequestURI());
            }
        };

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setResourceBase(testDir.getAbsolutePath());

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(logHandler);
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(new DefaultHandler());

        Server server = new Server(0);
        server.setHandler(handlerList);
        server.start();

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        try {
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }
            verifier.setAutoclean(false);
            verifier.deleteDirectory("target");
            try {
                verifier.deleteArtifacts("org.apache.maven.its.mng4554");
            } catch (IOException e) {
                // expected when running test on Windows using embedded Maven (JAR files locked by plugin class realm)
                assertFalse(new File(verifier.getArtifactMetadataPath(
                                "org.apache.maven.its.mng4554", null, null, "maven-metadata-mng4554.xml"))
                        .exists());
            }
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            NetworkConnector connector = (NetworkConnector) server.getConnectors()[0];
            filterProps.put("@port@", Integer.toString(connector.getLocalPort()));
            filterProps.put("@repo@", "repo-1");
            verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
            verifier.addCliArgument("-s");
            verifier.addCliArgument("settings.xml");

            verifier.setLogFileName("log-cached-1.txt");
            verifier.addCliArgument("a:touch");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            verifier.verifyFilePresent("target/touch.txt");
            assertTrue(requestedUris.contains(metadataUri), requestedUris.toString());

            requestedUris.clear();

            verifier.setLogFileName("log-cached-2.txt");
            verifier.addCliArgument("a:touch");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            assertFalse(requestedUris.contains(metadataUri), requestedUris.toString());
        } finally {
            server.stop();
            server.join();
        }
    }

    /**
     * Test that the local metadata holding the plugin prefix mapping can be forcefully updated via the command
     * line flag -U.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitForcedUpdate() throws Exception {
        File testDir = extractResources("/mng-4554");

        String metadataUri = "/repo-1/org/apache/maven/its/mng4554/maven-metadata.xml";

        final List<String> requestedUris = Collections.synchronizedList(new ArrayList<>());

        AbstractHandler logHandler = new AbstractHandler() {
            @Override
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                requestedUris.add(request.getRequestURI());
            }
        };

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setResourceBase(testDir.getAbsolutePath());

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(logHandler);
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(new DefaultHandler());

        Server server = new Server(0);
        server.setHandler(handlerList);
        server.start();

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        try {
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }
            verifier.setAutoclean(false);
            verifier.deleteDirectory("target");
            try {
                verifier.deleteArtifacts("org.apache.maven.its.mng4554");
            } catch (IOException e) {
                // expected when running test on Windows using embedded Maven (JAR files locked by plugin class realm)
                assertFalse(new File(verifier.getArtifactMetadataPath(
                                "org.apache.maven.its.mng4554", null, null, "maven-metadata-mng4554.xml"))
                        .exists());
            }
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            NetworkConnector connector = (NetworkConnector) server.getConnectors()[0];
            filterProps.put("@port@", Integer.toString(connector.getLocalPort()));
            filterProps.put("@repo@", "repo-1");
            verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
            verifier.addCliArgument("-U");
            verifier.addCliArgument("-s");
            verifier.addCliArgument("settings.xml");

            verifier.setLogFileName("log-forced-1.txt");
            verifier.addCliArgument("a:touch");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            verifier.verifyFilePresent("target/touch.txt");
            assertTrue(requestedUris.contains(metadataUri), requestedUris.toString());

            requestedUris.clear();

            verifier.setLogFileName("log-forced-2.txt");
            verifier.addCliArgument("a:touch");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            assertTrue(requestedUris.contains(metadataUri), requestedUris.toString());
        } finally {
            server.stop();
            server.join();
        }
    }

    /**
     * Test that the local metadata holding the plugin prefix mapping is automatically refetched from the remote
     * repositories if the local metadata fails to resolve a new/other plugin prefix.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRefetched() throws Exception {
        requiresMavenVersion("[3.0-alpha-3,)");

        File testDir = extractResources("/mng-4554");

        String metadataUri = "/repo-it/org/apache/maven/its/mng4554/maven-metadata.xml";

        final List<String> requestedUris = Collections.synchronizedList(new ArrayList<>());

        AbstractHandler logHandler = new AbstractHandler() {
            @Override
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                requestedUris.add(request.getRequestURI());
            }
        };

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setResourceBase(testDir.getAbsolutePath());

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(logHandler);
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(new DefaultHandler());

        Server server = new Server(0);
        server.setHandler(handlerList);
        server.start();

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        try {
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }
            verifier.setAutoclean(false);
            verifier.deleteDirectory("target");
            try {
                verifier.deleteArtifacts("org.apache.maven.its.mng4554");
            } catch (IOException e) {
                // expected when running test on Windows using embedded Maven (JAR files locked by plugin class realm)
                assertFalse(new File(verifier.getArtifactMetadataPath(
                                "org.apache.maven.its.mng4554", null, null, "maven-metadata-mng4554.xml"))
                        .exists());
            }
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            NetworkConnector connector = (NetworkConnector) server.getConnectors()[0];
            filterProps.put("@port@", Integer.toString(connector.getLocalPort()));
            filterProps.put("@repo@", "repo-it");
            verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
            verifier.addCliArgument("-s");
            verifier.addCliArgument("settings.xml");

            FileUtils.copyDirectoryStructure(new File(testDir, "repo-1"), new File(testDir, "repo-it"));

            verifier.setLogFileName("log-refetched-1.txt");
            verifier.addCliArgument("a:touch");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            verifier.verifyFilePresent("target/touch.txt");
            assertTrue(requestedUris.contains(metadataUri), requestedUris.toString());

            requestedUris.clear();

            // simulate deployment of new plugin which updates the prefix mapping in the remote repo
            FileUtils.copyDirectoryStructure(new File(testDir, "repo-2"), new File(testDir, "repo-it"));

            verifier.setLogFileName("log-refetched-2.txt");
            verifier.addCliArgument("b:touch");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            assertTrue(requestedUris.contains(metadataUri), requestedUris.toString());
        } finally {
            server.stop();
            server.join();
        }
    }
}
