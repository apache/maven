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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.maven.it.utils.DeployedResource;
import org.codehaus.plexus.util.StringUtils;
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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4470">MNG-4470</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4470AuthenticatedDeploymentToProxyTest extends AbstractMavenIntegrationTestCase {
    private Server server;

    private int port;

    private volatile boolean deployed;

    private final Deque<DeployedResource> deployedResources = new ConcurrentLinkedDeque<>();

    @BeforeEach
    protected void setUp() throws Exception {
        Handler proxyHandler = new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                String tn = Thread.currentThread().getName();
                System.out.println(tn + " Handling (proxy) " + request.getMethod() + " " + request.getHttpURI());

                String auth = request.getHeaders().get("Proxy-Authorization");
                if (auth != null) {
                    auth = auth.substring(auth.indexOf(' ') + 1).trim();
                    auth = new String(Base64.getDecoder().decode(auth), StandardCharsets.US_ASCII);
                }
                System.out.println(tn + " Proxy-Authorization: " + auth);

                if (!"proxyuser:proxypass".equals(auth)) {
                    response.setStatus(407);
                    response.getHeaders().add("Proxy-Authenticate", "Basic realm=\"Squid proxy-caching web server\"");
                    callback.succeeded();
                    return true;
                }

                DeployedResource deployedResource = new DeployedResource();

                deployedResource.httpMethod = request.getMethod();
                deployedResource.requestUri = Request.getPathInContext(request);
                deployedResource.transferEncoding = request.getHeaders().get("Transfer-Encoding");
                deployedResource.contentLength = request.getHeaders().get("Content-Length");

                deployedResources.add(deployedResource);
                System.out.println(tn + " Done (proxy) " + request.getMethod() + " " + request.getHttpURI());
                return false;
            }
        };

        Handler repoHandler = new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                String tn = Thread.currentThread().getName();
                System.out.println(tn + " Handling (repos) " + request.getMethod() + " " + request.getHttpURI());

                if ("PUT".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(200);
                    deployed = true;
                } else {
                    response.setStatus(404);
                }

                DeployedResource deployedResource = new DeployedResource();

                deployedResource.httpMethod = request.getMethod();
                deployedResource.requestUri = Request.getPathInContext(request);
                deployedResource.transferEncoding = request.getHeaders().get("Transfer-Encoding");
                deployedResource.contentLength = request.getHeaders().get("Content-Length");

                deployedResources.add(deployedResource);
                System.out.println(tn + " Done (repos) " + request.getMethod() + " " + request.getHttpURI());

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

        Handler.Sequence handlerList = new Handler.Sequence();
        handlerList.addHandler(proxyHandler);
        handlerList.addHandler(securityHandler);

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
     * Test that deployment (of a release) to a proxy that requires authentication works.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRelease() throws Exception {
        testit("release");
    }

    /**
     * Test that deployment (of a snapshot) to a proxy that requires authentication works.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitSnapshot() throws Exception {
        testit("snapshot");
    }

    private void testit(String project) throws Exception {
        File testDir = extractResources("/mng-4470/" + project);

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.filterFile(
                "settings-template.xml",
                "settings.xml",
                "UTF-8",
                Collections.singletonMap("@port@", Integer.toString(port)));
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        for (DeployedResource deployedResource : deployedResources) {
            if (StringUtils.equalsIgnoreCase("chunked", deployedResource.transferEncoding)) {
                fail("deployedResource " + deployedResource
                        + " use chunked transfert encoding some http server doesn't support that");
            }
        }

        assertTrue(deployed);
    }
}
