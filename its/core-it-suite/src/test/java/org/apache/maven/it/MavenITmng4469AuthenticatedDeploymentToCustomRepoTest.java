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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.jetty.util.security.Constraint.__BASIC_AUTH;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4469">MNG-4469</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4469AuthenticatedDeploymentToCustomRepoTest extends AbstractMavenIntegrationTestCase {
    private Server server;

    private int port;

    private volatile boolean deployed;

    public MavenITmng4469AuthenticatedDeploymentToCustomRepoTest() {
        super("[2.0.3,3.0-alpha-3),[3.0-alpha-6,)");
    }

    @BeforeEach
    protected void setUp() throws Exception {
        Handler repoHandler = new AbstractHandler() {
            @Override
            public void handle(
                    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                System.out.println("Handling " + request.getMethod() + " " + request.getRequestURL());

                if ("PUT".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    deployed = true;
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }

                ((Request) request).setHandled(true);
            }
        };

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[] {"deployer"});
        constraint.setAuthenticate(true);

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");

        HashLoginService userRealm = new HashLoginService("TestRealm");
        UserStore userStore = new UserStore();
        userStore.addUser("testuser", new Password("testtest"), new String[] {"deployer"});
        userRealm.setUserStore(userStore);

        server = new Server(0);
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setLoginService(userRealm);
        securityHandler.setAuthMethod(__BASIC_AUTH);
        securityHandler.setConstraintMappings(new ConstraintMapping[] {constraintMapping});

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(securityHandler);
        handlerList.addHandler(repoHandler);

        server.setHandler(handlerList);
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
     * Test that deployment to a custom repository (i.e. created by a plugin) that requires authentification works.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4469");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("-Dfile=settings.xml");
        verifier.addCliArgument("-DgroupId=org.apache.maven.its.mng4469");
        verifier.addCliArgument("-DartifactId=it");
        verifier.addCliArgument("-Dversion=0.1");
        verifier.addCliArgument("-DrepositoryId=mng4469");
        verifier.addCliArgument("-DrepositoryUrl=http://localhost:" + port + "/repo");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-artifact:2.1-SNAPSHOT:deploy-file");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTrue(deployed);
    }
}
