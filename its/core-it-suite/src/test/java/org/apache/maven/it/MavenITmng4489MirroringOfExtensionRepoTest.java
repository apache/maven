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
import java.util.Map;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.Test;

import static org.eclipse.jetty.util.security.Constraint.__BASIC_AUTH;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4489">MNG-4489</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4489MirroringOfExtensionRepoTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4489MirroringOfExtensionRepoTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-alpha-6,)");
    }

    /**
     * Test that repositories contributed by extension POMs during transitive dependency resolution are subject to
     * mirror and authentication configuration.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4489");

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[] {"user"});
        constraint.setAuthenticate(true);

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");

        HashLoginService userRealm = new HashLoginService("TestRealm");
        UserStore userStore = new UserStore();
        userStore.addUser("testuser", new Password("testtest"), new String[] {"user"});
        userRealm.setUserStore(userStore);

        Server server = new Server(0);
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setLoginService(userRealm);
        securityHandler.setAuthMethod(__BASIC_AUTH);
        securityHandler.setConstraintMappings(new ConstraintMapping[] {constraintMapping});

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setResourceBase(testDir.getAbsolutePath());

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(securityHandler);
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(new DefaultHandler());

        server.setHandler(handlerList);

        try {
            server.start();
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }
            int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            System.out.println("Bound server socket to the port " + port);
            Verifier verifier = newVerifier(testDir.getAbsolutePath());
            verifier.setAutoclean(false);
            verifier.deleteDirectory("target");
            verifier.deleteArtifacts("org.apache.maven.its.mng4489");
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            filterProps.put("@port@", Integer.toString(port));
            verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8", filterProps);
            verifier.addCliArgument("-s");
            verifier.addCliArgument("settings.xml");
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            verifier.verifyArtifactPresent("org.apache.maven.its.mng4489", "ext-dep", "0.1", "jar");
            verifier.verifyArtifactPresent("org.apache.maven.its.mng4489", "ext-dep", "0.1", "pom");
        } finally {
            server.stop();
            server.join();
        }
    }
}
