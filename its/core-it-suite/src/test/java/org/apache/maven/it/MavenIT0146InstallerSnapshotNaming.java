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
import java.net.InetAddress;
import java.util.Map;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MavenIT0146InstallerSnapshotNaming extends AbstractMavenIntegrationTestCase {
    private Server server;

    private int port;

    private final File testDir;

    public MavenIT0146InstallerSnapshotNaming() throws IOException {
        super("(2.0.2,)");
        testDir = extractResources("/it0146");
    }

    @BeforeEach
    protected void setUp() throws Exception {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(new File(testDir, "repo").getAbsolutePath());
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {resourceHandler, new DefaultHandler()});

        server = new Server(0);
        server.setHandler(handlers);
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
    public void testitRemoteDownloadTimestampedName() throws Exception {
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        Map<String, String> properties = verifier.newDefaultFilterMap();
        properties.put("@host@", InetAddress.getLoopbackAddress().getCanonicalHostName());
        properties.put("@port@", Integer.toString(port));

        verifier.filterFile("settings-template.xml", "settings.xml", properties);

        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");

        verifier.deleteArtifacts("org.apache.maven.its.it0146");

        verifier.addCliArgument("-X");

        verifier.deleteDirectory("target");

        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/appassembler/repo/dep-0.1-20110726.105319-1.jar");
    }

    @Test
    public void testitNonTimestampedNameWithInstalledSNAPSHOT() throws Exception {
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.it0146");
        verifier.addCliArgument("-f");
        verifier.addCliArgument("project/pom.xml");
        verifier.deleteDirectory("project/target");
        verifier.setLogFileName("log2.txt");

        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(testDir.getAbsolutePath());

        Map<String, String> properties = verifier.newDefaultFilterMap();
        properties.put("@host@", InetAddress.getLoopbackAddress().getCanonicalHostName());
        properties.put("@port@", Integer.toString(port));

        verifier.filterFile("settings-template.xml", "settings.xml", properties);

        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.setLogFileName("log3.txt");

        verifier.addCliArgument("-X");

        verifier.deleteDirectory("target");

        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/appassembler/repo/dep-0.1-SNAPSHOT.jar");
    }
}
