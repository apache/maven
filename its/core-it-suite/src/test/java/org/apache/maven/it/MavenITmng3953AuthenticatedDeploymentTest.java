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

import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3953">MNG-3953</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3953AuthenticatedDeploymentTest extends AbstractMavenIntegrationTestCase {
    private Server server;

    private int port;

    private volatile boolean deployed;

    @BeforeEach
    protected void setUp() throws Exception {
        Handler repoHandler = new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                System.out.println("Handling " + request.getMethod() + " " + request.getHttpURI());

                if ("PUT".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(200);
                    deployed = true;
                } else {
                    response.setStatus(404);
                }

                callback.succeeded();
                return true;
            }
        };

        HashLoginService userRealm = new HashLoginService("TestRealm");
        UserStore userStore = new UserStore();
        userStore.addUser("testuser", new Password("testtest"), new String[] {"deployer"});
        userRealm.setUserStore(userStore);

        server = new Server(0);
        SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
        securityHandler.setLoginService(userRealm);
        securityHandler.setAuthenticator(new BasicAuthenticator());
        securityHandler.put("/*", Constraint.from("auth", Constraint.Authorization.ANY_USER));

        securityHandler.setHandler(repoHandler);
        server.setHandler(securityHandler);
        server.start();
        if (server.isFailed()) {
            fail("Couldn't bind the server socket to a free port!");
        }
        port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
        System.out.println("Bound server socket to the port " + port);
        deployed = false;
    }

    @AfterEach
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            server.join();
        }
    }

    /**
     * Test that deployment (of a release) to a repository that requires authentification works.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRelease() throws Exception {
        testitMNG3953("release");
    }

    /**
     * Test that deployment (of a snapshot) to a repository that requires authentification works.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitSnapshot() throws Exception {
        testitMNG3953("snapshot");
    }

    private void testitMNG3953(String project) throws Exception {
        File testDir = extractResources("/mng-3953/" + project);

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("-DdeploymentPort=" + port);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTrue(deployed);
    }
}
