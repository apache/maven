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

import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4068">MNG-4068</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4068AuthenticatedMirrorTest extends AbstractMavenIntegrationTestCase {

    private File testDir;

    private Server server;

    private int port;

    @BeforeEach
    protected void setUp() throws Exception {
        testDir = extractResources("/mng-4068");

        HashLoginService userRealm = new HashLoginService("TestRealm");
        UserStore userStore = new UserStore();
        userStore.addUser("testuser", new Password("testtest"), new String[] {"user"});
        userRealm.setUserStore(userStore);

        server = new Server(0);
        SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
        securityHandler.setLoginService(userRealm);
        securityHandler.setAuthenticator(new BasicAuthenticator());
        securityHandler.put("/*", Constraint.from("auth", Constraint.Authorization.ANY_USER));

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setBaseResource(ResourceFactory.of(server).newResource(new File(testDir, "repo").toPath()));

        Handler.Sequence handlerList = new Handler.Sequence();
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(new DefaultHandler());

        securityHandler.setHandler(handlerList);
        server.setHandler(securityHandler);
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
     * Test that downloading of release/snapshot artifacts from an authenticated mirror works. This basically boils down
     * to using the proper id for the mirrored repository when looking up the credentials.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        Map<String, String> filterProps = new HashMap<>();
        filterProps.put("@mirrorPort@", Integer.toString(port));

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng4068");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.mng4068", "a", "0.1", "jar");
        verifier.verifyArtifactPresent("org.apache.maven.its.mng4068", "b", "0.1-SNAPSHOT", "jar");
    }
}
