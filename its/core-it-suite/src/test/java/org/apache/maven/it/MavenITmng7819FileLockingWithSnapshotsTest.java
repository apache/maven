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
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MavenITmng7819FileLockingWithSnapshotsTest extends AbstractMavenIntegrationTestCase {

    private Server server;

    private int port;

    protected MavenITmng7819FileLockingWithSnapshotsTest() {
        // broken: maven 3.9.2 and 4.0.0-alpha-5
        super("[3.9.0,3.9.2),(3.9.2,3.999.999],[4.0.0-alpha-6,)");
    }

    @BeforeEach
    protected void setUp() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7819-file-locking-with-snapshots");
        server = new Server(0);
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setWelcomeFiles(new String[] {"index.html"});
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase(new File(testDir, "repo").getAbsolutePath());
        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[] {resourceHandler});
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
    void testFileLockingAndSnapshots() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7819-file-locking-with-snapshots");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true);

        // produce required precondition state: local repository must not have any of the org.apache.maven.its.mng7819
        // artifacts
        String path = verifier.getArtifactPath("org.apache.maven.its.mng7819", "dependency", "1.0.0-SNAPSHOT", "pom");
        File groupDirectory = new File(path).getParentFile().getParentFile().getParentFile();
        FileUtils.deleteDirectory(groupDirectory);

        Map<String, String> properties = new HashMap<>();
        properties.put("@port@", Integer.toString(port));
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8", properties);

        verifier.addCliArgument("-e");
        verifier.addCliArgument("-s");
        verifier.addCliArgument(new File(testDir, "settings.xml").getAbsolutePath());
        verifier.addCliArgument("-Pmaven-core-it-repo");

        verifier.addCliArgument("-Daether.syncContext.named.nameMapper=file-gav");
        verifier.addCliArgument("-Daether.syncContext.named.factory=file-lock");
        verifier.addCliArgument("package");

        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
