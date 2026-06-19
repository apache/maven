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
import java.util.Properties;

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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4729">MNG-4729</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4729MirrorProxyAuthUsedByProjectBuilderTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test that the 2.x project builder obeys the network settings (mirror, proxy, auth) when building remote POMs
     * and discovering additional repositories.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4729");

        HashLoginService userRealm = new HashLoginService("TestRealm");
        UserStore userStore = new UserStore();
        userStore.addUser("testuser", new Password("testtest"), new String[] {"user"});
        userRealm.setUserStore(userStore);

        Server server = new Server(0);
        SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
        securityHandler.setLoginService(userRealm);
        securityHandler.setAuthenticator(new BasicAuthenticator());
        securityHandler.put("/*", Constraint.from("auth", Constraint.Authorization.ANY_USER));

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setBaseResource(ResourceFactory.of(server).newResource(testDir.toPath()));

        Handler.Sequence handlerList = new Handler.Sequence();
        handlerList.addHandler(repoHandler);
        handlerList.addHandler(new DefaultHandler());

        securityHandler.setHandler(handlerList);
        server.setHandler(securityHandler);
        server.start();

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        try {
            if (server.isFailed()) {
                fail("Couldn't bind the server socket to a free port!");
            }
            int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            System.out.println("Bound server socket to the port " + port);
            verifier.setAutoclean(false);
            verifier.deleteDirectory("target");
            verifier.deleteArtifacts("org.apache.maven.its.mng4729");
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            filterProps.put("@port@", Integer.toString(port));
            verifier.filterFile("settings-template.xml", "settings.xml", filterProps);
            verifier.addCliArgument("-s");
            verifier.addCliArgument("settings.xml");
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            Properties props = verifier.loadProperties("target/pom.properties");
            assertEquals("PASSED", props.get("org.apache.maven.its.mng4729:a:jar:0.1.project.name"));
        } finally {
            server.stop();
            server.join();
        }
    }
}
